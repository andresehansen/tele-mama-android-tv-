/*
 * AndyTV - Motor inteligente de fuentes (source-health)
 *
 * Al abrir la app:
 *  1. Aplica de inmediato el último orden guardado (localStorage).
 *  2. En segundo plano (baja concurrencia, sin bloquear la UI) prueba todas las fuentes.
 *  3. Reordena las fuentes de cada canal: viva primero, luego mayor definición/bitrate/audio.
 *  4. Guarda el resultado y un registro local de fallos (AndySourceHealth.getFailLog()).
 *
 * Reglas:
 *  - Canales con "pinSources": true conservan su orden (solo se registra su salud).
 *  - Nunca reordena el canal que se está reproduciendo.
 *  - Fuentes que no se pueden sondear desde el navegador (headers propios, embeds) quedan
 *    como "unknown": ni se promueven ni se castigan.
 *  - Las fuentes comunitarias (community: true) tienen una penalización para no desplazar
 *    a una fuente oficial que funciona bien.
 */
(function (root, factory) {
  const api = factory(root);
  if (typeof module !== "undefined" && module.exports) module.exports = api;
  else root.AndySourceHealth = api;
})(typeof window !== "undefined" ? window : globalThis, function (root) {
  "use strict";

  const ORDER_KEY = "andytv_source_order_v1";
  const FAIL_KEY = "andytv_fail_log_v1";
  const FAIL_MAX = 200;
  const FAIL_DEDUPE_MS = 6 * 3600 * 1000;
  const PROBE_TIMEOUT_MS = 6000;
  const CONCURRENCY = 3;
  const COMMUNITY_PENALTY = 150;
  const YT_ASSUMED_HEIGHT = 900;

  let cfg = { getChannels: () => [], proxyUrl: "", isBusy: () => false, onUpdate: null, storage: null, fetchFn: null };
  let policy = null;
  let scanning = false;
  let lastReport = [];

  /* ---------------- utilidades ---------------- */
  function store() {
    if (cfg.storage) return cfg.storage;
    try { return root.localStorage; } catch (e) { return null; }
  }
  function readJson(key, fallback) {
    try { const s = store(); const raw = s && s.getItem(key); return raw ? JSON.parse(raw) : fallback; } catch (e) { return fallback; }
  }
  function writeJson(key, value) {
    try { const s = store(); if (s) s.setItem(key, JSON.stringify(value)); } catch (e) {}
  }
  function doFetch(url, opts) { return (cfg.fetchFn || root.fetch.bind(root))(url, opts); }
  function proxied(url) {
    if (!cfg.proxyUrl) return url;
    const base = cfg.proxyUrl.endsWith("/") ? cfg.proxyUrl.slice(0, -1) : cfg.proxyUrl;
    return base + "/?url=" + encodeURIComponent(url);
  }
  async function fetchText(url, ms) {
    const ctrl = new AbortController();
    const timer = setTimeout(() => ctrl.abort(), ms || PROBE_TIMEOUT_MS);
    try {
      const res = await doFetch(url, { cache: "no-store", signal: ctrl.signal });
      const text = await res.text();
      return { ok: res.ok, status: res.status, text };
    } finally { clearTimeout(timer); }
  }

  function sourceKey(src) {
    const base = [src.type, src.url || src.ytVideoId || src.ytChannelId || src.streamId || src.label || ""].join("|");
    return src.useProxy ? base + "|proxy" : base;
  }

  /* ---------------- parseo de HLS ---------------- */
  function parseHls(text) {
    if (!text || text.indexOf("#EXTM3U") === -1) return { valid: false };
    if (text.indexOf("#EXT-X-ENDLIST") !== -1 && text.indexOf("#EXT-X-STREAM-INF") === -1) {
      return { valid: false, reason: "vod-ended" };
    }
    let height = 0, bandwidth = 0, audio = false, isMaster = false;
    const lines = text.split(/\r?\n/);
    for (const line of lines) {
      if (line.startsWith("#EXT-X-STREAM-INF")) {
        isMaster = true;
        const r = /RESOLUTION=(\d+)x(\d+)/i.exec(line);
        const b = /(?:^|[:,])BANDWIDTH=(\d+)/i.exec(line);
        if (r) height = Math.max(height, parseInt(r[2], 10));
        if (b) bandwidth = Math.max(bandwidth, parseInt(b[1], 10));
        if (/mp4a|ac-3|ec-3/i.test(line)) audio = true;
      } else if (/#EXT-X-MEDIA:.*TYPE=AUDIO/i.test(line)) {
        audio = true;
      }
    }
    if (!isMaster && text.indexOf("#EXTINF") === -1) return { valid: false, reason: "empty-playlist" };
    return { valid: true, isMaster, height, bandwidth, audio };
  }

  /* ---------------- sondeo por tipo de fuente ---------------- */
  async function probeHls(src) {
    const url = src.url;
    if (!url) return { state: "dead", reason: "sin-url" };
    if (src.headers && Object.keys(src.headers).length) return { state: "unknown", reason: "requiere-headers" };
    const isHttpsPage = root.location && root.location.protocol === "https:";
    const mixed = isHttpsPage && url.startsWith("http:");
    const attempts = mixed ? [proxied(url)] : (src.useProxy ? [proxied(url), url] : [url, proxied(url)]);
    const seen = new Set();
    let lastReason = "sin-respuesta";
    for (const target of attempts) {
      if (seen.has(target) || (!cfg.proxyUrl && target !== url)) continue;
      seen.add(target);
      const t0 = Date.now();
      try {
        const res = await fetchText(target);
        if (!res.ok) { lastReason = "http-" + res.status; continue; }
        const info = parseHls(res.text);
        if (!info.valid) { lastReason = info.reason || "manifest-invalido"; continue; }
        return { state: "alive", latencyMs: Date.now() - t0, height: info.height, bandwidth: info.bandwidth, audio: info.audio };
      } catch (e) { lastReason = e && e.name === "AbortError" ? "timeout" : "red/cors"; }
    }
    return { state: "dead", reason: lastReason };
  }

  async function probeYouTube(src) {
    const id = src.ytVideoId;
    if (!id) return { state: "unknown", reason: "sin-videoId" };
    const api = "https://www.youtube.com/oembed?format=json&url=" +
      encodeURIComponent("https://www.youtube.com/watch?v=" + id);
    const targets = cfg.proxyUrl ? [api, proxied(api)] : [api];
    for (const t of targets) {
      try {
        const res = await fetchText(t, 5000);
        if (res.status === 200) return { state: "alive", height: YT_ASSUMED_HEIGHT, audio: true };
        if (res.status === 401 || res.status === 403) return { state: "dead", reason: "embed-deshabilitado" };
        if (res.status === 404) return { state: "dead", reason: "video-no-existe" };
      } catch (e) { /* probar siguiente ruta */ }
    }
    return { state: "unknown", reason: "oembed-inaccesible" };
  }

  async function probeTelefeToken(channel, src) {
    const api = channel.tokenApiUrl;
    const streamId = src.streamId || channel.streamId;
    if (!api || !streamId) return { state: "unknown", reason: "sin-config-token" };
    try {
      const tok = await fetchText(api, 6000);
      if (!tok.ok) return { state: "dead", reason: "token-http-" + tok.status };
      const data = JSON.parse(tok.text);
      if (!data.access_token) return { state: "dead", reason: "sin-token" };
      const m3u8 = "https://mdstrm.com/live-stream-playlist/" + streamId + ".m3u8?access_token=" + data.access_token;
      return await probeHls({ url: m3u8, useProxy: true });
    } catch (e) { return { state: "unknown", reason: "token-inaccesible" }; }
  }

  async function probeSource(channel, src) {
    try {
      switch (src.type) {
        case "hls": return await probeHls(src);
        case "youtube-live":
        case "youtube-video": return await probeYouTube(src);
        case "telefe-token": return await probeTelefeToken(channel, src);
        default: return { state: "unknown", reason: "tipo-no-sondeable" };
      }
    } catch (e) { return { state: "unknown", reason: "error-interno" }; }
  }

  /* ---------------- puntaje y orden ---------------- */
  function scoreSource(src, probe) {
    let score;
    if (!probe || probe.state === "unknown") score = 500;
    else if (probe.state === "dead") score = 0;
    else {
      // Definición (alto) + audio. El bitrate se informa pero no desempata: una señal
      // internacional con más bitrate no debe desplazar a la señal local de igual resolución.
      const h = probe.height || 480;
      score = 1000 + h + (probe.audio ? 30 : 0);
    }
    if (src.community && score > 0) score -= COMMUNITY_PENALTY;
    return score;
  }

  function rankSources(sources, probes) {
    return sources
      .map((s, i) => ({ s, i, score: scoreSource(s, probes[sourceKey(s)]) }))
      .sort((a, b) => (b.score - a.score) || (a.i - b.i))
      .map(x => x.s);
  }

  function applyOrder(channel, orderKeys) {
    if (!channel || !Array.isArray(channel.sources) || channel.sources.length < 2) return false;
    if (channel.pinSources || cfg.isBusy(channel)) return false;
    const pos = new Map(orderKeys.map((k, i) => [k, i]));
    const sorted = channel.sources
      .map((s, i) => ({ s, i, p: pos.has(sourceKey(s)) ? pos.get(sourceKey(s)) : 1e6 + i }))
      .sort((a, b) => a.p - b.p)
      .map(x => x.s);
    const changed = sorted.some((s, i) => s !== channel.sources[i]);
    if (changed) { channel.sources.splice(0, channel.sources.length, ...sorted); }
    channel.currentSourceIndex = 0;
    return changed;
  }

  /* ---------------- registro de fallos ---------------- */
  function logFailure(channel, src, reason) {
    const log = readJson(FAIL_KEY, []);
    const key = (channel && channel.id) + "|" + sourceKey(src || {});
    const now = Date.now();
    const dup = log.slice().reverse().find(e => e.key === key && now - e.t < FAIL_DEDUPE_MS);
    if (dup) return;
    log.push({
      t: now, key,
      channelId: channel && channel.id,
      source: (src && (src.label || src.url || src.ytVideoId)) || "?",
      reason: reason || "desconocido"
    });
    while (log.length > FAIL_MAX) log.shift();
    writeJson(FAIL_KEY, log);
  }
  function getFailLog() { return readJson(FAIL_KEY, []); }
  function clearFailLog() { writeJson(FAIL_KEY, []); }

  /* ---------------- escaneo ---------------- */
  async function runPool(tasks, limit) {
    let next = 0;
    const workers = Array.from({ length: Math.min(limit, tasks.length) }, async () => {
      while (next < tasks.length) { const i = next++; await tasks[i](); }
    });
    await Promise.all(workers);
  }

  async function scan() {
    if (scanning) return lastReport;
    scanning = true;
    const channels = cfg.getChannels() || [];
    const probesByChannel = {};
    const tasks = [];
    channels.forEach(ch => {
      probesByChannel[ch.id] = {};
      (ch.sources || []).forEach(src => {
        tasks.push(async () => {
          const p = await probeSource(ch, src);
          probesByChannel[ch.id][sourceKey(src)] = p;
          if (p.state === "dead") logFailure(ch, src, "scan:" + (p.reason || "dead"));
        });
      });
    });
    try {
      await runPool(tasks, CONCURRENCY);
      const saved = readJson(ORDER_KEY, { channels: {} });
      saved.channels = saved.channels || {};
      const report = [];
      let anyChange = false;
      (cfg.getChannels() || []).forEach(ch => {
        const probes = probesByChannel[ch.id];
        if (!probes || !ch.sources) return;
        const ranked = rankSources(ch.sources, probes);
        saved.channels[ch.id] = ranked.map(sourceKey);
        if (applyOrder(ch, saved.channels[ch.id])) anyChange = true;
        ch.sources.forEach(s => {
          const p = probes[sourceKey(s)] || {};
          report.push({
            canal: ch.name, fuente: s.label || s.url || s.ytVideoId, estado: p.state,
            alto: p.height || "", kbps: p.bandwidth ? Math.round(p.bandwidth / 1000) : "",
            audio: p.audio === undefined ? "" : (p.audio ? "si" : "no"),
            motivo: p.reason || "", fijado: ch.pinSources ? "si" : ""
          });
        });
      });
      saved.ts = Date.now();
      writeJson(ORDER_KEY, saved);
      lastReport = report;
      if (anyChange && typeof cfg.onUpdate === "function") cfg.onUpdate();
      return report;
    } finally { scanning = false; }
  }

  /* ---------------- política comunitaria ---------------- */
  function hostMatches(host, allowed) {
    return allowed.some(a => host === a || host.endsWith("." + a));
  }
  function isBareIp(host) { return /^\d{1,3}(\.\d{1,3}){3}$/.test(host) || host.indexOf(":") !== -1; }
  function isAllowedCommunitySource(url, headers, channel) {
    if (!policy) return false; // sin política cargada: cerrado por defecto
    if (channel && policy.paidChannelIds && policy.paidChannelIds.indexOf(channel.id) !== -1) return false;
    let u;
    try { u = new URL(url); } catch (e) { return false; }
    if (policy.httpsOnly && u.protocol !== "https:") return false;
    if (policy.blockBareIp && isBareIp(u.hostname)) return false;
    if (policy.blockCustomHeaders && headers && Object.keys(headers).length) return false;
    return hostMatches(u.hostname, policy.allowedHosts || []);
  }
  async function loadPolicy(url) {
    try {
      const res = await doFetch(url || "source-policy.json?t=" + Math.floor(Date.now() / 60000), { cache: "no-store" });
      if (res.ok) policy = await res.json();
    } catch (e) { /* queda cerrado */ }
    return policy;
  }
  function setPolicy(p) { policy = p; }

  /* ---------------- API ---------------- */
  function applySavedOrder() {
    const saved = readJson(ORDER_KEY, null);
    if (!saved || !saved.channels) return 0;
    let n = 0;
    (cfg.getChannels() || []).forEach(ch => {
      if (saved.channels[ch.id] && applyOrder(ch, saved.channels[ch.id])) n++;
    });
    return n;
  }

  function init(options) {
    cfg = Object.assign(cfg, options || {});
    applySavedOrder();
  }

  /** Inicia el escaneo en segundo plano cuando el dispositivo está tranquilo. */
  function startBackground(delayMs) {
    const run = () => scan().catch(e => console.warn("[source-health] escaneo fallido:", e));
    setTimeout(() => {
      if (typeof root.requestIdleCallback === "function") root.requestIdleCallback(run, { timeout: 4000 });
      else run();
    }, delayMs == null ? 2500 : delayMs);
  }

  return {
    init, scan, startBackground, applySavedOrder, loadPolicy, setPolicy,
    isAllowedCommunitySource, logFailure, getFailLog, clearFailLog,
    report: () => lastReport,
    _internals: { parseHls, scoreSource, rankSources, sourceKey, applyOrder, probeSource, isAllowedCommunitySource }
  };
});

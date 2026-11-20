const test = require("node:test");
const assert = require("node:assert");
const fs = require("fs");
const path = require("path");
const engine = require("../source-health.js");
const { parseHls, scoreSource, rankSources, sourceKey, applyOrder } = engine._internals;

const policy = JSON.parse(fs.readFileSync(path.join(__dirname, "..", "source-policy.json"), "utf8"));
const mem = () => { const m = {}; return { getItem: k => (k in m ? m[k] : null), setItem: (k, v) => { m[k] = v; } }; };

test("parseHls: master con resolución, bitrate y audio", () => {
  const r = parseHls('#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=2000000,RESOLUTION=1280x720,CODECS="avc1,mp4a.40.2"\na.m3u8\n#EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080,CODECS="avc1,mp4a.40.2"\nb.m3u8');
  assert.deepStrictEqual([r.valid, r.height, r.bandwidth, r.audio], [true, 1080, 5000000, true]);
});

test("parseHls: rechaza VOD terminado y texto inválido", () => {
  assert.strictEqual(parseHls("#EXTM3U\n#EXTINF:5,\nx.ts\n#EXT-X-ENDLIST").valid, false);
  assert.strictEqual(parseHls("<html>").valid, false);
});

test("orden: viva por definición primero, muerta al final, desempate estable", () => {
  const a = { type: "hls", url: "a" }, b = { type: "hls", url: "b" }, c = { type: "hls", url: "c" }, d = { type: "embed", url: "d" };
  const probes = {
    [sourceKey(a)]: { state: "alive", height: 720, audio: true },
    [sourceKey(b)]: { state: "dead" },
    [sourceKey(c)]: { state: "alive", height: 1080, audio: true },
    [sourceKey(d)]: { state: "unknown" }
  };
  assert.deepStrictEqual(rankSources([a, b, c, d], probes).map(s => s.url), ["c", "a", "d", "b"]);
});

test("el bitrate no desplaza a la señal local de igual resolución", () => {
  const local = { type: "hls", url: "local" }, intl = { type: "hls", url: "intl" };
  const probes = {
    [sourceKey(local)]: { state: "alive", height: 1080, bandwidth: 4e6, audio: true },
    [sourceKey(intl)]: { state: "alive", height: 1080, bandwidth: 9e6, audio: true }
  };
  assert.strictEqual(rankSources([local, intl], probes)[0].url, "local");
});

test("fuente comunitaria viva no desplaza a una oficial equivalente", () => {
  const off = { type: "hls", url: "o" }, com = { type: "hls", url: "c", community: true };
  const probes = {
    [sourceKey(off)]: { state: "alive", height: 1080, audio: true },
    [sourceKey(com)]: { state: "alive", height: 1080, audio: true }
  };
  assert.strictEqual(rankSources([com, off], probes)[0].url, "o");
});

test("canal fijado u ocupado no se reordena", () => {
  engine.init({ getChannels: () => [], storage: mem(), isBusy: ch => ch.busy });
  const mk = extra => Object.assign({ sources: [{ type: "hls", url: "1" }, { type: "hls", url: "2" }], currentSourceIndex: 0 }, extra);
  const keys = ["hls|2", "hls|1"];
  assert.strictEqual(applyOrder(mk({ pinSources: true }), keys), false);
  assert.strictEqual(applyOrder(mk({ busy: true }), keys), false);
  const ch = mk({});
  assert.strictEqual(applyOrder(ch, keys), true);
  assert.deepStrictEqual(ch.sources.map(s => s.url), ["2", "1"]);
});

test("Telefe está fijado en channels.json", () => {
  const chs = JSON.parse(fs.readFileSync(path.join(__dirname, "..", "channels.json"), "utf8"));
  assert.strictEqual(chs.find(c => c.id === "telefe").pinSources, true);
});

test("política comunitaria: https, dominio permitido, sin IP, sin headers, no pago", () => {
  engine.setPolicy(policy);
  const ok = engine.isAllowedCommunitySource;
  const ch = { id: "canal26" };
  assert.strictEqual(ok("https://stream-gtlc.telecentro.net.ar/x.m3u8", undefined, ch), true);
  assert.strictEqual(ok("http://stream-gtlc.telecentro.net.ar/x.m3u8", undefined, ch), false);
  assert.strictEqual(ok("https://45.173.231.22/x.m3u8", undefined, ch), false);
  assert.strictEqual(ok("https://dominio-random.example/x.m3u8", undefined, ch), false);
  assert.strictEqual(ok("https://stream-gtlc.telecentro.net.ar/x.m3u8", { Referer: "https://x" }, ch), false);
  assert.strictEqual(ok("https://live-04-11-tyc-app-web.vodgc.net/x.m3u8", undefined, { id: "tycsports" }), false);
  engine.setPolicy(null);
  assert.strictEqual(ok("https://stream-gtlc.telecentro.net.ar/x.m3u8", undefined, ch), false, "sin política = cerrado");
});

test("channels.json y channels.js están sincronizados", () => {
  const root = path.join(__dirname, "..");
  const json = JSON.parse(fs.readFileSync(path.join(root, "channels.json"), "utf8"));
  const js = fs.readFileSync(path.join(root, "channels.js"), "utf8");
  const m = /window\.DEFAULT_CHANNELS\s*=\s*([\s\S]*);\s*$/.exec(js);
  assert.deepStrictEqual(JSON.parse(m[1]), json);
});

test("sourceKey diferencia fuentes directa y proxy con la misma URL", () => {
  const direct = { type: "hls", url: "https://dai.google.com/master.m3u8", useProxy: false };
  const proxy = { type: "hls", url: "https://dai.google.com/master.m3u8", useProxy: true };
  assert.notStrictEqual(sourceKey(direct), sourceKey(proxy));
  assert.strictEqual(sourceKey(direct), "hls|https://dai.google.com/master.m3u8");
  assert.strictEqual(sourceKey(proxy), "hls|https://dai.google.com/master.m3u8|proxy");
});

test("América TV tiene Referer en fuente principal y respaldo proxy", () => {
  const chs = JSON.parse(fs.readFileSync(path.join(__dirname, "..", "channels.json"), "utf8"));
  const america = chs.find(c => c.id === "americatv");
  assert.strictEqual(america.pinSources, true);
  assert.strictEqual(america.sources[0].headers.Referer, "https://vmf.edge-apps.net/");
  assert.strictEqual(america.sources[1].useProxy, true);
  assert.strictEqual(america.sources[1].preferNativeAndroid, false);
});

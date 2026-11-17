#!/usr/bin/env python3
"""
Sincroniza fuentes comunitarias desde iptv-org (https://iptv-org.github.io/api/) hacia channels.json.
Se ejecuta una vez al día desde .github/workflows/sync-iptv-org.yml (o a mano).

Reglas (ver source-policy.json):
  - Solo canales mapeados en "channelMap" y que NO estén en "paidChannelIds".
  - Solo URLs https, dominio permitido ("allowedHosts"), sin IP directa y sin headers
    especiales (referrer / user_agent).
  - La fuente debe responder con un manifest HLS válido y en vivo.
  - Se agregan AL FINAL de las fuentes del canal, marcadas community:true / origin:"iptv-org".
    Nunca reemplazan ni modifican fuentes oficiales cargadas a mano.
  - Las fuentes comunitarias de corridas anteriores se reconstruyen en cada ejecución,
    por lo que las que dejan de existir o de responder se eliminan solas.

Uso:  python tools/sync_iptv_org.py [--dry-run]
"""
import json
import re
import shutil
import sys
import urllib.request
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parent.parent
API = "https://iptv-org.github.io/api/"
UA = {"User-Agent": "AndyTV-sync/1.0 (+https://github.com/andresehansen/tele-mama-android-tv-)"}
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "www"


def fetch(url, timeout=30):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return r.read().decode("utf-8", "replace")


def is_bare_ip(host):
    return bool(re.fullmatch(r"\d{1,3}(\.\d{1,3}){3}", host)) or ":" in host


def host_allowed(host, allowed):
    return any(host == a or host.endswith("." + a) for a in allowed)


def passes_policy(stream, policy):
    url = stream.get("url") or ""
    u = urlparse(url)
    if policy.get("httpsOnly", True) and u.scheme != "https":
        return False
    if policy.get("blockBareIp", True) and is_bare_ip(u.hostname or ""):
        return False
    if policy.get("blockCustomHeaders", True) and (stream.get("referrer") or stream.get("user_agent")):
        return False
    return host_allowed(u.hostname or "", policy.get("allowedHosts", []))


def hls_alive(url):
    try:
        text = fetch(url, timeout=10)
    except Exception:
        return False
    if "#EXTM3U" not in text:
        return False
    if "#EXT-X-ENDLIST" in text and "#EXT-X-STREAM-INF" not in text:
        return False
    return "#EXT-X-STREAM-INF" in text or "#EXTINF" in text


def write_outputs(channels):
    body = json.dumps(channels, ensure_ascii=False, indent=2)
    (ROOT / "channels.json").write_text(body, encoding="utf-8")
    (ROOT / "channels.js").write_text("/* Canales de AndyTV */\nwindow.DEFAULT_CHANNELS = " + body + ";\n", encoding="utf-8")
    if ASSETS.exists():
        for name in ("channels.json", "channels.js"):
            shutil.copyfile(ROOT / name, ASSETS / name)


def main():
    dry = "--dry-run" in sys.argv
    policy = json.loads((ROOT / "source-policy.json").read_text(encoding="utf-8"))
    channels = json.loads((ROOT / "channels.json").read_text(encoding="utf-8"))
    streams = json.loads(fetch(API + "streams.json", 120))
    by_channel = {}
    for s in streams:
        by_channel.setdefault(s.get("channel"), []).append(s)

    paid = set(policy.get("paidChannelIds", []))
    cmap = policy.get("channelMap", {})
    cap = int(policy.get("maxCommunitySourcesPerChannel", 3))
    added_total = removed_total = 0

    for ch in channels:
        if ch["id"] in paid or ch["id"] not in cmap:
            continue
        sources = ch.setdefault("sources", [])
        before = [s for s in sources if s.get("community") and s.get("origin") == "iptv-org"]
        manual = [s for s in sources if s not in before]
        known_urls = {s.get("url") for s in manual if s.get("url")}

        fresh = []
        for st in by_channel.get(cmap[ch["id"]], []):
            url = st.get("url")
            if not url or url in known_urls or any(f["url"] == url for f in fresh):
                continue
            if not passes_policy(st, policy):
                continue
            if not hls_alive(url):
                continue
            quality = st.get("quality") or "HD"
            fresh.append({
                "label": f"Comunidad iptv-org ({quality})",
                "type": "hls",
                "url": url,
                "useProxy": False,
                "community": True,
                "origin": "iptv-org",
            })
            if len(fresh) >= cap:
                break

        old_urls = {s["url"] for s in before}
        new_urls = {s["url"] for s in fresh}
        added_total += len(new_urls - old_urls)
        removed_total += len(old_urls - new_urls)
        ch["sources"] = manual + fresh
        print(f"{ch['name']}: +{len(new_urls - old_urls)} -{len(old_urls - new_urls)} (comunitarias: {len(fresh)})")

    print(f"Total: +{added_total} -{removed_total}")
    if added_total == 0 and removed_total == 0:
        print("Sin cambios.")
        return
    if dry:
        print("--dry-run: no se escribió nada.")
        return
    write_outputs(channels)
    print("channels.json / channels.js actualizados.")


if __name__ == "__main__":
    main()

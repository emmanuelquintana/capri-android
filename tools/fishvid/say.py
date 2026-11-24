#!/usr/bin/env python3
"""Genera frases con la voz de Fish de Capri para revisar o montar videos. Lee tools/fishvid/lines.json
([{"id","lang","voice","text"}]) y deja out/<id>.wav. La clave solo llega por FISH_API_KEY."""
import json, os, sys, time, urllib.request, urllib.error
KEY = os.environ.get("FISH_API_KEY", "").strip()
MODEL = os.environ.get("FISH_MODEL", "s2.1-pro-free")
if not KEY: sys.exit("Falta FISH_API_KEY")
os.makedirs("out", exist_ok=True)
for row in json.load(open("tools/fishvid/lines.json", encoding="utf-8")):
    body = json.dumps({"text": row["text"], "reference_id": row["voice"], "format": "wav", "sample_rate": 44100,
                       "latency": "normal", "normalize": True, "language": row["lang"]}).encode()
    for attempt in range(6):
        req = urllib.request.Request("https://api.fish.audio/v1/tts", data=body, method="POST", headers={
            "Authorization": f"Bearer {KEY}", "Content-Type": "application/json", "model": MODEL, "User-Agent": "Capri-voz"})
        try:
            with urllib.request.urlopen(req, timeout=120) as r: data = r.read()
            open(f"out/{row['id']}.wav", "wb").write(data); print(row["id"], len(data), flush=True); break
        except urllib.error.HTTPError as e:
            print(row["id"], "HTTP", e.code, e.read()[:200], flush=True)
            if e.code in (401, 402, 403): sys.exit(1)
        except Exception as e:
            print(row["id"], e, flush=True)
        time.sleep(4 * (attempt + 1))
    else:
        sys.exit(f"no salió {row['id']}")

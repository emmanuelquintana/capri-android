#!/usr/bin/env python3
"""Las frases grabadas de Capri, regrabadas con su voz de Fish Audio (9.0.0).

Lee los paquetes 8.0.0 (capri-clips-<lang>.zip), genera cada frase con el mismo texto y el mismo ID en la voz de
Fish de ese idioma, la codifica en Opus 48 kHz mono (como antes) y escribe los paquetes nuevos en out/, con un
manifiesto válido para VoiceClipLibrary (mismo id de paquete, versión 9.0.0, sha256 y bytes de cada frase).

La clave llega solo por la variable FISH_API_KEY (secreto de GitHub): nunca se escribe en un archivo ni en el log.
"""
import concurrent.futures as cf
import hashlib
import io
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
import zipfile

API = "https://api.fish.audio/v1/tts"
VOICES = {"es-MX": "0a56e85c27c6490caf07bf61d2a229f0", "en-US": "92028b1fbc0348d09ad2d1f4132638b5"}
VERSION = "9.0.0"
MODEL = os.environ.get("FISH_MODEL", "s2.1-pro")
KEY = os.environ.get("FISH_API_KEY", "").strip()


def tts(text: str, voice: str, lang: str) -> bytes:
    body = json.dumps({
        "text": text, "reference_id": voice, "format": "wav", "sample_rate": 44100,
        "latency": "normal", "normalize": True, "language": lang,
    }).encode()
    for attempt in range(6):
        req = urllib.request.Request(API, data=body, method="POST", headers={
            "Authorization": f"Bearer {KEY}", "Content-Type": "application/json", "model": MODEL,
            "User-Agent": "Capri-voz"})
        try:
            with urllib.request.urlopen(req, timeout=120) as r:
                data = r.read()
            if len(data) < 2000:
                raise RuntimeError(f"audio demasiado corto ({len(data)} bytes)")
            return data
        except urllib.error.HTTPError as e:
            msg = e.read()[:300].decode("utf-8", "replace")
            if e.code in (401, 402, 403):
                sys.exit(f"Fish Audio respondió {e.code}: {msg} (revisa la clave o el saldo)")
            print(f"  HTTP {e.code}, otra vez en {4 * (attempt + 1)} s: {msg}", flush=True)
        except Exception as e:  # red cortada
            print(f"  {type(e).__name__}: {e}, otra vez en {4 * (attempt + 1)} s", flush=True)
        time.sleep(4 * (attempt + 1))
    raise RuntimeError(f"no salió: {text!r}")


def opus(wav: bytes) -> tuple[bytes, int]:
    """WAV → Opus 48 kHz mono (con un poco de silencio recortado al inicio y al final) y su duración en ms."""
    p = subprocess.run(["ffmpeg", "-v", "error", "-i", "pipe:0",
                        "-af", "silenceremove=start_periods=1:start_threshold=-50dB,areverse,"
                               "silenceremove=start_periods=1:start_threshold=-50dB,areverse,apad=pad_dur=0.12",
                        "-ac", "1", "-ar", "48000", "-c:a", "libopus", "-b:a", "48k", "-vbr", "on",
                        "-f", "opus", "pipe:1"], input=wav, capture_output=True, check=True)
    data = p.stdout
    import tempfile
    with tempfile.NamedTemporaryFile(suffix=".opus") as f:
        f.write(data); f.flush()
        d = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", f.name],
                           capture_output=True, check=True)
    return data, int(round(float(d.stdout.decode().strip()) * 1000))


def build(lang: str, src: str, out_dir: str, samples: str) -> dict:
    with zipfile.ZipFile(src) as z:
        manifest = json.loads(z.read("manifest.json"))
    voice = VOICES[lang]
    clips = manifest["clips"]
    print(f"{lang}: {len(clips)} frases con la voz {voice}", flush=True)

    def one(c):
        wav = tts(c["text"], voice, lang)
        data, ms = opus(wav)
        return c, wav, data, ms

    made = {}
    with cf.ThreadPoolExecutor(max_workers=3) as pool:  # Fish: 5 a la vez por clave, 3 deja margen
        for i, (c, wav, data, ms) in enumerate(pool.map(one, clips), 1):
            made[c["id"]] = (data, ms)
            print(f"  [{i}/{len(clips)}] {c['id']} {ms} ms", flush=True)
            if i <= 12:
                open(os.path.join(samples, f"{lang}-{c['id']}.wav"), "wb").write(wav)
    new = dict(manifest)
    new["version"] = VERSION
    new["clips"] = []
    for c in clips:
        data, ms = made[c["id"]]
        row = dict(c)
        row.update({
            "durationMs": max(1, min(40000, ms)),
            "bytes": len(data),
            "sha256": hashlib.sha256(data).hexdigest(),
            "engine": f"fish-{MODEL}",
            "profile": f"fish:{voice}",
            "processing": {"version": None, "preset": "OFF", "state": "raw"},
        })
        new["clips"].append(row)
    path = os.path.join(out_dir, os.path.basename(src))
    # solo archivos (sin entradas de carpeta): VoicePackManager exige manifest.json + las frases, nada más
    with zipfile.ZipFile(path, "w", zipfile.ZIP_STORED) as z:
        z.writestr("manifest.json", json.dumps(new, ensure_ascii=False, indent=1))
        for row in new["clips"]:
            z.writestr(row["asset"], made[row["id"]][0])
    digest = hashlib.sha256(open(path, "rb").read()).hexdigest()
    size = os.path.getsize(path)
    print(f"{lang}: {path} {size} bytes sha256 {digest}", flush=True)
    return {"lang": lang, "file": os.path.basename(src), "bytes": size, "sha256": digest, "clips": len(clips)}


def main():
    if not KEY:
        sys.exit("Falta el secreto FISH_API_KEY en el repositorio (Settings › Secrets and variables › Actions).")
    out_dir, samples = "out", "out/muestras"
    os.makedirs(samples, exist_ok=True)
    packs = [build(lang, f"capri-clips-{lang}.zip", out_dir, samples) for lang in ("es-MX", "en-US")]
    json.dump(packs, open(os.path.join(out_dir, "packs.json"), "w"), indent=1)


if __name__ == "__main__":
    main()

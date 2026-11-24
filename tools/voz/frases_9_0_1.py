#!/usr/bin/env python3
"""
Frases de Capri 9.0.1 (parche 1.12.1): parte de las frases 9.0.0 ya publicadas.

- A TODAS les pone un respiro de silencio al inicio: empezaban justo en la primera sílaba y algunos teléfonos se
  comían el principio al reproducirlas («Pausa breve» sonaba «…usa breve»).
- Las frases de REGRABAR se vuelven a pedir a Fish Audio (misma voz, mismo texto) sin recortar el inicio, y solo
  se aceptan si Whisper oye el texto completo (hasta 6 intentos; si ninguno, se queda la anterior con su respiro).
Mismos IDs y textos. Sale out/capri-clips-<lang>.zip + out/packs.json + out/informe.txt.
"""
import hashlib, json, os, re, subprocess, sys, tempfile, time, unicodedata, urllib.request, urllib.error, zipfile

API = "https://api.fish.audio/v1/tts"
VOICES = {"es-MX": "0a56e85c27c6490caf07bf61d2a229f0", "en-US": "92028b1fbc0348d09ad2d1f4132638b5"}
VERSION = "9.0.1"
MODEL = os.environ.get("FISH_MODEL", "s2.1-pro-free")
KEY = os.environ.get("FISH_API_KEY", "").strip()
LEAD = 0.18  # s de silencio al inicio
REGRABAR = {
    "es-MX": ["game.pause.soft", "pet.original-06.playful", "morning.original-01.happy", "joy.original-02.happy",
              "reminder.original-02.neutral", "pet.original-05.playful", "greeting.original-01.happy",
              "joy.joy.happy"],
    "en-US": ["joy.original-02.happy", "anger.original-02.angry", "task.done.neutral", "night.original-04.soft",
              "company.original-03.soft"],
}
_whisper = None


def words(t):
    t = unicodedata.normalize("NFD", t.lower().replace("’", "'"))
    t = "".join(c for c in t if unicodedata.category(c) != "Mn").replace("'", "")
    return re.sub(r"[^a-z0-9 ]", " ", t).split()


def heard(pcm_wav: bytes, lang: str) -> str:
    global _whisper
    if _whisper is None:
        from faster_whisper import WhisperModel
        _whisper = WhisperModel("medium", device="cpu", compute_type="int8")
    import numpy as np
    pcm = np.frombuffer(pcm_wav, np.int16).astype(np.float32) / 32768
    segs, _ = _whisper.transcribe(pcm, language=lang[:2], beam_size=5)
    return " ".join(s.text.strip() for s in segs)


def tts(text, voice, lang):
    body = json.dumps({"text": text, "reference_id": voice, "format": "wav", "sample_rate": 44100,
                       "latency": "normal", "normalize": True, "language": lang}).encode()
    for attempt in range(6):
        req = urllib.request.Request(API, data=body, method="POST", headers={
            "Authorization": f"Bearer {KEY}", "Content-Type": "application/json", "model": MODEL, "User-Agent": "Capri-voz"})
        try:
            with urllib.request.urlopen(req, timeout=120) as r:
                data = r.read()
            if len(data) > 2000:
                return data
        except urllib.error.HTTPError as e:
            if e.code in (401, 402, 403):
                sys.exit(f"Fish Audio respondió {e.code} (revisa la clave o el saldo)")
            print(f"  HTTP {e.code}, otra vez", flush=True)
        except Exception as e:
            print(f"  {type(e).__name__}: {e}, otra vez", flush=True)
        time.sleep(4 * (attempt + 1))
    raise RuntimeError(f"no salió: {text!r}")


def ff(args, data):
    return subprocess.run(["ffmpeg", "-v", "error", *args], input=data, capture_output=True, check=True).stdout


def to_opus(audio: bytes, trim_start: bool) -> tuple[bytes, int]:
    # inicio: deja como mucho 0,1 s del silencio que traiga y luego añade el respiro; final: recorta y deja 0,12 s
    start = "silenceremove=start_periods=1:start_threshold=-50dB:start_silence=0.1," if trim_start else ""
    af = (f"{start}areverse,silenceremove=start_periods=1:start_threshold=-50dB,areverse,"
          f"adelay={int(LEAD * 1000)}:all=1,apad=pad_dur=0.12")
    data = ff(["-i", "pipe:0", "-af", af, "-ac", "1", "-ar", "48000", "-c:a", "libopus", "-b:a", "48k",
               "-vbr", "on", "-f", "opus", "pipe:1"], audio)
    with tempfile.NamedTemporaryFile(suffix=".opus") as f:
        f.write(data); f.flush()
        d = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", f.name],
                           capture_output=True, check=True)
    return data, int(round(float(d.stdout.decode().strip()) * 1000))


def wav16(audio: bytes) -> bytes:
    """PCM 16 kHz mono, 16 bits, sin cabecera."""
    return ff(["-i", "pipe:0", "-ac", "1", "-ar", "16000", "-f", "s16le", "pipe:1"], audio)


def build(lang, src, out, log):
    z = zipfile.ZipFile(src)
    man = json.loads(z.read("manifest.json"))
    redo = set(REGRABAR.get(lang, []))
    new = dict(man); new["version"] = VERSION; new["clips"] = []
    files = {}
    for c in man["clips"]:
        old = z.read(c["asset"])
        data, ms = to_opus(old, trim_start=False)
        note = "respiro"
        if c["id"] in redo:
            want = words(c["text"])
            for i in range(6):
                wav = tts(c["text"], VOICES[lang], lang)
                cand, cms = to_opus(wav, trim_start=True)
                got = heard(wav16(cand), lang)
                miss = [w for w in want if w not in words(got)]
                print(f"  {lang} {c['id']} intento {i + 1}: «{got}» faltan {miss}", flush=True)
                if not miss:
                    data, ms, note = cand, cms, f"regrabada ({got})"
                    break
            else:
                note = "regrabar falló, queda la anterior con respiro"
        files[c["asset"]] = data
        row = dict(c)
        row.update({"durationMs": max(1, min(40000, ms)), "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()})
        new["clips"].append(row)
        log.write(f"{lang} {c['id']}: {note}\n")
    path = os.path.join(out, os.path.basename(src))
    with zipfile.ZipFile(path, "w", zipfile.ZIP_STORED) as o:
        o.writestr("manifest.json", json.dumps(new, ensure_ascii=False, indent=1))
        for row in new["clips"]:
            o.writestr(row["asset"], files[row["asset"]])
    digest = hashlib.sha256(open(path, "rb").read()).hexdigest()
    return {"lang": lang, "file": os.path.basename(src), "bytes": os.path.getsize(path), "sha256": digest,
            "clips": len(new["clips"])}


def main():
    if not KEY:
        sys.exit("Falta FISH_API_KEY")
    src, out = sys.argv[1], sys.argv[2]
    os.makedirs(out, exist_ok=True)
    with open(os.path.join(out, "informe.txt"), "w") as log:
        packs = [build(l, os.path.join(src, f"capri-clips-{l}.zip"), out, log) for l in ("es-MX", "en-US")]
    json.dump(packs, open(os.path.join(out, "packs.json"), "w"), indent=1)
    print(json.dumps(packs, indent=1))


if __name__ == "__main__":
    main()

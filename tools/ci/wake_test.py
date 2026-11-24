# (1.12.0) Her passive ears (WakeWord.kt) on a PC: what the small Vosk model writes for her name and a request,
# and whether Ears.NAME finds her name in it. Voices: espeak-ng (robotic, a hard case) at a few speeds.
import json, re, subprocess, sys, wave
from vosk import Model, KaldiRecognizer

NAME = re.compile(r"(?<![a-zа-я])(?:[ck]a ?[pb]r[iy]s?|[ck]a[pb]i|ка[пб]р[иы]|ка[пб]и|カプリ|かぷり)(?![a-zа-я])")
def plain(s):
    import unicodedata
    return ''.join(c for c in unicodedata.normalize('NFD', s.lower()) if unicodedata.category(c) != 'Mn')

lang, model_dir = sys.argv[1], sys.argv[2]
phrases = {
    'es': ['Oye Capri', 'Capri', 'Oye Capri, ¿qué hora es?', 'Capri, ¿cómo estás?', '¿Qué hora es?', 'Capri, pon música', 'Capri, abre WhatsApp'],
    'en': ['Hey Capri', 'Capri, what time is it?', 'Capri, how are you?'],
}[lang]
voice = {'es': 'es-419', 'en': 'en-us'}[lang]
model = Model(model_dir)
hits = 0; named = 0
for p in phrases:
    for speed in (130, 175):
        subprocess.run(['espeak-ng', '-v', voice, '-s', str(speed), '-w', '/tmp/p.wav', p], check=True)
        subprocess.run(['sox', '/tmp/p.wav', '-r', '16000', '-c', '1', '-b', '16', '/tmp/p16.wav'], check=True)
        w = wave.open('/tmp/p16.wav', 'rb')
        r = KaldiRecognizer(model, 16000)
        out = []
        while True:
            d = w.readframes(1600)
            if not d: break
            if r.AcceptWaveform(d): out.append(json.loads(r.Result()).get('text', ''))
        out.append(json.loads(r.FinalResult()).get('text', ''))
        text = ' '.join(t for t in out if t).strip()
        has = bool(NAME.search(plain(text)))
        want = 'capri' in p.lower()
        named += want; hits += (has and want)
        print(f"[{lang} {speed}] «{p}» -> «{text}» name={'yes' if has else 'NO'}")
print(f"RESULT {lang}: her name found {hits}/{named}")

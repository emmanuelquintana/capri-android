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
# said without her name: she must stay quiet on these
others = {
    'es': ['¿Qué hora es?', 'Compré pan y leche', 'La cabra come pasto', 'Oye, pásame la sal', 'Vamos al cine el viernes', 'Ya casi es hora de comer'],
    'en': ['What time is it?', 'Hey, pass me the salt', 'The cab is here', 'I bought some bread'],
}[lang]
phrases = phrases + [o for o in others if o not in phrases]
voice = {'es': 'es-419', 'en': 'en-us'}[lang]
model = Model(model_dir)
# (1.12.0) the spotter: a second listener that only knows her name (a grammar), fed the same sound
SPOT = json.dumps({'es': ['oye capri', 'capri', '[unk]'], 'en': ['hey capri', 'capri', '[unk]']}[lang])
def spot(path):
    w = wave.open(path, 'rb')
    r = KaldiRecognizer(model, 16000, SPOT)
    r.SetWords(True)
    best = 0.0
    def take(res):
        nonlocal best
        for x in res.get('result', []):
            if x.get('word') == 'capri': best = max(best, x.get('conf', 0))
    while True:
        d = w.readframes(1600)
        if not d: break
        if r.AcceptWaveform(d): take(json.loads(r.Result()))
    take(json.loads(r.FinalResult()))
    return best
confs = []
hits = 0; named = 0; shits = 0; false = 0; sfalse = 0; quiet = 0
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
        conf = spot('/tmp/p16.wav')
        sp = conf >= 0.9
        confs.append((round(conf, 2), want))
        named += want; hits += (has and want); shits += (sp and want)
        quiet += (not want); false += (has and not want); sfalse += (sp and not want)
        print(f"[{lang} {speed}] «{p}» -> «{text}» name={'yes' if has else 'NO'} spotter={'yes' if sp else 'NO'} conf={conf:.2f}")
print(f"RESULT {lang}: her name found {hits}/{named} (false {false}/{quiet}); spotter {shits}/{named} (false {sfalse}/{quiet})")
for t in (0.6, 0.7, 0.8, 0.9, 0.95, 0.99):
    print(f"  threshold {t}: name {sum(1 for c, w in confs if w and c >= t)}/{named}, false {sum(1 for c, w in confs if not w and c >= t)}/{quiet}")

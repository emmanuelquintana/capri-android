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

# (1.12.0) WakeWord.kt's way: both listeners on the same sound, the request rebuilt with her name
def both(path):
    w = wave.open(path, 'rb')
    r = KaldiRecognizer(model, 16000); r.SetWords(True)
    sp = KaldiRecognizer(model, 16000, SPOT); sp.SetWords(True)
    at = [None]; out = []
    def spotted(res):
        for x in res.get('result', []):
            if NAME.search(plain(x.get('word', ''))) and x.get('conf', 0) >= 0.9: at[0] = (x['start'], x['end'])
    def with_name(said):
        text = said.get('text', '').strip()
        if not text or NAME.search(plain(text)): return text
        spotted(json.loads(sp.FinalResult()))
        if not at[0]: return text
        a, b = at[0]; at[0] = None; rest = []
        for x in said.get('result', []):
            mid = (x['start'] + x['end']) / 2
            if a - 0.15 <= mid <= b + 0.15: continue
            if x['end'] <= a + 0.15 and a - x['end'] < 0.6 and len(x['word']) <= 4: continue
            rest.append(x['word'])
        return 'Capri, ' + ' '.join(rest) if rest else 'Capri'
    while True:
        d = w.readframes(1600)
        if not d: break
        if sp.AcceptWaveform(d): spotted(json.loads(sp.Result()))
        if r.AcceptWaveform(d): out.append(with_name(json.loads(r.Result())))
    out.append(with_name(json.loads(r.FinalResult())))
    return ' | '.join(t for t in out if t)
ok = 0; bad = 0
for p in phrases:
    for speed in (130, 175):
        subprocess.run(['espeak-ng', '-v', voice, '-s', str(speed), '-w', '/tmp/p.wav', p], check=True)
        subprocess.run(['sox', '/tmp/p.wav', '-r', '16000', '-c', '1', '-b', '16', '/tmp/p16.wav'], check=True)
        got = both('/tmp/p16.wav'); has = bool(NAME.search(plain(got))); want = 'capri' in p.lower()
        ok += has and want; bad += has and not want
        print(f"  app [{lang} {speed}] «{p}» -> «{got}»")
print(f"RESULT app {lang}: her name {ok}/{named}, false {bad}/{quiet}")

# (1.12.0) WakeWord.kt's quiet gate: a minute of a quiet room with two requests in it, both listeners timed
# with and without the gate, and whether her name is still heard
import time, array, random
def minute():
    subprocess.run(['espeak-ng', '-v', voice, '-s', '150', '-w', '/tmp/a.wav', phrases[2]], check=True)
    subprocess.run(['sox', '/tmp/a.wav', '-r', '16000', '-c', '1', '-b', '16', '/tmp/a16.wav'], check=True)
    talk = array.array('h', wave.open('/tmp/a16.wav', 'rb').readframes(10 ** 7))
    rnd = random.Random(1)
    room = lambda n: array.array('h', (int(rnd.gauss(0, 60)) for _ in range(n)))
    return room(16000 * 20) + talk + room(16000 * 25) + talk + room(16000 * 12)
def run(sound, gate):
    r = KaldiRecognizer(model, 16000); sp = KaldiRecognizer(model, 16000, SPOT); sp.SetWords(True)
    floor, hush, before, fed, heard, t0 = 400.0, 12, [], 0, [], time.process_time()
    for i in range(0, len(sound), 1600):
        buf = sound[i:i + 1600]
        peak = max(abs(v) for v in buf)
        loud = peak > max(floor * 2.2, 400)
        floor += (peak - floor) * (0.002 if loud else 0.05)
        hush = 0 if loud else hush + 1
        if gate and hush > 12:
            before = (before + [buf])[-3:]
            continue
        for b in before + [buf]:
            fed += 1
            sp.AcceptWaveform(b.tobytes())
            if r.AcceptWaveform(b.tobytes()): heard.append(json.loads(r.Result()).get('text', ''))
        before = []
    heard.append(json.loads(r.FinalResult()).get('text', ''))
    return time.process_time() - t0, fed, len(sound) // 1600, [h for h in heard if h]
s = minute()
for gate in (False, True):
    cpu, fed, total, heard = run(s, gate)
    print(f"GATE {lang} {'on ' if gate else 'off'}: {cpu:.1f} s of CPU for {total / 10:.0f} s of sound, fed {fed}/{total}, heard {heard}")

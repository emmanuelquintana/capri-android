import './compat.js'; // (first: what an old WebView lacks)
// The app's own Vestuario screen (ClosetActivity, closet.html): the same wardrobe sheet as over
// your screen (wardrobe.js), with her standing on a little stage above it. Each tap shows on her at
// once and is saved the way her page saves it (config.json's look), so she wears it when she's on,
// and if she's on right now she changes there too.
//
// Kotlin (ClosetActivity) gives `window.CapriCloset`: config() her saved settings plus the app's
// language, save(look) what you picked, onScreen() her wardrobe over your screen instead, done().
const bridge = window.CapriCloset;
// anything that goes wrong here, in the app's log (ClosetActivity)
const report = (what) => { try { bridge?.log(String(what)); } catch {} };
addEventListener('error', (e) => report(`${e.message} (${e.filename?.split('/').pop()}:${e.lineno})`));
addEventListener('unhandledrejection', (e) => report(`unhandled: ${e.reason?.stack || e.reason}`));
report('closet: start');
const WORDS = {
  es: { screen: 'Vestirla en tu pantalla', done: 'Listo' },
  en: { screen: 'Dress her on your screen', done: 'Done' },
  fr: { screen: 'L’habiller sur ton écran', done: 'OK' },
  pt: { screen: 'Vestir na sua tela', done: 'Pronto' },
  ru: { screen: 'Одеть на экране', done: 'Готово' },
};

let cfg = {};
try {
  cfg = JSON.parse(bridge?.config() || '{}') || {};
} catch {}
const lang = WORDS[cfg.appLang] ? cfg.appLang : 'en';
document.documentElement.lang = lang;

const load = (src) => new Promise((resolve, reject) => {
  const img = new Image();
  img.onload = () => resolve(img);
  img.onerror = reject;
  img.src = src;
});

const sprite = document.getElementById('sprite');
const her = sprite.parentElement;
const g = sprite.getContext('2d');
function show(picture, fresh) {
  g.clearRect(0, 0, sprite.width, sprite.height);
  g.drawImage(picture, 0, 0, sprite.width, sprite.height);
  if (!fresh) return;
  her.classList.remove('is-new');
  void her.offsetWidth; // (the glitch again, on every change)
  her.classList.add('is-new');
}

const original = await load(new URL('../capri/assets/capri/capri-full.png', import.meta.url).href);
show(original, false);
report('closet: her picture is in');
// her sheet's code after she's on the stage (so she shows at once)
const { wardrobe } = await import('./wardrobe.js');

// what wardrobe.js needs of her: her picture, and where a new one goes (no shader here)
let first = true;
const capri = {
  facing: 1,
  sprite: { image: original, gl: null, onPicture: (c) => { show(c, !first); first = false; } },
};
window.dev = { ...(window.dev || {}), capri, brain: { lang } };

const closet = wardrobe({
  cfg: () => cfg,
  setConfig: (patch) => {
    cfg = { ...cfg, ...patch };
    if ('look' in patch) {
      first = false;
      bridge?.save(JSON.stringify(patch.look));
    }
  },
  onToggle: null,
});
await closet?.ready(); // (which drawings are in, before the outfits are shown)
closet?.open();
report(`closet: open (${closet ? 'sheet' : 'no sheet'})`);

// below her sheet: the other way to dress her, and back to the app
const bar = document.querySelector('.bar');
document.body.append(bar);
const w = WORDS[lang];
const onScreen = document.getElementById('on-screen');
onScreen.textContent = w.screen;
onScreen.addEventListener('click', () => bridge?.onScreen());
const done = document.getElementById('done');
done.textContent = w.done;
done.addEventListener('click', () => bridge?.done());

// test hook (tools/preview): what she's wearing
window.__closet = closet;

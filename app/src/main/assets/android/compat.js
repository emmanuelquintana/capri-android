// Older phones (1.9.0), loaded before anything else of her page (host.js, closet.js).
//
// - What an older WebView (Chrome 85 to 102, as on many phones that don't update it often) doesn't
//   have yet, and her page and the desktop's code use: without them, things failed one by one there
//   (her AI's model list needs AbortSignal.timeout, Chrome 103; `.at()` is Chrome 92…).
// - Modo ligero (Perf.kt, Ajustes › Rendimiento): her canvases at most 1.5 pixels per CSS pixel. Her
//   picture and her effects are the GPU's biggest jobs, and on a dense screen that's about half the
//   pixels or less. Kotlin's touch pads still get the screen's real ratio (screenDpr()).

const define = (obj, name, value) => {
  if (obj && !(name in obj)) Object.defineProperty(obj, name, { value, writable: true, configurable: true });
};

if (typeof AbortSignal !== 'undefined') {
  define(AbortSignal, 'timeout', (ms) => {
    const c = new AbortController();
    setTimeout(() => c.abort(Object.assign(new Error('The operation timed out.'), { name: 'TimeoutError' })), ms);
    return c.signal;
  });
}

function at(i) {
  const n = Math.trunc(i) || 0;
  const k = n < 0 ? this.length + n : n;
  return k < 0 || k >= this.length ? undefined : this[k];
}
define(Array.prototype, 'at', at);
define(String.prototype, 'at', at);
if (typeof Int8Array !== 'undefined') define(Object.getPrototypeOf(Int8Array.prototype), 'at', at);

define(Array.prototype, 'findLast', function (fn, self) {
  for (let i = this.length - 1; i >= 0; i--) if (fn.call(self, this[i], i, this)) return this[i];
  return undefined;
});
define(Array.prototype, 'findLastIndex', function (fn, self) {
  for (let i = this.length - 1; i >= 0; i--) if (fn.call(self, this[i], i, this)) return i;
  return -1;
});
define(String.prototype, 'replaceAll', function (find, to) {
  if (find instanceof RegExp) return this.replace(find, to);
  return this.split(String(find)).join(typeof to === 'function' ? to(String(find)) : String(to));
});
define(Object, 'hasOwn', (o, k) => Object.prototype.hasOwnProperty.call(o, k));
if (typeof crypto !== 'undefined' && crypto.getRandomValues) {
  define(crypto, 'randomUUID', () => {
    const b = crypto.getRandomValues(new Uint8Array(16));
    b[6] = (b[6] & 0x0f) | 0x40;
    b[8] = (b[8] & 0x3f) | 0x80;
    const h = [...b].map((x) => x.toString(16).padStart(2, '0')).join('');
    return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
  });
}
define(globalThis, 'structuredClone', (v) => (v === undefined ? v : JSON.parse(JSON.stringify(v))));

// ---------- modo ligero ----------

const ratio = Object.getOwnPropertyDescriptor(window, 'devicePixelRatio')
  || Object.getOwnPropertyDescriptor(Object.getPrototypeOf(window), 'devicePixelRatio');
const real = () => (ratio?.get ? ratio.get.call(window) : ratio?.value) || 1;
// the screen's real pixels per CSS pixel (her touch pads are placed with it)
window.screenDpr = real;

let lite = false;
try {
  lite = Boolean(window.CapriNative?.lite?.());
} catch {}
export const LITE_DPR = 1.5;
if (lite && real() > LITE_DPR) {
  try {
    Object.defineProperty(window, 'devicePixelRatio', { get: () => Math.min(real(), LITE_DPR), configurable: true });
  } catch {}
}
window.capriLite = lite;
// her picture in 2D, without WebGL (1.9.1, Perf.flat: modo ligero, or a GPU that kept losing it)
try {
  window.capriFlat = Boolean(window.CapriNative?.flat?.());
} catch {
  window.capriFlat = lite;
}
if (lite) document.documentElement.classList.add('is-lite');

// Her picture on the phone's GPU, kept alive (1.9.0).
//
// Some phones lose her WebGL picture (memory short, the GPU reset, the screen going off and on) and
// never give it back: Chrome then paints a grey box with a sad face where she stood, and she stays
// like that ("se queda pillada"). And some can't make one at all, so her page never started.
// - No WebGL at all: she's drawn plainly with a 2D canvas instead (no shader effects, but there).
// - Lost and not back within a few seconds, or back but her shader didn't come back with it: Android
//   gives her a fresh page (Overlay.restart), which brings her back as she was.

import { SpriteRenderer, SPRITE } from '../capri/src/renderer/js/sprite.js';
import { send } from './native.js';

const BACK_WITHIN = 4000; // ms a lost picture has to come back by itself

const setup = SpriteRenderer.prototype.setup;
const resize = SpriteRenderer.prototype.resize;
const draw = SpriteRenderer.prototype.draw;

// drawn in 2D from the start (1.9.1): modo ligero, or a phone whose GPU kept losing her (Perf.flat)
function goFlat(r, why) {
  const g = r.canvas.getContext('2d');
  if (!g) return false;
  send('log', { level: 'I', text: `drawing her in 2D (${why})` });
  r.flat = g;
  r.gl = null;
  r.onPicture = () => {}; // (her wardrobe's picture is this.image, drawn on the next frame)
  return true;
}

SpriteRenderer.prototype.setup = function () {
  if (this.flat) return;
  if (window.capriFlat && !this.started && goFlat(this, window.capriLite ? 'modo ligero' : 'her GPU lost her picture lately')) return;
  const again = this.started; // (after a GPU reset, not her first time)
  try {
    setup.call(this);
    this.started = true;
  } catch (err) {
    if (again) {
      // (her shader didn't come back after a reset: only a fresh page will do)
      send('log', { level: 'E', text: `her picture didn't come back: ${err?.message || err}` });
      send('overlay:restart', { why: 'webgl-setup' });
      return;
    }
    // no WebGL here at all: plain 2D
    const g = this.canvas.getContext('2d'); // (null if WebGL was made and only her shader failed)
    if (!g) throw err;
    send('log', { level: 'W', text: `no WebGL (${err?.message || err}): drawing her in 2D` });
    this.flat = g;
    this.gl = null;
    this.onPicture = () => {}; // (her wardrobe's picture is this.image, drawn on the next frame)
  }
};

SpriteRenderer.prototype.resize = function (scale) {
  if (!this.flat) return resize.call(this, scale);
  this.scale = scale;
  const dpr = window.devicePixelRatio || 1;
  const { width, height } = SpriteRenderer.cssSize(scale);
  this.canvas.style.width = `${width}px`;
  this.canvas.style.height = `${height}px`;
  this.canvas.width = Math.round(width * dpr);
  this.canvas.height = Math.round(height * dpr);
};

SpriteRenderer.prototype.draw = function (s) {
  if (!this.flat) {
    if (this.gl?.isContextLost?.()) return; // (nothing to draw on until it's back)
    return draw.call(this, s);
  }
  const g = this.flat;
  const { width, height } = this.canvas;
  const k = width / (SPRITE.width + SPRITE.pad * 2);
  g.clearRect(0, 0, width, height);
  g.filter = 'brightness(1.3) saturate(1.2)'; // (as bright as her shader shows her)
  g.globalAlpha = Math.max(0, Math.min(1, (s?.alpha ?? 1) * (1 - (s?.dissolve || 0)) * (1 - (s?.collapse || 0))));
  g.drawImage(this.image, SPRITE.pad * k, SPRITE.pad * k, SPRITE.width * k, SPRITE.height * k);
  g.globalAlpha = 1;
  g.filter = 'none';
};

// lost and not back: a fresh page
export function watchGpu() {
  const canvas = document.getElementById('sprite');
  if (!canvas) return;
  let timer = null;
  canvas.addEventListener('webglcontextlost', () => {
    send('gpu:lost', {}); // (counted: twice lately and she's drawn in 2D from then on, Perf.kt)
    send('log', { level: 'W', text: 'her picture was lost (GPU)' });
    clearTimeout(timer);
    timer = setTimeout(() => {
      const gl = window.dev?.capri?.sprite?.gl;
      if (gl?.isContextLost?.()) send('overlay:restart', { why: 'webgl-lost' });
    }, BACK_WITHIN);
  });
  canvas.addEventListener('webglcontextrestored', () => {
    clearTimeout(timer);
    send('log', { level: 'I', text: 'her picture is back (GPU)' });
  });
}

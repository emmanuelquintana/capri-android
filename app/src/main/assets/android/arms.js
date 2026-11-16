// Her other arm, where her shader isn't the one drawing her (1.10.0): "en modo ligero le falta un
// brazo", and in her wardrobe's big picture and its little ones. Her picture (capri-full.png, and
// every outfit) has only the arm that reaches out; her shader draws the other one as it mirrored
// (sprite.js). Drawn in 2D, she had one arm. Here that same arm, mirrored, as the shader does.

import { SPRITE } from '../capri/src/renderer/js/sprite.js';

// the reaching arm's outline (SPRITE.armL), in her picture's pixels
const ARM = [[0, 208], [101, 157.5], [101, 176.1], [50, 207.8], [50, 234], [0, 234]];

const made = new WeakMap();

/** Her picture with both arms (a canvas, kept per picture); the picture as it is if it can't be. */
export function bothArms(img) {
  if (!img) return img;
  const hit = made.get(img);
  if (hit) return hit;
  const w = img.naturalWidth || img.width;
  const h = img.naturalHeight || img.height;
  if (!w || !h) return img;
  const kx = w / SPRITE.width;
  const ky = h / SPRITE.height;
  const c = document.createElement('canvas');
  c.width = w;
  c.height = h;
  const g = c.getContext('2d');
  if (!g) return img;
  // the arm, mirrored around x = mirror / 2, behind her body (as the shader puts it)
  g.save();
  g.translate(SPRITE.mirror * kx, 0);
  g.scale(-1, 1);
  g.beginPath();
  ARM.forEach(([x, y], i) => (i ? g.lineTo(x * kx, y * ky) : g.moveTo(x * kx, y * ky)));
  g.closePath();
  g.clip();
  g.drawImage(img, 0, 0, w, h);
  g.restore();
  g.drawImage(img, 0, 0, w, h);
  made.set(img, c);
  return c;
}

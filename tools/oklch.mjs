// oklch() colors as plain sRGB (#rrggbb, or rgba() with an alpha), for CSS.
//
// Older Android WebViews (Chrome before 111, still on many phones) don't know oklch(): every color
// written that way is dropped, and her panels and cards came out with no background (black text on
// the wallpaper). Her desktop CSS uses it, so tools/sync-upstream.mjs turns it into this on copy.
//
//   node tools/oklch.mjs <file.css> ...   rewrites the files in place

import fs from 'node:fs';
import { fileURLToPath } from 'node:url';

const num = (s, pct = 1) => (s.endsWith('%') ? (parseFloat(s) / 100) * pct : parseFloat(s));

// one oklch(L C H [/ A]) as sRGB, gamut-clamped
export function oklchToRgb(l, c, h, a = 1) {
  const hr = (h * Math.PI) / 180;
  const A = c * Math.cos(hr);
  const B = c * Math.sin(hr);
  const l_ = (l + 0.3963377774 * A + 0.2158037573 * B) ** 3;
  const m_ = (l - 0.1055613458 * A - 0.0638541728 * B) ** 3;
  const s_ = (l - 0.0894841775 * A - 1.291485548 * B) ** 3;
  const lin = [
    4.0767416621 * l_ - 3.3077115913 * m_ + 0.2309699292 * s_,
    -1.2684380046 * l_ + 2.6097574011 * m_ - 0.3413193965 * s_,
    -0.0041960863 * l_ - 0.7034186147 * m_ + 1.707614701 * s_,
  ];
  const [r, g, b] = lin.map((x) => {
    const v = x <= 0.0031308 ? 12.92 * x : 1.055 * Math.max(0, x) ** (1 / 2.4) - 0.055;
    return Math.round(Math.min(1, Math.max(0, v)) * 255);
  });
  if (a >= 1) return `#${[r, g, b].map((v) => v.toString(16).padStart(2, '0')).join('')}`;
  return `rgba(${r}, ${g}, ${b}, ${+a.toFixed(3)})`;
}

// every oklch(...) in a stylesheet's text, rewritten
export function plainColors(css) {
  return css.replace(/oklch\(\s*([\d.]+%?)\s+([\d.]+%?)\s+([\d.]+)(?:deg)?\s*(?:\/\s*([\d.]+%?)\s*)?\)/g,
    (_, l, c, h, a) => oklchToRgb(num(l), num(c, 0.4), parseFloat(h), a === undefined ? 1 : num(a)));
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  for (const file of process.argv.slice(2)) {
    const before = fs.readFileSync(file, 'utf8');
    const after = plainColors(before);
    if (/oklch\(/.test(after)) throw new Error(`${file}: an oklch() this doesn't understand`);
    if (after !== before) fs.writeFileSync(file, after);
    console.log(`${file}: plain colors`);
  }
}

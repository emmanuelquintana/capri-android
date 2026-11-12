// Her icon mode, with more feelings (1.10.0): "que el modo ícono tenga más reacciones y expresiones
// cuando hablamos o no con Capri". Her mask's eyes and a little sign by it, on what's going on:
// - what she says: laughing (eyes shut happy, a bounce), smug (half a wink), surprised (round eyes, !),
//   looking away, cross (a shake, the anger mark), in love (hearts);
// - thinking while her AI answers (dots); music or a video playing (notes, a sway);
// - petted (a finger held on her), dizzy (dragged around a lot);
// - bored a while, sleepy at night or in battery saver (z z).
// All of it drawn on her own chip, in her own colors.

import { drawMask, COLORS } from '../capri/src/renderer/js/fx.js';
import { reducedMotion } from '../capri/src/renderer/js/util.js';
import { on } from './native.js';

const FACE_FEEL = { laugh: 'happy', grin: 'happy', smug: 'smug', wide: 'surprised', aside: 'aside', many: 'angry', love: 'love', calm: null, look: null };

export function iconFaces({ cfg }) {
  const dev = window.dev;
  const { capri, panel, chip } = dev || {};
  if (!capri || !chip || !panel) return;
  const inIcon = () => Boolean(cfg().icon) && capri.stowed && !chip.landing;

  let feel = null; // { kind, until, at }
  let sound = false;
  let thinking = false;
  let lastAction = Date.now();
  const set = (kind, ms = 2500) => {
    feel = kind ? { kind, until: performance.now() + ms, at: performance.now() } : null;
    lastAction = Date.now();
  };

  // what she says: her face for it
  const say = panel.say.bind(panel);
  panel.say = (msg, ...rest) => {
    const shown = say(msg, ...rest);
    if (shown !== false && inIcon()) {
      const kind = FACE_FEEL[msg?.face] ?? (msg?.kind === 'alert' ? 'surprised' : null);
      if (kind) set(kind, Math.min(9000, msg?.hold || 4000));
      else lastAction = Date.now();
    }
    return shown;
  };
  if (panel.setChatBusy) {
    const busy = panel.setChatBusy.bind(panel);
    panel.setChatBusy = (onNow, ...rest) => {
      thinking = Boolean(onNow);
      lastAction = Date.now();
      return busy(onNow, ...rest);
    };
  }
  on('phone:activity', (s = {}) => { sound = Boolean(s.sound); });
  on('capri:heard', () => { if (inIcon()) set('surprised', 1200); });

  // a finger on her: held still, petted; dragged around a lot, dizzy
  let down = null;
  window.addEventListener('mousedown', (e) => {
    if (!inIcon() || !chip.contains(e.clientX, e.clientY, 6)) return;
    down = { at: Date.now(), path: 0, x: e.clientX, y: e.clientY };
  }, true);
  window.addEventListener('mousemove', (e) => {
    if (!down || !e.buttons) return;
    down.path += Math.hypot(e.clientX - down.x, e.clientY - down.y);
    down.x = e.clientX;
    down.y = e.clientY;
    if (down.path > 2500) set('dizzy', 2600);
    else if (Date.now() - down.at > 500 && down.path < 20) set('happy', 1600);
  }, true);
  window.addEventListener('mouseup', () => { down = null; }, true);

  const current = (now) => {
    if (feel && now < feel.until) return feel.kind;
    feel = null;
    if (thinking) return 'think';
    if (sound) return 'music';
    const night = new Date().getHours() >= 23 || new Date().getHours() < 6;
    if (window.capriSaving || Date.now() - lastAction > (night ? 2 : 6) * 60000) return 'sleepy';
    return null;
  };

  // the signs, by her mask (the chip's canvas has room above it)
  const text = (ctx, s, x, y, size, color, alpha = 1) => {
    ctx.save();
    ctx.globalAlpha = alpha;
    ctx.font = `700 ${size}px sans-serif`;
    ctx.textAlign = 'center';
    ctx.fillStyle = COLORS.ink;
    ctx.fillText(s, x + 1.5, y + 1.5);
    ctx.fillStyle = color;
    ctx.fillText(s, x, y);
    ctx.restore();
  };
  const heart = (ctx, x, y, r, color, alpha) => {
    ctx.save();
    ctx.globalAlpha = alpha;
    ctx.translate(x, y);
    ctx.beginPath();
    ctx.moveTo(0, r * 0.9);
    ctx.bezierCurveTo(-r * 1.4, -r * 0.2, -r * 0.6, -r * 1.2, 0, -r * 0.4);
    ctx.bezierCurveTo(r * 0.6, -r * 1.2, r * 1.4, -r * 0.2, 0, r * 0.9);
    ctx.fillStyle = color;
    ctx.fill();
    ctx.restore();
  };

  const draw = chip.draw.bind(chip);
  chip.draw = (now, frame) => {
    if (!inIcon()) return draw(now, frame);
    const kind = current(now);
    if (!kind) return draw(now, frame);
    const ctx = chip.ctx;
    const calm = reducedMotion.matches;
    const t = now / 1000;
    let dx = 0;
    let dy = calm ? 0 : [0, -1.5, -3, -1.5][Math.floor(frame / 6) % 4];
    let eyes = [1, 1];
    let look = null;
    const top = chip.cy - 72 * chip.k; // above her mask
    const right = chip.cx + 46 * chip.k;
    switch (kind) {
      case 'happy':
        eyes = [0, 0];
        if (!calm) dy = [0, -4, -7, -4][frame % 4];
        break;
      case 'smug':
        eyes = [1, 0.3];
        break;
      case 'surprised':
        eyes = [1.3, 1.3];
        if (!calm && now - (feel?.at || 0) < 300) dy = -6;
        break;
      case 'aside':
        look = { x: chip.x - 400, y: chip.y + 40 };
        eyes = [0.8, 0.8];
        break;
      case 'angry':
        eyes = [0.55, 0.55];
        if (!calm) dx = frame % 2 ? -2 : 2;
        break;
      case 'love':
        eyes = [0, 0];
        break;
      case 'think':
        eyes = [0.85, 0.85];
        look = { x: chip.x + 300, y: chip.y - 400 };
        break;
      case 'music':
        eyes = [0.45, 0.45];
        if (!calm) dx = Math.sin(t * 4) * 4;
        break;
      case 'dizzy':
        eyes = frame % 4 < 2 ? [1, 0.4] : [0.4, 1];
        if (!calm) dx = Math.sin(t * 9) * 3;
        break;
      case 'sleepy':
        eyes = [0.1, 0.1];
        if (!calm) dy = Math.sin(t * 1.2) * 2;
        break;
      default:
    }
    drawMask(ctx, chip.cx + dx, chip.cy + dy, chip.k, { eyes, look });
    // the sign
    const rise = (period, offset = 0) => ((t + offset) % period) / period; // 0..1, going up
    switch (kind) {
      case 'surprised':
        text(ctx, '!', right, top + 6, 26, COLORS.red);
        break;
      case 'angry': {
        ctx.save();
        ctx.translate(right, top + 6);
        ctx.strokeStyle = COLORS.red;
        ctx.lineWidth = 3;
        for (let i = 0; i < 4; i++) {
          ctx.rotate(Math.PI / 2);
          ctx.beginPath();
          ctx.arc(7, 7, 5, Math.PI, Math.PI * 1.5);
          ctx.stroke();
        }
        ctx.restore();
        break;
      }
      case 'love':
        for (let i = 0; i < 3; i++) {
          const p = calm ? 0.5 : rise(1.8, i * 0.6);
          heart(ctx, chip.cx - 26 + i * 26, top + 20 - p * 34, 6 + i, COLORS.red, 1 - p * 0.8);
        }
        break;
      case 'think':
        for (let i = 0; i < 3; i++) {
          const lit = calm || Math.floor(t * 3) % 4 > i;
          ctx.beginPath();
          ctx.arc(right - 18 + i * 10, top + 6, 3.4, 0, Math.PI * 2);
          ctx.fillStyle = lit ? COLORS.green : 'rgba(255,244,246,0.3)';
          ctx.fill();
        }
        break;
      case 'music':
        for (let i = 0; i < 2; i++) {
          const p = calm ? 0.4 : rise(2.2, i * 1.1);
          text(ctx, i ? '♫' : '♪', chip.cx - 30 + i * 52 + Math.sin((t + i) * 3) * 4, top + 26 - p * 30, 18, i ? COLORS.blue : COLORS.green, 1 - p * 0.7);
        }
        break;
      case 'dizzy':
        for (let i = 0; i < 3; i++) {
          const a = t * 3 + (i * Math.PI * 2) / 3;
          text(ctx, '✦', chip.cx + Math.cos(a) * 30, top + 14 + Math.sin(a) * 7, 12, COLORS.paper);
        }
        break;
      case 'sleepy':
        for (let i = 0; i < 2; i++) {
          const p = calm ? 0.5 : rise(2.6, i * 1.3);
          text(ctx, i ? 'Z' : 'z', right - 6 + p * 12, top + 22 - p * 26, 12 + i * 5, COLORS.paper, 1 - p * 0.6);
        }
        break;
      default:
    }
  };
}

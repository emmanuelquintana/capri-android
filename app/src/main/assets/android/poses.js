// Poses her one picture can't bend into (assets/capri/poses/pose-<name>.png, whole drawings of
// her): lying on top of your screen watching you play (gamer mode, kicking her legs), sprawled on
// her back or hugging her knees when she's bored, leaning on the edge of the screen, curled up
// asleep, waving. While she's in one, her real self (the sprite) steps aside and stays put; a tap
// on the pose, a word to her, her chat or anything she has to do brings her back.
//
// Only in her own suit (an outfit isn't drawn in these poses), and never with reduced motion.

import { on } from './native.js';

const POSES = new URL('../../assets/capri/poses/', location.href).href;
// her height on screen (420 sprite px) times this: the size of each drawing (h: height, w: width)
const SIZE = {
  'prone-watch': { w: 0.92 }, 'prone-kick': { w: 0.92 }, 'bored-back': { w: 1.05 }, 'curl-sleep': { w: 0.62 },
  'hug-knees': { h: 0.62 }, lean: { h: 1 }, wave: { h: 1 },
};
const reduced = matchMedia('(prefers-reduced-motion: reduce)');

export function poses({ cfg, wardrobe }) {
  const dev = window.dev;
  const capri = dev?.capri;
  if (!capri) return null;
  const pics = {};
  const load = (name) => pics[name] || (pics[name] = new Promise((resolve) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => resolve(null);
    img.src = `${POSES}pose-${name}.png`;
  }));
  Object.keys(SIZE).forEach(load);

  let now = null; // { name, el, frames, timer, until, onEnd }
  let lastEnd = 0; // when you last tapped her out of one: a while before the next

  // (in gamer mode she's lying around in her corner already: that's fine too)
  const free = (lying = false) => !capri.hidden && !capri.stowed && !capri.dormant && !capri.busy && !capri.dragging
    && (!capri.act || lying) && (!capri.lounge || lying) && !dev.chat?.open && !cfg().icon && !reduced.matches
    && ['stand', 'sit', ...(lying ? ['down'] : [])].includes(capri.stance) && (!cfg().look?.outfit || cfg().look.outfit === 'default');

  // where it goes: on her feet, or along the top of the screen, or against the nearest side
  function place(el, img, name, at) {
    const s = capri.scale || 1;
    const H = 420 * s;
    const k = SIZE[name] || { h: 1 };
    const w = k.w ? H * k.w : (H * k.h * img.naturalWidth) / img.naturalHeight;
    const h = (w * img.naturalHeight) / img.naturalWidth;
    const W = innerWidth;
    let x = capri.pos.x - w / 2;
    let y = capri.pos.y - h;
    let flip = capri.facing < 0;
    if (at === 'top') {
      x = W - w - 12;
      y = 40;
      flip = false; // her head toward the middle
    } else if (name === 'lean') {
      const right = capri.pos.x > W / 2;
      x = right ? W - w : 0;
      flip = !right; // her back against the edge
    }
    x = Math.max(0, Math.min(W - w, x));
    Object.assign(el.style, { left: `${x}px`, top: `${y}px`, width: `${w}px`, height: `${h}px`, transform: flip ? 'scaleX(-1)' : '' });
  }

  // her colors (a recolored mane or mask), her aura and how see-through she is, as on her sprite
  function dress(el, img) {
    const sprite = document.getElementById('sprite');
    el.style.filter = sprite?.style.filter || '';
    el.style.setProperty('--pose-alpha', sprite?.style.opacity || '1');
    const painted = wardrobe?.paint?.(img);
    return painted ? painted.toDataURL() : img.src;
  }

  async function show(name, { frames = null, every = 900, at = null, hold = 30000, onEnd = null, lying = false } = {}) {
    if (now || !free(lying)) return false;
    const list = frames || [name];
    const imgs = (await Promise.all(list.map(load))).filter(Boolean);
    if (!imgs.length || now || !free(lying)) return false;
    const el = document.createElement('img');
    el.className = 'capri-pose';
    el.alt = '';
    el.draggable = false;
    const srcs = imgs.map((img) => dress(el, img));
    el.src = srcs[0];
    place(el, imgs[0], list[0], at);
    document.body.append(el);
    requestAnimationFrame(() => el.classList.add('is-in'));
    capri.busy = true;
    const sprite = document.getElementById('sprite');
    if (sprite) sprite.style.visibility = 'hidden';
    let i = 0;
    const timer = srcs.length > 1 ? setInterval(() => { i = (i + 1) % srcs.length; el.src = srcs[i]; }, every) : null;
    el.addEventListener('click', () => { end(); lastEnd = Date.now(); capri.wake?.(); });
    now = { name, el, timer, onEnd, until: hold ? setTimeout(() => end(), hold) : null };
    return true;
  }

  function end() {
    if (!now) return;
    const { el, timer, until, onEnd } = now;
    now = null;
    clearInterval(timer);
    clearTimeout(until);
    el.classList.remove('is-in');
    setTimeout(() => el.remove(), 260);
    const sprite = document.getElementById('sprite');
    if (sprite) sprite.style.visibility = '';
    capri.busy = false;
    onEnd?.();
  }

  // gamer mode: on top of your screen, watching you play; asleep: curled up
  setInterval(() => {
    if (document.hidden) return;
    const c = cfg();
    if (now?.name === 'gamer' && !c.gamer) return end();
    if (now?.name === 'curl-sleep' && capri.mode !== 'sleep') return end();
    if (now || Date.now() - lastEnd < 45000) return;
    if (c.gamer && !dev.panel?.current) {
      show('gamer', { frames: ['prone-watch', 'prone-watch', 'prone-kick'], every: 700, at: 'top', hold: 0, lying: true });
    } else if (capri.mode === 'sleep') {
      show('curl-sleep', { hold: 0, lying: true });
    }
  }, 4000);

  // anything for her to do: she's back
  const back = () => now && end();
  on('capri:heard', back);
  const open = dev.chat?.start?.bind(dev.chat);
  if (open) dev.chat.start = (...a) => { back(); return open(...a); };
  const perform = capri.perform.bind(capri);
  // (her moves on her own wait: she's lying there; a tap or a word brings her back first)
  capri.perform = (...a) => (now && now.name !== 'wave' ? Promise.resolve() : perform(...a));

  return {
    show,
    end,
    get active() { return now?.name || null; },
    // a wave, for a moment
    wave: () => show('wave', { hold: 1800 }),
    // bored: sprawled, hugging her knees or leaning on the edge, a while
    bored: (ms) => show(['bored-back', 'hug-knees', 'lean'][Math.floor(Math.random() * 3)], { hold: ms }),
  };
}

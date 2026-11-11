// Poses and animations her one picture can't bend into: whole drawings of her.
// - Poses (assets/capri/poses/pose-<name>.png): one drawing, or a few swapped (her kicking legs).
// - Animations (assets/capri/anims/<name>.png, a strip of cels cut by tools/fit-assets.py from the
//   sheets made with capri-prompts-1.8.0-parte3-animaciones.zip; anims.json says how many cels,
//   how fast, whether it loops, and if she stands, sits or lies in it): played cel by cel, like anime.
// When both are there the animation wins; when neither is, she does what she always did.
//
// What for: lying on top of your screen watching you play (gamer mode), asleep curled up (and
// stretching when she wakes), sprawled, hugging her knees or leaning on the edge when she's bored,
// waving when you call her, dancing, laughing, crying, stomping, in love, shy, thinking while her AI
// answers, hugging a battery while your phone charges, out of juice when it's low, eating, on her
// phone, a Sandevistan dash. Part 5 (capri-prompts-1.8.0-parte5-animaciones.zip): scanning your
// screen while her AI looks at it, a goodbye when you hide her, dizzy after a hard throw, bobbing to
// your music on its card, stretching with you on a break, yawning late at night, hacking when she
// opens something for you, surprised by a notification, selfies and air guitar now and then. While she's in one, her real self (the sprite) steps aside and stays
// put; a tap on it, a word to her, her chat or anything she has to do brings her back.
//
// Only in her own suit (an outfit isn't drawn in these), and never with reduced motion.

import { on } from './native.js';

const POSES = new URL('../../assets/capri/poses/', location.href).href;
const ANIMS = new URL('../../assets/capri/anims/', location.href).href;
// her height on screen (420 sprite px) times this: the size of each drawing (h: height, w: width)
const SIZE = {
  'prone-watch': { w: 0.92 }, 'prone-kick': { w: 0.92 }, 'bored-back': { w: 1.05 }, 'curl-sleep': { w: 0.62 },
  'hug-knees': { h: 0.62 }, lean: { h: 1 }, wave: { h: 1 },
};
const KIND_SIZE = { stand: { h: 1.04 }, sit: { h: 0.66 }, lie: { w: 0.95 }, curl: { w: 0.62 } };
const reduced = matchMedia('(prefers-reduced-motion: reduce)');
const pick = (list) => list[Math.floor(Math.random() * list.length)];

export function poses({ cfg, wardrobe }) {
  const dev = window.dev;
  const capri = dev?.capri;
  if (!capri) return null;
  const pics = {};
  const load = (src) => pics[src] || (pics[src] = new Promise((resolve) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => resolve(null);
    img.src = src;
  }));
  Object.keys(SIZE).forEach((name) => load(`${POSES}pose-${name}.png`));
  // the animations that are in
  let anims = {};
  const ready = fetch(`${ANIMS}anims.json`).then((r) => (r.ok ? r.json() : {})).catch(() => ({})).then((list) => {
    anims = list || {};
    Object.keys(anims).forEach((name) => load(`${ANIMS}${name}.png`));
  });
  const has = (name) => Boolean(anims[name]);

  let now = null; // { name, el, timer, until, onEnd, at }
  let lastEnd = 0; // when you last tapped her out of one: a while before the next

  // (in gamer mode she's lying around in her corner already, and while you chat she may think: fine too;
  // and something that just happened to her (`now`) cuts into whatever move she was making)
  const free = ({ lying = false, chat = false, now: right = false } = {}) => !capri.hidden && !capri.stowed && !capri.dormant && !capri.busy && !capri.dragging
    && (!capri.act || lying || right) && (!capri.lounge || lying) && (chat || !dev.chat?.open) && !cfg().icon && !reduced.matches
    && ['stand', 'sit', ...(lying ? ['down'] : [])].includes(capri.stance) && (!cfg().look?.outfit || cfg().look.outfit === 'default');

  // where it goes: on her feet, or along the top of the screen, or against the nearest side
  function place(el, fw, fh, k, name, at) {
    const s = capri.scale || 1;
    const H = 420 * s;
    const w = k.w ? H * k.w : (H * k.h * fw) / fh;
    const h = (w * fh) / fw;
    const W = innerWidth;
    let x = capri.pos.x - w / 2;
    let y = capri.pos.y - h;
    let flip = capri.facing < 0;
    if (at === 'top') {
      x = W - w - 12;
      y = 40;
      flip = false; // her head toward the middle
    } else if (name.startsWith('lean')) {
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

  function start(name, el, { hold, onEnd, at }) {
    document.body.append(el);
    requestAnimationFrame(() => el.classList.add('is-in'));
    capri.busy = true;
    const sprite = document.getElementById('sprite');
    if (sprite) sprite.style.visibility = 'hidden';
    el.addEventListener('click', () => { end(); lastEnd = Date.now(); capri.wake?.(); });
    now = { name, el, onEnd, at, since: Date.now(), timer: null, until: hold ? setTimeout(() => end(), hold) : null };
  }

  // an animation: its strip, cel by cel
  async function play(name, { hold = 0, times = 0, at = null, onEnd = null, lying = false, chat = false, now: right = false } = {}) {
    await ready;
    const a = anims[name];
    if (!a || now || !free({ lying, chat, now: right })) return false;
    const img = await load(`${ANIMS}${name}.png`);
    if (!img || now || !free({ lying, chat, now: right })) return false;
    const el = document.createElement('div');
    el.className = 'capri-pose capri-anim';
    el.style.backgroundImage = `url("${dress(el, img)}")`;
    el.style.backgroundSize = `${a.n * 100}% 100%`;
    place(el, a.w, a.h, KIND_SIZE[a.kind] || KIND_SIZE.stand, name, at);
    // once through (or `times` times), unless it loops: then for `hold` ms, or until something brings her back
    const runs = a.loop ? times : Math.max(1, times);
    const total = runs ? runs * a.n : 0;
    if (!hold && total) hold = Math.round((total * 1000) / a.fps) + 200;
    start(name, el, { hold, onEnd, at });
    let i = 0;
    const show = () => { el.style.backgroundPosition = `${(i * 100) / Math.max(1, a.n - 1)}% 0`; };
    show();
    now.timer = setInterval(() => {
      if (!a.loop && i >= a.n - 1) return; // (stays on its last cel)
      i = (i + 1) % a.n;
      show();
    }, 1000 / a.fps);
    return true;
  }

  // a pose: one drawing, or a few swapped every `every` ms
  async function pose(name, { frames = null, every = 900, at = null, hold = 30000, onEnd = null, lying = false } = {}) {
    if (now || !free({ lying })) return false;
    const list = frames || [name];
    const imgs = (await Promise.all(list.map((n) => load(`${POSES}pose-${n}.png`)))).filter(Boolean);
    if (!imgs.length || now || !free({ lying })) return false;
    const el = document.createElement('img');
    el.className = 'capri-pose';
    el.alt = '';
    el.draggable = false;
    const srcs = imgs.map((img) => dress(el, img));
    el.src = srcs[0];
    place(el, imgs[0].naturalWidth, imgs[0].naturalHeight, SIZE[list[0]] || { h: 1 }, list[0], at);
    start(name, el, { hold, onEnd, at });
    let i = 0;
    if (srcs.length > 1) now.timer = setInterval(() => { i = (i + 1) % srcs.length; el.src = srcs[i]; }, every);
    return true;
  }

  // the animation when it's in, else nothing (she does what she always did)
  const cue = (name, opts = {}) => (has(name) ? play(name, { now: true, ...opts }) : Promise.resolve(false));

  // the animation when it's in, else the pose
  const either = (anim, poseName, opts = {}, poseOpts = {}) => (has(anim) ? play(anim, opts) : pose(poseName, { ...opts, ...poseOpts }));

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

  // gamer mode: on top of your screen, watching you play (and now and then playing along);
  // asleep: curled up, breathing (and a stretch when she wakes)
  let gamerTurns = 0;
  setInterval(() => {
    if (document.hidden) return;
    const c = cfg();
    if (now?.name?.startsWith('gamer') && !c.gamer) return end();
    if ((now?.name === 'sleep' || now?.name === 'curl-sleep') && capri.mode !== 'sleep') {
      end();
      if (has('wake')) setTimeout(() => play('wake'), 50);
      return;
    }
    if (now || Date.now() - lastEnd < 45000) return;
    if (c.gamer && !dev.panel?.current) {
      gamerTurns += 1;
      if (has('gamer-play') && gamerTurns % 4 === 0) play('gamer-play', { at: 'top', hold: 20000, lying: true });
      else if (has('gamer-watch')) play('gamer-watch', { at: 'top', hold: 60000, lying: true });
      else pose('gamer', { frames: ['prone-watch', 'prone-watch', 'prone-kick'], every: 700, at: 'top', hold: 60000, lying: true });
    } else if (capri.mode === 'sleep') {
      either('sleep', 'curl-sleep', { hold: 0, lying: true });
    }
  }, 4000);

  // her chat: thinking while her AI answers
  const panel = dev.panel;
  if (panel?.setChatBusy) {
    const busy = panel.setChatBusy.bind(panel);
    panel.setChatBusy = (on, ...rest) => {
      if (on && has('think') && !now) play('think', { hold: 30000, chat: true });
      else if (!on && now?.name === 'think') end();
      return busy(on, ...rest);
    };
  }

  // thrown hard (or twice in a row): dizzy a moment, once she's back on her feet or a ledge
  const throws = [];
  const thrown = capri.onThrown;
  capri.onThrown = (info = {}) => {
    thrown?.(info);
    const t = Date.now();
    throws.push(t);
    while (throws.length && t - throws[0] > 120000) throws.shift();
    if (info.how !== 'leap' && throws.length < 3 && (throws.length === 2 || info.hard > 0.6)) {
      setTimeout(() => cue('dizzy', { hold: 3800 }), 700);
    }
  };

  // anything for her to do: she's back (not what she was just told to do)
  const back = () => now && Date.now() - now.since > 400 && end();
  on('capri:heard', back);
  const open = dev.chat?.start?.bind(dev.chat);
  if (open) dev.chat.start = (...a) => { back(); return open(...a); };
  const perform = capri.perform.bind(capri);
  // (her moves on her own wait: she's lying there; a tap or a word brings her back first)
  capri.perform = (...a) => (now && now.name !== 'wave' ? Promise.resolve() : perform(...a));

  return {
    play,
    pose,
    cue,
    end,
    has,
    ready,
    get active() { return now?.name || null; },
    // a wave, for a moment
    wave: () => either('wave', 'wave', { times: 3 }, { hold: 1800 }),
    // bored: sprawled, hugging her knees, leaning on the edge, on her phone… a while
    bored: (ms) => {
      const list = ['bored-tap', 'hug-rock', 'lean-nails', 'phone'].filter(has);
      if (list.length && Math.random() < 0.8) return play(pick(list), { hold: ms });
      return pose(pick(['bored-back', 'hug-knees', 'lean']), { hold: ms });
    },
    // a feeling (moods.js): the animation for it, a few seconds
    feel: (mood) => {
      const name = { angry: 'angry', sad: 'cry', love: has('shy') && Math.random() < 0.35 ? 'shy' : 'love', laugh: 'laugh' }[mood];
      return name && has(name) ? play(name, { hold: 3600 }) : Promise.resolve(false);
    },
  };
}

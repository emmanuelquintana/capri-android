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
// Never with reduced motion. With an outfit on she still does them (1.8.2: before, any outfit kept her
// from ever moving like this); they're drawn in her own suit, and the outfit is back when she's done.

import { on, send } from './native.js';

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
  // (each drawing is loaded when she does it, and let go after: 1.8.2, all 30 at once was too much memory
  // for some phones, whose WebView then kept restarting her page)
  // the animations that are in
  let anims = {};
  const ready = fetch(`${ANIMS}anims.json`).then((r) => (r.ok ? r.json() : {})).catch(() => ({})).then((list) => {
    anims = list || {};
  });
  const has = (name) => Boolean(anims[name]) && !animsOff();

  // (1.10.0) a phone that went down while she was in one of them: if it happens twice in a few
  // hours, she does them no more there for 3 days (her plain self instead), rather than going down
  // again and again. Marked when one starts, cleared when it ends: still marked at the next start of
  // her page means the last one never ended.
  const store = (() => { try { return window.localStorage; } catch { return null; } })();
  const read = (key) => { try { return JSON.parse(store?.getItem(key) || 'null'); } catch { return null; } };
  const write = (key, value) => { try { store?.setItem(key, JSON.stringify(value)); } catch {} };
  const H = 3600000;
  {
    const was = read('capri.animOn');
    if (was && Date.now() - (was.beat || was.at) < 30000) { // (her page back right away: the app went down, not you stopping her)
      const downs = [...(read('capri.animDowns') || []).filter((t) => Date.now() - t < 6 * H), Date.now()];
      write('capri.animDowns', downs);
      if (downs.length >= 2) {
        write('capri.animOff', Date.now());
        setTimeout(() => send('log', { level: 'W', text: `the phone went down twice during her animations (last: ${was.name}): plain her for 3 days` }), 0);
      }
    }
    write('capri.animOn', null);
  }
  const animsOff = () => Boolean(window.capriSaving) || Date.now() - (read('capri.animOff') || 0) < 72 * H;
  let now = null; // { name, el, timer, until, onEnd, at }

  // (1.10.0) while a drawing of her is on, her real self is hidden: it isn't drawn again and again
  // underneath (before, being `busy` it was redrawn up to 24 times a second on the GPU for nothing,
  // all through a dance or her music). What she does (behave) still runs, a few times a second.
  const interval = capri.interval.bind(capri);
  capri.interval = () => (now ? 250 : interval());
  const drawFrame = capri.drawFrame.bind(capri);
  capri.drawFrame = (t = performance.now()) => {
    if (!now) return drawFrame(t);
    capri.lastDraw = t;
    capri.animate(t); // (her moves go on, and end, as always; only the drawing is skipped)
    capri.place();
  };
  let lastEnd = 0; // when you last tapped her out of one: a while before the next

  // (in gamer mode she's lying around in her corner already, and while you chat she may think: fine too;
  // and something that just happened to her (`now`) cuts into whatever move she was making)
  const free = ({ lying = false, chat = false, now: right = false } = {}) => !capri.hidden && !capri.stowed && !capri.dormant && !capri.busy && !capri.dragging
    && (!capri.act || lying || right) && (!capri.lounge || lying) && (chat || !dev.chat?.open) && !cfg().icon && !reduced.matches
    && ['stand', 'sit', ...(lying ? ['down'] : [])].includes(capri.stance);

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

  // her aura and how see-through she is, as on her sprite
  function dress(el, img) {
    const sprite = document.getElementById('sprite');
    el.style.filter = sprite?.style.filter || '';
    el.style.setProperty('--pose-alpha', sprite?.style.opacity || '1');
    return wardrobe?.paint?.(img) || img; // (a canvas when her outfit is painted on it)
  }
  const url = (pic) => (pic.toDataURL ? pic.toDataURL() : pic.src);

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
    // (1.10.0) in an outfit, its own drawing of this animation when there is one (anims/<name>@<outfit>.png)
    const outfit = wardrobe?.look?.().outfit;
    const key = outfit && outfit !== 'default' && anims[`${name}@${outfit}`] ? `${name}@${outfit}` : name;
    const a = anims[key];
    if (!a || now || animsOff() || !free({ lying, chat, now: right })) return false;
    const img = await load(`${ANIMS}${key}.png`);
    if (!img || now || !free({ lying, chat, now: right })) return false;
    // (1.10.0) cel by cel on a small canvas of its own, its size on screen: before, the whole strip
    // was her background, scaled up to the screen's pixels (a dance: 8 cels, thousands of pixels wide)
    // and painted again at every cel, with her aura's filter on top. On many phones that ran the GPU
    // out of memory while she danced, listened to music or rested: she turned into a white box, then
    // the whole screen went white and the app stopped.
    const el = document.createElement('canvas');
    el.className = 'capri-pose capri-anim';
    const k = KIND_SIZE[a.kind] || KIND_SIZE.stand;
    // (`body`: how much of the strip's height is her when she just stands, for the ones she jumps or reaches up in)
    place(el, a.w, a.h, a.body && k.h ? { h: k.h / a.body } : k, name, at);
    const sharp = Math.min(window.devicePixelRatio || 1, window.capriLite || window.capriFlat ? 1.25 : 2);
    el.width = Math.max(1, Math.round(parseFloat(el.style.width) * sharp));
    el.height = Math.max(1, Math.round(parseFloat(el.style.height) * sharp));
    const g = el.getContext('2d');
    if (!g) return false;
    const pic = dress(el, img);
    if (window.capriLite || window.capriFlat) el.style.filter = ''; // (no aura's filter on a weak GPU)
    // once through (or `times` times), unless it loops: then for `hold` ms, or until something brings her back
    const runs = a.loop ? times : Math.max(1, times);
    const total = runs ? runs * a.n : 0;
    if (!hold && total) hold = Math.round((total * 1000) / a.fps) + 200;
    start(name, el, { hold, onEnd, at });
    write('capri.animOn', { name, at: Date.now() });
    const fw = img.naturalWidth / a.n;
    const fh = img.naturalHeight;
    let i = 0;
    const show = () => {
      g.clearRect(0, 0, el.width, el.height);
      g.drawImage(pic, i * fw, 0, fw, fh, 0, 0, el.width, el.height);
    };
    show();
    let beat = Date.now();
    now.timer = setInterval(() => {
      if (document.hidden) return;
      if (Date.now() - beat > 5000) { beat = Date.now(); write('capri.animOn', { name, at: beat, beat }); }
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
    const srcs = imgs.map((img) => url(dress(el, img)));
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
    write('capri.animOn', null);
    for (const src of Object.keys(pics)) delete pics[src]; // (let its pictures go)
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

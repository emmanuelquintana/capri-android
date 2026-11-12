// The finger as her mouse.
//
// Her renderer was written for a mouse: it moves over her (hover), presses on her (drag, throw),
// right-clicks her (her menu). On a phone:
//   - tap          -> the WebView's own mouse events and click (Chromium sends them after a tap)
//   - drag         -> mousedown where the finger came down, then mousemove / mouseup (made here:
//                     Chromium sends no mouse events for a finger that moves). Right away, or
//                     after holding her still a while: either way she's grabbed, never her menu.
//   - long press   -> held still, a buzz and a ring under the finger; let go without moving and
//                     her quick menu opens. (Chromium's own long press, its contextmenu, is
//                     swallowed on her: it opened the menu under a finger that meant to drag her.)
//   - two fingers  -> mouse moves with no button: the pointer passing over her, to pet her mane
// Kotlin only lets touches reach the page where she is (TouchPads.kt), so the rest of the phone
// stays yours: this file tells it where that is, every frame something moved. As a ghost (or in
// gamer mode) touches go through her, all but her face: a handle to move her or open her menu.

import { send } from './native.js';

const SLOP = 10; // px a finger moves before it's a drag (Chromium's tap stops there too)
const RECENT_MS = 4000; // a touch this recent still counts as "the pointer is here"
const HOLD_MS = 420; // held still this long: let go for her menu
const HANDLE = 34; // half the side of her face's handle as a ghost, in px

export class Touch {
  constructor() {
    this.last = null; // { x, y, at }
    this.drag = null; // { x, y, target, started }
    this.interactive = false;
    this.regions = '';
    this.bind();
    this.watch();
  }

  setInteractive(on) {
    this.interactive = on;
  }

  lastPoint() {
    return this.last && { x: this.last.x, y: this.last.y };
  }

  // host.cursor(): where the pointer is, as main.js answers it
  cursor() {
    if (!this.last) return null;
    return { x: this.last.x, y: this.last.y, inside: performance.now() - this.last.at < RECENT_MS, display: 1 };
  }

  mouse(type, x, y, target, buttons = 0) {
    const el = target || document.elementFromPoint(x, y) || document.body;
    el.dispatchEvent(new MouseEvent(type, {
      bubbles: true, cancelable: true, composed: true, view: window,
      clientX: x, clientY: y, screenX: x, screenY: y, button: 0, buttons,
    }));
  }

  // As a ghost, the bit of her you can still hold: a square on her face.
  handle() {
    const capri = window.dev?.capri;
    if (!this.ghost || !capri || capri.hidden || capri.stowed || capri.p?.alpha < 0.5) return null;
    try {
      const h = capri.head();
      const r = Math.max(HANDLE, 26 * (capri.scale || 1));
      return { x: h.x, y: h.y, rect: [h.x - r, h.y - r, h.x + r, h.y + r] };
    } catch {
      return null;
    }
  }

  ring(p) {
    if (!this.ringEl) {
      this.ringEl = document.createElement('div');
      this.ringEl.className = 'hold-ring';
      document.body.append(this.ringEl);
    }
    const el = this.ringEl;
    el.style.left = `${Math.round(p.x)}px`;
    el.style.top = `${Math.round(p.y)}px`;
    el.classList.remove('is-on');
    void el.offsetWidth; // the animation again
    el.classList.add('is-on');
  }

  unring() {
    this.ringEl?.classList.remove('is-on');
  }

  bind() {
    const point = (t) => ({ x: t.clientX, y: t.clientY });
    // Chromium's long press on her, a right-click: she has her own (below)
    window.addEventListener('contextmenu', (e) => {
      if (!this.ours || performance.now() - this.ours > 2500) return;
      if (e.cancelable) e.preventDefault();
      e.stopImmediatePropagation();
    }, true);

    window.addEventListener('touchstart', (e) => {
      const p = point(e.touches[0]);
      this.last = { ...p, at: performance.now() };
      clearTimeout(this.holdTimer);
      this.unring();
      if (e.touches.length >= 2) {
        // two fingers on her: a pointer passing over, no button (petting)
        this.drag = null;
        this.pet = true;
        return;
      }
      this.pet = false;
      this.ours = 0;
      // on her panel or her menu, the finger scrolls and taps them as any page: nothing to make up
      if (e.target.closest?.('#panel, .cmenu, .cmenu-shade, .anote, .wardrobe, .capri-pose')) {
        this.drag = null;
        return;
      }
      const capri = window.dev?.capri;
      // a ghost, held by the face: she's pressed where she really is (her face), the finger's
      // offset kept so she doesn't jump to it
      const h = this.handle();
      const inHandle = h && p.x >= h.rect[0] && p.x <= h.rect[2] && p.y >= h.rect[1] && p.y <= h.rect[3];
      const off = inHandle ? { x: h.x - p.x, y: h.y - p.y } : { x: 0, y: 0 };
      const at = { x: p.x + off.x, y: p.y + off.y };
      // the pointer arrives over her first, as a mouse would
      this.mouse('mousemove', at.x, at.y);
      const target = document.elementFromPoint(at.x, at.y);
      let onHer = false;
      try {
        onHer = Boolean(inHandle || (capri && !capri.hidden && capri.hit(at.x, at.y)));
      } catch {}
      const d = { ...p, at, off, target, started: false, held: false, onHer };
      this.drag = d;
      if (!onHer) return;
      this.ours = performance.now();
      this.holdTimer = setTimeout(() => {
        if (this.drag !== d || d.started) return;
        d.held = true;
        this.ring(this.last || p);
        send('overlay:haptic');
      }, HOLD_MS);
    }, { passive: true });

    window.addEventListener('touchmove', (e) => {
      const p = point(e.touches[0]);
      this.last = { ...p, at: performance.now() };
      if (this.pet) {
        const a = e.touches[0], b = e.touches[1] || a;
        this.mouse('mousemove', (a.clientX + b.clientX) / 2, (a.clientY + b.clientY) / 2);
        if (e.cancelable) e.preventDefault();
        return;
      }
      const d = this.drag;
      if (!d) return;
      if (d.onHer) this.ours = performance.now();
      if (!d.started && Math.hypot(p.x - d.x, p.y - d.y) < SLOP) return;
      if (!d.started) {
        d.started = true;
        clearTimeout(this.holdTimer);
        this.unring();
        this.mouse('mousedown', d.at.x, d.at.y, d.target, 1);
      }
      this.mouse('mousemove', p.x + d.off.x, p.y + d.off.y, null, 1);
      if (e.cancelable) e.preventDefault();
    }, { passive: false });

    const end = (e) => {
      const d = this.drag;
      this.drag = null;
      clearTimeout(this.holdTimer);
      this.unring();
      if (e.touches?.length === 0) this.pet = false;
      if (!d) return;
      if (d.onHer) this.ours = performance.now();
      if (d.held && !d.started) {
        // held still and let go: her menu, where the finger was
        if (e.type === 'touchend') window.host?.openMenu?.();
        if (e.cancelable) e.preventDefault();
        return;
      }
      if (!d.started) {
        // a tap on her face as a ghost: Chromium's own click would land on what's under it, not her
        if (d.off.x || d.off.y) {
          this.mouse('mousedown', d.at.x, d.at.y, d.target, 1);
          this.mouse('mouseup', d.at.x, d.at.y, null, 0);
          if (e.cancelable) e.preventDefault();
        }
        return; // a tap: Chromium's own mousedown, mouseup and click
      }
      const t = e.changedTouches[0];
      this.mouse('mouseup', t.clientX + d.off.x, t.clientY + d.off.y, null, 0);
      if (e.cancelable) e.preventDefault();
    };
    window.addEventListener('touchend', end, { passive: false });
    window.addEventListener('touchcancel', end, { passive: false });
  }

  // Her body itself, not her whole drawing's square: that square is much bigger than her (room for
  // her poses, turned when she leans), and touches in its empty corners would land on nothing,
  // neither on her nor on the app under her. Three bands (mane, waist, legs), each as wide as
  // what of her is really there, found with her own hit test. While she's held, or can't be
  // touched (mid-teleport, see-through), the square or nothing.
  body(r) {
    const capri = window.dev?.capri;
    if (!r) return [];
    if (!capri) return [r];
    if (capri.hidden || capri.p?.alpha < 0.5) return [];
    if (this.drag || this.pet || capri.dragging) return [r];
    try {
      return this.bands(r, 14);
    } catch {
      return [r];
    }
  }

  // Her body as three bands (mane, waist, legs), each as wide as what of her is really there.
  bands(r, pad) {
    const capri = window.dev?.capri;
    const [l, t, rr, b] = r;
    const step = 12, bands = 3;
    const h = (b - t) / bands;
    const out = [];
    for (let i = 0; i < bands; i++) {
      let x1 = Infinity, y1 = Infinity, x2 = -Infinity, y2 = -Infinity;
      for (let y = t + i * h; y < t + (i + 1) * h; y += step) {
        for (let x = l; x < rr; x += step) {
          if (!capri.hit(x, y)) continue;
          if (x < x1) x1 = x;
          if (x > x2) x2 = x;
          if (y < y1) y1 = y;
          if (y > y2) y2 = y;
        }
      }
      if (x1 <= x2) out.push([Math.max(l, x1 - pad), Math.max(t, y1 - pad), Math.min(rr, x2 + pad), Math.min(b, y2 + pad)].map((v) => Math.round(v / 8) * 8));
    }
    return out;
  }

  // Where she is on the screen right now (her body, her panel, her chip, her mask, her notes, a
  // menu): left out of a screenshot she looks at, so she never comments on herself or on her own
  // words. In CSS px, [left, top, right, bottom].
  selfRects() {
    const box = (el) => {
      if (!el || el.hidden || el.closest('[hidden]')) return null;
      const r = el.getBoundingClientRect();
      return r.width < 2 || r.height < 2 ? null : [r.left - 6, r.top - 6, r.right + 6, r.bottom + 6];
    };
    const capri = window.dev?.capri;
    const sprite = box(document.getElementById('sprite'));
    let body = [];
    if (capri && !capri.hidden && !(capri.p?.alpha < 0.2) && sprite) {
      try {
        body = this.bands(sprite, 26);
      } catch {
        body = [sprite];
      }
    }
    return [
      ...body,
      box(document.getElementById('panel')),
      box(document.getElementById('chip')),
      box(document.getElementById('mask')),
      ...[...document.querySelectorAll('.anote:not([hidden]), .cmenu:not([hidden])')].map(box),
    ].filter(Boolean);
  }

  // Where touches should reach her, sent to Kotlin when it changes: her body, her panel, her biochip.
  watch() {
    const rectOf = (el) => {
      if (!el || el.hidden || el.closest('[hidden]')) return null;
      const r = el.getBoundingClientRect();
      if (r.width < 2 || r.height < 2) return null;
      // on a 16 px grid, a little wider than her: the window over her moves less often
      const q = 16;
      return [Math.floor(r.left / q) * q - q, Math.floor(r.top / q) * q - q, Math.ceil(r.right / q) * q + q, Math.ceil(r.bottom / q) * q + q];
    };
    // every frame while a finger is on her (the pads follow it), else ten times a second: her own
    // moves are slow next to that, and the pads are 16 px wider than her
    const tick = () => {
      if (this.drag || this.pet) requestAnimationFrame(tick);
      else setTimeout(() => requestAnimationFrame(tick), 90);
      // a ghost: nothing of her stops your touches but her face (and her panel and her menu, to talk and to undo it)
      const ghost = this.ghost;
      const pose = document.querySelector('.capri-pose');
      const handle = ghost && !pose && this.handle();
      const list = [
        // in a pose (poses.js) she's that drawing, not her sprite
        ...(pose ? [rectOf(pose)] : ghost ? [] : this.body(rectOf(document.getElementById('sprite')))),
        // a ghost keeps her face to hold her by (to move her, or for her menu)
        handle ? handle.rect.map((v) => Math.round(v / 8) * 8) : null,
        rectOf(document.getElementById('panel')),
        rectOf(document.getElementById('chip')),
        ghost || pose ? null : rectOf(document.getElementById('mask')),
        ...[...document.querySelectorAll('.cmenu:not([hidden]), .wardrobe:not([hidden])')].map(rectOf),
        // a menu up: the whole (dimmed) screen is hers, a tap beside it closes it
        ...[...document.querySelectorAll('.cmenu-shade:not([hidden])')].map(rectOf),
        // (1.9.0: as a ghost only she lets touches through; her cards too only if you chose it)
        ...(ghost && this.ghostCards ? [] : [...document.querySelectorAll('.anote:not([hidden])')].map(rectOf)),
      ].filter(Boolean);
      // (a finger already on her keeps her: Android sends the whole gesture to where it began)
      const all = list;
      const key = JSON.stringify(all);
      if (key === this.regions) return;
      this.regions = key;
      send('touch:regions', { rects: all, dpr: window.screenDpr?.() || devicePixelRatio }); // (the screen's real one, also in modo ligero: compat.js)
    };
    requestAnimationFrame(tick);
  }
}

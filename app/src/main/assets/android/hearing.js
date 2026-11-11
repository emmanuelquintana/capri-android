// She's listening to you (1.9.0): "cuando hablo con Capri se apaga y enciende el micro, añade una
// pequeña animación como que te escucha". Android's recognizer comes and goes between phrases (its
// mic dot with it), so while you're talking to her, her own sign says she's all ears: little sound
// bars by her head, bouncing while you speak, and her listening animation when it's drawn
// (anims/listen.png).
//
// When: after her name alone (what you say next is for her, 10 s), and while you speak right after
// it (Ears.kt: `ears:speech`, with `awake`). The TV talking doesn't count.

import { on } from './native.js';

export function hearing() {
  const dev = window.dev;
  const capri = dev?.capri;
  if (!capri) return null;
  const el = document.createElement('div');
  el.className = 'capri-ears';
  el.setAttribute('aria-hidden', 'true');
  el.innerHTML = '<i></i><i></i><i></i><i></i>';
  el.hidden = true;
  document.body.append(el);
  let until = 0;
  let frame = 0;

  // by her head, on the side she faces
  function follow() {
    if (el.hidden) return;
    try {
      const h = capri.head();
      const s = capri.scale || 0.6;
      const side = capri.facing < 0 ? 1 : -1;
      el.style.left = `${Math.round(h.x + side * 70 * s - 14)}px`;
      el.style.top = `${Math.round(h.y - 40 * s)}px`;
    } catch {}
    if (Date.now() > until) return off();
    frame = requestAnimationFrame(follow);
  }

  function show(ms) {
    if (capri.hidden || capri.stowed) return;
    until = Math.max(until, Date.now() + ms);
    if (el.hidden) {
      el.hidden = false;
      cancelAnimationFrame(frame);
      follow();
      if (dev.poses?.has('listen') && !dev.poses.active) dev.poses.cue('listen', { hold: Math.min(ms, 8000) });
    }
  }
  function off() {
    el.hidden = true;
    el.classList.remove('is-talking');
    until = 0;
    cancelAnimationFrame(frame);
    if (dev.poses?.active === 'listen') dev.poses.end();
  }

  // you're speaking (to her): the bars bounce
  on('ears:speech', ({ on: speaking, awake } = {}) => {
    if (speaking && (awake || !el.hidden)) {
      show(8000);
      el.classList.add('is-talking');
    } else if (!speaking) el.classList.remove('is-talking');
  });

  return {
    on: show,
    off,
  };
}

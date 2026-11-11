// Her reactions by herself to what your phone is doing (1.9.0).
//
// "Cuando escuchas música o cuando juego o cuando veo TikToks no veo que reaccione": before, she only
// did her music animation for the music card (with notification access) and her gamer ones in gamer
// mode. Now GameWatch.kt tells her page every few seconds (`phone:activity`):
// - `sound`: something is playing (music, a video). No permission needed.
// - `kind`: what kind of app is in front (game, video, music, social), only if "Acceso de uso" was
//   already given to her (Ajustes › Modo gamer), never which app.
// And she does something with it, now and then (how often follows how lively she is):
// bobbing her head or dancing to music, a snack while you watch videos, on her own phone while you
// scroll, lying on top of the screen watching your game.

import { on } from './native.js';

const pick = (list) => list[Math.floor(Math.random() * list.length)];
// ms between two of her reactions, by how lively she is (the app's Settings)
const EVERY = { calm: 240000, normal: 110000, restless: 55000 };
const FIRST = 2500; // the music has to keep playing this long before she reacts (not a notification's ding)

export function reactions({ cfg }) {
  const dev = window.dev;
  if (!dev?.capri) return;
  let state = { sound: false, kind: '' };
  let soundSince = 0;
  let last = 0;

  on('phone:activity', (s = {}) => {
    const sound = Boolean(s.sound);
    if (sound && !state.sound) soundSince = Date.now();
    state = { sound, kind: String(s.kind || '') };
    setTimeout(tick, sound ? FIRST + 100 : 300);
  });

  // what to do now, if anything
  function choice() {
    const { sound, kind } = state;
    const poses = dev.poses;
    const has = (n) => poses?.has(n);
    if (kind === 'game') return has('gamer-watch') && { name: pick(['gamer-watch', 'gamer-watch', 'gamer-play'].filter(has)), at: 'top', hold: 25000, lying: true };
    if (kind === 'video') return { name: pick(['eat', 'eat', 'laugh', 'phone'].filter(has)), hold: 18000 };
    if (sound && Date.now() - soundSince >= FIRST) return { name: pick(['music', 'music', 'dance', 'guitar'].filter(has)), hold: 20000 };
    if (kind === 'social' || kind === 'music') return { name: pick(['phone', 'music'].filter(has)), hold: 15000 };
    return null;
  }

  function tick() {
    const c = cfg();
    if (document.hidden || c.gamer || c.icon || c.visible === false) return; // (gamer mode has its own: poses.js)
    const poses = dev.poses;
    if (!poses || poses.active || dev.chat?.open || dev.panel?.current) return;
    if (Date.now() - last < (EVERY[c.lively] || EVERY.normal)) return;
    const next = choice();
    if (!next?.name) return;
    const { name, ...opts } = next;
    poses.cue(name, opts).then((done) => { if (done) last = Date.now(); });
  }
  setInterval(tick, 8000);
}

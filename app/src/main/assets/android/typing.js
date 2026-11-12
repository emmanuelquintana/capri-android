// While you type (1.10.0): "a la hora de escribir, Capri se acueste arriba del teclado, y dentro de
// juegos se siente con su celular en las manos". The keyboard up in another app: she lies along its
// top edge (her floor is already right above it); in a game (gamer mode), she sits with her phone in
// her hands. Drawn when the animations are in (assets/capri/anims: `keyboard`, `type-phone`; until
// then her nearest ones: lying bored, on her phone). The keyboard down: she gets up.

import { on } from './native.js';

export function typing({ cfg }) {
  const dev = window.dev;
  const capri = dev?.capri;
  const poses = dev?.poses;
  if (!capri || !poses) return;
  let mine = null; // the animation that's on because you're typing
  let timer = 0;

  const first = (...names) => names.find((n) => poses.has(n));
  on('ime:change', ({ height = 0 } = {}) => {
    clearTimeout(timer);
    const up = Number(height) > 0;
    if (!up) {
      if (mine && poses.active === mine) poses.end();
      mine = null;
      return;
    }
    // (her own chat has the keyboard: she's talking with you, not lying on it)
    timer = setTimeout(() => {
      if (dev.chat?.open || mine || poses.active || cfg().icon) return;
      const gamer = Boolean(cfg().gamer);
      const name = gamer ? first('type-phone', 'phone') : first('keyboard', 'bored-tap');
      if (!name) return;
      poses.play(name, { hold: 120000, lying: true, ...(gamer ? { at: 'top' } : {}) }).then((ok) => { if (ok) mine = name; });
    }, 700);
  });
}

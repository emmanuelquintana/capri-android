// While you type (1.10.0): "a la hora de escribir, Capri se acueste arriba del teclado, y dentro de
// juegos se siente con su celular en las manos". The keyboard up in another app: she lies along its
// top edge (her floor is already right above it); in a game (gamer mode), she sits with her phone in
// her hands. Drawn when the animations are in (assets/capri/anims: `keyboard`, `type-phone`; until
// then her nearest ones: lying bored, on her phone). The keyboard down: she gets up.

import { on, send } from './native.js';

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
      // (1.12.0, Emma: "no veo que Capri se acueste en mi teclado") why not, in her log
      // (whatever she was doing a while, she leaves it for your keyboard: only her sleep stays)
      if (!dev.chat?.open && !mine && poses.active && !/sleep|night|nap/.test(poses.active)) poses.end();
      const why = dev.chat?.open ? 'her chat is open' : mine ? '' : poses.active ? `busy with ${poses.active}` : cfg().icon ? 'she is an icon' : '';
      if (why) return void send('log', { level: 'I', text: `keyboard up (${height}): not lying, ${why}` });
      if (mine) return;
      const gamer = Boolean(cfg().gamer);
      const name = gamer ? first('phone') : first('keyboard', 'bored-tap');
      if (!name) return void send('log', { level: 'I', text: 'keyboard up: no keyboard animation' });
      poses.play(name, { hold: 120000, lying: true, ...(gamer ? { at: 'top' } : {}) }).then((ok) => {
        if (ok) mine = name;
        const c = capri;
        send('log', { level: 'I', text: `keyboard up (${height}): ${name} ${ok ? 'on' : `refused (stance ${c.stance}, hidden ${!!c.hidden}, busy ${!!c.busy}, act ${!!c.act}, lounge ${!!c.lounge}, dormant ${!!c.dormant})`}` });
      });
    }, 700);
  });
}

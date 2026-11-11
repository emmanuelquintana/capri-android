// Your classes and your calendar (1.9.0, Agenda.kt): when her reminder goes out and she's on your
// screen, she says it herself too, in her bubble, with a little jump. The phone already got her
// notification; nothing here goes anywhere else (no AI, no network).

import { on } from './native.js';

export function agenda() {
  const dev = window.dev;
  const { panel, capri } = dev || {};
  if (!panel || !capri) return null;
  on('agenda:remind', ({ kind, title, text } = {}) => {
    if (!title || capri.hidden || capri.stowed || capri.dormant) return; // (her notification already went out)
    capri.wake?.();
    const anim = kind === 'class' ? 'wave' : 'listen';
    if (dev.poses?.has(anim)) dev.poses.play(anim, { times: 2 });
    else capri.perform?.(kind === 'class' ? 'victory' : 'alarm', capri.towardCenter?.() || 1);
    panel.say({ kind: 'info', face: kind === 'class' ? 'wide' : 'look', line: `${title}\n${text || ''}`.trim(), hold: 10000 });
  });
  return {};
}

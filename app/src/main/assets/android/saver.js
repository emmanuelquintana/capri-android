// Battery saver (1.10.0, optional, Tú › Cuida de ti): "si la batería baja hasta 20/15 % las funciones
// que más batería consumen se desactivan solas". Below your level and not charging, until you charge
// (or it's 5 % above it again):
// - her ears rest (Ears.kt), her live look at your screen stops (Eyes.kt), her app watch rests (GameWatch.kt);
// - her animations wait, she's drawn less often and her reactions to your videos and music rest.
// She says so when it starts and when it ends.

import { on, send } from './native.js';

const LINES = {
  es: { on: 'Batería al {p} %: modo ahorro. Dejo de escuchar y me muevo menos hasta que me cargues.', off: 'Ya hay batería. Salgo del modo ahorro.' },
  en: { on: 'Battery at {p}%: saver mode. I stop listening and move less until you charge me.', off: 'Battery’s back. Leaving saver mode.' },
  fr: { on: 'Batterie à {p} % : mode économie. J’arrête d’écouter et je bouge moins jusqu’à ce que tu me charges.', off: 'La batterie est revenue. Je quitte le mode économie.' },
  pt: { on: 'Bateria em {p}%: modo economia. Paro de escutar e me mexo menos até você me carregar.', off: 'A bateria voltou. Saindo do modo economia.' },
  ru: { on: 'Заряд {p}%: режим экономии. Я перестаю слушать и меньше двигаюсь, пока ты меня не зарядишь.', off: 'Заряд есть. Выхожу из режима экономии.' },
  vi: { on: 'Pin còn {p}%: chế độ tiết kiệm. Mình ngừng nghe và ít cử động cho đến khi bạn sạc cho mình.', off: 'Pin đã có lại. Thoát chế độ tiết kiệm.' },
  ja: { on: 'バッテリー{p}%：節約モード。充電してくれるまで、聞くのをやめて動きも減らすね。', off: 'バッテリー回復。節約モード終了。' },
  de: { on: 'Akku bei {p} %: Sparmodus. Ich höre nicht mehr zu und bewege mich weniger, bis du mich lädst.', off: 'Akku ist wieder da. Sparmodus beendet.' },
  it: { on: 'Batteria al {p}%: modalità risparmio. Smetto di ascoltare e mi muovo meno finché non mi ricarichi.', off: 'La batteria è tornata. Esco dalla modalità risparmio.' },
};

export function saver({ cfg, lang, setConfig }) {
  const dev = window.dev;
  const capri = dev?.capri;
  if (!capri) return null;
  let saving = false;
  let last = null;

  const level = () => [15, 20, 30].includes(Number(cfg().saverAt)) ? Number(cfg().saverAt) : 20;
  const set = (onNow, p) => {
    if (onNow === saving) return;
    saving = onNow;
    window.capriSaving = onNow;
    send('saver:set', { on: onNow });
    document.documentElement.classList.toggle('is-saving', onNow);
    if (onNow) {
      dev.poses?.end?.();
      if (cfg().watchScreen) setConfig({ watchScreen: false });
    }
    const t = LINES[lang()] || LINES.en;
    dev.panel?.say({ kind: 'info', face: onNow ? 'aside' : 'laugh', line: (onNow ? t.on : t.off).replace('{p}', p ?? ''), hold: 6000 });
  };
  const check = () => {
    const b = last;
    if (!b) return;
    if (!cfg().saver) return set(false);
    const p = Math.round(b.percent);
    if (b.charging || p >= level() + 5) set(false, p);
    else if (p <= level()) set(true, p);
  };
  on('battery:update', (b) => {
    if (!b || typeof b.percent !== 'number') return;
    last = b;
    check();
  });
  send('battery:now');

  // (1.12.0) the phone warm (Android's thermal state: moderate or worse): drawn half as often too, until it cools
  let hot = false;
  on('phone:hot', (h) => {
    const now = Number(h?.level) >= 3;
    if (now === hot) return;
    hot = now;
    window.capriHot = now;
    document.documentElement.classList.toggle('is-hot', now);
  });

  // drawn half as often while saving or warm; a modest phone (modo ligero) a third less often
  const interval = capri.interval.bind(capri);
  capri.interval = () => interval() * (saving || hot ? 2 : window.capriLite ? 1.5 : 1);

  return { check, get on() { return saving; } };
}

// An app uninstalled (1.10.0): she fires her beam at it and it falls apart in pixels.
// "Al eliminar una aplicación, Capri lance su rayo desintegrando la app" (CapriService tells her
// page, `phone:removed`). Its name comes from the list of your apps kept here while it was still
// installed. (A deleted message can't be seen: Android doesn't tell other apps about it.)

import { on, call } from './native.js';

const LINES = {
  es: ['Adiós, {app}. 💥', '{app}… desintegrada.', 'Una app menos. De nada.', 'Borrada del mapa: {app}.'],
  en: ['Bye, {app}. 💥', '{app}… disintegrated.', 'One app less. You’re welcome.', 'Wiped off the map: {app}.'],
  fr: ['Adieu, {app}. 💥', '{app}… désintégrée.', 'Une appli de moins. De rien.', 'Rayée de la carte : {app}.'],
  pt: ['Tchau, {app}. 💥', '{app}… desintegrado.', 'Um app a menos. De nada.', 'Apagado do mapa: {app}.'],
  ru: ['Прощай, {app}. 💥', '{app}… распылено.', 'Одним приложением меньше. Не за что.', 'Стёрто с карты: {app}.'],
};
const pick = (list) => list[Math.floor(Math.random() * list.length)];

export function laser({ lang, cfg }) {
  const dev = window.dev;
  const capri = dev?.capri;
  if (!capri) return;
  const names = new Map();
  const refresh = () => call('apps:launchables').then((list) => {
    for (const a of list || []) if (a?.id) names.set(a.id, a.name);
  }).catch(() => {});
  refresh();
  setInterval(() => { if (!document.hidden) refresh(); }, 10 * 60 * 1000);

  on('phone:removed', ({ pkg } = {}) => {
    if (!pkg || capri.hidden || capri.stowed || cfg().icon) return;
    const name = names.get(pkg) || String(pkg).split('.').pop().replace(/^\w/, (c) => c.toUpperCase());
    names.delete(pkg);
    fire(name);
  });

  function fire(name) {
    // the app, as a tile, on the side of the screen away from her
    const left = capri.pos.x > innerWidth / 2;
    const x = left ? innerWidth * 0.28 : innerWidth * 0.72;
    const y = Math.max(120, Math.min(innerHeight * 0.45, capri.pos.y - 180));
    const tile = document.createElement('div');
    tile.className = 'capri-doomed';
    tile.style.left = `${x - 38}px`;
    tile.style.top = `${y - 38}px`;
    const icon = document.createElement('b');
    icon.textContent = [...name.trim()][0]?.toUpperCase() || '?';
    const label = document.createElement('span');
    label.textContent = name;
    tile.append(icon, label);
    document.body.append(tile);
    requestAnimationFrame(() => tile.classList.add('is-in'));

    const hit = () => {
      tile.classList.add('is-hit');
      const fx = capri.fx;
      fx?.burst?.(x, y, { count: 26, power: 0.9, spread: 40 });
      fx?.ring?.(x, y, { size: 0.4 });
      setTimeout(() => tile.remove(), 900);
      const t = LINES[lang()] || LINES.en;
      setTimeout(() => {
        if (!dev.panel?.current) dev.panel?.say({ kind: 'info', face: 'smug', line: pick(t).replace('{app}', name), hold: 3600 });
      }, 500);
    };
    setTimeout(() => {
      // her own bolt, from her hand, when she's free to; else the bolt alone
      if (capri.zap?.(x, y) && capri.zapAt) capri.zapAt.onHit = hit;
      else {
        const from = { x: capri.pos.x, y: capri.pos.y - 200 * (capri.scale || 1) };
        capri.fx?.bolt?.(from, { x, y }, { life: 260, forks: 2, width: 3 });
        capri.voice?.play?.('bolt');
        hit();
      }
    }, 650);
  }
}

// She dresses herself (1.10.0, optional: Personalizar › Que se vista sola). "Que ella escoja entre
// los estilos que hay y varíe su ropa… que el cambio tome tiempo para que no se ande cambiando cada
// 2 minutos, y que diga mira mi nuevo estilo". Every 6, 12 or 24 hours (your pick) she picks another
// of her outfits (the ones she has: none that's still locked, the wedding dress only once it's hers),
// with a feel for the moment: pajamas some nights, Christmas in December, the witch at Halloween.
// She shows it off. Her colors are hers and never change; the things you put on top stay as they are.

const LINES = {
  es: ['¡Mira mi nuevo estilo! Hoy: {o}.', '¿Cómo me veo? Me puse {o}.', 'Me cambié: {o}. ¿Te gusta?', 'Nuevo look: {o}. Admírame.'],
  en: ['Check out my new style! Today: {o}.', 'How do I look? I put on {o}.', 'I changed: {o}. Like it?', 'New look: {o}. Admire me.'],
  fr: ['Regarde mon nouveau style ! Aujourd’hui : {o}.', 'Comment je suis ? J’ai mis {o}.', 'Je me suis changée : {o}. Ça te plaît ?', 'Nouveau look : {o}. Admire-moi.'],
  pt: ['Olha meu novo estilo! Hoje: {o}.', 'Como eu tô? Coloquei {o}.', 'Me troquei: {o}. Gostou?', 'Visual novo: {o}. Me admira.'],
  ru: ['Смотри, мой новый стиль! Сегодня: {o}.', 'Как я выгляжу? Я надела: {o}.', 'Я переоделась: {o}. Нравится?', 'Новый образ: {o}. Любуйся.'],
};
const pick = (list) => list[Math.floor(Math.random() * list.length)];
const HOUR = 3600 * 1000;

export function autoDress({ cfg, setConfig, lang }) {
  const dev = window.dev;
  const wardrobe = dev?.wardrobe;
  if (!wardrobe?.available) return null;

  const every = () => ([6, 12, 24].includes(Number(cfg().autoDressHours)) ? Number(cfg().autoDressHours) : 12) * HOUR;

  function choose() {
    const now = new Date();
    const list = wardrobe.available();
    const current = wardrobe.look().outfit;
    const others = list.filter((id) => id !== current);
    if (!others.length) return null;
    const m = now.getMonth();
    const d = now.getDate();
    const h = now.getHours();
    const fits = (id) => others.includes(id);
    if (m === 11 && d >= 15 && fits('santa') && Math.random() < 0.6) return 'santa';
    if (m === 9 && d >= 25 && fits('bruja') && Math.random() < 0.6) return 'bruja';
    if ((h >= 22 || h < 6) && fits('pijama') && Math.random() < 0.5) return 'pijama';
    // (not the seasonal ones out of season)
    const plain = others.filter((id) => !(id === 'santa' && m !== 11) && !(id === 'bruja' && m !== 9) && !(id === 'pijama' && h >= 8 && h < 21));
    return pick(plain.length ? plain : others);
  }

  function dress({ force = false } = {}) {
    const c = cfg();
    if (!c.autoDress || dev.capri?.hidden || dev.capri?.stowed || c.icon || wardrobe.isOpen) return;
    if (!force && Date.now() - (Number(c.autoDressAt) || 0) < every()) return;
    if (dev.chat?.open || dev.panel?.current) return; // (not in the middle of something: later)
    const id = choose();
    setConfig({ autoDressAt: Date.now() });
    if (!id) return;
    wardrobe.wear({ outfit: id });
    dev.capri?.perform?.('hop');
    setTimeout(() => {
      dev.poses?.cue?.('selfie', { times: 2 });
      const t = LINES[lang()] || LINES.en;
      dev.panel?.say({ kind: 'info', face: 'smug', line: pick(t).replace('{o}', wardrobe.outfitName(id)), hold: 5500 });
    }, 700);
  }

  // first time on: she changes a little later; then every so often
  setTimeout(() => {
    if (cfg().autoDress && !cfg().autoDressAt) setConfig({ autoDressAt: Date.now() - every() + 5 * 60 * 1000 });
  }, 3000);
  setInterval(() => { if (!document.hidden) dress(); }, 5 * 60 * 1000);
  return { dress };
}

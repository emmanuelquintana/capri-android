// Your birthday (1.9.0, Emma: "que te pregunte tu cumpleaños y cree animaciones para felicitarte").
// - She asks, once in a while in her chat, until she knows it (or you tell her any time: "mi cumpleaños
//   es el 3 de mayo"). Kept in her config as { m, d }; her AI knows it too.
// - On the day: the first time she's on your screen she celebrates you: her birthday animation (a cake,
//   blowing out the candles) and her party one when they're drawn (anims/birthday.png, anims/party.png),
//   else her dance; confetti, a cut-in, and she says it out loud. Once a year.

import { send } from './native.js';

const MONTHS = [
  /ene(?:ro)?|jan(?:uary|vier|eiro)?|янв/iu, /feb(?:rero|ruary)?|f[ée]v(?:rier|ereiro)?|фев/iu, /mar(?:zo|ch|s|[çc]o)?|мар/iu,
  /abr(?:il)?|apr(?:il)?|avr(?:il)?|апр/iu, /may(?:o)?|mai(?:o)?|мая|май/iu, /jun(?:io|e|ho)?|juin|июн/iu,
  /jul(?:io|y|ho)?|juil(?:let)?|июл/iu, /ago(?:sto)?|aug(?:ust)?|ao[uû]t|авг/iu, /sep(?:tiembre|tember|t(?:embre|embro)?)?|set(?:iembre|embro)?|сен/iu,
  /oct(?:ubre|ober|obre)?|out(?:ubro)?|окт/iu, /nov(?:iembre|ember|embre|embro)?|ноя/iu, /dic(?:iembre)?|dec(?:ember)?|d[ée]c(?:embre)?|dez(?:embro)?|дек/iu,
];
const ABOUT = /cumple(?:años)?|birthday|b-?day|anniversaire|anivers[áa]rio|день рождения|др\b/iu;
const LINES = {
  es: { ask: 'Oye, {name}… ¿cuándo es tu cumpleaños? Quiero saberlo para no olvidarlo. 🎂', got: '¡Anotado! El {d} de {month} es tu día. Ya verás. 🎂', party: '¡FELIZ CUMPLEAÑOS, {name}! 🎂 Hoy la ciudad es tuya, choom.', months: ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'] },
  en: { ask: 'Hey, {name}… when’s your birthday? I want to know so I never forget it. 🎂', got: 'Noted! {month} {d} is your day. You’ll see. 🎂', party: 'HAPPY BIRTHDAY, {name}! 🎂 The city is yours today, choom.', months: ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'] },
  fr: { ask: 'Dis, {name}… c’est quand ton anniversaire ? Je veux le savoir pour ne jamais l’oublier. 🎂', got: 'Noté ! Le {d} {month}, c’est ton jour. Tu verras. 🎂', party: 'JOYEUX ANNIVERSAIRE, {name} ! 🎂 La ville est à toi aujourd’hui, choom.', months: ['janvier', 'février', 'mars', 'avril', 'mai', 'juin', 'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre'] },
  pt: { ask: 'Ei, {name}… quando é seu aniversário? Quero saber pra nunca esquecer. 🎂', got: 'Anotado! {d} de {month} é o seu dia. Você vai ver. 🎂', party: 'FELIZ ANIVERSÁRIO, {name}! 🎂 Hoje a cidade é sua, choom.', months: ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'] },
  ru: { ask: 'Слушай, {name}… когда у тебя день рождения? Хочу знать, чтобы никогда не забыть. 🎂', got: 'Записала! {d} {month} — твой день. Вот увидишь. 🎂', party: 'С ДНЁМ РОЖДЕНИЯ, {name}! 🎂 Сегодня город твой, чум.', months: ['января', 'февраля', 'марта', 'апреля', 'мая', 'июня', 'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря'] },
};
const LOCALES = { es: 'es-MX', en: 'en-US', fr: 'fr-FR', pt: 'pt-BR', ru: 'ru-RU' };
const fill = (line, vars) => line.replace(/\{(\w+)\}/g, (_, k) => vars[k] ?? '');

// "3 de mayo", "May 3rd", "3/5", "el 03-05" → { m: 5, d: 3 } (or null)
export function parseBirthday(text, lang = 'es') {
  const t = String(text || '').toLowerCase();
  for (let i = 0; i < 12; i++) {
    const re = new RegExp(`(?:(\\d{1,2})(?:st|nd|rd|th|er|º|°)?\\s*(?:de\\s+|d['’]\\s*)?)?(?<![\\p{L}])(?:${MONTHS[i].source})(?![\\p{L}]{4,})\\.?(?:\\s*(?:de\\s+)?(\\d{1,2})(?:st|nd|rd|th)?(?!\\d))?`, 'iu');
    const m = re.exec(t);
    const d = Number(m?.[1] || m?.[2]);
    if (m && d >= 1 && d <= 31) return { m: i + 1, d };
  }
  const n = /(?<!\d)(\d{1,2})\s*[/.-]\s*(\d{1,2})(?:\s*[/.-]\s*\d{2,4})?(?!\d)/.exec(t);
  if (n) {
    let [a, b] = [Number(n[1]), Number(n[2])];
    if (lang === 'en' && a <= 12 && b > 12) [a, b] = [b, a]; // (May/3 written the US way: 5/23)
    else if (lang === 'en' && a <= 12 && b <= 12) [a, b] = [b, a];
    if (b > 12 && a <= 12) [a, b] = [b, a];
    if (a >= 1 && a <= 31 && b >= 1 && b <= 12) return { m: b, d: a };
  }
  return null;
}

export function birthday({ cfg, setConfig }) {
  const dev = window.dev;
  const { chat, panel, capri } = dev || {};
  if (!chat || !panel || !capri) return null;
  const lang = () => (LINES[dev.brain?.lang] ? dev.brain.lang : 'en');
  const name = () => cfg().userName || 'choom';
  let waiting = 0; // her question is out: your next messages may be the answer

  // her AI knows it
  const personal = chat.personal.bind(chat);
  chat.personal = () => {
    const lines = personal();
    const b = cfg().birthday;
    if (b?.m) lines.push(`The user's birthday is ${LINES.en.months[b.m - 1]} ${b.d}.${isToday(b) ? ' It is TODAY: wish them a happy birthday warmly.' : ''}`);
    return lines;
  };

  // what you tell her
  const send0 = chat.send.bind(chat);
  chat.send = (text) => {
    if (!chat.turn && (waiting > 0 || ABOUT.test(String(text || '')))) {
      const b = parseBirthday(text, lang());
      if (b) {
        setConfig({ birthday: b });
        waiting = 0;
        setTimeout(() => chat.interject(fill(LINES[lang()].got, { d: b.d, month: LINES[lang()].months[b.m - 1] }), [], null), 2500);
      } else if (waiting > 0) waiting -= 1;
    }
    return send0(text);
  };

  // her question: after her answer to your first message of a chat, every few days until she knows
  const busy = panel.setChatBusy.bind(panel);
  let askedThisChat = false;
  panel.setChatBusy = (on, ...rest) => {
    const out = busy(on, ...rest);
    const c = cfg();
    if (!on && chat.open && !askedThisChat && !c.birthday && (c.birthdayAsks || 0) < 4 && Date.now() - (c.birthdayAskedAt || 0) > 3 * 86400000 && chat.history.length >= 4) {
      askedThisChat = true;
      setTimeout(() => {
        if (chat.interject(fill(LINES[lang()].ask, { name: name() }), [], null)) {
          waiting = 2;
          setConfig({ birthdayAskedAt: Date.now(), birthdayAsks: (cfg().birthdayAsks || 0) + 1 });
        }
      }, 1800);
    }
    return out;
  };
  const close = chat.close.bind(chat);
  chat.close = (...a) => { askedThisChat = false; return close(...a); };

  // the day
  function isToday(b) {
    const now = new Date();
    return b && now.getMonth() + 1 === b.m && now.getDate() === b.d;
  }
  async function celebrate() {
    const c = cfg();
    const year = new Date().getFullYear();
    setConfig({ birthdayParty: year });
    const line = fill(LINES[lang()].party, { name: name() });
    capri.wake?.();
    panel.say({ kind: 'done', face: 'laugh', line, hold: 9000 });
    send('voice:say', { text: line.replace(/[\p{Extended_Pictographic}\u{FE0F}]/gu, ''), lang: LOCALES[lang()] });
    confetti();
    try { dev.fx?.cutin?.(...capri.cutinAt(-(capri.towardCenter?.() || 1)), { kind: 'laugh', life: 900 }); } catch {}
    const poses = dev.poses;
    const first = poses?.has('birthday') ? 'birthday' : poses?.has('party') ? 'party' : 'dance';
    if (poses && await poses.cue(first, { hold: 8000 })) {
      if (first === 'birthday' && poses.has('party')) setTimeout(() => poses.cue('party', { hold: 8000 }), 8400);
    } else capri.perform?.('victory', capri.towardCenter?.() || 1);
    void c;
  }
  function confetti() {
    const colors = ['#fcee08', '#02f0ff', '#ff1744', '#08f467', '#8a3cff', '#ffffff'];
    for (let i = 0; i < 70; i++) {
      const el = document.createElement('span');
      el.className = 'capri-confetti';
      el.style.left = `${Math.random() * innerWidth}px`;
      el.style.background = colors[i % colors.length];
      el.style.setProperty('--dx', `${(Math.random() - 0.5) * 160}px`);
      el.style.setProperty('--spin', `${Math.round((Math.random() - 0.5) * 1080)}deg`);
      el.style.animationDelay = `${Math.random() * 900}ms`;
      el.style.animationDuration = `${2600 + Math.random() * 1800}ms`;
      document.body.append(el);
      setTimeout(() => el.remove(), 5800);
    }
  }
  const check = () => {
    const c = cfg();
    if (!c.birthday || c.birthdayParty === new Date().getFullYear() || !isToday(c.birthday)) return;
    if (document.hidden || capri.hidden || capri.stowed || c.visible === false || chat.open) return;
    celebrate();
  };
  setTimeout(check, 12000);
  setInterval(check, 10 * 60000);

  return { celebrate, parse: parseBirthday };
}

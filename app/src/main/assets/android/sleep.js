// Modo sueño (1.11.0, Sleep.kt): she sleeps with you. Good night → she curls up and dozes off on your
// screen; the screen on in the middle of the night → she notices and sends you back to sleep; her alarm →
// she wakes up and keeps telling you to get up (and to turn it off) until you do; morning → your night,
// in her words. Also asked out loud or in her chat: "Capri, modo sueño", "buenas noches, despiértame a las 7".

import { on, send, call } from './native.js';

const T = {
  es: {
    night: ['Buenas noches, {name}. Aquí me quedo, cuidándote.', 'A dormir. Yo vigilo la pantalla… y a ti.', 'Apago luces. Si prendes el celular, me entero.'],
    alarmSet: 'Te despierto a las {time}. Duerme ya.',
    peek: ['¿Qué haces despierta/o? Son las {time}. A dormir.', '{n}ª vez que prendes la pantalla… suelta el celular, {name}.', 'Shh. Es de noche. Ciérralo y duerme.'],
    ring: ['¡Arriba, {name}! ¡Ya es hora!', '¡Despierta! Apaga la alarma, que no me voy a callar.', '¡Buenos días! La alarma no se apaga sola, choom.', 'Vamos, vamos, ¡fuera de la cama!'],
    snooze: ['Cinco minutos. Ni uno más.', 'Ok… cinco. Te estoy contando.'],
    report: 'Buenos días. {summary}',
    good: ['Dormiste bien. Eso me gusta.', 'Así se duerme. Preem.'],
    bad: ['Dormiste poquito… hoy tómalo con calma.', 'Hay que dormir más, {name}. Lo digo en serio.'],
    noScreen: 'No prendiste la pantalla en toda la noche.',
    screen: 'Prendiste la pantalla {n} veces.',
    slept: 'Dormiste {h} h {m} min.',
    stop: 'Listo, ya despertaste. Buenos días.',
    notOn: 'No estábamos en modo sueño… pero buenos días.',
  },
  en: {
    night: ['Good night, {name}. I’ll stay right here, watching over you.', 'Bedtime. I’ll guard the screen… and you.', 'Lights out. If you pick up the phone, I’ll know.'],
    alarmSet: 'I’ll wake you at {time}. Sleep now.',
    peek: ['What are you doing up? It’s {time}. Back to sleep.', 'Screen on again, number {n}… put the phone down, {name}.', 'Shh. It’s night. Close it and sleep.'],
    ring: ['Up, {name}! It’s time!', 'Wake up! Turn off the alarm, I’m not going to shut up.', 'Good morning! The alarm won’t turn itself off, choom.', 'Come on, come on, out of bed!'],
    snooze: ['Five minutes. Not one more.', 'Okay… five. I’m counting.'],
    report: 'Good morning. {summary}',
    good: ['You slept well. I like that.', 'That’s how you sleep. Preem.'],
    bad: ['You barely slept… take it easy today.', 'You need more sleep, {name}. I mean it.'],
    noScreen: 'You didn’t turn on the screen all night.',
    screen: 'You turned on the screen {n} times.',
    slept: 'You slept {h} h {m} min.',
    stop: 'Done, you’re up. Good morning.',
    notOn: 'We weren’t in sleep mode… but good morning.',
  },
  fr: {
    night: ['Bonne nuit, {name}. Je reste là, je veille sur toi.', 'Au lit. Je surveille l’écran… et toi.', 'Lumières éteintes. Si tu prends le téléphone, je le saurai.'],
    alarmSet: 'Je te réveille à {time}. Dors maintenant.',
    peek: ['Qu’est-ce que tu fais debout ? Il est {time}. Au lit.', 'Écran allumé encore, la {n}e fois… lâche le téléphone, {name}.', 'Chut. C’est la nuit. Ferme-le et dors.'],
    ring: ['Debout, {name} ! C’est l’heure !', 'Réveille-toi ! Éteins le réveil, je ne vais pas me taire.', 'Bonjour ! Le réveil ne s’éteint pas tout seul, choom.', 'Allez, allez, hors du lit !'],
    snooze: ['Cinq minutes. Pas une de plus.', 'D’accord… cinq. Je compte.'],
    report: 'Bonjour. {summary}',
    good: ['Tu as bien dormi. J’aime ça.', 'Voilà comment on dort. Preem.'],
    bad: ['Tu as peu dormi… vas-y doucement aujourd’hui.', 'Il faut dormir plus, {name}. Sérieusement.'],
    noScreen: 'Tu n’as pas allumé l’écran de la nuit.',
    screen: 'Tu as allumé l’écran {n} fois.',
    slept: 'Tu as dormi {h} h {m} min.',
    stop: 'C’est bon, tu es réveillé·e. Bonjour.',
    notOn: 'On n’était pas en mode sommeil… mais bonjour.',
  },
  pt: {
    night: ['Boa noite, {name}. Fico aqui, cuidando de você.', 'Hora de dormir. Eu vigio a tela… e você.', 'Luzes apagadas. Se pegar o celular, eu fico sabendo.'],
    alarmSet: 'Te acordo às {time}. Dorme agora.',
    peek: ['O que você está fazendo acordado/a? São {time}. Volta a dormir.', 'Tela ligada de novo, a {n}ª vez… larga o celular, {name}.', 'Shh. É de noite. Fecha e dorme.'],
    ring: ['Levanta, {name}! Tá na hora!', 'Acorda! Desliga o alarme, que eu não vou calar a boca.', 'Bom dia! O alarme não desliga sozinho, choom.', 'Vamos, vamos, sai da cama!'],
    snooze: ['Cinco minutos. Nem um a mais.', 'Tá… cinco. Tô contando.'],
    report: 'Bom dia. {summary}',
    good: ['Você dormiu bem. Gosto disso.', 'Assim que se dorme. Preem.'],
    bad: ['Você dormiu pouquinho… vai com calma hoje.', 'Precisa dormir mais, {name}. Tô falando sério.'],
    noScreen: 'Você não ligou a tela a noite toda.',
    screen: 'Você ligou a tela {n} vezes.',
    slept: 'Você dormiu {h} h {m} min.',
    stop: 'Pronto, acordou. Bom dia.',
    notOn: 'A gente não estava no modo sono… mas bom dia.',
  },
  ru: {
    night: ['Спокойной ночи, {name}. Я здесь, присмотрю за тобой.', 'Пора спать. Я стерегу экран… и тебя.', 'Свет выключен. Возьмёшь телефон — я узнаю.'],
    alarmSet: 'Разбужу тебя в {time}. Спи.',
    peek: ['Ты чего не спишь? Сейчас {time}. Спать.', 'Экран опять включён, уже {n}-й раз… положи телефон, {name}.', 'Тсс. Ночь. Закрывай и спи.'],
    ring: ['Подъём, {name}! Пора!', 'Просыпайся! Выключи будильник, я не замолчу.', 'Доброе утро! Будильник сам не выключится, чумба.', 'Давай-давай, вылезай из кровати!'],
    snooze: ['Пять минут. Ни минутой больше.', 'Ладно… пять. Я считаю.'],
    report: 'Доброе утро. {summary}',
    good: ['Ты хорошо поспал(а). Мне нравится.', 'Вот так надо спать. Преем.'],
    bad: ['Ты почти не спал(а)… сегодня полегче.', 'Надо спать больше, {name}. Серьёзно.'],
    noScreen: 'Ты ни разу не включал(а) экран за ночь.',
    screen: 'Ты включал(а) экран {n} раз.',
    slept: 'Ты спал(а) {h} ч {m} мин.',
    stop: 'Всё, ты проснулся(ась). Доброе утро.',
    notOn: 'Мы не были в режиме сна… но доброе утро.',
  },
};
export const SLEEP_ASK = /modo (?:sue[ñn]o|siesta|dormir)|vamos a dormir|me voy a dormir|duerme conmigo|a dormir(?: juntas| conmigo)?\b|desp[ié]rtame|sleep mode|nap mode|sleep with me|wake me (?:up )?at|going to sleep|mode sommeil|dors avec moi|r[ée]veille-moi|modo sono|dorme comigo|me acorda|режим сна|спи со мной|разбуди меня/iu;
export const WOKE_ASK = /ya despert[eé]|ya me levant[eé]|estoy despiert|i'?m (?:up|awake)|je suis r[ée]veill|j'?suis debout|j[aá] acordei|я проснул|я встал/iu;
const AT = /(?:a las|a la|at|à|às|в)\s*(\d{1,2})(?::|\.|h)?(\d{2})?\s*(am|pm|a\.?\s?m\.?|p\.?\s?m\.?|de la ma[ñn]ana|de la tarde|de la noche)?/iu;

/** "despiértame a las 7:30" → that time next (ms), or 0. */
export function alarmFrom(text) {
  const m = AT.exec(String(text || ''));
  if (!m) return 0;
  let h = Number(m[1]);
  const min = Number(m[2] || 0);
  const ampm = (m[3] || '').toLowerCase();
  if (/p|tarde|noche/.test(ampm) && h < 12) h += 12;
  if (/a\.?\s?m|ma[ñn]ana/.test(ampm) && h === 12) h = 0;
  if (h > 23 || min > 59) return 0;
  const d = new Date();
  d.setHours(h, min, 0, 0);
  if (d.getTime() <= Date.now() + 60000) d.setDate(d.getDate() + 1);
  return d.getTime();
}

export function sleepMode({ cfg, lang }) {
  const dev = window.dev;
  const capri = dev?.capri;
  const panel = dev?.panel;
  if (!capri || !panel) return null;
  const t = () => T[lang()] || T.en;
  const pick = (l) => l[Math.floor(Math.random() * l.length)];
  const name = () => String(cfg().userName || '').trim();
  const fill = (s, v = {}) => s.replace(/\{(\w+)\}/g, (_, k) => (k === 'name' ? name() || 'choom' : v[k] ?? '')).replace(/,\s*choom([.!?])/, ', choom$1');
  const clock = (ms = Date.now()) => new Intl.DateTimeFormat(lang(), { hour: 'numeric', minute: '2-digit' }).format(new Date(ms));
  const say = (line, face = 'calm', hold = 6000, aloud = false) => {
    panel.say({ kind: 'info', face, line, hold });
    if (aloud) send('voice:say', { text: line, lang: { es: 'es-MX', en: 'en-US', fr: 'fr-FR', pt: 'pt-BR', ru: 'ru-RU' }[lang()] || 'es-MX' });
  };
  let ringing = 0;
  const stopRing = () => { clearInterval(ringing); ringing = 0; };
  const asleep = () => setTimeout(() => capri.doze?.(), 4500);

  on('sleep:on', ({ alarm = 0 } = {}) => {
    stopRing();
    // (1.11.0) without an alarm to tell you about: her recorded good night, when she has it (voicelines.js)
    const own = alarm ? null : window.dev?.voicelines?.moment('night', '');
    if (own?.spoken) say(own.line, 'calm', Math.max(5000, own.ms + 1500));
    else say(fill(pick(t().night)) + (alarm ? ` ${fill(t().alarmSet, { time: clock(alarm) })}` : ''), 'calm', 5000);
    asleep();
  });
  on('sleep:peek', ({ times = 1 } = {}) => {
    setTimeout(async () => {
      if (capri.dormant) await capri.rouse?.();
      say(fill(pick(t().peek), { time: clock(), n: times }), 'aside', 5500);
      asleep();
    }, 1200);
  });
  on('sleep:alarm', async () => {
    if (capri.dormant) await capri.rouse?.();
    capri.perform?.('hop');
    const shout = () => {
      if (capri.dormant) capri.rouse?.();
      capri.perform?.('hop');
      say(fill(pick(t().ring)), 'wide', 9000, true);
    };
    stopRing();
    shout();
    ringing = setInterval(shout, 20000);
    setTimeout(stopRing, 15 * 60 * 1000); // (never forever)
  });
  on('sleep:snooze', () => {
    stopRing();
    say(pick(t().snooze), 'smug', 4000);
    asleep();
  });
  on('sleep:report', async (night = {}) => {
    stopRing();
    if (capri.dormant) await capri.rouse?.();
    const sleptMs = Math.max(0, (night.end || 0) - (night.start || 0) - (night.awakeMs || 0));
    const h = Math.floor(sleptMs / 3600000);
    const m = Math.floor((sleptMs % 3600000) / 60000);
    const summary = [fill(t().slept, { h, m }), night.times ? fill(t().screen, { n: night.times }) : t().noScreen, fill(pick(sleptMs >= 6.5 * 3600000 ? t().good : t().bad))].join(' ');
    setTimeout(() => say(fill(t().report, { summary }), sleptMs >= 6.5 * 3600000 ? 'laugh' : 'aside', 12000, true), 1200);
    try { dev.chat?.remember?.({ role: 'assistant', content: fill(t().report, { summary }) }); } catch {}
  });
  // her page started in the middle of a night (Capri turned on from Modo sueño): asleep right away
  call('sleep:state').then((s) => { if (s?.on) asleep(); }).catch(() => {});

  return {
    /** "Capri, modo sueño (despiértame a las 7)" / "ya desperté": done here, { line, face } or null. */
    ask(text) {
      if (WOKE_ASK.test(text)) {
        call('sleep:stop').then((night) => { if (!night) say(t().notOn, 'laugh', 4000); }).catch(() => {});
        return { line: t().stop, face: 'laugh' };
      }
      if (!SLEEP_ASK.test(text)) return null;
      const alarm = alarmFrom(text);
      send('sleep:start', { alarm });
      return { line: fill(pick(t().night)) + (alarm ? ` ${fill(t().alarmSet, { time: clock(alarm) })}` : ''), face: 'calm' };
    },
  };
}

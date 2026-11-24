// (1.11.0) Capri Voice V8: her own recorded phrases (CapriVoice.kt), on the moments made for them.
//
// The rule (Emma's): a visual moment stays visual; a spoken moment plays its phrase; her AI's answers are
// said word for word as her AI wrote them (Ears.kt plays a phrase for one only if it's exactly the same
// words); her sound effects (voice.js) stay as they are. So only some of the lines she already says here
// (a pat, a throw, back after a while, a goodbye) become one of her phrases, now and then, and when one
// does, the bubble shows the words she really says. Without her packs, or with her voice off, nothing
// changes: the same line as always, shown.
//
// Her phrases are found by the catalog's own ids (eventId, from the V8 pack's manifest), never by file.

import { call, send, on } from './native.js';
import { cutin } from './moods.js';

const LOCALES = { es: 'es-MX', en: 'en-US' };

// her moments → the V8 events that fit them (eventId, or its family: "pet" is pet.original-02, -03…),
// how often she says one instead of the usual line, and how long before another of the same
// (1.11.8, Emma: "no se escuchan tan frecuente las voces pregrabadas") more often, and on more of her moments:
// 1.11.7 used 9 of her 100 phrases' kinds, on 4 of 10 pats; now her hello, her idle thoughts, the start and the
// end of a chat, a lost connection, a game and a reminder have theirs too.
const MOMENTS = {
  pet: { events: ['pet'], chance: 0.85, every: 25e3 },
  drag: { events: ['drag'], chance: 0.8, every: 25e3 },
  move: { events: ['move', 'gesture'], chance: 0.7, every: 30e3 },
  anger: { events: ['anger'], chance: 1, every: 60e3 },
  back: { events: ['greeting.return'], chance: 1, every: 5 * 60e3 },
  hello: { events: ['greeting'], chance: 1, every: 30 * 60e3 },
  morning: { events: ['morning'], chance: 1, every: 3 * 3600e3 },
  bye: { events: ['goodbye'], chance: 1, every: 0 },
  night: { events: ['night', 'sleep'], chance: 1, every: 0 },
  idle: { events: ['spontaneous', 'company', 'gesture'], chance: 0.6, every: 3 * 60e3 },
  chatStart: { events: ['chat-start'], chance: 0.8, every: 5 * 60e3, chat: true },
  chatEnd: { events: ['chat-end'], chance: 0.7, every: 5 * 60e3, chat: true },
  offline: { events: ['offline.offline', 'offline.original-01'], chance: 1, every: 10 * 60e3 },
  game: { events: ['game.focus'], chance: 1, every: 10 * 60e3 },
  gameBack: { events: ['game.return'], chance: 1, every: 10 * 60e3 },
  reminder: { events: ['reminder.original-01'], chance: 1, every: 10 * 60e3 },
  done: { events: ['task.done'], chance: 0.8, every: 5 * 60e3 },
  // (1.12.0) called by her name while she dozed: she wakes up ("¿Ya es hora de levantarse?")
  wake: { events: ['wake'], chance: 1, every: 60e3 },
  // (1.11.8, Emma: "añade la frase guardada de vez en cuando de tienes un mensaje o tienes una notificación")
  message: { events: ['notification.original-04'], chance: 0.4, every: 4 * 60e3 },
  notice: { events: ['notification.original-01'], chance: 0.3, every: 6 * 60e3 },
};
// the lines of hers that are those moments (lines.js), wherever she says them from
const POOLS = {
  pet: ['pet.high', 'pet.low', 'pet.enough'],
  drag: ['thrown.ow', 'thrown.fun', 'thrown.screen', 'thrown.ledge', 'thrown.feet', 'thrown.again'],
  anger: ['thrown.angry'],
  move: ['thrown.dodge', 'screens.hello'],
  back: ['bond.back', 'bond.missed'],
  hello: ['hello', 'online', 'custom.onlineName'],
  idle: ['idle', 'gaze', 'uplink', 'bond.idle'],
  chatStart: ['chat.hello', 'chat.again'],
  offline: ['pc.offline'],
  game: ['pc.game'],
  gameBack: ['pc.gameBack'],
  reminder: ['remind.nowBare'],
  done: ['remind.doneLine'],
};
const GAP = 12e3; // never two of her phrases closer than this (she isn't a talking toy)

export function voiceLines({ cfg, lang }) {
  const dev = window.dev;
  if (!dev?.panel) return null;
  const { panel } = dev;
  let catalog = {};
  const load = (c) => { catalog = c && typeof c === 'object' ? c : {}; };
  call('voice:packs').then(load).catch(() => {});
  on('voice:packs', load);

  const last = {};      // moment → when
  const lastClip = {};  // moment → the phrase before (not the same twice in a row)
  let lastAny = 0;
  let seq = 0;

  const locale = () => LOCALES[lang()] || null;
  const allowed = (inChat = false) => {
    const c = cfg();
    return c.speak !== false && c.voiceClips !== false && !c.gamer && (inChat || !dev.chat?.open) && !dev.capri?.hidden;
  };

  // one phrase for that moment, if her pack for this language has one (and now is a good time)
  const phrase = (moment, force = false) => {
    const rule = MOMENTS[moment];
    const loc = locale();
    const list = loc && catalog[loc];
    if (!rule || !list?.length || !allowed(rule.chat)) return null;
    const now = Date.now();
    if (!force && (now - lastAny < GAP || now - (last[moment] || 0) < rule.every || Math.random() > rule.chance)) return null;
    const fits = list.filter((c) => rule.events.some((e) => c.eventId === e || c.eventId.startsWith(`${e}.`)));
    const fresh = fits.filter((c) => c.id !== lastClip[moment]);
    const pool = fresh.length ? fresh : fits;
    if (!pool.length) return null;
    const clip = pool[Math.floor(Math.random() * pool.length)];
    last[moment] = lastAny = now;
    lastClip[moment] = clip.id;
    return clip;
  };

  const speak = (clip) => {
    const id = `clip-${++seq}`;
    send('voice:say', { id, text: clip.text, lang: locale(), clip: clip.id, kind: 'event' });
    return id;
  };

  // which moment a line of hers is (its pool in lines.js)
  const poolOf = (line) => {
    const t = dev.brain?.t;
    if (!t || !line) return null;
    for (const [moment, paths] of Object.entries(POOLS)) {
      for (const path of paths) {
        const pool = path.split('.').reduce((o, k) => o?.[k], t);
        if (Array.isArray(pool) && pool.includes(line)) return moment;
      }
    }
    return null;
  };

  // her lines: one that is such a moment may become her phrase (shown as she says it)
  const say = panel.say.bind(panel);
  panel.say = (msg = {}) => {
    // (1.11.8) her chat's hello is a moment too (the words in her bubble become the ones she says)
    const pool = !msg.reply && !msg.guide && poolOf(msg.line);
    const moment = msg.chat ? (pool === 'chatStart' && pool) : pool;
    const clip = moment && phrase(moment);
    if (!clip) return say(msg);
    // (her chat's panel stays until you close it: hold null is kept)
    const shown = say({ ...msg, line: clip.text, hold: msg.hold === null ? null : Math.max(msg.hold || 0, clip.ms + 1500) });
    if (shown !== false) speak(clip);
    return shown;
  };

  // (1.11.7) CI only (a debug build's service sends it, CapriService FLOWTEST): one of her lines from that pool
  // of lines.js, said the way she says it (panel.say, so as a moment of hers), to check her phrase plays
  on('debug:moment', ({ pool = '' } = {}) => {
    const list = pool.split('.').reduce((o, k) => o?.[k], dev.brain?.t);
    const line = Array.isArray(list) ? list[0] : null;
    console.log(`[capri] debug moment ${pool} line=${line ? 'yes' : 'no'} catalog=${Object.keys(catalog).join(',')} allowed=${allowed()}`);
    if (line) panel.say({ kind: 'info', face: 'angry', line });
  });

  // (1.11.8) the end of a chat where you talked: her goodbye for it, shown and said
  const chat = dev.chat;
  let talked = false;
  if (chat?.send && chat?.close) {
    const sendIt = chat.send.bind(chat);
    chat.send = function (...args) { talked = true; return sendIt(...args); };
    const closeIt = chat.close.bind(chat);
    chat.close = function (...args) {
      const was = chat.open && talked;
      talked = false;
      const out = closeIt(...args);
      if (was) {
        setTimeout(() => {
          const clip = phrase('chatEnd');
          if (!clip) return;
          const shown = say({ kind: 'info', face: 'smug', line: clip.text, hold: clip.ms + 1500 });
          if (shown !== false) speak(clip);
        }, 600);
      }
      return out;
    };
  }

  // her phrase's own gesture as it starts (the catalog's animationHint): her cut-ins, her wave
  const CUTS = { angry: 'angry', cry: 'cry', love: 'love', sleepy: 'sleepy' };
  on('voice:event', (e = {}) => {
    if (e.state === 'VOICE_STARTED' && e.source === 'clip') {
      dev.voiceTalking = true;
      if (CUTS[e.hint]) cutin(CUTS[e.hint]);
    } else if (['VOICE_FINISHED', 'VOICE_CANCELLED', 'VOICE_ERROR', 'VOICE_UNAVAILABLE'].includes(e.state)) {
      dev.voiceTalking = false;
    }
  });

  return {
    /** A spoken moment of hers (listen.js: a goodbye, she's back): her phrase for it, or the usual line.
     *  Returns the line to show and, when a phrase plays, true as `spoken`. */
    moment(moment, line) {
      const clip = phrase(moment, true);
      if (!clip) return { line, spoken: false };
      speak(clip);
      return { line: clip.text, spoken: true, ms: clip.ms };
    },
    /** (1.11.8) a notification just came (notes.js): now and then her phrase for it, "a message just came in"
     *  for a chat app's, "you've got a new notification" for the rest. Said only: the card shows it already. */
    notice(isMessage) {
      if (dev.voiceTalking || dev.chat?.open) return false;
      const clip = phrase(isMessage ? 'message' : 'notice');
      if (!clip) return false;
      speak(clip);
      return true;
    },
    /** What she's saying stops (a newer request, she was hidden). */
    stop: () => send('voice:stop', {}),
    has: () => Boolean(catalog[locale()]?.length),
  };
}

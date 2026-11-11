// Streaming mode (1.9.0, the community: "un modo streaming que se pueda conectar a TikTok u otra
// app y que pueda leer los comentarios de los seguidores"). While you're live (TikTok, Twitch,
// YouTube, Kick… any app that shows the chat on screen), she reads your viewers' new comments off
// your screen and answers them, out loud and in her panel, as your co-host.
// No account, no login, no connection to those apps: she only sees your screen, with her live look
// (Android's screen sharing, asked when you turn it on), like her other looks. Her AI has to see
// pictures. Every ~20 s at most, so a free key's limits hold.
//
// On: Ajustes, or "Capri, modo streaming" (typed or said); off the same way ("quita el modo streaming").

import { send } from './native.js';
import { OFF_ASK } from './listen.js';

export const STREAM_ASK = /modo (?:streaming|stream|directo|en vivo|live)|streaming mode|stream mode|live mode|mode (?:stream|live|direct)|modo (?:transmiss[ãa]o|ao vivo)|режим (?:стрима|трансляции|эфира)/iu;
const EVERY = 20000;
const LOCALES = { es: 'es-MX', en: 'en-US', fr: 'fr-FR', pt: 'pt-BR', ru: 'ru-RU' };
const LINES = {
  es: { on: '¡Modo streaming! Leo los comentarios de tu live y les contesto. Saluden, chooms 👋', off: 'Se acabó el stream. ¡Buen live!', blind: 'Para leer tu chat necesito ver tu pantalla: acepta compartirla.' },
  en: { on: 'Streaming mode! I’ll read your live’s comments and answer them. Say hi, chooms 👋', off: 'Stream’s over. Great live!', blind: 'To read your chat I need to see your screen: allow sharing it.' },
  fr: { on: 'Mode streaming ! Je lis les commentaires de ton live et je leur réponds. Coucou les chooms 👋', off: 'Fin du stream. Super live !', blind: 'Pour lire ton chat, je dois voir ton écran : accepte de le partager.' },
  pt: { on: 'Modo streaming! Vou ler os comentários da sua live e responder. Oi, chooms 👋', off: 'Acabou a live. Mandou bem!', blind: 'Pra ler seu chat eu preciso ver sua tela: aceite compartilhar.' },
  ru: { on: 'Режим стрима! Читаю комментарии в твоём эфире и отвечаю. Привет, чумы 👋', off: 'Стрим окончен. Отличный эфир!', blind: 'Чтобы читать чат, мне нужно видеть твой экран: разреши показ экрана.' },
};

export function streaming({ cfg, setConfig }) {
  const dev = window.dev;
  const { chat, panel, look } = dev || {};
  if (!chat || !panel) return null;
  const lang = () => (LINES[dev.brain?.lang] ? dev.brain.lang : 'en');
  const seen = []; // comments she already read: "user: text"
  let busy = false;
  let next = 0;

  function set(on) {
    setConfig({ streaming: on, ...(on && { watchScreen: true }) }); // (her live look: Android asks to share the screen)
    say(LINES[lang()][on ? 'on' : 'off'], on ? 'laugh' : 'smug');
    next = Date.now() + 6000;
    seen.length = 0;
  }

  function say(line, face = 'smug', quote = '') {
    panel.say({ kind: 'info', badge: 'LIVE', face, line, quote, hold: Math.min(14000, 4000 + line.length * 60), noWink: true });
    send('voice:say', { text: line.replace(/[\p{Extended_Pictographic}\u{FE0F}]/gu, ''), lang: LOCALES[lang()] });
  }

  // typed to her: on or off
  const send0 = chat.send.bind(chat);
  chat.send = (text) => {
    if (!chat.turn && STREAM_ASK.test(String(text || ''))) {
      chat.close?.();
      panel.hide?.();
      return set(!OFF_ASK.test(text));
    }
    return send0(text);
  };

  async function tick() {
    const c = cfg();
    if (!c.streaming || busy || document.hidden || Date.now() < next || chat.open) return;
    if (look) look.nextAt = Date.now() + 60000; // (her usual comments on your screen wait meanwhile)
    if (!c.watchScreen) return;
    busy = true;
    next = Date.now() + EVERY;
    try {
      const snap = await window.host.lookSnap();
      if (!snap?.ok || snap.id === 'blind') return;
      const res = await window.host.lookSay({
        id: snap.id,
        persona: chat.lookPersona(),
        prompt: `STREAM. The user is live-streaming right now and you're their co-host. Read the viewers' comments visible on the screen (the live chat: username and message). Already answered, skip these: ${JSON.stringify(seen.slice(-25))}.
Answer ONLY with JSON: {"comments":[{"user":"…","text":"…"}],"reply":"…"} where comments are the new ones you can read (newest last) and reply is what you say out loud to them, in character, addressing people by their username: one or two short sentences, fun, friendly with the viewers, never rude, never reading out anything private. If there are no new comments, reply is "". ${chat.languageNote()}`,
      });
      if (!res?.ok) {
        if (res?.code === 'novision') {
          say(String(dev.brain?.t?.look?.noVision || '').replace('{model}', String(res.model || '…')), 'aside');
          setConfig({ streaming: false });
        }
        return;
      }
      const json = /\{[\s\S]*\}/.exec(String(res.text || ''))?.[0];
      const out = json ? JSON.parse(json) : null;
      const fresh = (Array.isArray(out?.comments) ? out.comments : [])
        .map((m) => `${String(m?.user || '').slice(0, 40)}: ${String(m?.text || '').slice(0, 140)}`)
        .filter((k) => k.length > 2 && !seen.includes(k));
      seen.push(...fresh);
      while (seen.length > 60) seen.shift();
      const reply = String(out?.reply || '').trim();
      if (fresh.length && reply && cfg().streaming) say(reply, 'laugh', fresh.at(-1));
    } catch (err) {
      send('log', { level: 'W', text: `streaming: ${err?.message || err}` });
    } finally {
      busy = false;
    }
  }
  setInterval(tick, 3000);

  return { set, wants: (text) => STREAM_ASK.test(String(text || '')) };
}

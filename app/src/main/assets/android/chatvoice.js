// (1.11.7, Emma: "No habla… solo escucho su voz cuando le doy al botón de probar") Her chat answers out loud.
// Until now only what she answered when you talked to her ("Capri, …", listen.js) was said; what she answers
// in her chat (the bubble one, and the app's Chat tab) was only written, and that's where everyone talks to
// her. Now each of her chat answers goes to her voice too (Ears.kt: one of her own phrases when it's word for
// word the same, else the voice you picked for her answers), unless her voice is off (speak) or you turned
// this off (speakChat, Ajustes de voz). Long answers: their first sentences, up to a few hundred letters.

import { send } from './native.js';

const LOCALES = { es: 'es-MX', en: 'en-US', fr: 'fr-FR', pt: 'pt-BR', ru: 'ru-RU' };
const MAX = 400;

// what her voice reads: no tags, no markdown, no emoji, no links
export function spokenText(line) {
  let s = String(line || '')
    .replace(/\[\[[^\]]*\]\]/g, '')
    .replace(/https?:\/\/\S+/g, '')
    .replace(/[*_`#>~|]/g, '')
    .replace(/[\p{Extended_Pictographic}\u{FE0F}\u{200D}]/gu, '')
    .replace(/\s+/g, ' ')
    .trim();
  if (s.length > MAX) {
    const cut = s.slice(0, MAX);
    const end = Math.max(...['. ', '! ', '? ', '… '].map((m) => cut.lastIndexOf(m)));
    s = end > 60 ? cut.slice(0, end + 1) : cut.replace(/\s+\S*$/, '') + '…';
  }
  return s;
}

export function chatVoice({ cfg, lang }) {
  const dev = window.dev;
  const chat = dev?.chat;
  let seq = 0;
  const on = () => {
    const c = cfg();
    return c.speak !== false && c.speakChat !== false;
  };
  const say = (line) => {
    if (!on()) return false;
    const text = spokenText(line);
    if (!text) return false;
    dev.voicelines?.stop?.();
    send('voice:say', { id: `chat-${++seq}`, kind: 'answer', text, lang: LOCALES[lang()] || 'es-MX' });
    return true;
  };
  // her bubble chat: every answer of hers passes through remember() (chat.js); listen.js and appchat.js
  // say theirs themselves, and mark it (dev.chatVoiceSkip) so it isn't said twice
  if (chat?.remember) {
    const remember = chat.remember.bind(chat);
    chat.remember = function (message, ...rest) {
      const out = remember(message, ...rest);
      try {
        if (message?.role === 'assistant' && typeof message.content === 'string' && !dev.chatVoiceSkip) say(message.content);
      } catch {}
      return out;
    };
  }
  return { say };
}

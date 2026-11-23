// (1.11.7, Emma: "No habla… solo escucho su voz cuando le doy al botón de probar") Her chat answers out loud.
// Until now only what she answered when you talked to her ("Capri, …", listen.js) was said; what she answers
// in her chat (the bubble one, and the app's Chat tab) was only written, and that's where everyone talks to
// her. Now each of her chat answers goes to her voice too (Ears.kt: one of her own phrases when it's word for
// word the same, else the voice you picked for her answers), unless her voice is off (speak) or you turned
// this off (speakChat, Ajustes de voz). Long answers: their first sentences, up to a few hundred letters.

//
// (1.11.8, Emma: "que aparezca el mensaje de respuesta pero que haya un indicador que diga el porcentaje de
// generación de voz de ese mensaje") Her written answer shows at once, as always; under it, a small meter says
// how much of its voice is made (her neural voice sends `pct`), then that she's speaking. And her voice starts
// loading the moment you send her something (voice:warm), not once her AI has answered.

import { send, on } from './native.js';

const LOCALES = { es: 'es-MX', en: 'en-US', fr: 'fr-FR', pt: 'pt-BR', ru: 'ru-RU' };
const MAX = 320;

// the meter's words
const METER = {
  es: { prep: 'Preparando su voz…', gen: 'Generando voz', talk: 'Hablando' },
  en: { prep: 'Getting her voice ready…', gen: 'Making her voice', talk: 'Speaking' },
  fr: { prep: 'Préparation de sa voix…', gen: 'Génération de la voix', talk: 'Elle parle' },
  pt: { prep: 'Preparando a voz dela…', gen: 'Gerando a voz', talk: 'Falando' },
  ru: { prep: 'Готовлю её голос…', gen: 'Создаю голос', talk: 'Говорит' },
};

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
  const say = (line, id = `chat-${++seq}`) => {
    if (!on()) return false;
    const text = spokenText(line);
    if (!text) return false;
    dev.voicelines?.stop?.();
    current = id;
    send('voice:say', { id, kind: 'answer', text, lang: LOCALES[lang()] || 'es-MX' });
    return true;
  };
  // (1.11.8) her voice's model starts loading now, while her AI is still writing
  const warm = () => {
    if (on()) send('voice:warm', { lang: LOCALES[lang()] || 'es-MX' });
  };

  // ---- the meter under her answer in her bubble chat ----
  let current = '';
  let meter = null;
  const panel = dev?.panel;
  const meterEl = () => {
    if (meter?.isConnected) return meter;
    const line = panel?.line;
    if (!line?.parentNode) return null;
    meter = document.createElement('div');
    meter.className = 'capri-voice-meter';
    meter.hidden = true;
    meter.innerHTML = '<span class="cvm-icon">🔊</span><span class="cvm-text"></span><span class="cvm-bar"><i></i></span>';
    line.parentNode.insertBefore(meter, line.nextSibling);
    return meter;
  };
  const show = (text, pct) => {
    const el = meterEl();
    if (!el) return;
    if (text == null) { el.hidden = true; return; }
    el.hidden = false;
    el.querySelector('.cvm-text').textContent = text;
    const bar = el.querySelector('.cvm-bar');
    bar.hidden = pct == null;
    if (pct != null) bar.firstChild.style.width = `${Math.max(3, Math.min(100, pct))}%`;
    panel?.place?.();
  };
  if (panel?.say) {
    const panelSay = panel.say.bind(panel);
    panel.say = (msg = {}) => {
      if (!msg.chat) { try { show(null); } catch {} }
      return panelSay(msg);
    };
  }
  on('voice:event', (e = {}) => {
    if (!e.id || e.id !== current || !panel?.current?.chat) return;
    const w = METER[lang()] || METER.en;
    const s = e.state;
    if (s === 'VOICE_PREPARING') show(w.prep, 0);
    else if (s === 'VOICE_PROGRESS' && e.generating !== false && typeof e.pct === 'number') show(`${w.gen} ${e.pct} %`, e.pct);
    else if (s === 'VOICE_STARTED' || s === 'VOICE_READY') show(e.source === 'qwen-mobile' && typeof e.pct === 'number' && e.pct < 100 ? `${w.talk} · ${e.pct} %` : w.talk, null);
    else if (['VOICE_FINISHED', 'VOICE_CANCELLED', 'VOICE_ERROR', 'VOICE_UNAVAILABLE'].includes(s)) { current = ''; show(null); }
  });
  // her bubble chat: every answer of hers passes through remember() (chat.js); listen.js and appchat.js
  // say theirs themselves, and mark it (dev.chatVoiceSkip) so it isn't said twice
  // a new message of yours: the meter of the answer before goes, her voice starts loading
  if (chat?.send) {
    const sendIt = chat.send.bind(chat);
    chat.send = function (...args) {
      try { show(null); current = ''; warm(); } catch {}
      return sendIt(...args);
    };
  }
  if (chat?.close) {
    const closeIt = chat.close.bind(chat);
    chat.close = function (...args) {
      try { show(null); } catch {}
      return closeIt(...args);
    };
  }
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
  return { say, warm };
}

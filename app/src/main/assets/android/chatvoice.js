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

import { send, on as listen } from './native.js';
import { clean } from '../capri/src/renderer/js/chat.js';

const LOCALES = { es: 'es-MX', en: 'en-US', fr: 'fr-FR', pt: 'pt-BR', ru: 'ru-RU', vi: 'vi-VN', ja: 'ja-JP', de: 'de-DE', it: 'it-IT' };
const MAX = 320;

// the meter's words
const METER = {
  es: { prep: 'Preparando su voz…', gen: 'Generando voz', talk: 'Hablando' },
  en: { prep: 'Getting her voice ready…', gen: 'Making her voice', talk: 'Speaking' },
  fr: { prep: 'Préparation de sa voix…', gen: 'Génération de la voix', talk: 'Elle parle' },
  pt: { prep: 'Preparando a voz dela…', gen: 'Gerando a voz', talk: 'Falando' },
  ru: { prep: 'Готовлю её голос…', gen: 'Создаю голос', talk: 'Говорит' },
  vi: { prep: 'Đang chuẩn bị giọng cô ấy…', gen: 'Đang tạo giọng', talk: 'Đang nói' },
  ja: { prep: '声を準備中…', gen: '声を生成中', talk: '話し中' },
  de: { prep: 'Ihre Stimme wird vorbereitet…', gen: 'Ihre Stimme entsteht', talk: 'Spricht' },
  it: { prep: 'Preparo la sua voce…', gen: 'Creo la sua voce', talk: 'Sta parlando' },
};

// what her voice reads: no tags, no markdown, no emoji, no links
const speakable = (line) => String(line || '')
  .replace(/\[\[[^\]]*(\]\]|$)/g, '')
  .replace(/https?:\/\/\S+/g, '')
  .replace(/[*_`#>~|]/g, '')
  .replace(/[\p{Extended_Pictographic}\u{FE0F}\u{200D}]/gu, '')
  .replace(/\s+/g, ' ')
  .trim();

const letters = (x) => String(x || '').toLowerCase().replace(/[^\p{L}\p{N}]+/gu, '');
// what's left of `text` after `said` (its letters), or '' when `text` doesn't start like that
export function restAfter(text, said) {
  const want = letters(said);
  if (!want) return text.trim();
  if (!letters(text).startsWith(want)) return '';
  let n = 0;
  for (let i = 0; i < text.length; i++) {
    if (/[\p{L}\p{N}]/u.test(text[i])) n += letters(text[i]).length;
    if (n >= want.length) return text.slice(i + 1).replace(/^[\s.,;:!?…»”"')\]]+/u, '').trim();
  }
  return '';
}

export function spokenText(line) {
  let s = speakable(line);
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
  const locale = () => LOCALES[lang()] || 'es-MX';
  const say = (line, id = `chat-${++seq}`) => {
    if (!on()) return false;
    const text = spokenText(line);
    // (1.11.8) already being said as her AI wrote it: only what's left, and it's the end
    const s = [...streams.values()].find((x) => x.voiceId === id && x.sent);
    if (s) {
      for (const [k, v] of streams) if (v === s) streams.delete(k);
      // (1.12.0) the rest of it, from where her voice is: compared by letters only (spaces or a quote mark
      // written differently in the end don't make her start over). Never said again from the start: if the
      // end is something else, she finishes what she started.
      send('voice:say', { id, kind: 'answer', text: restAfter(text, s.sent), lang: locale(), part: 'last' });
      return true;
    }
    if (!text) return false;
    dev.voicelines?.stop?.();
    current = id;
    send('voice:say', { id, kind: 'answer', text, lang: locale() });
    return true;
  };

  // ---- (1.11.8) her answer said while her AI writes it: each finished sentence goes to her voice right away,
  // so her neural voice starts on the first one instead of waiting for the whole answer (Ears.kt sayPart).
  const streams = new Map(); // her AI's request id → { voiceId, raw, sent }
  const follow = (chatId, voiceId) => {
    if (!on() || !chatId) return;
    streams.set(chatId, { voiceId, raw: '', sent: '' });
    if (streams.size > 4) streams.delete(streams.keys().next().value);
  };
  listen('chat:delta', ({ id, text } = {}) => {
    const s = streams.get(id);
    if (!s || !on()) return;
    s.raw += String(text || '');
    const said = speakable(clean(s.raw));
    // up to the last finished sentence (the rest is still being written)
    let end = -1;
    for (const m of said.matchAll(/[.!?…]+(?=\s)/g)) end = m.index + m[0].length;
    // (1.11.8, Emma: "en el S25 Ultra todavía tarda en hablar") a long first sentence: her voice starts on its
    // first part (up to a comma), so the first sound comes sooner
    if (!s.sent && end < 0 && said.length >= 40) {
      for (const m of said.slice(0, 60).matchAll(/[,;:](?=\s)/g)) if (m.index >= 12) end = m.index + 1;
    }
    if (end <= s.sent.length || end > MAX) return;
    const piece = said.slice(s.sent.length, end).trim();
    if (!piece || (!s.sent && piece.length < 12)) return;
    if (!s.sent) {
      dev.voicelines?.stop?.();
      current = s.voiceId;
    }
    s.sent = said.slice(0, end);
    send('voice:say', { id: s.voiceId, kind: 'answer', text: piece, lang: locale(), part: 'more' });
  });
  // (1.11.8) her voice's model starts loading now, while her AI is still writing
  const warm = () => {
    if (on()) send('voice:warm', { lang: LOCALES[lang()] || 'es-MX' });
  };

  // ---- the meter under her answer in her bubble chat ----
  let current = '';
  let bubbleVoice = '';
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
  listen('voice:event', (e = {}) => {
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
      const out = sendIt(...args);
      try { if (chat.turn) { bubbleVoice = `chat-${++seq}`; follow(chat.turn, bubbleVoice); } } catch {}
      return out;
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
        if (message?.role === 'assistant' && typeof message.content === 'string' && !dev.chatVoiceSkip) {
          say(message.content, bubbleVoice || undefined);
          bubbleVoice = '';
        }
      } catch {}
      return out;
    };
  }
  return { say, warm, follow };
}

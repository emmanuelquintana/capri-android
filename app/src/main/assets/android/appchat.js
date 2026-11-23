// Her chat inside the app (1.11.0, Emma: "que el centro de la app sea directamente un chat con ella, para
// chatearte con Capri cómodamente"). The app's Chat tab (MainActivity) sends what you write here
// ("app:chat"); she answers with the same brain as her bubble chat (her persona, her memory, her moods,
// what she can do on your phone) and the answer goes back to the app ("appchat:reply"). Both sides of it
// land in your chat history (chatlog.js), so the tab shows the whole conversation, bubble chat included.

import { on, send } from './native.js';
import { extraAsk } from './asks.js';
import { clean, mood } from '../capri/src/renderer/js/chat.js';

export function appChat({ lang }) {
  let busy = 0;
  on('app:chat', async ({ id = '', text = '' } = {}) => {
    const dev = window.dev;
    const chat = dev?.chat;
    text = String(text).trim().slice(0, 2000);
    // (1.11.7) her answer out loud too (chatvoice.js), not only written in the Chat tab
    const reply = (line, face = 'smug', ok = true) => {
      send('appchat:reply', { id, text: String(line || '…'), face, ok });
      // (1.11.8) with the id of your message: the Chat tab shows its voice's percentage under her answer
      if (ok && line) dev?.chatVoice?.say(line, `app-${id}`);
    };
    if (!chat || !text) return reply('…', 'aside', false);
    if (busy && Date.now() - busy < 120000) return reply(WAIT[lang()] || WAIT.en, 'aside', false);
    busy = Date.now();
    dev?.chatVoice?.warm?.(); // (1.11.8) her voice starts loading while her AI writes
    try {
      dev.capri?.pulse?.(); // (over your screen too: she heard you)
      // what she does right away (games, history, a WhatsApp message, music, an app…), not asked to her AI
      const did = extraAsk(text, lang());
      if (did) return reply(did.line, did.face);
      const asked = chat.asked?.(text);
      if (asked && !['look', 'close', 'files', 'shot'].includes(asked.kind)) {
        chat.listApps?.();
        const done = await chat.doNow(asked).catch(() => null);
        if (done?.line) {
          chat.history.push({ role: 'user', content: text });
          dev.chatVoiceSkip = true; chat.remember({ role: 'assistant', content: done.line }); dev.chatVoiceSkip = false;
          return reply(done.line, done.face || 'smug');
        }
      }
      const status = await window.host.chatStatus().catch(() => ({ ok: false }));
      if (!status.ok) return reply(NO_AI[lang()] || NO_AI.en, 'aside', false);
      const messages = [
        { role: 'system', content: chat.persona() },
        ...chat.history.slice(-30),
        { role: 'user', content: `${text}\n\n(Note for you, never repeat it: ${chat.languageNote()})` },
      ];
      const res = await window.host.chatSend({ id: Date.now(), messages }).catch((err) => ({ ok: false, error: err.message }));
      if (!res?.ok) return reply(res?.code === 'busy' ? chat.t?.swamped : chat.t?.failed || '…', 'wide', false);
      const said = clean(res.text) || '…';
      chat.history.push({ role: 'user', content: text });
      dev.chatVoiceSkip = true; chat.remember({ role: 'assistant', content: said }); dev.chatVoiceSkip = false; // (into your history too: chatlog.js; said by reply())
      reply(said, mood(said));
    } catch (err) {
      console.error('[capri] app chat', err);
      reply('…', 'wide', false);
    } finally {
      busy = 0;
    }
  });
}

const WAIT = {
  es: 'Dame un segundo, sigo pensando lo de antes.',
  en: 'Give me a second, still thinking about the last one.',
  fr: 'Une seconde, je réfléchis encore au message d’avant.',
  pt: 'Me dá um segundo, ainda tô pensando na anterior.',
  ru: 'Секунду, я ещё думаю над прошлым.',
};
const NO_AI = {
  es: 'Todavía no tengo una IA para hablar. Elige una en Ajustes › Su IA y vuelve.',
  en: 'I don’t have an AI to talk with yet. Pick one in Settings › Her AI and come back.',
  fr: 'Je n’ai pas encore d’IA pour parler. Choisis-en une dans Réglages › Son IA.',
  pt: 'Ainda não tenho uma IA pra conversar. Escolha uma em Ajustes › A IA dela.',
  ru: 'У меня пока нет ИИ для разговора. Выбери его в Настройках › Её ИИ.',
};

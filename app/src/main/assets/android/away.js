// Her chat closed while she was still thinking (1.10.0): she keeps thinking.
//
// "Cuando alguien le hace una pregunta a Capri y toca una parte de la pantalla sin querer, Capri no
// cancele la pregunta": before, a tap anywhere outside her chat closed it and stopped her answer.
// Now the question goes on while you look at something else or play; when her answer is in, it comes
// up in her bubble (a tap on «Responder» opens her chat again, the conversation as it was). Opening
// her chat before it's in: it comes in there, as always. Every answer is also kept in your chat
// history (chatlog.js).

import { send } from './native.js';

const T = {
  es: { reply: 'Responder', back: 'Te contesto:' },
  en: { reply: 'Reply', back: 'Here’s my answer:' },
  fr: { reply: 'Répondre', back: 'Voilà ma réponse :' },
  pt: { reply: 'Responder', back: 'Te respondo:' },
  ru: { reply: 'Ответить', back: 'Вот мой ответ:' },
};

export function keepAsking({ lang }) {
  const dev = window.dev;
  const chat = dev?.chat;
  const panel = dev?.panel;
  if (!chat || !panel) return;
  let away = 0; // the turn that goes on with her chat closed

  const close = chat.close.bind(chat);
  chat.close = function (...args) {
    if (!this.open || !this.turn) return close(...args);
    // closed as usual, only her answer isn't stopped
    const turn = this.turn;
    this.turn = 0;
    try { close(...args); } finally { this.turn = turn; }
    away = turn;
    send('log', { level: 'I', text: 'her chat closed while she thought: she goes on' });
  };

  const start = chat.start?.bind(chat);
  if (start) chat.start = (...args) => { away = 0; return start(...args); };

  const ask = chat.send.bind(chat);
  chat.send = async function (text, ...rest) {
    await ask(text, ...rest);
    if (!away || this.open) return;
    away = 0;
    const last = this.history.at(-1);
    if (last?.role !== 'assistant') return; // (it failed: nothing to show)
    const t = T[lang()] || T.en;
    const answer = String(last.content || '');
    const long = answer.length > 160;
    panel.say({
      kind: 'done',
      face: 'look',
      tag: t.back,
      quote: String(text).slice(0, 80),
      ...(long ? { reply: answer } : { line: answer }),
      hold: Math.min(30000, 8000 + answer.length * 60),
      actions: [{ id: 'reply', label: t.reply, primary: true }],
      onAction: () => dev.chat?.start?.(),
    });
    dev.capri?.perform?.('hop');
  };
}

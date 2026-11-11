// "Compartir → Capri" (1.9.0, the community: "que puedas compartir cosas como videos o imágenes a
// Capri, así como pasa con ChatGPT"). Any app's share button: ShareActivity.kt takes the picture (a
// video as three of its frames) and her page gets `share:in`. She opens her chat and reacts to it
// like a friend you sent it to, with the conversation so far in mind; then you go on talking about it.
// A text or a link alone goes in as your message. Her AI has to see pictures for the rest (as for
// her looks at your screen): she says so if hers can't.

import { call, on } from './native.js';

const SAID = {
  es: { image: '📷 Te mandé una imagen', video: '🎬 Te mandé un video' },
  en: { image: '📷 Sent you a picture', video: '🎬 Sent you a video' },
  fr: { image: '📷 Je t’ai envoyé une image', video: '🎬 Je t’ai envoyé une vidéo' },
  pt: { image: '📷 Te mandei uma imagem', video: '🎬 Te mandei um vídeo' },
  ru: { image: '📷 Картинка для тебя', video: '🎬 Видео для тебя' },
};
const fmt = (line, vars) => String(line || '').replace(/\{(\w+)\}/g, (_, k) => vars[k] ?? '');

export function sharing() {
  const dev = window.dev;
  on('share:in', () => take().catch((err) => console.error('[capri] share', err)));

  async function take() {
    const got = await call('share:take');
    if (!got || !dev.chat) return;
    const { chat, panel, brain } = dev;
    const lang = SAID[brain?.lang] ? brain.lang : 'en';
    await chat.start();
    if (!chat.open) return; // (her setup first: no AI yet)
    for (let i = 0; i < 60 && chat.turn; i++) await new Promise((r) => setTimeout(r, 500));
    const text = String(got.text || '').trim();
    if (got.kind === 'text' || !got.image) {
      if (text) chat.send(text);
      return;
    }
    const id = Date.now();
    chat.turn = id;
    const quote = [SAID[lang][got.kind] || SAID[lang].image, text].filter(Boolean).join(': ');
    chat.history.push({ role: 'user', content: quote });
    panel.setActions?.([]);
    panel.chatTurn({ quote, face: 'surprised' });
    panel.setChatBusy(true);
    const what = got.kind === 'video'
      ? 'three frames of a video (from its start, middle and end, side by side)'
      : 'a picture';
    const res = await window.host.lookSay({
      id: window.host.lookPut(got.image),
      persona: chat.lookPersona(),
      history: chat.history.slice(-12, -1),
      prompt: `${text || '(no message, just this)'}\n\n(Note for you, never repeat it: the user just shared ${what} with you from another app, like sending it to a friend. React to what's in it as yourself, in a few sentences: what it is, what you think, a joke if it fits; if they asked something about it, answer that. ${chat.languageNote()})`,
    });
    if (chat.turn !== id) return;
    chat.turn = 0;
    const tl = brain.t.look;
    let answer;
    if (!res?.ok) {
      const model = String(res?.model || '…').replace(/:latest$/, '');
      chat.history.pop();
      answer = res?.code === 'novision' ? fmt(tl.noVision, { model }) : res?.code === 'noai' ? tl.noAI : fmt(tl.failed, { error: res?.error || '…' });
      panel.setFace('wide');
    } else {
      answer = String(res.text || '').replace(/<think>[\s\S]*?<\/think>/g, '').trim() || '…';
      chat.remember({ role: 'assistant', content: answer });
      panel.setFace('laugh');
    }
    panel.stream(answer, { done: true });
    panel.setChatBusy(false);
  }
}

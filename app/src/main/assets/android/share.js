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
  vi: { image: '📷 Gửi bạn một bức ảnh', video: '🎬 Gửi bạn một video' },
  ja: { image: '📷 画像を送ったよ', video: '🎬 動画を送ったよ' },
  de: { image: '📷 Hab dir ein Bild geschickt', video: '🎬 Hab dir ein Video geschickt' },
  it: { image: '📷 Ti ho mandato una foto', video: '🎬 Ti ho mandato un video' },
};
// (1.12.0) documents and sounds
const SAID2 = {
  es: { pdf: '📄 Te mandé un PDF', doc: '📝 Te mandé un documento', audio: '🎵 Te mandé un audio', nomic: 'No pude usar el micrófono. ¿Me diste permiso?', noear: 'Para escuchar audios necesito Gemini: ponme una clave gratis de Gemini en Ajustes › IA (puede ser además de la que ya tienes).' },
  en: { pdf: '📄 Sent you a PDF', doc: '📝 Sent you a document', audio: '🎵 Sent you an audio', nomic: 'I couldn’t use the mic. Did you give me permission?', noear: 'To listen to audio I need Gemini: give me a free Gemini key in Settings › AI (it can be on top of the one you have).' },
  fr: { pdf: '📄 Je t’ai envoyé un PDF', doc: '📝 Je t’ai envoyé un document', audio: '🎵 Je t’ai envoyé un audio', nomic: 'Je n’ai pas pu utiliser le micro. Tu m’as donné la permission ?', noear: 'Pour écouter un audio il me faut Gemini : donne-moi une clé Gemini gratuite dans Réglages › IA.' },
  pt: { pdf: '📄 Te mandei um PDF', doc: '📝 Te mandei um documento', audio: '🎵 Te mandei um áudio', nomic: 'Não consegui usar o microfone. Você me deu permissão?', noear: 'Pra ouvir áudios eu preciso do Gemini: coloca uma chave grátis do Gemini em Ajustes › IA.' },
  ru: { pdf: '📄 PDF для тебя', doc: '📝 Документ для тебя', audio: '🎵 Аудио для тебя', nomic: 'Не получилось включить микрофон. Ты дал разрешение?', noear: 'Чтобы слушать аудио, мне нужен Gemini: добавь бесплатный ключ Gemini в Настройки › ИИ.' },
  vi: { pdf: '📄 Gửi bạn một PDF', doc: '📝 Gửi bạn một tài liệu', audio: '🎵 Gửi bạn một đoạn âm thanh', nomic: 'Mình không dùng được micro. Bạn đã cho phép chưa?', noear: 'Để nghe âm thanh mình cần Gemini: thêm khóa Gemini miễn phí trong Cài đặt › AI.' },
  ja: { pdf: '📄 PDFを送ったよ', doc: '📝 文書を送ったよ', audio: '🎵 音声を送ったよ', nomic: 'マイクが使えなかった。許可してくれた？', noear: '音声を聞くにはGeminiが必要：設定 › AI で無料のGeminiキーを追加してね。' },
  de: { pdf: '📄 Hab dir ein PDF geschickt', doc: '📝 Hab dir ein Dokument geschickt', audio: '🎵 Hab dir ein Audio geschickt', nomic: 'Ich konnte das Mikro nicht benutzen. Hast du es erlaubt?', noear: 'Um Audio zu hören, brauche ich Gemini: gib mir einen kostenlosen Gemini-Schlüssel unter Einstellungen › KI.' },
  it: { pdf: '📄 Ti ho mandato un PDF', doc: '📝 Ti ho mandato un documento', audio: '🎵 Ti ho mandato un audio', nomic: 'Non sono riuscita a usare il microfono. Mi hai dato il permesso?', noear: 'Per ascoltare un audio mi serve Gemini: dammi una chiave Gemini gratis in Impostazioni › IA.' },
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
    const said2 = SAID2[brain?.lang] || SAID2.en;
    // (1.12.0) a Word or text file: its words go in with your message, and she works on them like a friend who reads it
    if (got.kind === 'doc' && got.doc) {
      const name = got.name ? ` (${got.name})` : '';
      chat.send(`${said2.doc}${name}${text ? `: ${text}` : ''}\n\n(Note for you, never repeat it: this is the document's text. Read it and help with it as yourself: what it's about in a few lines, and anything they asked (mistakes, a summary, ideas to make it better). The text:\n"""\n${String(got.doc).slice(0, 12000)}\n""")`);
      return;
    }
    // (1.12.0) a song or a sound: Gemini hears it (host.js hearSound)
    if (got.kind === 'audio' && got.audio) return hear({ audio: got.audio, format: got.format, quote: [said2.audio, got.name, text].filter(Boolean).join(' · '), text });
    if (got.kind === 'text' || !got.image) {
      if (text) chat.send(text);
      return;
    }
    const id = Date.now();
    chat.turn = id;
    const quote = [said2[got.kind] || SAID[lang][got.kind] || SAID[lang].image, text].filter(Boolean).join(': ');
    chat.history.push({ role: 'user', content: quote });
    panel.setActions?.([]);
    panel.chatTurn({ quote, face: 'surprised' });
    panel.setChatBusy(true);
    const what = got.kind === 'video'
      ? 'three frames of a video (from its start, middle and end, side by side)'
      : got.kind === 'pdf'
        ? `the first pages of a PDF document${got.name ? ` called "${got.name}"` : ''}, side by side (read them: what it's about, and help with what they ask: mistakes, a summary, ideas)`
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

  // a sound, heard by her AI (Gemini): what song it is, who sings it, what she thinks of it
  async function hear({ audio, format, quote, text }) {
    const { chat, panel } = dev;
    const id = Date.now();
    chat.turn = id;
    chat.history.push({ role: 'user', content: quote });
    panel.setActions?.([]);
    panel.chatTurn({ quote, face: 'surprised' });
    panel.setChatBusy(true);
    const res = await window.host.hearSay({
      audio, format,
      persona: chat.lookPersona(),
      history: chat.history.slice(-8, -1),
      prompt: `${text || '(no message, just this)'}\n\n(Note for you, never repeat it: the user shared a sound with you. If it's music: say which song it is and who sings it if you recognize it (say so honestly if you aren't sure), and what you think of it. If it's someone talking: what they say, briefly. Answer as yourself, a few sentences. ${chat.languageNote()})`,
    });
    if (chat.turn !== id) return;
    chat.turn = 0;
    let answer;
    if (!res?.ok) {
      chat.history.pop();
      const s2 = SAID2[dev.brain?.lang] || SAID2.en;
      answer = res?.code === 'noear' ? s2.noear : fmt(dev.brain.t.look.failed, { error: res?.error || '…' });
      panel.setFace('wide');
    } else {
      answer = String(res.text || '').replace(/<think>[\s\S]*?<\/think>/g, '').trim() || '…';
      chat.remember({ role: 'assistant', content: answer });
      panel.setFace('laugh');
    }
    panel.stream(answer, { done: true });
    panel.setChatBusy(false);
  }
  // "Capri, ¿qué canción es esta?": she listens for 10 s (Ears.kt: her ears step aside meanwhile), then hears it
  async function song(quote) {
    const { chat } = dev;
    await chat.start();
    if (!chat.open) return;
    for (let i = 0; i < 60 && chat.turn; i++) await new Promise((r) => setTimeout(r, 500));
    dev.panel.chatTurn({ quote: `🎧 ${quote}`, face: 'focus' });
    const rec = await window.host.hearRecord(10);
    if (!rec?.audio) return dev.panel.stream((SAID2[dev.brain?.lang] || SAID2.en).nomic || '…', { done: true });
    return hear({ audio: rec.audio, format: 'wav', quote: `🎧 ${quote}`, text: quote });
  }
  return { hear, song };
}

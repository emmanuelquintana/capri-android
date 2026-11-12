// New things to ask her (1.10.0), out loud ("Capri, …", listen.js) or in her chat:
// - "juguemos ajedrez", "play with me": her games screen opens (games.html).
// - "mi historial", "chat history": your chat history (chatlog.html).
// - "mándale a Ana por WhatsApp que ya voy": WhatsApp (or Telegram, Messenger, SMS…) opens with the
//   message written; you pick who and tap send. Android doesn't let an app send a message for you
//   (and she never would without you seeing it).

import { send } from './native.js';

const GAMES_ASK = /juguemos|jugar (?:algo|contigo|conmigo|ajedrez|buscaminas|cartas|al? (?:21|gato|ajedrez|buscaminas|blackjack|tres en raya))|juega(?:s)? conmigo|quiero jugar (?:contigo|algo|ajedrez|buscaminas|cartas)|tus juegos|let'?s play|play (?:a game|with me|chess|minesweeper|cards|blackjack|tic.?tac.?toe)|your games|jouons|on joue|joue avec moi|jouer aux? (?:échecs|cartes|démineur|morpion)|vamos jogar|bora jogar|joga comigo|jogar (?:xadrez|cartas|campo minado|jogo da velha)|давай (?:сыграем|поиграем)|поиграй со мной|сыграем в/iu;
const HISTORY_ASK = /(?:mi|el|nuestro|tu) historial|historial (?:de|del) chat|chat history|our (?:chat|history)|historique|hist[oó]rico (?:do|de) chat|meu hist[oó]rico|история (?:чата|переписки)/iu;
const APPS = [
  ['whatsapp', /whats ?app|wasap|wsp|guasap|ватсап|вотсап/iu, 'com.whatsapp'],
  ['telegram', /telegram|телеграм/iu, 'org.telegram.messenger'],
  ['messenger', /messenger|мессенджер/iu, 'com.facebook.orca'],
  ['instagram', /instagram|insta\b|инстаграм/iu, 'com.instagram.android'],
  ['signal', /\bsignal\b/iu, 'org.thoughtcrime.securesms'],
  ['sms', /\bsms\b|mensaje de texto|text message|texto|смс/iu, null],
];
const MSG_VERB = /m[aá]nd(?:a|ale|ales)|env[ií]a(?:le|les)?|escr[ií]be(?:le|les)?|dile|send|text|message|tell|envoie|[ée]cris|dis(?:-lui)?|envia|escreve|fala|manda|напиши|отправь|скажи|передай/iu;
const MSG_BODY = /(?:\bque\b|diciendo|saying|that|\bque\b|qu['’]|dizendo|falando|что|:)\s*(.+)$/iu;
const MSG_TO = /(?:^|\s)(?:a|to|à|para|pra)\s+((?:(?:mi|my|ma|mon|minha|meu)\s+)?[\p{L}'’-]+)/giu;
const NOT_WHO = /^(?:un|una|a|an|um|uma|un|une|mensaje|message|mensagem|whats ?app|telegram|sms|texto|text)$/iu;
const who = (said) => [...said.matchAll(MSG_TO)].map((m) => m[1]).find((w) => !NOT_WHO.test(w.split(/\s+/).pop())) || '';

const T = {
  es: { games: '¡Va! Elige el juego.', history: 'Aquí está todo lo que hemos hablado.', msg: 'Listo: te abrí {app} con el mensaje. Elige a {to}y toca enviar.', msgNoText: 'Te abrí {app}. ¿Qué le quieres decir? Escríbelo tú, yo no envío nada sin ti.', anyone: '' },
  en: { games: 'Okay! Pick a game.', history: 'Here’s everything we’ve talked about.', msg: 'Done: I opened {app} with the message. Pick {to}and tap send.', msgNoText: 'I opened {app}. Write what you want to say: I never send anything without you.', anyone: '' },
  fr: { games: 'D’accord ! Choisis un jeu.', history: 'Voilà tout ce qu’on s’est dit.', msg: 'C’est prêt : j’ai ouvert {app} avec le message. Choisis {to}et touche envoyer.', msgNoText: 'J’ai ouvert {app}. Écris ce que tu veux dire : je n’envoie jamais rien sans toi.', anyone: '' },
  pt: { games: 'Bora! Escolhe o jogo.', history: 'Aqui está tudo o que a gente conversou.', msg: 'Pronto: abri o {app} com a mensagem. Escolhe {to}e toca em enviar.', msgNoText: 'Abri o {app}. Escreve o que quer dizer: eu nunca envio nada sem você.', anyone: '' },
  ru: { games: 'Давай! Выбирай игру.', history: 'Вот всё, о чём мы говорили.', msg: 'Готово: я открыла {app} с сообщением. Выбери {to}и нажми «отправить».', msgNoText: 'Я открыла {app}. Напиши, что хочешь сказать: я ничего не отправляю без тебя.', anyone: '' },
};
const NAMES = { whatsapp: 'WhatsApp', telegram: 'Telegram', messenger: 'Messenger', instagram: 'Instagram', signal: 'Signal', sms: 'SMS' };

/** What she does for this, or null: { line, face }. */
export function extraAsk(text, lang) {
  const t = T[lang] || T.en;
  const said = String(text || '').trim();
  if (!said) return null;
  const words = said.split(/\s+/).length;
  if (GAMES_ASK.test(said) && words <= 10) {
    send('page:open', { page: 'games' });
    return { line: t.games, face: 'laugh' };
  }
  if (HISTORY_ASK.test(said) && words <= 10) {
    send('page:open', { page: 'chatlog' });
    return { line: t.history, face: 'look' };
  }
  const app = APPS.find(([, re]) => re.test(said));
  if (app && MSG_VERB.test(said)) {
    const [id, re, pkg] = app;
    // what to say: after "que", "diciendo", ":"…; else what's after the app's name
    let body = MSG_BODY.exec(said.replace(re, ' '))?.[1] || said.split(re)[1] || '';
    body = body.replace(/^[\s,.:;-]+/, '').replace(/^(?:que|diciendo|saying|that|dizendo|что)\s+/iu, '').trim();
    const to = who(said.split(MSG_BODY)[0] || said);
    send('msg:share', { app: id, pkg, text: body });
    const line = (body ? t.msg : t.msgNoText).replace('{app}', NAMES[id]).replace('{to}', to ? `${to} ` : '');
    return { line, face: 'smug' };
  }
  return null;
}

// her chat: these happen here, not as a question to her AI (which would pretend it sent it)
export function chatAsks({ lang }) {
  const chat = window.dev?.chat;
  if (!chat) return;
  const ask = chat.send.bind(chat);
  chat.send = async function (text, ...rest) {
    if (this.turn || !this.open || this.agent) return ask(text, ...rest);
    const did = extraAsk(text, lang());
    if (!did) return ask(text, ...rest);
    this.history.push({ role: 'user', content: text });
    this.panel.chatTurn?.({ quote: text, face: did.face });
    this.panel.stream(did.line, { done: true });
    this.panel.setFace(did.face);
    this.remember({ role: 'assistant', content: did.line });
    return undefined;
  };
}

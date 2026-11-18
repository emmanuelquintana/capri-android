// Modo reacción (1.11.0, Emma: "lo que vi con ella en YouTube, que aprendió cosas que me gustan gracias a los
// videos: un modo reacción donde, al ver los videos junto con ella, opine, anote o algo por el estilo").
// On ("Capri, modo reacción" / "veamos videos juntas", or Personalizar): while a video plays (GameWatch's
// `phone:activity`: something playing, a video app in front), she watches it with you through her live look
// at your screen and, every minute or so, says what she thinks of what's on it, like a friend on the couch.
// What she said goes into your chat history (the app's Chat tab shows it: her notes of what you watched
// together) and into her conversation, so she remembers what you like. Her AI has to see pictures.

import { on, send } from './native.js';
import { clean } from '../capri/src/renderer/js/chat.js';

const NOTE = "(You're watching this video together with the user, right now, like a friend on the couch. React to what's happening on the screen in one or two short sentences: an opinion, a joke, a reaction or a quick question. If it shows something they seem to like (a topic, a creator, an artist, a game), say it naturally so you remember it. Never describe the screen like a robot, never mention screenshots.)";
export const REACT_ON = /modo reacci[oó]n|veamos (?:videos|un video|youtube|tiktok)|ve(?:r|amos)? (?:videos|el video|esto) conmigo|reacciona(?: a(?:l)?)? (?:video|esto)|reaction mode|watch (?:videos|this|it) with me|react to (?:this|the video)|mode r[ée]action|regardons (?:des vid[ée]os|[çc]a)|modo rea[çc][ãa]o|assiste (?:comigo|isso comigo)|vamos ver (?:v[ií]deos|isso)|режим реакции|посмотри (?:со мной|это)|давай смотреть/iu;
const OFF = /quita|apaga|sin |desactiva|off|stop|d[ée]sactive|arr[êe]te|desliga|tira|выключи|хватит/iu;
const T = {
  es: { on: '¡Modo reacción! Pon tu video, yo lo veo contigo y te digo qué opino.', off: 'Listo, ya no comento tus videos. Fue divertido.', blind: 'Para ver tus videos contigo necesito ver tu pantalla: permíteme compartirla.' },
  en: { on: 'Reaction mode! Put on your video, I’ll watch it with you and tell you what I think.', off: 'Okay, no more comments on your videos. That was fun.', blind: 'To watch your videos with you I need to see your screen: let me share it.' },
  fr: { on: 'Mode réaction ! Lance ta vidéo, je la regarde avec toi et je te dis ce que j’en pense.', off: 'D’accord, je ne commente plus tes vidéos. C’était drôle.', blind: 'Pour regarder tes vidéos avec toi, j’ai besoin de voir ton écran : laisse-moi le partager.' },
  pt: { on: 'Modo reação! Coloca seu vídeo, eu assisto com você e digo o que acho.', off: 'Pronto, não comento mais seus vídeos. Foi divertido.', blind: 'Pra ver seus vídeos com você eu preciso ver sua tela: deixa eu compartilhar.' },
  ru: { on: 'Режим реакции! Включай видео, я посмотрю с тобой и скажу, что думаю.', off: 'Ладно, больше не комментирую твои видео. Было весело.', blind: 'Чтобы смотреть видео с тобой, мне нужно видеть экран: разреши показ.' },
};
const rand = (a, b) => a + Math.random() * (b - a);

export function reactMode({ cfg, setConfig, lang }) {
  const dev = window.dev;
  const chat = dev?.chat;
  if (!chat || !window.host?.lookSay) return null;
  let state = { sound: false, kind: '', at: 0 };
  on('phone:activity', (s = {}) => { state = { sound: Boolean(s.sound), kind: String(s.kind || ''), at: Date.now() }; });
  // a video on: a video app in front (with "Acceso de uso") or, without it, something playing that isn't music
  const watching = () => Date.now() - state.at < 20000 && (state.kind === 'video' || (state.sound && state.kind !== 'music' && state.kind !== 'game'));
  const active = () => Boolean(cfg().reactMode && cfg().watchScreen);

  const lookSay = window.host.lookSay;
  window.host.lookSay = async (req = {}) => {
    const mine = active() && watching() && !String(req.prompt || '').includes('Note for you, never repeat it: the picture');
    const res = await lookSay(mine ? { ...req, prompt: `${req.prompt}\n${NOTE}` } : req);
    if (mine && res?.ok && res.text) {
      const text = clean(res.text);
      if (text) {
        // her note of what you watched together: your history, and her conversation
        send('chatlog:add', { q: '🎬', a: text, at: Date.now() });
        chat.history.push({ role: 'assistant', content: `(watching a video together) ${text}` });
      }
    }
    return res;
  };

  let next = 0;
  setInterval(() => {
    if (!active() || !watching() || document.hidden || !dev.look) return (next = 0);
    const now = Date.now();
    if (!next) next = now + rand(25, 45) * 1000;
    if (now < next || dev.look.looking || dev.panel?.current || chat.open || window.host.lookResting?.() > now) return;
    next = now + rand(60, 110) * 1000;
    dev.look.now().catch?.(() => {});
  }, 5000);

  return {
    /** "Capri, modo reacción" / "quita el modo reacción": { line, face } or null. */
    ask(text) {
      if (!REACT_ON.test(text)) return null;
      const t = T[lang()] || T.en;
      if (OFF.test(text)) {
        setConfig({ reactMode: false });
        return { line: t.off, face: 'calm' };
      }
      setConfig({ reactMode: true, watchScreen: true });
      return { line: t.on, face: 'laugh' };
    },
  };
}

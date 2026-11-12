// She asks how you are (1.10.0): "que cada cierto tiempo pregunte cómo estás". Every few hours at
// most (and only once in a while: not every day at the same time), when you're on your phone and
// she's free, she asks; three buttons to answer. What you said she keeps in mind for her next chats
// (memory.js), and if it's not good, she offers to talk. Off with her long-term memory off (Tú).

const T = {
  es: {
    ask: ['¿Cómo estás, {name}?', 'Ey, {name}… ¿cómo va tu día?', '¿Todo bien por ahí, {name}?', 'Pregunta rápida: ¿cómo te sientes hoy?'],
    good: 'Bien', meh: 'Más o menos', bad: 'Mal',
    goodLine: ['¡Me alegra! Sigue así.', 'Eso quería oír.', 'Bien. Yo también, ahora.'],
    mehLine: ['Hay días así. Aquí estoy si quieres hablar.', 'Ok… si algo te molesta, cuéntame.'],
    badLine: ['Lo siento, {name}. ¿Quieres contarme? Abro el chat.', 'Ey… aquí estoy. ¿Hablamos?'],
    talk: 'Hablemos',
    said: { good: 'bien', meh: 'más o menos', bad: 'mal' },
  },
  en: {
    ask: ['How are you, {name}?', 'Hey, {name}… how’s your day going?', 'All good over there, {name}?', 'Quick question: how are you feeling today?'],
    good: 'Good', meh: 'So-so', bad: 'Bad',
    goodLine: ['Glad to hear it! Keep it up.', 'That’s what I wanted to hear.', 'Good. Me too, now.'],
    mehLine: ['Some days are like that. I’m here if you want to talk.', 'Okay… if something’s bugging you, tell me.'],
    badLine: ['I’m sorry, {name}. Want to tell me? I’ll open the chat.', 'Hey… I’m here. Wanna talk?'],
    talk: 'Let’s talk',
    said: { good: 'good', meh: 'so-so', bad: 'bad' },
  },
  fr: {
    ask: ['Comment tu vas, {name} ?', 'Hé, {name}… ta journée se passe bien ?', 'Tout va bien de ton côté, {name} ?', 'Petite question : comment tu te sens aujourd’hui ?'],
    good: 'Bien', meh: 'Bof', bad: 'Mal',
    goodLine: ['Ça me fait plaisir ! Continue.', 'C’est ce que je voulais entendre.', 'Bien. Moi aussi, du coup.'],
    mehLine: ['Il y a des jours comme ça. Je suis là si tu veux parler.', 'D’accord… si quelque chose te dérange, dis-le-moi.'],
    badLine: ['Désolée, {name}. Tu veux m’en parler ? J’ouvre le chat.', 'Hé… je suis là. On parle ?'],
    talk: 'Parlons',
    said: { good: 'bien', meh: 'bof', bad: 'mal' },
  },
  pt: {
    ask: ['Como você está, {name}?', 'Ei, {name}… como tá o seu dia?', 'Tudo bem por aí, {name}?', 'Pergunta rápida: como você tá se sentindo hoje?'],
    good: 'Bem', meh: 'Mais ou menos', bad: 'Mal',
    goodLine: ['Que bom! Continua assim.', 'Era isso que eu queria ouvir.', 'Boa. Eu também, agora.'],
    mehLine: ['Tem dias assim. Tô aqui se quiser conversar.', 'Tá… se algo te incomoda, me conta.'],
    badLine: ['Sinto muito, {name}. Quer me contar? Abro o chat.', 'Ei… tô aqui. Vamos conversar?'],
    talk: 'Vamos conversar',
    said: { good: 'bem', meh: 'mais ou menos', bad: 'mal' },
  },
  ru: {
    ask: ['Как ты, {name}?', 'Эй, {name}… как проходит день?', 'У тебя всё хорошо, {name}?', 'Быстрый вопрос: как ты сегодня себя чувствуешь?'],
    good: 'Хорошо', meh: 'Так себе', bad: 'Плохо',
    goodLine: ['Рада слышать! Так держать.', 'Это я и хотела услышать.', 'Хорошо. Мне теперь тоже.'],
    mehLine: ['Бывают такие дни. Я рядом, если захочешь поговорить.', 'Ладно… если что-то беспокоит, расскажи.'],
    badLine: ['Мне жаль, {name}. Хочешь рассказать? Открою чат.', 'Эй… я рядом. Поговорим?'],
    talk: 'Поговорим',
    said: { good: 'хорошо', meh: 'так себе', bad: 'плохо' },
  },
};
const pick = (list) => list[Math.floor(Math.random() * list.length)];
const HOUR = 3600 * 1000;

export function checkIn({ cfg, setConfig, lang, readMemory, saveMemory }) {
  const dev = window.dev;
  const { capri, panel, chat } = dev || {};
  if (!capri || !panel) return null;
  const name = () => String(cfg().userName || '').trim();
  const fill = (s) => s.replace(/,?\s*\{name\}/g, (m) => (name() ? m.replace('{name}', name()) : '')).replace(/\s+([?!.])/g, '$1');

  const due = () => {
    const c = cfg();
    if (c.longMemory === false || c.checkIn === false) return false;
    const h = new Date().getHours();
    if (h < 9 || h >= 23) return false; // (not too early, not too late)
    return Date.now() - (Number(c.checkInAt) || 0) > (5 + Math.random() * 4) * HOUR;
  };
  const free = () => !capri.hidden && !capri.stowed && !capri.dormant && !capri.busy && !chat?.open && !panel.current && !dev.wardrobe?.isOpen && !cfg().gamer && !window.capriSaving;

  function ask() {
    const t = T[lang()] || T.en;
    setConfig({ checkInAt: Date.now() });
    capri.perform?.('hop');
    panel.say({
      kind: 'done',
      face: 'look',
      line: fill(pick(t.ask)),
      hold: 20000,
      actions: [{ id: 'good', label: t.good, primary: true }, { id: 'meh', label: t.meh }, { id: 'bad', label: t.bad }],
      onAction: (id) => answered(id),
    });
  }
  function answered(id) {
    const t = T[lang()] || T.en;
    saveMemory?.({ feeling: { text: t.said[id] || id, at: Date.now() } });
    if (id === 'good') {
      capri.perform?.('victory');
      panel.say({ kind: 'info', face: 'laugh', line: fill(pick(t.goodLine)), hold: 4000 });
    } else if (id === 'meh') {
      panel.say({ kind: 'info', face: 'calm', line: fill(pick(t.mehLine)), hold: 6000, actions: [{ id: 'talk', label: t.talk }], onAction: () => chat?.start?.() });
    } else {
      panel.say({ kind: 'info', face: 'aside', line: fill(pick(t.badLine)), hold: 12000, actions: [{ id: 'talk', label: t.talk, primary: true }], onAction: () => chat?.start?.() });
    }
  }

  // the first one a while after she starts, never right away
  if (!cfg().checkInAt) setConfig({ checkInAt: Date.now() - 4 * HOUR });
  setInterval(() => {
    if (document.hidden || !due() || !free()) return;
    if (Math.random() < 0.5) ask(); // (not at once when it's due: some time in the next minutes)
  }, 4 * 60 * 1000);
  return { ask };
}

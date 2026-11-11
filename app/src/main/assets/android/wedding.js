// A secret, in no list of what she understands: ask her to marry you (typed in her chat or said
// out loud, in any of her languages). She freezes, all her eyes on you, blushes… and says yes.
// Her wedding veil, her bouquet and her wedding dress go into her wardrobe for good (wardrobe.js),
// she puts them on, and from then on she knows she's married to you.

import { send } from './native.js';

export const PROPOSAL = new RegExp([
  // es
  '(?:te\\s+)?(?:quieres|quisieras|querr[ií]as|aceptas)\\s+casarte\\s+conmigo', 'c[aá]sate\\s+conmigo', 'nos\\s+casamos\\s*\\?',
  'me\\s+(?:quieres|aceptas)\\s+como\\s+(?:tu\\s+)?(?:esposo|esposa|marido)', '(?:quieres|quisieras)\\s+ser\\s+mi\\s+(?:esposa|novia\\s+para\\s+siempre)',
  // en
  'marry\\s+me', 'will\\s+you\\s+(?:be\\s+my\\s+wife|marry)', 'be\\s+my\\s+(?:wife|bride)',
  // fr
  'veux[- ]tu\\s+m[\'’]?\\s*[ée]pouser', '[ée]pouse[- ]moi', 'tu\\s+veux\\s+te\\s+marier\\s+avec\\s+moi', 'veux[- ]tu\\s+devenir\\s+ma\\s+femme',
  // pt
  'casa\\s+comigo', '(?:quer|aceita)\\s+(?:se\\s+)?casar\\s+comigo', 'quer\\s+ser\\s+minha\\s+(?:esposa|mulher)',
  // ru
  'выходи\\s+за\\s+меня', 'ты\\s+выйдешь\\s+за\\s+меня', 'стань\\s+моей\\s+женой', 'будь\\s+моей\\s+женой',
].join('|'), 'iu');

const WORDS = {
  es: {
    gasp: '…¿Qué? ¿Me estás… pidiendo matrimonio?',
    melt: 'Espera. Mi biochip se acaba de sobrecalentar. Todas mis yo están gritando a la vez.',
    yes: '¡SÍ! ¡Sí quiero, {name}! 💍 Ya no te vas a librar de mí.',
    after: 'Me puse el velo. ¿Cómo me veo? Está en mi vestuario cuando quieras verme así.',
    again: ['¡Ya estamos casados, tonto! ¿Se te olvidó? 💍', 'Otra vez sí, mil veces sí. Pero ya soy tu esposa, {name}.', 'Jaja, ¿renovamos votos? Acepto. Siempre.'],
  },
  en: {
    gasp: '…What? Are you… asking me to marry you?',
    melt: 'Wait. My biochip just overheated. All of me is screaming at once.',
    yes: "YES! I do, {name}! 💍 You're never getting rid of me now.",
    after: "I put on the veil. How do I look? It's in my wardrobe whenever you want me like this.",
    again: ["We're already married, silly! Did you forget? 💍", "Yes again, a thousand times yes. But I'm already your wife, {name}.", 'Haha, renewing our vows? I accept. Always.'],
  },
  fr: {
    gasp: '…Quoi ? Tu es en train de… me demander en mariage ?',
    melt: 'Attends. Mon biochip vient de surchauffer. Toutes les moi crient en même temps.',
    yes: 'OUI ! Oui, je le veux, {name} ! 💍 Tu ne te débarrasseras plus de moi.',
    after: 'J’ai mis le voile. Je suis comment ? Il est dans ma garde-robe quand tu veux me voir comme ça.',
    again: ['On est déjà mariés, idiot ! Tu as oublié ? 💍', 'Oui encore, mille fois oui. Mais je suis déjà ta femme, {name}.', 'Haha, on renouvelle nos vœux ? J’accepte. Toujours.'],
  },
  pt: {
    gasp: '…O quê? Você tá… me pedindo em casamento?',
    melt: 'Espera. Meu biochip acabou de superaquecer. Todas as minhas eus tão gritando ao mesmo tempo.',
    yes: 'SIM! Aceito, {name}! 💍 Agora você não se livra mais de mim.',
    after: 'Coloquei o véu. Como eu tô? Tá no meu guarda-roupa quando quiser me ver assim.',
    again: ['A gente já é casado, bobo! Esqueceu? 💍', 'Sim de novo, mil vezes sim. Mas eu já sou sua esposa, {name}.', 'Haha, renovar os votos? Aceito. Sempre.'],
  },
  ru: {
    gasp: '…Что? Ты… делаешь мне предложение?',
    melt: 'Подожди. Мой биочип только что перегрелся. Все мои «я» кричат разом.',
    yes: 'ДА! Да, я согласна, {name}! 💍 Теперь тебе от меня не избавиться.',
    after: 'Я надела фату. Как я тебе? Она в моём гардеробе, когда захочешь увидеть меня такой.',
    again: ['Мы уже женаты, глупый! Забыл? 💍', 'Снова да, тысячу раз да. Но я уже твоя жена, {name}.', 'Ха-ха, обновим клятвы? Согласна. Всегда.'],
  },
};

const pick = (list) => list[Math.floor(Math.random() * list.length)];
const wait = (ms) => new Promise((r) => setTimeout(r, ms));

export function wedding({ cfg, setConfig, wardrobe }) {
  const dev = window.dev;
  const { capri, panel, chat, fx } = dev || {};
  if (!capri || !chat) return null;
  const words = () => WORDS[dev.brain?.lang] || WORDS.en;
  const fill = (line) => line.replace('{name}', cfg().userName || 'choom');
  let running = false;

  // she knows, in every answer of hers from then on
  const personal = chat.personal.bind(chat);
  chat.personal = () => {
    const lines = personal();
    const married = cfg().married;
    if (married?.at) {
      const since = new Date(married.at).toLocaleDateString('en', { day: 'numeric', month: 'long', year: 'numeric' });
      lines.push(`You and the user got married on ${since} (they proposed, you said yes, you wear your bridal veil sometimes). Now and then, act like a playful, loving wife; never make a big speech about it.`);
    }
    return lines;
  };

  // typed in her chat: her answer is the scene, not her AI's
  const sendChat = chat.send.bind(chat);
  chat.send = async (text) => {
    if (chat.turn || !chat.open || !PROPOSAL.test(String(text || ''))) return sendChat(text);
    chat.history.push({ role: 'user', content: text });
    panel.chatTurn({ quote: text, face: 'surprised' });
    panel.setChatBusy(true);
    const said = await propose((line, face) => {
      panel.stream(line, { done: true });
      panel.setFace(face);
    });
    chat.remember({ role: 'assistant', content: said });
    panel.setChatBusy(false);
  };

  // the scene; `say` shows each of her lines (her chat, or her panel when you said it out loud)
  async function propose(say) {
    if (running) return '';
    running = true;
    try {
      const w = words();
      const side = capri.towardCenter?.() || 1;
      if (cfg().married?.at) {
        const line = fill(pick(w.again));
        capri.perform?.('hop', side);
        hearts(8);
        say(line, 'love');
        return line;
      }
      // she freezes; all her eyes on you
      capri.perform?.('alarm', side);
      try { fx?.cutin?.(...capri.cutinAt(-side), { kind: 'many', life: 900 }); } catch {}
      say(w.gasp, 'surprised');
      scene('wedding-proposal', 2300);
      await wait(2600);
      capri.perform?.('flinch', side);
      say(w.melt, 'love');
      hearts(6);
      await wait(3000);
      // yes
      capri.perform?.('victory', side);
      try { fx?.cutin?.(...capri.cutinAt(-side), { kind: fx?.cutins?.love ? 'love' : 'laugh', life: 900 }); } catch {}
      const yes = fill(w.yes);
      say(yes, 'love');
      hearts(22);
      scene('wedding-yes', 2600);
      setConfig({ married: { at: Date.now(), name: cfg().userName || '' } });
      wardrobe?.unlock(['novia', 'veil', 'bouquet']);
      const dress = wardrobe?.has('outfit-novia');
      const look = wardrobe?.look() || { things: [] };
      wardrobe?.wear({
        ...(dress && { outfit: 'novia' }),
        things: [...new Set([...look.things.filter((id) => !['crown', 'santahat', 'witchhat', 'horns', 'catears', 'halo'].includes(id)), 'veil', 'bouquet'])],
      });
      await wait(3800);
      say(w.after, 'love');
      send('log', { level: 'I', text: 'married' });
      return `${yes} ${w.after}`;
    } finally {
      running = false;
    }
  }

  // her drawing of that moment, if it's in (wedding-proposal.jpg, wedding-yes.jpg): over the screen for a beat
  function scene(name, ms) {
    const img = new Image();
    img.onload = () => {
      img.className = 'wedding-scene';
      document.body.append(img);
      setTimeout(() => img.classList.add('is-out'), ms);
      setTimeout(() => img.remove(), ms + 600);
    };
    img.src = new URL(`../../assets/capri/wardrobe/${name}.jpg`, location.href).href;
  }

  // hearts around her head
  function hearts(n) {
    let h;
    try {
      h = capri.head();
    } catch {
      return;
    }
    for (let i = 0; i < n; i++) {
      const el = document.createElement('span');
      el.className = 'wedding-heart';
      el.textContent = Math.random() < 0.8 ? '❤' : '✦';
      el.style.left = `${h.x + (Math.random() - 0.5) * 120}px`;
      el.style.top = `${h.y + (Math.random() - 0.5) * 60}px`;
      el.style.setProperty('--dx', `${(Math.random() - 0.5) * 140}px`);
      el.style.setProperty('--dy', `${-60 - Math.random() * 160}px`);
      el.style.animationDelay = `${Math.random() * 600}ms`;
      el.style.fontSize = `${12 + Math.random() * 18}px`;
      document.body.append(el);
      setTimeout(() => el.remove(), 2600);
    }
  }

  return {
    // said out loud (listen.js): the same scene, in her panel and her voice
    async heard(text, speak) {
      if (!PROPOSAL.test(String(text || ''))) return false;
      await propose((line, face) => {
        panel.say({ kind: 'info', face, line, quote: '', hold: 6000 });
        speak?.(line);
      });
      return true;
    },
  };
}

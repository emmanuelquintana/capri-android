// Her tips while you play (1.9.0, the community: "que te ayude en los juegos, dándote consejos
// durante la partida"). Two ways:
// - You ask, in her chat or out loud: "Capri, dame un consejo", "¿qué hago aquí?", "¿cómo paso este
//   nivel?": she looks at your screen and tells you something useful for what's on it right now.
// - In gamer mode, with her live look on (she's watching your screen) and her tips on (Ajustes):
//   every few minutes, a tip for the game in front of her instead of a plain comment.
// Her AI has to see pictures, as for any of her looks.

const ASK = /(?:dame|dime|tienes|alg[uú]n|un) (?:un )?(?:consejo|tip|truco)|qu[ée] (?:hago|me recomiendas|deber[ií]a hacer)|c[oó]mo (?:paso|gano|le gano|venzo|derroto|le hago|supero)|ay[uú]dame (?:con|en|a pasar|a ganar)|give me (?:a )?(?:tip|hint|advice)|any (?:tips|advice)|what (?:should|do) i do|how do i (?:beat|win|pass|get past)|help me (?:win|beat|with this)|un conseil|une astuce|qu['’]est-ce que je fais|comment (?:je )?(?:gagne|passe|bats)|me d[áa] (?:uma )?dica|alguma dica|o que eu fa[çc]o|como (?:eu )?(?:passo|ganho|venço)|дай (?:мне )?совет|подскажи|что (?:мне )?делать|как (?:пройти|победить|выиграть)/iu;
const NOTE = "(They're playing a game and want a tip. Look at what's on the screen right now (the game, the level, the situation, their stats or cards if any) and give one or two concrete, useful tips for exactly this moment, like a friend who knows the game: what to do next, what to watch out for. Short: two or three sentences, no long lists. If it isn't a game, help with whatever they're stuck on.)";
const AUTO_NOTE = "(They're playing right now: one short, concrete tip for what you see in the game, one or two sentences, or a quick reaction if there's nothing useful to say. Never long.)";
const rand = (a, b) => a + Math.random() * (b - a);

export function coaching({ cfg }) {
  const dev = window.dev;
  const chat = dev?.chat;
  if (!chat?.asked) return null;
  const wants = (text) => ASK.test(String(text || ''));

  const asked = chat.asked.bind(chat);
  chat.asked = (text) => (chat.look && wants(text) && !dev.translate?.wants(text) ? { kind: 'look' } : asked(text));

  // her look, told a tip is wanted
  const lookSay = window.host.lookSay;
  window.host.lookSay = (req = {}) => {
    const first = String(req.prompt || '').split('\n')[0];
    if (wants(first)) req = { ...req, prompt: `${req.prompt}\n${NOTE}` };
    else if (cfg().gamer && !dev.translate?.wants(first)) req = { ...req, prompt: `${req.prompt}\n${AUTO_NOTE}` };
    return lookSay(req);
  };

  // gamer mode with her live look: a tip now and then
  let next = 0;
  setInterval(() => {
    const c = cfg();
    if (!c.gamer || !c.watchScreen || c.gameTips === false || document.hidden || !dev.look) return (next = 0);
    const now = Date.now();
    if (!next) next = now + rand(1.5, 3) * 60000;
    if (now < next || dev.look.looking || dev.panel?.current || chat.open) return;
    next = now + rand(3, 5) * 60000;
    dev.look.now().catch?.(() => {});
  }, 10000);

  return { wants };
}

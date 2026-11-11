// "Capri, tradúceme esto" (1.9.0, the community: "que al ver la pantalla traduzca al idioma que uno
// quiera, como un mensaje en inglés"). In her chat or out loud: she looks at your screen (as when you
// ask her to look, with the same permission) and writes the text on it in the language you named,
// or in yours if you didn't name one. Then a line of her own, at most.
// A text you typed to translate ("traduce 'good morning' al francés") is her chat's as always.

const ASK = /(?:^|[^\p{L}])(?:trad[uú]ce(?:me|lo|la|los)?|traduc(?:ir|e|eme)|trad[uú]zca(?:me)?|translate|traduis(?:[- ]moi)?|traduire|traduz(?:a|ir)?|переведи(?:те)?|перевод)(?![\p{L}])/iu;
// what on screen: "esto", "lo que dice", "mi pantalla", "este mensaje"… (or nothing else said)
const SCREEN = /pantalla|esto|eso|est[ea] (?:mensaje|texto|chat|post|comentario)|lo que (?:dice|hay|sale|pone)|screen|this|that|what it says|the message|[ée]cran|[çc]a|ce (?:message|texte)|ce qu['’]il y a|tela|isso|isto|essa mensagem|o que diz|экран|это|сообщение/iu;
// the language you want, by its name in any of her languages
const LANGS = [
  ['Spanish', /espa[ñn]ol|castellano|spanish|espagnol|espanhol|испанск/iu],
  ['English', /ingl[ée]s|english|anglais|английск/iu],
  ['French', /franc[ée]s|french|fran[çc]ais|франц/iu],
  ['Portuguese', /portugu[ée]s|portuguese|portugais|португал/iu],
  ['Russian', /ruso|russian|russe|russo|русск/iu],
  ['Japanese', /japon[ée]s|japanese|japonais|японск/iu],
  ['Korean', /coreano|korean|cor[ée]en|корейск/iu],
  ['Chinese', /chino|chinese|chinois|chin[êe]s|китайск/iu],
  ['Italian', /italiano|italian|italien|итальянск/iu],
  ['German', /alem[aá]n|german|allemand|alem[ãa]o|немецк/iu],
  ['Arabic', /[aá]rabe|arabic|арабск/iu],
];
const MINE = { es: 'Spanish', en: 'English', fr: 'French', pt: 'Portuguese', ru: 'Russian' };
const words = (s) => s.trim().split(/\s+/).filter(Boolean).length;

export function translating() {
  const dev = window.dev;
  const chat = dev?.chat;
  if (!chat?.asked) return null;

  // the screen, translated: she asks to look (her chat's way), not what you typed
  const wants = (text) => {
    const t = String(text || '');
    if (!ASK.test(t)) return false;
    if (/["“«'‘].{3,}["”»'’]/.test(t)) return false; // (a quoted text to translate: her chat)
    const rest = t.replace(ASK, ' ').replace(LANGS.find(([, re]) => re.test(t))?.[1] || /$^/, ' ');
    return SCREEN.test(t) || words(rest) <= 3;
  };
  const target = (text) => LANGS.find(([, re]) => re.test(text))?.[0] || MINE[dev.brain?.lang] || 'English';

  const asked = chat.asked.bind(chat);
  chat.asked = (text) => (chat.look && wants(text) ? { kind: 'look' } : asked(text));

  // her look, told it's a translation that's wanted
  const lookSay = window.host.lookSay;
  window.host.lookSay = (req = {}) => {
    const first = String(req.prompt || '').split('\n')[0];
    if (wants(first)) {
      const lang = target(first);
      req = {
        ...req,
        prompt: `${req.prompt}\n(They want the text on their screen translated into ${lang}. Write the translation of the screen's main text (the message, post, caption or page, not the phone's buttons, clock or bars), faithfully and whole, in ${lang}, in the order it appears; who said what if it's a chat. Then at most one short line of your own. If there's no text to translate, say so.)`,
      };
    }
    return lookSay(req);
  };

  return { wants };
}

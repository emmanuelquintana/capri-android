// (1.12.0, community: "que pueda sentir cuando movamos, agitemos el teléfono, y reaccione") You shake the phone
// (Shake.kt, while she's out): she gets dizzy, shakes it off and says something about it. Not more often than
// every 20 s, never in gamer mode or while you talk to her.

import { on } from './native.js';

const LINES = {
  es: ['¡Oye! ¡Que me mareo!', 'Terremoto en Night City… ah no, eras tú.', '¿Me quieres sacar del teléfono o qué?', 'Todo me da vueltas, choom.', 'Cuidado, que mis implantes no son a prueba de sacudidas.'],
  en: ['Hey! I’m getting dizzy!', 'Earthquake in Night City… oh, it was you.', 'Trying to shake me out of the phone?', 'Everything’s spinning, choom.', 'Easy, my chrome isn’t shake-proof.'],
  fr: ['Hé ! J’ai le tournis !', 'Séisme à Night City… ah non, c’était toi.', 'Tu veux me faire sortir du téléphone ?', 'Tout tourne, choom.', 'Doucement, mes implants ne sont pas antichocs.'],
  pt: ['Ei! Tô ficando tonta!', 'Terremoto em Night City… ah, era você.', 'Quer me tirar do celular na sacudida?', 'Tá tudo girando, choom.', 'Calma, meus implantes não são à prova de chacoalhão.'],
  ru: ['Эй! У меня голова кружится!', 'Землетрясение в Найт-Сити… а, это ты.', 'Хочешь вытрясти меня из телефона?', 'Всё кружится, чумба.', 'Полегче, мой хром не противоударный.'],
  vi: ['Này! Mình chóng mặt rồi!', 'Động đất ở Night City… à, là bạn.', 'Muốn lắc mình ra khỏi điện thoại à?', 'Mọi thứ quay vòng, choom.', 'Nhẹ thôi, chrome của mình không chống sốc đâu.'],
  ja: ['ちょっと！目が回る！', 'ナイトシティで地震…あ、あなたか。', 'わたしをスマホから振り落とす気？', '世界が回ってる、チューム。', 'やめて、わたしのクロームは耐震じゃないの。'],
  de: ['Hey! Mir wird schwindelig!', 'Erdbeben in Night City… ach, das warst du.', 'Willst du mich aus dem Handy schütteln?', 'Alles dreht sich, Choom.', 'Vorsicht, mein Chrom ist nicht stoßfest.'],
  it: ['Ehi! Mi gira la testa!', 'Terremoto a Night City… ah, eri tu.', 'Vuoi scuotermi fuori dal telefono?', 'Gira tutto, choom.', 'Piano, il mio chrome non è antiurto.'],
};

export function shaking({ cfg, lang }) {
  let last = 0;
  on('phone:shaken', () => {
    const dev = window.dev;
    const c = cfg();
    if (!dev?.capri || c.gamer || c.shake === false || dev.chat?.open || Date.now() - last < 20000) return;
    last = Date.now();
    const list = LINES[lang()] || LINES.en;
    dev.capri.perform?.('dust', dev.capri.towardCenter?.() || 1);
    dev.capri.feel?.('dizzy');
    // her recorded phrase for being moved around, when there is one (it's what's shown then)
    const line = list[Math.floor(Math.random() * list.length)];
    const own = dev.voicelines?.moment?.('drag', line);
    dev.panel?.say?.({ kind: 'info', face: 'surprised', line: own?.line || line, quote: '', hold: 4500 });
  });
}

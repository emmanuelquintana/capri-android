// Capri as a real rival in her games (1.10.0, Emma: "un sistema complejo para la IA que pueda jugar
// contra ti… que realmente se sienta que no es casi automático").
//
// Each game gives her the moves it can see, each with a score (how good she thinks it is), and tells
// her what happens. She does the rest, like a person would:
// - a mood that moves with the game: confidence, tilt (annoyed after your good moves or her mistakes),
//   focus (in the zone on long rallies or close games), and it shows on her face and in her play;
// - a skill that isn't fixed: the level you chose, plus how the rivalry is going (if you keep winning
//   she gets serious; if she keeps crushing you she eases off a little), minus her tilt;
// - believable mistakes: a weaker move that still makes sense (never a random one), sometimes noticed
//   a moment later ("ugh, eso fue un error");
// - time to think that depends on the position: quick when it's obvious, long when two moves are close,
//   sometimes eyeing one move and changing her mind;
// - she reads your moves (a great one, a slip, a better one you missed) and remembers your habits
//   between games (your hands at rock-paper-scissors, how you open at chess, when you stand at 21)
//   and your record against her, on this phone only.
// She talks with a cooldown and a little chance, so she comments, she doesn't narrate.

const KEY = 'capri.rival.v1';
const clamp = (v, a = 0, b = 1) => Math.max(a, Math.min(b, v));
const pick = (list) => list[Math.floor(Math.random() * list.length)];

const L = {
  es: {
    greetFirst: ['Primera vez que jugamos a esto. No te emociones.', 'A ver qué tal eres en esto.'],
    greetAhead: ['Vamos {h}-{y}, a mi favor. Solo digo.', '{h} a {y}. ¿Vienes por la revancha?'],
    greetBehind: ['Vas {y}-{h} contra mí. Hoy lo arreglo.', 'Me debes una. {y} a {h}… por ahora.'],
    greetEven: ['Empatados, {y}-{h}. Esta desempata.', '{h} a {y}. Nadie cede.'],
    serious: ['Ok. Me ganaste {n} seguidas. Ahora juego en serio.', 'Se acabó la amabilidad.'],
    easeOff: ['Te voy a tener un poquito de piedad. Un poquito.'],
    obvious: ['Obvio.', 'Fácil.', 'Esa estaba cantada.'],
    hard: ['Esta está difícil…', 'Mmm… déjame ver bien.', 'No me apures.'],
    hesitate: ['¿Aquí? …no.', 'Iba a… no, mejor no.', 'Casi caigo.'],
    regret: ['Ugh. Eso fue un error.', 'No debí hacer eso.', 'Olvida lo que viste.'],
    youBlunder: ['¿Seguro? …Gracias.', 'Uy. Eso me sirve.', 'Mmm, no debiste.'],
    youGood: ['Buena jugada. Me molesta.', 'Ok, esa no la vi.', 'Hmph. Bien.'],
    youMissed: ['Tenías una mejor. No te digo cuál.', 'Había algo mejor ahí…'],
    idle: ['¿Sigues ahí?', 'Tómate tu tiempo… pero no tanto.', 'Mi chip se está enfriando.'],
    tilt: ['Ok. Ya me enojé.', 'Ahora sí va en serio.', 'Grr.'],
    comeback: ['Todavía no termina.', 'Me voy a recuperar.'],
    confident: ['Esto ya es mío.', 'Ríndete con dignidad.', 'Lo tengo.'],
    focus: ['Shh. Estoy en la zona.', 'Concentración total.'],
    rally: ['¡{n} seguidas! Esto se puso bueno.', '{n} golpes… no te rindes, ¿eh?'],
    pattern: ['Siempre {x}. Ya te conozco.', 'Otra vez {x}… predecible.'],
    announce: ['Voy a sacar {x}.', 'Te aviso: {x}.', '{x}. Palabra de netrunner.'],
    lied: ['¿Me creíste? Ternura.', 'Nunca confíes en una IA.'],
    honest: ['Te dije la verdad. Nadie me cree.', 'Era en serio.'],
    counted: ['Quedan muchas cartas altas… lo siento en el chip.', 'Las cuento, ¿sabes?'],
    riskHit: ['Pido. Que sea lo que el chip quiera.', 'Va otra. Sin miedo.'],
    safeStand: ['Me planto. No me tientes.', 'Hasta aquí.'],
    tease21: ['¿Pides o te da miedo?', 'Yo me plantaría… o no.'],
    stand15: ['Te plantas con poco, como siempre.', 'Siempre te plantas en {x}.'],
    hint: ['Psst… la de la fila {r}, columna {c} es segura. No le digas a nadie.', 'Una pista: fila {r}, columna {c}. De nada.'],
    fast: ['Vas muy rápido… cuidado.', 'Uff, qué confianza.'],
    openRepeat: ['Otra vez abres igual. Predecible.', 'Siempre la misma apertura.'],
    material: ['Voy ganando material. Solo digo.', 'Cuenta las piezas.'],
    behind: ['Ok, me falta material. Todavía puedo.', 'Esto se complicó.'],
  },
  en: {
    greetFirst: ['First time we play this. Don’t get excited.', 'Let’s see how good you are at this.'],
    greetAhead: ['It’s {h}-{y}, my way. Just saying.', '{h} to {y}. Here for revenge?'],
    greetBehind: ['You’re {y}-{h} against me. I fix that today.', 'I owe you one. {y} to {h}… for now.'],
    greetEven: ['Tied, {y}-{h}. This one breaks it.', '{h} to {y}. Nobody gives in.'],
    serious: ['Ok. You beat me {n} in a row. Now I play for real.', 'No more nice Capri.'],
    easeOff: ['I’ll have a little mercy on you. A little.'],
    obvious: ['Obviously.', 'Easy.', 'Saw that coming.'],
    hard: ['This one’s tricky…', 'Hmm… let me look properly.', 'Don’t rush me.'],
    hesitate: ['Here? …no.', 'I was going to… no.', 'Almost fell for it.'],
    regret: ['Ugh. That was a mistake.', 'Shouldn’t have done that.', 'Forget what you saw.'],
    youBlunder: ['Sure? …Thanks.', 'Oops. I’ll take that.', 'Hmm, you shouldn’t have.'],
    youGood: ['Good move. It annoys me.', 'Ok, didn’t see that one.', 'Hmph. Nice.'],
    youMissed: ['You had a better one. Not telling which.', 'There was something better there…'],
    idle: ['Still there?', 'Take your time… but not that much.', 'My chip’s getting cold.'],
    tilt: ['Ok. Now I’m mad.', 'Now it’s serious.', 'Grr.'],
    comeback: ['It’s not over.', 'I’ll come back.'],
    confident: ['This one’s mine.', 'Surrender with dignity.', 'Got it.'],
    focus: ['Shh. I’m in the zone.', 'Full focus.'],
    rally: ['{n} in a row! Now it’s fun.', '{n} hits… you don’t give up, huh?'],
    pattern: ['Always {x}. I know you now.', '{x} again… predictable.'],
    announce: ['I’m throwing {x}.', 'Heads up: {x}.', '{x}. Netrunner’s word.'],
    lied: ['You believed me? Cute.', 'Never trust an AI.'],
    honest: ['I told you the truth. Nobody believes me.', 'I meant it.'],
    counted: ['Lots of high cards left… I feel it in my chip.', 'I count them, you know?'],
    riskHit: ['Hit. Whatever the chip wants.', 'One more. No fear.'],
    safeStand: ['I stand. Don’t tempt me.', 'That’s enough.'],
    tease21: ['Hit, or are you scared?', 'I’d stand… or not.'],
    stand15: ['Standing low, as always.', 'You always stand on {x}.'],
    hint: ['Psst… row {r}, column {c} is safe. Don’t tell anyone.', 'A hint: row {r}, column {c}. You’re welcome.'],
    fast: ['You’re going fast… careful.', 'Wow, confident.'],
    openRepeat: ['Same opening again. Predictable.', 'Always the same opening.'],
    material: ['I’m up material. Just saying.', 'Count the pieces.'],
    behind: ['Ok, I’m down material. I can still do it.', 'This got complicated.'],
  },
  fr: {
    greetFirst: ['Première fois qu’on joue à ça. Ne t’emballe pas.', 'Voyons ce que tu vaux.'],
    greetAhead: ['{h}-{y} pour moi. Je dis ça…', '{h} à {y}. Tu viens te venger ?'],
    greetBehind: ['Tu mènes {y}-{h}. Je répare ça aujourd’hui.', 'Je t’en dois une. {y} à {h}… pour l’instant.'],
    greetEven: ['Égalité, {y}-{h}. Celle-ci départage.', '{h} à {y}. Personne ne lâche.'],
    serious: ['Ok. Tu m’as battue {n} fois de suite. Maintenant je joue sérieux.', 'Fini la gentille Capri.'],
    easeOff: ['Je vais avoir un peu pitié de toi. Un peu.'],
    obvious: ['Évident.', 'Facile.', 'Je l’avais vu venir.'],
    hard: ['Celle-là est difficile…', 'Mmm… laisse-moi bien regarder.', 'Ne me presse pas.'],
    hesitate: ['Ici ? …non.', 'J’allais… non.', 'J’ai failli tomber dans le piège.'],
    regret: ['Pff. C’était une erreur.', 'Je n’aurais pas dû.', 'Oublie ce que tu as vu.'],
    youBlunder: ['Sûr ? …Merci.', 'Oups. Je prends.', 'Mmm, tu n’aurais pas dû.'],
    youGood: ['Bien joué. Ça m’agace.', 'Ok, je ne l’avais pas vue.', 'Hmph. Pas mal.'],
    youMissed: ['Il y avait mieux. Je ne dis pas quoi.', 'Il y avait mieux là…'],
    idle: ['Tu es toujours là ?', 'Prends ton temps… mais pas trop.', 'Ma puce refroidit.'],
    tilt: ['Ok. Là je suis fâchée.', 'Maintenant c’est sérieux.', 'Grr.'],
    comeback: ['Ce n’est pas fini.', 'Je vais remonter.'],
    confident: ['Celle-là est à moi.', 'Rends-toi avec dignité.', 'Je l’ai.'],
    focus: ['Chut. Je suis dans la zone.', 'Concentration totale.'],
    rally: ['{n} d’affilée ! Ça devient bon.', '{n} coups… tu ne lâches rien, hein ?'],
    pattern: ['Toujours {x}. Je te connais.', 'Encore {x}… prévisible.'],
    announce: ['Je vais jouer {x}.', 'Je te préviens : {x}.', '{x}. Parole de netrunneuse.'],
    lied: ['Tu m’as crue ? Mignon.', 'Ne fais jamais confiance à une IA.'],
    honest: ['Je disais la vérité. Personne ne me croit.', 'J’étais sérieuse.'],
    counted: ['Il reste plein de cartes hautes… je le sens dans ma puce.', 'Je les compte, tu sais ?'],
    riskHit: ['Carte. Comme la puce voudra.', 'Encore une. Sans peur.'],
    safeStand: ['Je reste. Ne me tente pas.', 'Ça suffit.'],
    tease21: ['Tu tires ou tu as peur ?', 'Moi je resterais… ou pas.'],
    stand15: ['Tu restes bas, comme toujours.', 'Tu restes toujours à {x}.'],
    hint: ['Psst… ligne {r}, colonne {c}, c’est sûr. Ne le dis à personne.', 'Un indice : ligne {r}, colonne {c}. De rien.'],
    fast: ['Tu vas vite… attention.', 'Quelle confiance.'],
    openRepeat: ['Encore la même ouverture. Prévisible.', 'Toujours la même ouverture.'],
    material: ['J’ai plus de matériel. Je dis ça…', 'Compte les pièces.'],
    behind: ['Ok, il me manque du matériel. Je peux encore.', 'Ça se complique.'],
  },
  pt: {
    greetFirst: ['Primeira vez que a gente joga isso. Não se empolga.', 'Vamos ver se você é boa… ou bom nisso.'],
    greetAhead: ['Tá {h}-{y} pra mim. Só falando.', '{h} a {y}. Veio pela revanche?'],
    greetBehind: ['Você tá {y}-{h} contra mim. Hoje eu conserto.', 'Te devo uma. {y} a {h}… por enquanto.'],
    greetEven: ['Empatados, {y}-{h}. Essa desempata.', '{h} a {y}. Ninguém cede.'],
    serious: ['Ok. Você ganhou {n} seguidas. Agora eu jogo sério.', 'Acabou a Capri boazinha.'],
    easeOff: ['Vou ter um pouquinho de pena de você. Um pouquinho.'],
    obvious: ['Óbvio.', 'Fácil.', 'Essa tava na cara.'],
    hard: ['Essa tá difícil…', 'Hmm… deixa eu ver direito.', 'Não me apressa.'],
    hesitate: ['Aqui? …não.', 'Eu ia… não, melhor não.', 'Quase caí.'],
    regret: ['Argh. Isso foi um erro.', 'Não devia ter feito isso.', 'Esquece o que você viu.'],
    youBlunder: ['Certeza? …Valeu.', 'Opa. Isso me serve.', 'Hmm, não devia.'],
    youGood: ['Boa jogada. Me irrita.', 'Ok, essa eu não vi.', 'Hmph. Boa.'],
    youMissed: ['Tinha uma melhor. Não digo qual.', 'Tinha algo melhor ali…'],
    idle: ['Ainda tá aí?', 'Leva seu tempo… mas nem tanto.', 'Meu chip tá esfriando.'],
    tilt: ['Ok. Agora fiquei brava.', 'Agora é sério.', 'Grr.'],
    comeback: ['Ainda não acabou.', 'Vou me recuperar.'],
    confident: ['Essa já é minha.', 'Se rende com dignidade.', 'Tá comigo.'],
    focus: ['Shh. Tô na zona.', 'Concentração total.'],
    rally: ['{n} seguidas! Agora ficou bom.', '{n} batidas… não desiste, né?'],
    pattern: ['Sempre {x}. Já te conheço.', '{x} de novo… previsível.'],
    announce: ['Vou jogar {x}.', 'Tô avisando: {x}.', '{x}. Palavra de netrunner.'],
    lied: ['Acreditou? Fofo.', 'Nunca confie numa IA.'],
    honest: ['Eu falei a verdade. Ninguém acredita.', 'Era sério.'],
    counted: ['Sobraram muitas cartas altas… sinto no chip.', 'Eu conto elas, sabia?'],
    riskHit: ['Peço. Seja o que o chip quiser.', 'Mais uma. Sem medo.'],
    safeStand: ['Paro. Não me tenta.', 'Até aqui.'],
    tease21: ['Vai pedir ou tá com medo?', 'Eu pararia… ou não.'],
    stand15: ['Parando baixo, como sempre.', 'Você sempre para em {x}.'],
    hint: ['Psiu… linha {r}, coluna {c} é segura. Não conta pra ninguém.', 'Uma dica: linha {r}, coluna {c}. De nada.'],
    fast: ['Tá indo rápido… cuidado.', 'Nossa, que confiança.'],
    openRepeat: ['Mesma abertura de novo. Previsível.', 'Sempre a mesma abertura.'],
    material: ['Tô com mais material. Só falando.', 'Conta as peças.'],
    behind: ['Ok, tô com menos material. Ainda dá.', 'Complicou.'],
  },
  ru: {
    greetFirst: ['Первый раз играем в это. Не радуйся раньше времени.', 'Посмотрим, на что ты способен(на).'],
    greetAhead: ['{h}-{y} в мою пользу. Просто говорю.', '{h}:{y}. Пришёл(ла) за реваншем?'],
    greetBehind: ['Ты ведёшь {y}-{h}. Сегодня исправлю.', 'Я тебе должна. {y}:{h}… пока что.'],
    greetEven: ['Ничья, {y}-{h}. Эта решит.', '{h}:{y}. Никто не уступает.'],
    serious: ['Ладно. Ты выиграл(а) {n} подряд. Теперь всерьёз.', 'Добрая Капри закончилась.'],
    easeOff: ['Немного тебя пожалею. Совсем немного.'],
    obvious: ['Очевидно.', 'Легко.', 'Я это видела.'],
    hard: ['Сложная…', 'Хм… дай подумать.', 'Не торопи меня.'],
    hesitate: ['Сюда? …нет.', 'Я хотела… нет.', 'Чуть не попалась.'],
    regret: ['Ух. Это была ошибка.', 'Зря я так.', 'Забудь, что видел(а).'],
    youBlunder: ['Уверен(а)? …Спасибо.', 'Упс. Беру.', 'Хм, зря ты.'],
    youGood: ['Хороший ход. Бесит.', 'Ладно, этого я не видела.', 'Хмф. Неплохо.'],
    youMissed: ['Был ход получше. Не скажу какой.', 'Там было кое-что получше…'],
    idle: ['Ты тут?', 'Не спеши… но не настолько.', 'Мой чип остывает.'],
    tilt: ['Всё. Я злюсь.', 'Теперь всерьёз.', 'Ррр.'],
    comeback: ['Ещё не конец.', 'Я отыграюсь.'],
    confident: ['Это уже моё.', 'Сдавайся достойно.', 'Попался(ась).'],
    focus: ['Тсс. Я в потоке.', 'Полная концентрация.'],
    rally: ['{n} подряд! Становится интересно.', '{n} ударов… не сдаёшься, да?'],
    pattern: ['Всегда {x}. Я тебя знаю.', 'Опять {x}… предсказуемо.'],
    announce: ['Я выкину {x}.', 'Предупреждаю: {x}.', '{x}. Слово нетраннера.'],
    lied: ['Поверил(а)? Мило.', 'Никогда не верь ИИ.'],
    honest: ['Я сказала правду. Мне никто не верит.', 'Я серьёзно.'],
    counted: ['Осталось много старших карт… чувствую чипом.', 'Я их считаю, знаешь ли.'],
    riskHit: ['Ещё. Как чип решит.', 'Ещё одну. Без страха.'],
    safeStand: ['Хватит. Не искушай.', 'Стоп.'],
    tease21: ['Берёшь или страшно?', 'Я бы остановилась… или нет.'],
    stand15: ['Опять останавливаешься рано.', 'Ты всегда останавливаешься на {x}.'],
    hint: ['Пс… ряд {r}, столбец {c} безопасен. Никому не говори.', 'Подсказка: ряд {r}, столбец {c}. Не за что.'],
    fast: ['Слишком быстро… осторожно.', 'Ого, какая уверенность.'],
    openRepeat: ['Опять тот же дебют. Предсказуемо.', 'Всегда один и тот же дебют.'],
    material: ['У меня больше фигур. Просто говорю.', 'Посчитай фигуры.'],
    behind: ['Ладно, у меня меньше фигур. Ещё не всё.', 'Всё усложнилось.'],
  },
};

function load() {
  try { return JSON.parse(localStorage.getItem(KEY) || '{}') || {}; } catch { return {}; }
}
function save(all) {
  try { localStorage.setItem(KEY, JSON.stringify(all)); } catch {}
}

/**
 * Her, as your rival in one game. talk(text, face, key) shows her line (games.js say(), and what she does).
 * level: 'easy' | 'normal' | 'hard'.
 */
export function rival({ game, level, lang, talk }) {
  const lines = L[lang] || L.en;
  const all = load();
  const rec = all[game] || { you: 0, her: 0, draw: 0, last: [], habits: {} };
  all[game] = rec;
  const base = { easy: 0.3, normal: 0.62, hard: 0.92 }[level] ?? 0.62;
  // how the rivalry goes lately: + you've been winning, - she has
  const form = (rec.last || []).slice(-6).reduce((s, r) => s + (r === 'you' ? 1 : r === 'her' ? -1 : 0), 0);
  const youStreak = (() => { let n = 0; for (const r of [...(rec.last || [])].reverse()) { if (r === 'you') n += 1; else break; } return n; })();
  const mood = {
    confidence: clamp(0.5 - form * 0.05),
    tilt: 0,
    focus: 0.3,
  };
  let lastTalk = 0;
  let idleTimer = 0;
  let idleCount = 0;

  const fill = (s, vars = {}) => s.replace(/\{(\w+)\}/g, (_, k) => (vars[k] ?? ''));

  /** A line, now and then: priority 2 always speaks, 1 usually (with a pause), 0 sometimes. */
  function line(key, face, vars = {}, priority = 1) {
    const list = lines[key];
    if (!list) return false;
    const now = Date.now();
    const gap = now - lastTalk;
    if (priority < 2 && gap < 3500) return false;
    if (priority === 0 && Math.random() > 0.35) return false;
    lastTalk = now;
    talk(fill(pick(list), vars), face, key);
    return true;
  }

  /** Her face for how she feels right now. */
  function face() {
    if (mood.tilt > 0.6) return 'angry';
    if (mood.focus > 0.75) return 'focus';
    if (mood.confidence > 0.75) return 'smug';
    if (mood.confidence < 0.25) return 'surprised';
    return 'look';
  }

  /** Her skill this moment: the level, the rivalry, her mood. */
  function skill() {
    let s = base;
    if (youStreak >= 2 || form >= 3) s += 0.12; // you've been winning: she tries harder
    if (form <= -4) s -= 0.12; // she's crushing you: a little mercy
    s -= mood.tilt * 0.22;
    s += (mood.focus - 0.3) * 0.12;
    return clamp(s, 0.05, 0.99);
  }

  /**
   * Her choice among the moves: [{ move, score }], higher score = better for her. Not always the
   * best: a softmax that gets sharper the better she plays, and when tilted, a rash one now and then.
   * Returns { move, best, gap (0..1 how much worse than the best), close (the top two were close) }.
   * (scale: see below; without it, the best is 1 and the worst 0)
   */
  function choose(options, { scale = 0 } = {}) {
    if (!options.length) return null;
    const sorted = [...options].sort((a, b) => b.score - a.score);
    const top = sorted[0].score;
    const low = sorted.at(-1).score;
    const span = Math.max(1e-6, top - low);
    // scale: how much worse (in the game's own units) counts as "a lot worse" (chess: 3 pawns)
    const norm = sorted.map((o) => ({ ...o, n: scale ? 1 - Math.min(1, (top - o.score) / scale) : (o.score - low) / span }));
    const sk = skill();
    const temp = 0.02 + (1 - sk) * 0.32 + mood.tilt * 0.12;
    let chosen;
    if (mood.tilt > 0.55 && Math.random() < mood.tilt * 0.18 && norm.length > 2) {
      // rash: one from the upper-middle, never the worst
      chosen = norm[1 + Math.floor(Math.random() * Math.min(3, norm.length - 2))];
    } else {
      const w = norm.map((o) => Math.exp((o.n - 1) / temp));
      let r = Math.random() * w.reduce((a, b) => a + b, 0);
      chosen = norm[norm.length - 1];
      for (let i = 0; i < norm.length; i++) { r -= w[i]; if (r <= 0) { chosen = norm[i]; break; } }
    }
    const close = norm.length > 1 && norm[0].n - norm[1].n < 0.08;
    return { move: chosen.move, best: sorted[0].move, second: sorted[1]?.move, gap: 1 - chosen.n, close, alone: norm.length === 1, sorted };
  }

  /** How long she thinks, in ms: longer when it's close or there's a lot to look at; quick when obvious. */
  function thinkTime({ choice, many = 8 } = {}) {
    let ms = 380 + Math.min(many, 40) * 22;
    if (choice?.alone) ms = 280;
    else if (choice && !choice.close && choice.gap === 0 && choice.sorted.length > 1 && choice.sorted[0].score - choice.sorted[1].score > 0) ms *= 0.75;
    if (choice?.close) ms *= 1.7;
    ms *= 1.15 - mood.focus * 0.3;
    if (mood.tilt > 0.5) ms *= 0.7; // annoyed: plays faster
    return Math.round(ms * (0.8 + Math.random() * 0.5));
  }

  /** After her move: maybe she says something about it (and maybe she sees her own mistake). */
  function afterHer(choice) {
    if (!choice) return;
    if (choice.gap > 0.35 && skill() > 0.4 && Math.random() < 0.55) {
      setTimeout(() => {
        mood.tilt = clamp(mood.tilt + 0.15);
        line('regret', 'cringe', {}, 2);
      }, 900 + Math.random() * 900);
      return;
    }
    if (choice.close) line('hard', 'thinking', {}, 0);
    else if (choice.alone || (choice.gap === 0 && Math.random() < 0.4)) line('obvious', 'smug', {}, 0);
  }

  /** Before her move: maybe she eyes another one first (preview(move) shows it; returns ms used). */
  function hesitation(choice, preview) {
    if (!choice || choice.alone || !choice.second) return 0;
    const chance = choice.close ? 0.55 : 0.18;
    if (Math.random() > chance) return 0;
    const other = choice.move === choice.best ? choice.second : choice.best;
    preview(other);
    setTimeout(() => { preview(null); line('hesitate', 'thinking', {}, 0); }, 520);
    return 700;
  }

  /** Your move, judged: quality 0 (a blunder) … 1 (the best there was). */
  function yourMove(quality, { missed = false } = {}) {
    clearTimeout(idleTimer);
    idleCount = 0;
    if (quality >= 0.97 && missed === false) {
      mood.confidence = clamp(mood.confidence - 0.06);
      mood.tilt = clamp(mood.tilt + 0.05);
      line('youGood', 'surprised', {}, 0);
    } else if (quality < 0.35) {
      mood.confidence = clamp(mood.confidence + 0.15);
      mood.tilt = clamp(mood.tilt - 0.1);
      line('youBlunder', 'grin', {}, 1);
    } else if (missed && Math.random() < 0.5) {
      line('youMissed', 'wink', {}, 0);
    }
  }

  /** It's your turn: if you take long, she says so (and again, less often). */
  function yourTurn() {
    clearTimeout(idleTimer);
    idleTimer = setTimeout(() => {
      idleCount += 1;
      line('idle', 'bored', {}, 1);
      if (idleCount < 3) yourTurn();
    }, 16000 + idleCount * 12000);
  }

  /** How it's going: >0 good for her, <0 good for you (-1..1). Moves her mood. */
  function standing(v) {
    const was = mood.confidence;
    mood.confidence = clamp(mood.confidence * 0.6 + (0.5 + v / 2) * 0.4);
    if (v > 0.45 && was <= 0.7 && mood.confidence > 0.7) line('confident', 'smug', {}, 0);
    if (v < -0.45 && was >= 0.3 && mood.confidence < 0.3) {
      mood.tilt = clamp(mood.tilt + 0.25);
      line(mood.tilt > 0.5 ? 'tilt' : 'comeback', 'angry', {}, 1);
    }
  }

  /** A point, a capture, a rally: small moves of her mood. */
  function event(kind, n) {
    if (kind === 'herPoint') { mood.confidence = clamp(mood.confidence + 0.08); mood.tilt = clamp(mood.tilt - 0.12); }
    if (kind === 'yourPoint') { mood.confidence = clamp(mood.confidence - 0.08); mood.tilt = clamp(mood.tilt + 0.12); }
    if (kind === 'rally') {
      mood.focus = clamp(0.3 + n * 0.05);
      if (n >= 8 && n % 4 === 0) line('rally', 'focus', { n }, 1);
    }
    if (kind === 'calm') { mood.focus = clamp(mood.focus - 0.2, 0.3); }
  }

  /** Something she remembers about how you play: count[key] += 1, and the most common one. */
  function habit(name, value) {
    rec.habits = rec.habits || {};
    const h = (rec.habits[name] = rec.habits[name] || {});
    h[value] = (h[value] || 0) + 1;
    save(all);
  }
  function habitOf(name) {
    const h = rec.habits?.[name];
    if (!h) return null;
    const total = Object.values(h).reduce((a, b) => a + b, 0);
    const [top, n] = Object.entries(h).sort((a, b) => b[1] - a[1])[0] || [];
    return top == null ? null : { value: top, share: n / total, total, counts: h };
  }

  /** The game begins: she greets you by your record against her (and says if she's getting serious). */
  function start() {
    const vars = { h: rec.her, y: rec.you };
    if (youStreak >= 2) line('serious', 'angry', { n: youStreak }, 2);
    else if (!rec.you && !rec.her && !rec.draw) line('greetFirst', 'smug', vars, 2);
    else if (rec.her > rec.you) line('greetAhead', 'smug', vars, 2);
    else if (rec.her < rec.you) line('greetBehind', 'angry', vars, 2);
    else line('greetEven', 'focus', vars, 2);
    if (form <= -4 && Math.random() < 0.5) setTimeout(() => line('easeOff', 'wink', {}, 2), 2600);
  }

  /** The game ended: her record of it. */
  function end(result) {
    clearTimeout(idleTimer);
    rec[result] = (rec[result] || 0) + 1;
    rec.last = [...(rec.last || []), result].slice(-12);
    save(all);
  }

  function stop() { clearTimeout(idleTimer); }

  return { stop, line, face, skill, choose, thinkTime, afterHer, hesitation, yourMove, yourTurn, standing, event, habit, habitOf, start, end, mood, record: () => ({ ...rec }), lines };
}

/** Your record against her in a game (the menu shows it). */
export function record(game) {
  const r = load()[game];
  return r ? { you: r.you || 0, her: r.her || 0, draw: r.draw || 0 } : null;
}

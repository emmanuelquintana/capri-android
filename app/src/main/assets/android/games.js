// Games with her (1.10.0, games.html in the app: PageActivity "games"). "Que puedas jugar juegos
// con ella (no juegos que exijan mucho) sino clásicos como ajedrez, buscaminas, cartas…".
// Four, all here on the phone, no internet: chess, minesweeper, 21 (blackjack, she deals) and
// tic-tac-toe. She plays (or watches) and says what she thinks, with her faces.

const bridge = window.CapriPage;
const info = (() => { try { return JSON.parse(bridge?.info?.() || '{}'); } catch { return {}; } })();
const LANGS = ['es', 'en', 'fr', 'pt', 'ru'];
const lang = LANGS.includes(info.lang) ? info.lang : 'es';
const you = String(info.name || '').trim();
const FACES = '../capri/assets/capri/';
const pick = (list) => list[Math.floor(Math.random() * list.length)];
const $ = (sel, root = document) => root.querySelector(sel);
const el = (tag, cls, text) => {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  if (text != null) e.textContent = text;
  return e;
};
const buzz = () => { try { bridge?.haptic?.(); } catch {} };

// ---------- her words ----------
const W = {
  es: {
    title: 'Juegos', pick: 'Elige un juego', back: 'Menú', again: 'Otra vez', level: 'Dificultad',
    pong: 'Ping pong', rps: 'Piedra, papel o tijera',
    pongHow: 'Mueve tu pala con el dedo. A 5 puntos.', rpsHow: 'Al mejor de 5. Yo también elijo.',
    rock: 'Piedra', paper: 'Papel', scissors: 'Tijera', score: 'Marcador',
    herPoint: ['Punto mío.', 'Muy lenta… o lento.', 'Ja, no la viste.'],
    yourPoint: ['Ok, esa fue buena.', 'Suerte.', 'Hmph. Punto tuyo.'],
    rpsTie: ['Lo mismo. ¿Me lees la mente?', 'Empate. Otra.'],
    rpsMine: ['Te leí.', 'Predecible.', 'Esa era fácil.'],
    rpsYours: ['¿Cómo supiste?', 'Ugh. Bien.', 'Ok, esa no la vi venir.'],
    easy: 'Fácil', normal: 'Normal', hard: 'Difícil',
    chess: 'Ajedrez', mines: 'Buscaminas', cards: '21', ttt: 'Tres en raya',
    chessHow: 'Tú juegas con blancas.', minesHow: 'Toca para abrir; usa la bandera para marcar.', cardsHow: 'Llega a 21 sin pasarte. Yo reparto.', tttHow: 'Tú eres X, yo soy O.',
    hit: 'Pedir', stand: 'Plantarse', flag: 'Bandera', you: 'Tú', her: 'Capri',
    hello: ['¿Jugamos? Elige uno, {name}.', 'Tengo ganas de ganarte en algo.', 'Juegos clásicos. Me encantan.'],
    start: ['Va, a ver qué tienes.', 'Prepárate.', 'No te voy a dejar ganar.', 'Concéntrate, {name}.'],
    think: ['Mmm…', 'Déjame pensar…', 'Interesante…', 'Calculando…'],
    youWin: ['Ganaste… esta vez.', 'Ok, ok, eres buena… o bueno. Otra.', 'Me ganaste. No se lo cuentes a nadie.', '¡Bien jugado, {name}!'],
    herWin: ['Gané. Obviamente.', 'Ja. Otra vez será.', 'Eso fue fácil.', 'Netrunner 1, tú 0.'],
    draw: ['Empate. Nadie pierde, nadie gana.', 'Tablas. Respeto.', 'Empate… sospechoso.'],
    check: ['Jaque.', 'Cuidado con tu rey.', 'Jaque, {name}.'],
    inCheck: ['Uy, jaque. Bien visto.', 'Mi rey… ok.'],
    took: ['Esa me la quedo.', 'Gracias por la pieza.', 'Mía.'],
    lost: ['Ey, esa era mía.', 'Ok, te la dejo pasar.', 'Hmph.'],
    boom: ['¡BOOM! Ja, una mina.', 'Explotaste. Literalmente.', 'Uy… eso dolió.'],
    cleared: ['¡Limpiaste todo! Impresionante.', 'Ni una mina. Eres de las buenas.'],
    bust: ['Te pasaste.', '¡Más de 21! Perdiste.'],
    herBust: ['Me pasé… no digas nada.', 'Ugh, me pasé.'],
    blackjack: ['¡21 de una! Suerte.', 'Blackjack. Ok, eso fue bonito.'],
    hitLine: ['¿Otra? Valiente.', 'Arriesgado…'],
  },
  en: {
    title: 'Games', pick: 'Pick a game', back: 'Menu', again: 'Again', level: 'Difficulty',
    pong: 'Ping pong', rps: 'Rock, paper, scissors',
    pongHow: 'Move your paddle with your finger. First to 5.', rpsHow: 'Best of 5. I pick too.',
    rock: 'Rock', paper: 'Paper', scissors: 'Scissors', score: 'Score',
    herPoint: ['My point.', 'Too slow.', 'Ha, you didn’t even see it.'],
    yourPoint: ['Ok, that was a good one.', 'Lucky.', 'Hmph. Your point.'],
    rpsTie: ['Same thing. Are you reading my mind?', 'Tie. Again.'],
    rpsMine: ['Read you.', 'Predictable.', 'That one was easy.'],
    rpsYours: ['How did you know?', 'Ugh. Fine.', 'Ok, didn’t see that coming.'],
    easy: 'Easy', normal: 'Normal', hard: 'Hard',
    chess: 'Chess', mines: 'Minesweeper', cards: '21', ttt: 'Tic-tac-toe',
    chessHow: 'You play white.', minesHow: 'Tap to open; use the flag to mark.', cardsHow: 'Get to 21 without going over. I deal.', tttHow: 'You’re X, I’m O.',
    hit: 'Hit', stand: 'Stand', flag: 'Flag', you: 'You', her: 'Capri',
    hello: ['Wanna play? Pick one, {name}.', 'I feel like beating you at something.', 'Classic games. I love them.'],
    start: ['Okay, show me what you’ve got.', 'Get ready.', 'I’m not letting you win.', 'Focus, {name}.'],
    think: ['Hmm…', 'Let me think…', 'Interesting…', 'Calculating…'],
    youWin: ['You won… this time.', 'Okay, okay, you’re good. Again.', 'You beat me. Don’t tell anyone.', 'Well played, {name}!'],
    herWin: ['I won. Obviously.', 'Ha. Next time.', 'That was easy.', 'Netrunner 1, you 0.'],
    draw: ['A draw. Nobody wins, nobody loses.', 'Draw. Respect.', 'A draw… suspicious.'],
    check: ['Check.', 'Watch your king.', 'Check, {name}.'],
    inCheck: ['Oh, check. Nice one.', 'My king… fine.'],
    took: ['I’ll keep that one.', 'Thanks for the piece.', 'Mine.'],
    lost: ['Hey, that one was mine.', 'Okay, I’ll let that slide.', 'Hmph.'],
    boom: ['BOOM! Ha, a mine.', 'You blew up. Literally.', 'Ouch… that hurt.'],
    cleared: ['You cleared it all! Impressive.', 'Not a single mine. You’re good.'],
    bust: ['You went over.', 'Over 21! You lose.'],
    herBust: ['I went over… don’t say anything.', 'Ugh, bust.'],
    blackjack: ['21 right away! Lucky.', 'Blackjack. Okay, that was nice.'],
    hitLine: ['Another? Brave.', 'Risky…'],
  },
  fr: {
    title: 'Jeux', pick: 'Choisis un jeu', back: 'Menu', again: 'Encore', level: 'Difficulté',
    pong: 'Ping-pong', rps: 'Pierre, feuille, ciseaux',
    pongHow: 'Bouge ta raquette avec le doigt. En 5 points.', rpsHow: 'En 3 manches gagnantes sur 5. Je choisis aussi.',
    rock: 'Pierre', paper: 'Feuille', scissors: 'Ciseaux', score: 'Score',
    herPoint: ['Point pour moi.', 'Trop lent.', 'Ha, tu ne l’as même pas vue.'],
    yourPoint: ['Ok, celle-là était bien.', 'Coup de chance.', 'Hmph. Point pour toi.'],
    rpsTie: ['Pareil. Tu lis dans mes pensées ?', 'Égalité. Encore.'],
    rpsMine: ['Je t’ai lu.', 'Prévisible.', 'Trop facile.'],
    rpsYours: ['Comment tu as su ?', 'Pff. D’accord.', 'Ok, je ne l’ai pas vue venir.'],
    easy: 'Facile', normal: 'Normal', hard: 'Difficile',
    chess: 'Échecs', mines: 'Démineur', cards: '21', ttt: 'Morpion',
    chessHow: 'Tu joues les blancs.', minesHow: 'Touche pour ouvrir ; le drapeau pour marquer.', cardsHow: 'Atteins 21 sans dépasser. Je distribue.', tttHow: 'Tu es X, je suis O.',
    hit: 'Carte', stand: 'Rester', flag: 'Drapeau', you: 'Toi', her: 'Capri',
    hello: ['On joue ? Choisis, {name}.', 'J’ai envie de te battre à quelque chose.', 'Des jeux classiques. J’adore.'],
    start: ['Allez, montre-moi.', 'Prépare-toi.', 'Je ne vais pas te laisser gagner.', 'Concentre-toi, {name}.'],
    think: ['Hmm…', 'Laisse-moi réfléchir…', 'Intéressant…', 'Je calcule…'],
    youWin: ['Tu as gagné… cette fois.', 'Bon, bon, tu es fort·e. Encore.', 'Tu m’as battue. Ne le dis à personne.', 'Bien joué, {name} !'],
    herWin: ['J’ai gagné. Évidemment.', 'Ha. La prochaine fois.', 'C’était facile.', 'Netrunner 1, toi 0.'],
    draw: ['Égalité. Personne ne gagne.', 'Nulle. Respect.', 'Égalité… louche.'],
    check: ['Échec.', 'Attention à ton roi.', 'Échec, {name}.'],
    inCheck: ['Oh, échec. Bien vu.', 'Mon roi… d’accord.'],
    took: ['Je garde celle-là.', 'Merci pour la pièce.', 'À moi.'],
    lost: ['Hé, c’était la mienne.', 'Bon, je laisse passer.', 'Hmph.'],
    boom: ['BOUM ! Ha, une mine.', 'Tu as explosé. Littéralement.', 'Aïe… ça fait mal.'],
    cleared: ['Tout déminé ! Impressionnant.', 'Pas une seule mine. Bravo.'],
    bust: ['Tu as dépassé.', 'Plus de 21 ! Perdu.'],
    herBust: ['J’ai dépassé… ne dis rien.', 'Raté, j’ai dépassé.'],
    blackjack: ['21 d’entrée ! Chanceux·se.', 'Blackjack. Bon, c’était joli.'],
    hitLine: ['Encore une ? Courageux·se.', 'Risqué…'],
  },
  pt: {
    title: 'Jogos', pick: 'Escolha um jogo', back: 'Menu', again: 'De novo', level: 'Dificuldade',
    pong: 'Pingue-pongue', rps: 'Pedra, papel e tesoura',
    pongHow: 'Mova sua raquete com o dedo. Até 5 pontos.', rpsHow: 'Melhor de 5. Eu também escolho.',
    rock: 'Pedra', paper: 'Papel', scissors: 'Tesoura', score: 'Placar',
    herPoint: ['Ponto meu.', 'Lento demais.', 'Ha, nem viu.'],
    yourPoint: ['Ok, essa foi boa.', 'Sorte.', 'Hmph. Ponto seu.'],
    rpsTie: ['A mesma coisa. Tá lendo minha mente?', 'Empate. De novo.'],
    rpsMine: ['Te li.', 'Previsível.', 'Essa foi fácil.'],
    rpsYours: ['Como você soube?', 'Argh. Tá bom.', 'Ok, essa eu não vi chegando.'],
    easy: 'Fácil', normal: 'Normal', hard: 'Difícil',
    chess: 'Xadrez', mines: 'Campo minado', cards: '21', ttt: 'Jogo da velha',
    chessHow: 'Você joga de brancas.', minesHow: 'Toque para abrir; use a bandeira para marcar.', cardsHow: 'Chegue a 21 sem passar. Eu dou as cartas.', tttHow: 'Você é X, eu sou O.',
    hit: 'Pedir', stand: 'Parar', flag: 'Bandeira', you: 'Você', her: 'Capri',
    hello: ['Vamos jogar? Escolha um, {name}.', 'Tô com vontade de ganhar de você em alguma coisa.', 'Jogos clássicos. Amo.'],
    start: ['Vai, mostra o que você tem.', 'Se prepara.', 'Não vou te deixar ganhar.', 'Foco, {name}.'],
    think: ['Hmm…', 'Deixa eu pensar…', 'Interessante…', 'Calculando…'],
    youWin: ['Você ganhou… dessa vez.', 'Tá, tá, você é bom. De novo.', 'Você me venceu. Não conta pra ninguém.', 'Boa jogada, {name}!'],
    herWin: ['Ganhei. Óbvio.', 'Ha. Fica pra próxima.', 'Essa foi fácil.', 'Netrunner 1, você 0.'],
    draw: ['Empate. Ninguém ganha, ninguém perde.', 'Empate. Respeito.', 'Empate… suspeito.'],
    check: ['Xeque.', 'Cuidado com seu rei.', 'Xeque, {name}.'],
    inCheck: ['Opa, xeque. Boa.', 'Meu rei… tá bom.'],
    took: ['Essa eu fico.', 'Valeu pela peça.', 'Minha.'],
    lost: ['Ei, essa era minha.', 'Tá, deixo passar.', 'Hmph.'],
    boom: ['BUM! Ha, uma mina.', 'Você explodiu. Literalmente.', 'Ai… essa doeu.'],
    cleared: ['Você limpou tudo! Impressionante.', 'Nenhuma mina. Mandou bem.'],
    bust: ['Você passou.', 'Mais de 21! Perdeu.'],
    herBust: ['Passei… não fala nada.', 'Argh, estourei.'],
    blackjack: ['21 de cara! Sorte.', 'Blackjack. Tá, essa foi bonita.'],
    hitLine: ['Mais uma? Corajoso.', 'Arriscado…'],
  },
  ru: {
    title: 'Игры', pick: 'Выбери игру', back: 'Меню', again: 'Ещё раз', level: 'Сложность',
    pong: 'Пинг-понг', rps: 'Камень, ножницы, бумага',
    pongHow: 'Двигай ракетку пальцем. До 5 очков.', rpsHow: 'До трёх побед. Я тоже выбираю.',
    rock: 'Камень', paper: 'Бумага', scissors: 'Ножницы', score: 'Счёт',
    herPoint: ['Очко моё.', 'Слишком медленно.', 'Ха, ты даже не увидел(а).'],
    yourPoint: ['Ладно, это было неплохо.', 'Повезло.', 'Хмф. Твоё очко.'],
    rpsTie: ['Одно и то же. Ты читаешь мои мысли?', 'Ничья. Ещё.'],
    rpsMine: ['Я тебя прочитала.', 'Предсказуемо.', 'Это было легко.'],
    rpsYours: ['Как ты узнал(а)?', 'Ух. Ладно.', 'Ок, этого я не ожидала.'],
    easy: 'Легко', normal: 'Нормально', hard: 'Сложно',
    chess: 'Шахматы', mines: 'Сапёр', cards: '21', ttt: 'Крестики-нолики',
    chessHow: 'Ты играешь белыми.', minesHow: 'Нажми, чтобы открыть; флажок — чтобы отметить.', cardsHow: 'Набери 21 и не перебери. Я сдаю.', tttHow: 'Ты — X, я — O.',
    hit: 'Ещё', stand: 'Хватит', flag: 'Флажок', you: 'Ты', her: 'Капри',
    hello: ['Сыграем? Выбирай, {name}.', 'Хочу тебя в чём-нибудь обыграть.', 'Классические игры. Обожаю.'],
    start: ['Давай, покажи, что умеешь.', 'Готовься.', 'Поддаваться не буду.', 'Сосредоточься, {name}.'],
    think: ['Хмм…', 'Дай подумать…', 'Интересно…', 'Считаю…'],
    youWin: ['Ты выиграл… на этот раз.', 'Ладно, ладно, ты хорош. Ещё.', 'Ты меня обыграл. Никому не говори.', 'Хорошая игра, {name}!'],
    herWin: ['Я выиграла. Очевидно.', 'Ха. В другой раз.', 'Это было легко.', 'Нетраннер 1, ты 0.'],
    draw: ['Ничья. Никто не проиграл.', 'Ничья. Уважаю.', 'Ничья… подозрительно.'],
    check: ['Шах.', 'Береги короля.', 'Шах, {name}.'],
    inCheck: ['Ой, шах. Неплохо.', 'Мой король… ладно.'],
    took: ['Эту я заберу.', 'Спасибо за фигуру.', 'Моя.'],
    lost: ['Эй, это была моя.', 'Ладно, прощаю.', 'Хмф.'],
    boom: ['БУМ! Ха, мина.', 'Ты взорвался. Буквально.', 'Ай… больно.'],
    cleared: ['Всё разминировано! Впечатляет.', 'Ни одной мины. Молодец.'],
    bust: ['Перебор.', 'Больше 21! Проиграл.'],
    herBust: ['У меня перебор… молчи.', 'Ну вот, перебор.'],
    blackjack: ['21 сразу! Везёт.', 'Блэкджек. Ладно, красиво.'],
    hitLine: ['Ещё? Смело.', 'Рискованно…'],
  },
}[lang];
const fill = (s) => s.replace(/\{name\}/g, you || (lang === 'en' ? 'you' : lang === 'ru' ? 'ты' : lang === 'fr' ? 'toi' : lang === 'pt' ? 'você' : 'tú')).replace(/,\s*(tú|you|toi|você|ты)([.!?])/i, '$2');

// her face and her line, at the top
const face = $('#face');
const line = $('#line');
let lineTimer = 0;
function say(key, mood = 'look') {
  const text = Array.isArray(W[key]) ? fill(pick(W[key])) : fill(key);
  face.src = mood === 'grin' ? `${FACES}capri-face.png` : `${FACES}face-${mood}.png`;
  face.classList.remove('is-new');
  void face.offsetWidth;
  face.classList.add('is-new');
  line.textContent = '';
  clearInterval(lineTimer);
  const chars = [...text];
  let i = 0;
  lineTimer = setInterval(() => {
    i += 1;
    line.textContent = chars.slice(0, i).join('');
    if (i >= chars.length) clearInterval(lineTimer);
  }, 22);
}
const played = (game, result) => { try { bridge?.played?.(JSON.stringify({ game, result, at: Date.now() })); } catch {} };

// ---------- the menu ----------
const stage = $('#stage');
const GAMES = [
  { id: 'chess', icon: '♞', how: 'chessHow', run: chess },
  { id: 'mines', icon: '✹', how: 'minesHow', run: mines },
  { id: 'cards', icon: '♠', how: 'cardsHow', run: cards },
  { id: 'ttt', icon: '✕', how: 'tttHow', run: ttt },
  { id: 'pong', icon: '◐', how: 'pongHow', run: pong },
  { id: 'rps', icon: '✊', how: 'rpsHow', run: rps },
];
let level = (() => { try { return localStorage.getItem('capri.games.level') || 'normal'; } catch { return 'normal'; } })();
let current = null;
let stopGame = null; // a game with its own loop leaves this to stop it
const halt = () => { try { stopGame?.(); } catch {} stopGame = null; };

function menu() {
  halt();
  current = null;
  stage.innerHTML = '';
  $('#title').textContent = W.title;
  const grid = el('div', 'menu');
  for (const g of GAMES) {
    const b = el('button', 'game');
    b.append(el('span', 'game__icon', g.icon), el('span', 'game__name', W[g.id]), el('span', 'game__how', W[g.how]));
    b.addEventListener('click', () => { buzz(); open(g); });
    grid.append(b);
  }
  stage.append(el('p', 'tag', `// ${W.pick}`), grid);
  const lv = el('div', 'levels');
  lv.append(el('span', 'tag', `// ${W.level}`));
  for (const id of ['easy', 'normal', 'hard']) {
    const b = el('button', `pill${id === level ? ' is-on' : ''}`, W[id]);
    b.addEventListener('click', () => {
      level = id;
      try { localStorage.setItem('capri.games.level', id); } catch {}
      lv.querySelectorAll('.pill').forEach((p) => p.classList.toggle('is-on', p === b));
    });
    lv.append(b);
  }
  stage.append(lv);
  say('hello', 'grin');
}

function open(g) {
  halt();
  current = g;
  stage.innerHTML = '';
  $('#title').textContent = W[g.id];
  const bar = el('div', 'bar');
  const back = el('button', 'pill', `‹ ${W.back}`);
  back.addEventListener('click', menu);
  const again = el('button', 'pill', `↻ ${W.again}`);
  again.addEventListener('click', () => open(g));
  bar.append(back, again);
  const box = el('div', 'box');
  stage.append(bar, box);
  say('start', 'smug');
  g.run(box);
}

window.capriBack = () => {
  if (!current) return false;
  menu();
  return true;
};

// ---------- tic-tac-toe ----------
function ttt(box) {
  const cells = Array(9).fill(null);
  const LINES = [[0, 1, 2], [3, 4, 5], [6, 7, 8], [0, 3, 6], [1, 4, 7], [2, 5, 8], [0, 4, 8], [2, 4, 6]];
  const winner = (b) => {
    for (const [a, c, d] of LINES) if (b[a] && b[a] === b[c] && b[a] === b[d]) return b[a];
    return b.every(Boolean) ? 'draw' : null;
  };
  const grid = el('div', 'ttt');
  const btns = cells.map((_, i) => {
    const b = el('button', 'ttt__cell');
    b.addEventListener('click', () => play(i));
    grid.append(b);
    return b;
  });
  box.append(grid);
  let over = false;
  const draw = () => cells.forEach((v, i) => { btns[i].textContent = v || ''; btns[i].className = `ttt__cell${v ? ` is-${v.toLowerCase()}` : ''}`; });
  function best(b, me) {
    // minimax: her best move (O), whole tree (it's small)
    const score = (bb, turn, depth) => {
      const w = winner(bb);
      if (w === 'O') return 10 - depth;
      if (w === 'X') return depth - 10;
      if (w === 'draw') return 0;
      let top = turn === 'O' ? -99 : 99;
      for (let i = 0; i < 9; i++) {
        if (bb[i]) continue;
        bb[i] = turn;
        const s = score(bb, turn === 'O' ? 'X' : 'O', depth + 1);
        bb[i] = null;
        top = turn === 'O' ? Math.max(top, s) : Math.min(top, s);
      }
      return top;
    };
    let pickAt = -1;
    let top = -99;
    for (let i = 0; i < 9; i++) {
      if (b[i]) continue;
      b[i] = me;
      const s = score(b, 'X', 1);
      b[i] = null;
      if (s > top) { top = s; pickAt = i; }
    }
    return pickAt;
  }
  function end(w) {
    over = true;
    if (w === 'X') { say('youWin', 'surprised'); played('ttt', 'you'); }
    else if (w === 'O') { say('herWin', 'smug'); played('ttt', 'her'); }
    else { say('draw', 'calm'); played('ttt', 'draw'); }
  }
  function play(i) {
    if (over || cells[i]) return;
    buzz();
    cells[i] = 'X';
    draw();
    let w = winner(cells);
    if (w) return end(w);
    say('think', 'thinking');
    setTimeout(() => {
      const free = cells.map((v, k) => (v ? -1 : k)).filter((k) => k >= 0);
      const slip = { easy: 0.55, normal: 0.22, hard: 0 }[level];
      const at = Math.random() < slip ? pick(free) : best(cells, 'O');
      cells[at] = 'O';
      draw();
      w = winner(cells);
      if (w) end(w);
      else face.src = `${FACES}face-focus.png`;
    }, 450 + Math.random() * 400);
  }
  draw();
}

// ---------- minesweeper ----------
function mines(box) {
  const size = { easy: [8, 8, 8], normal: [9, 9, 12], hard: [10, 12, 22] }[level];
  const [W2, H2, N] = size;
  const cells = Array.from({ length: W2 * H2 }, () => ({ mine: false, open: false, flag: false, n: 0 }));
  let placed = false;
  let over = false;
  let flagging = false;
  const around = (i) => {
    const x = i % W2;
    const y = Math.floor(i / W2);
    const out = [];
    for (let dy = -1; dy <= 1; dy++) for (let dx = -1; dx <= 1; dx++) {
      if (!dx && !dy) continue;
      const nx = x + dx;
      const ny = y + dy;
      if (nx >= 0 && ny >= 0 && nx < W2 && ny < H2) out.push(ny * W2 + nx);
    }
    return out;
  };
  const place = (safe) => {
    const banned = new Set([safe, ...around(safe)]);
    let left = N;
    while (left) {
      const i = Math.floor(Math.random() * cells.length);
      if (cells[i].mine || banned.has(i)) continue;
      cells[i].mine = true;
      left -= 1;
    }
    cells.forEach((c, i) => { c.n = around(i).filter((k) => cells[k].mine).length; });
    placed = true;
  };
  const info2 = el('div', 'mines__info');
  const flagBtn = el('button', 'pill', `⚑ ${W.flag}`);
  flagBtn.addEventListener('click', () => { flagging = !flagging; flagBtn.classList.toggle('is-on', flagging); });
  const count = el('span', 'mines__count');
  info2.append(count, flagBtn);
  const grid = el('div', 'mines');
  grid.style.gridTemplateColumns = `repeat(${W2}, 1fr)`;
  const btns = cells.map((_, i) => {
    const b = el('button', 'mines__cell');
    let held = 0;
    b.addEventListener('touchstart', () => { held = setTimeout(() => { held = -1; toggle(i); }, 420); }, { passive: true });
    b.addEventListener('touchend', () => { if (held > 0) clearTimeout(held); });
    b.addEventListener('click', () => {
      if (held === -1) { held = 0; return; }
      if (flagging) toggle(i);
      else reveal(i);
    });
    b.addEventListener('contextmenu', (e) => { e.preventDefault(); toggle(i); });
    grid.append(b);
    return b;
  });
  box.append(info2, grid);
  const draw = () => {
    cells.forEach((c, i) => {
      const b = btns[i];
      b.className = `mines__cell${c.open ? ' is-open' : ''}${c.open && c.mine ? ' is-boom' : ''}${c.flag ? ' is-flag' : ''}`;
      b.textContent = c.open ? (c.mine ? '✹' : c.n || '') : c.flag ? '⚑' : '';
      if (c.open && c.n && !c.mine) b.dataset.n = c.n;
      else delete b.dataset.n;
    });
    count.textContent = `✹ ${N - cells.filter((c) => c.flag).length}`;
  };
  const toggle = (i) => {
    if (over || cells[i].open) return;
    buzz();
    cells[i].flag = !cells[i].flag;
    draw();
  };
  let opened = 0;
  const reveal = (i) => {
    if (over || cells[i].open || cells[i].flag) return;
    if (!placed) place(i);
    const c = cells[i];
    if (c.mine) {
      over = true;
      cells.forEach((k) => { if (k.mine) k.open = true; });
      draw();
      buzz();
      say('boom', 'laugh');
      played('mines', 'her');
      return;
    }
    const stack = [i];
    while (stack.length) {
      const k = stack.pop();
      const cc = cells[k];
      if (cc.open || cc.flag) continue;
      cc.open = true;
      opened += 1;
      if (!cc.n) stack.push(...around(k).filter((j) => !cells[j].open && !cells[j].mine));
    }
    draw();
    if (cells.every((k) => k.mine || k.open)) {
      over = true;
      say('cleared', 'love');
      played('mines', 'you');
    } else if (opened > 6 && Math.random() < 0.12) {
      say('think', 'focus');
    }
  };
  draw();
}

// ---------- 21 ----------
function cards(box) {
  const SUITS = ['♠', '♥', '♦', '♣'];
  const RANKS = ['A', '2', '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K'];
  const deck = [];
  for (const s of SUITS) for (const r of RANKS) deck.push({ s, r });
  for (let i = deck.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [deck[i], deck[j]] = [deck[j], deck[i]];
  }
  const value = (hand) => {
    let v = 0;
    let aces = 0;
    for (const c of hand) {
      if (c.r === 'A') { v += 11; aces += 1; } else v += ['J', 'Q', 'K'].includes(c.r) ? 10 : Number(c.r);
    }
    while (v > 21 && aces) { v -= 10; aces -= 1; }
    return v;
  };
  const mine = [deck.pop(), deck.pop()];
  const hers = [deck.pop(), deck.pop()];
  let hidden = true;
  let over = false;
  const herRow = el('div', 'cards__row');
  const youRow = el('div', 'cards__row');
  const herSum = el('p', 'tag');
  const youSum = el('p', 'tag');
  const acts = el('div', 'cards__acts');
  const hit = el('button', 'pill pill--big', W.hit);
  const stand = el('button', 'pill pill--big', W.stand);
  acts.append(hit, stand);
  box.append(herSum, herRow, youSum, youRow, acts);
  const card = (c, back = false) => {
    const d = el('div', `card${back ? ' is-back' : ''}${['♥', '♦'].includes(c.s) ? ' is-red' : ''}`);
    if (!back) d.innerHTML = `<b>${c.r}</b><i>${c.s}</i>`;
    return d;
  };
  const draw = () => {
    herRow.replaceChildren(...hers.map((c, i) => card(c, hidden && i === 1)));
    youRow.replaceChildren(...mine.map((c) => card(c)));
    herSum.textContent = `// ${W.her} ${hidden ? '?' : value(hers)}`;
    youSum.textContent = `// ${W.you} ${value(mine)}`;
    hit.disabled = stand.disabled = over;
  };
  const finish = (who) => {
    over = true;
    hidden = false;
    draw();
    played('cards', who);
  };
  const herTurn = () => {
    hidden = false;
    draw();
    say('think', 'thinking');
    const step = () => {
      if (value(hers) < 17) {
        hers.push(deck.pop());
        draw();
        setTimeout(step, 650);
        return;
      }
      const h = value(hers);
      const y = value(mine);
      if (h > 21) { say('herBust', 'cringe'); finish('you'); } else if (h > y) { say('herWin', 'smug'); finish('her'); } else if (h < y) { say('youWin', 'surprised'); finish('you'); } else { say('draw', 'calm'); finish('draw'); }
    };
    setTimeout(step, 700);
  };
  hit.addEventListener('click', () => {
    if (over) return;
    buzz();
    mine.push(deck.pop());
    draw();
    if (value(mine) > 21) { say('bust', 'laugh'); finish('her'); } else if (value(mine) === 21) herTurn();
    else if (Math.random() < 0.3) say('hitLine', 'aside');
  });
  stand.addEventListener('click', () => {
    if (over) return;
    buzz();
    over = true;
    hit.disabled = stand.disabled = true;
    herTurn();
  });
  draw();
  if (value(mine) === 21) { say('blackjack', 'wide'); setTimeout(herTurn, 600); }
}

// ---------- chess ----------
// Board: 64 squares, 0 = a8 … 63 = h1 (white at the bottom). A piece: 'wP', 'bK'…
function chess(box) {
  const GLYPH = { K: '♚', Q: '♛', R: '♜', B: '♝', N: '♞', P: '♟' };
  const VAL = { P: 100, N: 320, B: 330, R: 500, Q: 900, K: 0 };
  const start = 'rnbqkbnrpppppppp' + '.'.repeat(32) + 'PPPPPPPPRNBQKBNR';
  let s = {
    b: [...start].map((c) => (c === '.' ? null : (c === c.toUpperCase() ? 'w' : 'b') + c.toUpperCase())),
    turn: 'w', castle: { wK: true, wQ: true, bK: true, bQ: true }, ep: null,
  };
  const rc = (i) => [Math.floor(i / 8), i % 8];
  const at = (r, c) => (r >= 0 && r < 8 && c >= 0 && c < 8 ? r * 8 + c : -1);
  const other = (col) => (col === 'w' ? 'b' : 'w');

  function attacked(st, sq, by) {
    const [r, c] = rc(sq);
    const b = st.b;
    const pr = by === 'w' ? 1 : -1; // a white pawn attacks from the row below
    for (const dc of [-1, 1]) { const k = at(r + pr, c + dc); if (k >= 0 && b[k] === `${by}P`) return true; }
    for (const [dr, dc] of [[1, 2], [2, 1], [-1, 2], [-2, 1], [1, -2], [2, -1], [-1, -2], [-2, -1]]) { const k = at(r + dr, c + dc); if (k >= 0 && b[k] === `${by}N`) return true; }
    for (let dr = -1; dr <= 1; dr++) for (let dc = -1; dc <= 1; dc++) { if (!dr && !dc) continue; const k = at(r + dr, c + dc); if (k >= 0 && b[k] === `${by}K`) return true; }
    const ray = (dirs, kinds) => {
      for (const [dr, dc] of dirs) {
        let rr = r + dr;
        let cc = c + dc;
        while (rr >= 0 && rr < 8 && cc >= 0 && cc < 8) {
          const p = b[rr * 8 + cc];
          if (p) { if (p[0] === by && kinds.includes(p[1])) return true; break; }
          rr += dr; cc += dc;
        }
      }
      return false;
    };
    return ray([[1, 0], [-1, 0], [0, 1], [0, -1]], 'RQ') || ray([[1, 1], [1, -1], [-1, 1], [-1, -1]], 'BQ');
  }

  function pseudo(st) {
    const moves = [];
    const b = st.b;
    const me = st.turn;
    for (let i = 0; i < 64; i++) {
      const p = b[i];
      if (!p || p[0] !== me) continue;
      const [r, c] = rc(i);
      const add = (to, extra = {}) => moves.push({ from: i, to, ...extra });
      const kind = p[1];
      if (kind === 'P') {
        const dir = me === 'w' ? -1 : 1;
        const home = me === 'w' ? 6 : 1;
        const last = me === 'w' ? 0 : 7;
        const one = at(r + dir, c);
        if (one >= 0 && !b[one]) {
          add(one, r + dir === last ? { promo: 'Q' } : {});
          const two = at(r + 2 * dir, c);
          if (r === home && !b[two]) add(two, { double: true });
        }
        for (const dc of [-1, 1]) {
          const k = at(r + dir, c + dc);
          if (k < 0) continue;
          if (b[k] && b[k][0] !== me) add(k, r + dir === last ? { promo: 'Q' } : {});
          else if (k === st.ep) add(k, { enPassant: true });
        }
      } else if (kind === 'N' || kind === 'K') {
        const steps = kind === 'N' ? [[1, 2], [2, 1], [-1, 2], [-2, 1], [1, -2], [2, -1], [-1, -2], [-2, -1]] : [[1, 0], [-1, 0], [0, 1], [0, -1], [1, 1], [1, -1], [-1, 1], [-1, -1]];
        for (const [dr, dc] of steps) { const k = at(r + dr, c + dc); if (k >= 0 && (!b[k] || b[k][0] !== me)) add(k); }
        if (kind === 'K') {
          const row = me === 'w' ? 7 : 0;
          if (i === row * 8 + 4 && !attacked(st, i, other(me))) {
            if (st.castle[`${me}K`] && !b[row * 8 + 5] && !b[row * 8 + 6] && b[row * 8 + 7] === `${me}R` && !attacked(st, row * 8 + 5, other(me))) add(row * 8 + 6, { castle: 'K' });
            if (st.castle[`${me}Q`] && !b[row * 8 + 3] && !b[row * 8 + 2] && !b[row * 8 + 1] && b[row * 8] === `${me}R` && !attacked(st, row * 8 + 3, other(me))) add(row * 8 + 2, { castle: 'Q' });
          }
        }
      } else {
        const dirs = { R: [[1, 0], [-1, 0], [0, 1], [0, -1]], B: [[1, 1], [1, -1], [-1, 1], [-1, -1]], Q: [[1, 0], [-1, 0], [0, 1], [0, -1], [1, 1], [1, -1], [-1, 1], [-1, -1]] }[kind];
        for (const [dr, dc] of dirs) {
          let rr = r + dr;
          let cc = c + dc;
          while (rr >= 0 && rr < 8 && cc >= 0 && cc < 8) {
            const k = rr * 8 + cc;
            if (b[k]) { if (b[k][0] !== me) add(k); break; }
            add(k);
            rr += dr; cc += dc;
          }
        }
      }
    }
    return moves;
  }

  function apply(st, m) {
    const b = st.b.slice();
    const p = b[m.from];
    const took = m.enPassant ? b[m.to + (st.turn === 'w' ? 8 : -8)] : b[m.to];
    b[m.to] = m.promo ? `${st.turn}${m.promo}` : p;
    b[m.from] = null;
    if (m.enPassant) b[m.to + (st.turn === 'w' ? 8 : -8)] = null;
    if (m.castle) {
      const row = st.turn === 'w' ? 7 : 0;
      if (m.castle === 'K') { b[row * 8 + 5] = b[row * 8 + 7]; b[row * 8 + 7] = null; } else { b[row * 8 + 3] = b[row * 8]; b[row * 8] = null; }
    }
    const castle = { ...st.castle };
    if (p[1] === 'K') { castle[`${st.turn}K`] = false; castle[`${st.turn}Q`] = false; }
    for (const [sq, key] of [[63, 'wK'], [56, 'wQ'], [7, 'bK'], [0, 'bQ']]) if (m.from === sq || m.to === sq) castle[key] = false;
    return { b, turn: other(st.turn), castle, ep: m.double ? (m.from + m.to) / 2 : null, took };
  }

  const kingAt = (st, col) => st.b.indexOf(`${col}K`);
  const legal = (st) => pseudo(st).filter((m) => { const n = apply(st, m); return !attacked(n, kingAt(n, st.turn), n.turn); });
  const inCheck = (st) => attacked(st, kingAt(st, st.turn), other(st.turn));

  // her eye for a position: material, and pieces toward the middle (pawns: forward)
  const CENTER = Array.from({ length: 64 }, (_, i) => { const [r, c] = rc(i); return 14 - (Math.abs(3.5 - r) + Math.abs(3.5 - c)) * 4; });
  function evaluate(st) {
    let v = 0;
    for (let i = 0; i < 64; i++) {
      const p = st.b[i];
      if (!p) continue;
      const [r] = rc(i);
      let x = VAL[p[1]];
      if (p[1] === 'N' || p[1] === 'B') x += CENTER[i];
      if (p[1] === 'P') x += (p[0] === 'w' ? 6 - r : r - 1) * 8 + CENTER[i] / 3;
      if (p[1] === 'Q') x += CENTER[i] / 3;
      v += p[0] === 'b' ? x : -x;
    }
    return v; // good for her (black)
  }
  function search(st, depth, alpha, beta) {
    const moves = legal(st);
    if (!moves.length) return inCheck(st) ? (st.turn === 'b' ? -99999 - depth : 99999 + depth) : 0;
    if (!depth) return evaluate(st);
    // captures first: alpha-beta cuts more
    moves.sort((a, b) => (st.b[b.to] ? VAL[st.b[b.to][1]] : 0) - (st.b[a.to] ? VAL[st.b[a.to][1]] : 0));
    if (st.turn === 'b') {
      let best = -Infinity;
      for (const m of moves) {
        best = Math.max(best, search(apply(st, m), depth - 1, alpha, beta));
        alpha = Math.max(alpha, best);
        if (alpha >= beta) break;
      }
      return best;
    }
    let best = Infinity;
    for (const m of moves) {
      best = Math.min(best, search(apply(st, m), depth - 1, alpha, beta));
      beta = Math.min(beta, best);
      if (alpha >= beta) break;
    }
    return best;
  }
  function think(st) {
    const moves = legal(st);
    const depth = { easy: 1, normal: 2, hard: 3 }[level];
    let best = null;
    let top = -Infinity;
    for (const m of moves) {
      const v = search(apply(st, m), depth - 1, -Infinity, Infinity) + Math.random() * (level === 'easy' ? 120 : 12);
      if (v > top) { top = v; best = m; }
    }
    return best;
  }

  const board = el('div', 'chess');
  const squares = [];
  for (let i = 0; i < 64; i++) {
    const [r, c] = rc(i);
    const d = el('button', `chess__sq${(r + c) % 2 ? ' is-dark' : ''}`);
    d.addEventListener('click', () => tap(i));
    squares.push(d);
    board.append(d);
  }
  const caps = el('p', 'tag chess__caps');
  box.append(board, caps);
  let chosen = null;
  let targets = [];
  let over = false;
  let lastMove = null;
  const taken = { w: [], b: [] };
  function draw() {
    const check = inCheck(s) ? kingAt(s, s.turn) : -1;
    for (let i = 0; i < 64; i++) {
      const p = s.b[i];
      const d = squares[i];
      d.textContent = p ? GLYPH[p[1]] : '';
      d.className = `chess__sq${(Math.floor(i / 8) + (i % 8)) % 2 ? ' is-dark' : ''}${p ? ` is-${p[0]}` : ''}${i === chosen ? ' is-chosen' : ''}${targets.some((m) => m.to === i) ? ' is-target' : ''}${lastMove && (lastMove.from === i || lastMove.to === i) ? ' is-last' : ''}${i === check ? ' is-check' : ''}`;
    }
    caps.textContent = `${taken.b.map((k) => GLYPH[k]).join('')}  ·  ${taken.w.map((k) => GLYPH[k]).join('')}`;
  }
  function done() {
    const moves = legal(s);
    if (moves.length) return false;
    over = true;
    if (inCheck(s)) {
      if (s.turn === 'b') { say('youWin', 'surprised'); played('chess', 'you'); } else { say('herWin', 'smug'); played('chess', 'her'); }
    } else { say('draw', 'calm'); played('chess', 'draw'); }
    return true;
  }
  function move(m) {
    const n = apply(s, m);
    if (n.took) taken[s.turn === 'w' ? 'w' : 'b'].push(n.took[1]);
    lastMove = m;
    s = n;
  }
  function tap(i) {
    if (over || s.turn !== 'w') return;
    const t = targets.find((m) => m.to === i);
    if (t) {
      buzz();
      const took = s.b[i] || t.enPassant;
      move(t);
      chosen = null;
      targets = [];
      draw();
      if (done()) return;
      if (took && Math.random() < 0.6) say('lost', 'angry');
      else if (inCheck(s)) say('inCheck', 'surprised');
      else say('think', 'thinking');
      setTimeout(herMove, 250);
      return;
    }
    if (s.b[i]?.[0] === 'w') {
      chosen = i;
      targets = legal(s).filter((m) => m.from === i);
    } else {
      chosen = null;
      targets = [];
    }
    draw();
  }
  function herMove() {
    const m = think(s);
    if (!m) return done();
    const took = s.b[m.to] || m.enPassant;
    setTimeout(() => {
      move(m);
      draw();
      if (done()) return;
      if (inCheck(s)) say('check', 'smug');
      else if (took && Math.random() < 0.6) say('took', 'grin');
      else face.src = `${FACES}face-focus.png`;
    }, 200);
  }
  draw();
}

// ---------- ping pong ----------
function pong(box) {
  const css = getComputedStyle(document.documentElement);
  const YELLOW = css.getPropertyValue('--yellow').trim() || '#fcee0a';
  const GREEN = '#36ff7a';
  const score = el('div', 'mines__info');
  const you = el('span', '', `${W.you} 0`);
  const her = el('span', '', `${W.her} 0`);
  score.append(you, her);
  const cv = el('canvas', 'pong');
  box.append(score, cv);
  const dpr = Math.min(2, window.devicePixelRatio || 1);
  const w = Math.min(box.clientWidth || 340, 460);
  const h = Math.round(Math.min(w * 1.35, Math.max(320, window.innerHeight - cv.getBoundingClientRect().top - 24)));
  cv.width = w * dpr; cv.height = h * dpr;
  cv.style.width = `${w}px`; cv.style.height = `${h}px`;
  const g = cv.getContext('2d');
  g.scale(dpr, dpr);
  const pw = w * 0.24, ph = 10, r = 7;
  const skill = { easy: [0.45, 34], normal: [0.7, 16], hard: [0.95, 4] }[level]; // her speed (share of ball's), her aim error
  const st = { me: w / 2, cap: w / 2, bx: w / 2, by: h / 2, vx: 0, vy: 0, mine: 0, hers: 0, wait: 0, aim: 0, over: false };
  const serve = (toward) => {
    const speed = h * { easy: 0.55, normal: 0.7, hard: 0.85 }[level];
    const a = (Math.random() - 0.5) * 0.9;
    st.bx = w / 2; st.by = h / 2;
    st.vx = Math.sin(a) * speed; st.vy = Math.cos(a) * speed * toward;
    st.wait = 0.8;
    st.aim = (Math.random() - 0.5) * 2 * skill[1];
  };
  const point = (mine) => {
    buzz();
    if (mine) { st.mine += 1; you.textContent = `${W.you} ${st.mine}`; }
    else { st.hers += 1; her.textContent = `${W.her} ${st.hers}`; }
    if (st.mine >= 5 || st.hers >= 5) {
      st.over = true;
      if (st.mine >= 5) { say('youWin', 'surprised'); played('pong', 'you'); }
      else { say('herWin', 'smug'); played('pong', 'her'); }
      return;
    }
    say(mine ? 'yourPoint' : 'herPoint', mine ? 'angry' : 'grin');
    serve(mine ? -1 : 1);
  };
  const follow = (e) => {
    const t = e.touches?.[0] || e;
    const rect = cv.getBoundingClientRect();
    st.me = Math.max(pw / 2, Math.min(w - pw / 2, t.clientX - rect.left));
    if (e.cancelable) e.preventDefault();
  };
  cv.addEventListener('touchstart', follow, { passive: false });
  cv.addEventListener('touchmove', follow, { passive: false });
  cv.addEventListener('pointermove', (e) => { if (e.pointerType === 'mouse') follow(e); });
  const hitPaddle = (px, y, dir) => {
    if (Math.abs(st.bx - px) > pw / 2 + r) return false;
    const off = (st.bx - px) / (pw / 2); // -1..1: where on the paddle
    const speed = Math.min(Math.hypot(st.vx, st.vy) * 1.06, h * 1.9);
    const a = off * 1.05;
    st.vx = Math.sin(a) * speed; st.vy = Math.cos(a) * speed * dir;
    st.by = y;
    st.aim = (Math.random() - 0.5) * 2 * skill[1];
    return true;
  };
  let last = performance.now();
  let raf = 0;
  const tick = (now) => {
    const dt = Math.min(0.033, (now - last) / 1000);
    last = now;
    if (!st.over) {
      if (st.wait > 0) st.wait -= dt;
      else {
        st.bx += st.vx * dt; st.by += st.vy * dt;
        if (st.bx < r) { st.bx = r; st.vx = Math.abs(st.vx); }
        if (st.bx > w - r) { st.bx = w - r; st.vx = -Math.abs(st.vx); }
        const myY = h - 18 - ph, herY = 18 + ph;
        if (st.vy > 0 && st.by + r >= myY && st.by < myY + ph) { if (hitPaddle(st.me, myY - r, -1)) buzz(); }
        if (st.vy < 0 && st.by - r <= herY && st.by > herY - ph) hitPaddle(st.cap, herY + r, 1);
        if (st.by > h + r) point(false);
        else if (st.by < -r) point(true);
      }
      // her paddle: chases the ball when it comes to her, drifts to the middle when it doesn't
      const goal = st.vy < 0 ? st.bx + st.aim : w / 2;
      const max = Math.hypot(st.vx, st.vy) * skill[0] * dt;
      st.cap += Math.max(-max, Math.min(max, goal - st.cap));
      st.cap = Math.max(pw / 2, Math.min(w - pw / 2, st.cap));
    }
    g.clearRect(0, 0, w, h);
    g.fillStyle = 'rgba(31, 30, 23, 0.9)';
    g.fillRect(0, 0, w, h);
    g.strokeStyle = 'rgba(242, 241, 232, 0.18)';
    g.setLineDash([8, 8]);
    g.beginPath(); g.moveTo(0, h / 2); g.lineTo(w, h / 2); g.stroke();
    g.setLineDash([]);
    g.shadowBlur = 12;
    g.fillStyle = GREEN; g.shadowColor = GREEN;
    g.fillRect(st.cap - pw / 2, 18, pw, ph);
    g.fillStyle = YELLOW; g.shadowColor = YELLOW;
    g.fillRect(st.me - pw / 2, h - 18 - ph, pw, ph);
    if (!st.over) { g.beginPath(); g.arc(st.bx, st.by, r, 0, Math.PI * 2); g.fill(); }
    g.shadowBlur = 0;
    raf = requestAnimationFrame(tick);
  };
  serve(1);
  raf = requestAnimationFrame(tick);
  stopGame = () => cancelAnimationFrame(raf);
}

// ---------- rock, paper, scissors ----------
function rps(box) {
  const HANDS = [['rock', '✊'], ['paper', '✋'], ['scissors', '✌']];
  const beats = (a, b) => (a + 2) % 3 === b; // rock 0 > scissors 2, paper 1 > rock 0, scissors 2 > paper 1
  const score = el('div', 'mines__info');
  const you = el('span', '', `${W.you} 0`);
  const her = el('span', '', `${W.her} 0`);
  score.append(you, her);
  const show = el('div', 'rps__show');
  const mineHand = el('span', 'rps__hand', '?');
  const herHand = el('span', 'rps__hand is-her', '?');
  show.append(mineHand, el('span', 'rps__vs', 'VS'), herHand);
  const acts = el('div', 'rps__acts');
  const btns = HANDS.map(([id, icon], i) => {
    const b = el('button', 'game rps__pick');
    b.append(el('span', 'game__icon', icon), el('span', 'game__name', W[id]));
    b.addEventListener('click', () => throwHand(i));
    acts.append(b);
    return b;
  });
  box.append(score, show, acts);
  const st = { mine: 0, hers: 0, seen: [], busy: false };
  // she guesses your next hand from what you did after your last one (no peeking)
  const guess = () => {
    const h = st.seen;
    const smart = { easy: 0, normal: 0.45, hard: 0.8 }[level];
    if (h.length < 2 || Math.random() >= smart) return Math.floor(Math.random() * 3);
    const after = [0, 0, 0];
    for (let i = 1; i < h.length; i++) if (h[i - 1] === h[h.length - 1]) after[h[i]] += 1;
    const counts = after.some(Boolean) ? after : h.reduce((c, x) => (c[x] += 1, c), [0, 0, 0]);
    const likely = counts.indexOf(Math.max(...counts));
    return (likely + 1) % 3; // what beats it
  };
  function throwHand(i) {
    if (st.busy || st.over) return;
    buzz();
    st.busy = true;
    const hers = guess();
    st.seen.push(i);
    mineHand.textContent = HANDS[i][1];
    herHand.textContent = '…';
    btns.forEach((b, k) => b.classList.toggle('is-chosen', k === i));
    say('think', 'thinking');
    setTimeout(() => {
      herHand.textContent = HANDS[hers][1];
      herHand.classList.remove('is-pop'); void herHand.offsetWidth; herHand.classList.add('is-pop');
      if (i === hers) say('rpsTie', 'calm');
      else if (beats(i, hers)) { st.mine += 1; you.textContent = `${W.you} ${st.mine}`; say('rpsYours', 'angry'); }
      else { st.hers += 1; her.textContent = `${W.her} ${st.hers}`; say('rpsMine', 'grin'); }
      st.busy = false;
      if (st.mine >= 3 || st.hers >= 3) {
        st.over = true;
        btns.forEach((b) => { b.disabled = true; });
        setTimeout(() => {
          if (st.mine >= 3) { say('youWin', 'surprised'); played('rps', 'you'); }
          else { say('herWin', 'smug'); played('rps', 'her'); }
        }, 900);
      }
    }, 650);
  }
}

menu();

// Games with her (1.10.0, games.html in the app: PageActivity "games"). "Que puedas jugar juegos
// con ella (no juegos que exijan mucho) sino clásicos como ajedrez, buscaminas, cartas…".
// Six, all here on the phone, no internet: chess, minesweeper, 21 (blackjack, she deals),
// tic-tac-toe, ping pong and rock-paper-scissors. She plays (or watches) and says what she thinks,
// with her faces. How she plays, like a person and not a machine: brain.js.

import { rival, record } from './brain.js';

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
    stampYou: 'Ganaste', stampHer: 'Capri gana', stampDraw: 'Empate',
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
    stampYou: 'You win', stampHer: 'Capri wins', stampDraw: 'Draw',
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
    stampYou: 'Gagné', stampHer: 'Capri gagne', stampDraw: 'Égalité',
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
    stampYou: 'Você venceu', stampHer: 'Capri venceu', stampDraw: 'Empate',
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
    stampYou: 'Победа', stampHer: 'Капри победила', stampDraw: 'Ничья',
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
// (1.11.0) three more games: Conecta 4, Memorama (her faces), Secuencia neural
const W_MORE = {
  es: {
    c4: 'Conecta 4', c4How: 'Cuatro en línea antes que yo. Tú amarillo.',
    memo: 'Memorama', memoHow: 'Parejas de mis caras. Por turnos, yo también recuerdo.',
    simon: 'Secuencia neural', simonHow: 'Repite la secuencia. Supera mi marca.',
    memoMine: ['Pareja. Mía.', 'Te dije que me acuerdo de todo.', 'Fácil.'],
    memoYours: ['Ok, buena memoria.', 'Hmph. Esa me la sabía.', 'Bien visto.'],
    memoMiss: ['Uy, no.', 'Juraría que estaba ahí…', 'Ups.'],
    simonWatch: ['Mira bien.', 'Atención…', 'Sigue el patrón.'],
    simonGo: ['Tu turno.', 'Ahora tú.', 'Repítela.'],
    simonGood: ['Bien. Otra más larga.', 'Nada mal.', 'Sigue así.'],
    simonFail: ['Te perdiste en la ronda {n}.', 'Error en la {n}. Mi chip no se equivoca.'],
    simonGoal: 'Mi marca: {n}', round: 'Ronda',
  },
  en: {
    c4: 'Connect 4', c4How: 'Four in a row before me. You’re yellow.',
    memo: 'Memory', memoHow: 'Pairs of my faces. Taking turns, I remember too.',
    simon: 'Neural sequence', simonHow: 'Repeat the sequence. Beat my record.',
    memoMine: ['Pair. Mine.', 'Told you I remember everything.', 'Easy.'],
    memoYours: ['Ok, good memory.', 'Hmph. I knew that one.', 'Nice catch.'],
    memoMiss: ['Nope.', 'I swear it was there…', 'Oops.'],
    simonWatch: ['Watch closely.', 'Eyes on me…', 'Follow the pattern.'],
    simonGo: ['Your turn.', 'Now you.', 'Repeat it.'],
    simonGood: ['Good. A longer one.', 'Not bad.', 'Keep going.'],
    simonFail: ['You lost it in round {n}.', 'Wrong in round {n}. My chip doesn’t miss.'],
    simonGoal: 'My record: {n}', round: 'Round',
  },
  fr: {
    c4: 'Puissance 4', c4How: 'Quatre alignés avant moi. Tu es jaune.',
    memo: 'Memory', memoHow: 'Des paires de mes visages. Chacun son tour, je me souviens aussi.',
    simon: 'Séquence neurale', simonHow: 'Répète la séquence. Bats mon record.',
    memoMine: ['Paire. À moi.', 'Je t’avais dit que je me souviens de tout.', 'Facile.'],
    memoYours: ['Ok, bonne mémoire.', 'Hmph. Je la connaissais.', 'Bien vu.'],
    memoMiss: ['Raté.', 'J’aurais juré qu’elle était là…', 'Oups.'],
    simonWatch: ['Regarde bien.', 'Attention…', 'Suis le motif.'],
    simonGo: ['À toi.', 'Maintenant toi.', 'Répète.'],
    simonGood: ['Bien. Une plus longue.', 'Pas mal.', 'Continue.'],
    simonFail: ['Perdu à la manche {n}.', 'Erreur à la manche {n}. Ma puce ne rate jamais.'],
    simonGoal: 'Mon record : {n}', round: 'Manche',
  },
  pt: {
    c4: 'Lig 4', c4How: 'Quatro em linha antes de mim. Você é amarelo.',
    memo: 'Jogo da memória', memoHow: 'Pares das minhas caras. Por turnos, eu também lembro.',
    simon: 'Sequência neural', simonHow: 'Repita a sequência. Supere meu recorde.',
    memoMine: ['Par. Meu.', 'Eu disse que lembro de tudo.', 'Fácil.'],
    memoYours: ['Ok, boa memória.', 'Hmph. Essa eu sabia.', 'Boa.'],
    memoMiss: ['Ih, não.', 'Juro que estava ali…', 'Ops.'],
    simonWatch: ['Olha bem.', 'Atenção…', 'Siga o padrão.'],
    simonGo: ['Sua vez.', 'Agora você.', 'Repete.'],
    simonGood: ['Boa. Uma mais longa.', 'Nada mal.', 'Continua.'],
    simonFail: ['Você se perdeu na rodada {n}.', 'Erro na rodada {n}. Meu chip não erra.'],
    simonGoal: 'Meu recorde: {n}', round: 'Rodada',
  },
  ru: {
    c4: 'Четыре в ряд', c4How: 'Собери четыре в ряд раньше меня. Ты жёлтые.',
    memo: 'Мемори', memoHow: 'Пары моих лиц. По очереди, я тоже запоминаю.',
    simon: 'Нейропоследовательность', simonHow: 'Повтори последовательность. Побей мой рекорд.',
    memoMine: ['Пара. Моя.', 'Я же говорила, что всё помню.', 'Легко.'],
    memoYours: ['Ок, хорошая память.', 'Хмф. Я её знала.', 'Неплохо.'],
    memoMiss: ['Не-а.', 'Клянусь, она была тут…', 'Упс.'],
    simonWatch: ['Смотри внимательно.', 'Внимание…', 'Следи за узором.'],
    simonGo: ['Твой ход.', 'Теперь ты.', 'Повтори.'],
    simonGood: ['Хорошо. Длиннее.', 'Неплохо.', 'Продолжай.'],
    simonFail: ['Ты сбился в раунде {n}.', 'Ошибка в раунде {n}. Мой чип не ошибается.'],
    simonGoal: 'Мой рекорд: {n}', round: 'Раунд',
  },
};
Object.assign(W, W_MORE[lang] || W_MORE.es);
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
// (1.10.0) her, from the corner, alive: an animation for what she's doing or feeling. The games'
// own ones (anim-game-*.png, made from tools/prompts-juegos) come first when they're there; until
// then, her everyday animations that fit; and if none loads, a still pose.
const body = $('#body');
const ANIMS = `${FACES}anims/`;
const ACTS = {
  menu: ['game-hello', 'wave', 'wave'],
  watch: ['game-watch', 'gamer-watch', 'prone-watch'],
  pong: ['game-paddle', 'gamer-play', 'prone-watch'],
  think: ['game-think', 'think', 'lean'],
  hesitate: ['game-hesitate', 'think', 'lean'],
  her: ['game-cheer', 'party', 'prone-kick'],
  you: ['game-pout', 'cry', 'hug-knees'],
  draw: ['game-shrug', 'bored-tap', 'bored-back'],
  shock: ['game-shock', 'surprised', 'hug-knees'],
  tilt: ['game-tilt', 'angry', 'lean'],
  zone: ['game-zone', 'hack', 'lean'],
  smug: ['game-smug', 'lean-nails', 'lean'],
  bluff: ['game-bluff', 'shy', 'wave'],
  laugh: ['game-laugh', 'laugh', 'prone-kick'],
  regret: ['game-facepalm', 'dizzy', 'hug-knees'],
  idle: ['game-idle', 'yawn', 'bored-back'],
};
// what she says → what she does (brain.js line keys)
const LINE_ACTS = {
  youGood: 'shock', behind: 'shock', tilt: 'tilt', serious: 'tilt', comeback: 'tilt', regret: 'regret',
  confident: 'smug', material: 'smug', obvious: 'smug', pattern: 'smug', openRepeat: 'smug', counted: 'smug', stand15: 'smug',
  focus: 'zone', rally: 'zone', riskHit: 'zone', announce: 'bluff', hint: 'bluff', tease21: 'bluff',
  lied: 'laugh', youBlunder: 'laugh', idle: 'idle', hesitate: 'hesitate', hard: 'think',
};
let animMeta = null;
let baseAct = 'menu';
let nowAct = '';
let actBack = 0;
fetch(`${ANIMS}anims.json`).then((r) => r.json()).then((j) => { animMeta = j; const a = nowAct; nowAct = ''; act(a || baseAct); }).catch(() => {});
/** She does `id` (ACTS); with `ms`, then back to what she was doing (baseAct). */
function act(id, ms = 0) {
  clearTimeout(actBack);
  if (ms) actBack = setTimeout(() => act(baseAct), ms);
  if (id === nowAct) return;
  nowAct = id;
  const [own, everyday, still] = ACTS[id] || ACTS.watch;
  const name = animMeta?.[own] ? own : animMeta?.[everyday] ? everyday : null;
  body.getAnimations?.().forEach((a) => a.cancel());
  if (name) {
    const m = animMeta[name];
    const tall = m.kind === 'lie' ? 96 : 150;
    const k = tall / m.h;
    const w = Math.round(m.w * k);
    body.classList.toggle('is-wide', m.kind === 'lie');
    Object.assign(body.style, { width: `${w}px`, height: `${tall}px`, backgroundImage: `url(${ANIMS}${name}.png)`, backgroundSize: `${w * m.n}px ${tall}px`, backgroundPosition: '0 0' });
    body.animate?.([{ backgroundPosition: '0 0' }, { backgroundPosition: `-${w * m.n}px 0` }], { duration: (m.n / (m.fps || 6)) * 1000, iterations: Infinity, easing: `steps(${m.n})` });
  } else {
    const wide = ['prone-watch', 'prone-kick', 'bored-back'].includes(still);
    body.classList.toggle('is-wide', wide);
    Object.assign(body.style, { width: wide ? '170px' : '90px', height: wide ? '96px' : '150px', backgroundImage: `url(${FACES}poses/pose-${still}.png)`, backgroundSize: 'contain', backgroundPosition: 'right bottom' });
  }
  body.classList.remove('is-new'); void body.offsetWidth; body.classList.add('is-new');
}
const pose = (id) => {
  if (id === 'menu' || id === 'play' || id === 'pong') { baseAct = id === 'play' ? 'watch' : id; act(baseAct); } else act(id);
};
// the end of a game: a stamp across the screen
function stamp(result) {
  document.querySelector('.stamp')?.remove();
  const s = el('div', `stamp is-${result}`, W[result === 'you' ? 'stampYou' : result === 'her' ? 'stampHer' : 'stampDraw']);
  document.body.append(s);
  setTimeout(() => s.remove(), 2600);
}
const played = (game, result) => {
  brain?.end(result);
  pose(result);
  stamp(result);
  try { bridge?.played?.(JSON.stringify({ game, result, at: Date.now() })); } catch {}
};

// ---------- the menu ----------
const stage = $('#stage');
const GAMES = [
  { id: 'chess', icon: '♞', how: 'chessHow', run: chess },
  { id: 'mines', icon: '✹', how: 'minesHow', run: mines },
  { id: 'cards', icon: '♠', how: 'cardsHow', run: cards },
  { id: 'ttt', icon: '✕', how: 'tttHow', run: ttt },
  { id: 'pong', icon: '◐', how: 'pongHow', run: pong },
  { id: 'rps', icon: '✊', how: 'rpsHow', run: rps },
  { id: 'c4', icon: '◉', how: 'c4How', run: connect4 },
  { id: 'memo', icon: '▣', how: 'memoHow', run: memory },
  { id: 'simon', icon: '◈', how: 'simonHow', run: sequence },
];
let level = (() => { try { return localStorage.getItem('capri.games.level') || 'normal'; } catch { return 'normal'; } })();
let current = null;
let stopGame = null; // a game with its own loop leaves this to stop it
let brain = null; // her, as your rival in the game that's open (brain.js)
const halt = () => { try { stopGame?.(); } catch {} stopGame = null; brain?.stop(); brain = null; };

function menu() {
  halt();
  current = null;
  stage.innerHTML = '';
  $('#title').textContent = W.title;
  const grid = el('div', 'menu');
  for (const g of GAMES) {
    const b = el('button', 'game');
    b.append(el('span', 'game__icon', g.icon), el('span', 'game__name', W[g.id]), el('span', 'game__how', W[g.how]));
    // your record against her
    const r = record(g.id);
    if (r && (r.you || r.her || r.draw)) b.append(el('span', 'game__rec', `${W.you} ${r.you} · ${W.her} ${r.her}`));
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
  pose('menu');
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
  brain = rival({ game: g.id, level, lang, talk: (text, mood, key) => { say(text, mood); if (LINE_ACTS[key]) act(LINE_ACTS[key], 2600); } });
  brain.start();
  pose(g.id === 'pong' ? 'pong' : 'play');
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
  let eye = -1; // a cell she's eyeing before she decides
  const draw = () => cells.forEach((v, i) => { btns[i].textContent = v || ''; btns[i].className = `ttt__cell${v ? ` is-${v.toLowerCase()}` : ''}${!v && i === eye ? ' is-eye' : ''}`; });
  // minimax, the whole tree (it's small): how it ends for her (O) with best play from here
  const value = (bb, turn, depth) => {
    const w = winner(bb);
    if (w === 'O') return 10 - depth;
    if (w === 'X') return depth - 10;
    if (w === 'draw') return 0;
    let top = turn === 'O' ? -99 : 99;
    for (let i = 0; i < 9; i++) {
      if (bb[i]) continue;
      bb[i] = turn;
      const v = value(bb, turn === 'O' ? 'X' : 'O', depth + 1);
      bb[i] = null;
      top = turn === 'O' ? Math.max(top, v) : Math.min(top, v);
    }
    return top;
  };
  // every free cell and what it's worth to `who` (higher = better for them)
  const options = (who) => {
    const out = [];
    for (let i = 0; i < 9; i++) {
      if (cells[i]) continue;
      cells[i] = who;
      const v = value(cells, who === 'O' ? 'X' : 'O', 1);
      cells[i] = null;
      out.push({ move: i, score: who === 'O' ? v : -v });
    }
    return out;
  };
  function end(w) {
    over = true;
    if (w === 'X') { say('youWin', 'surprised'); played('ttt', 'you'); }
    else if (w === 'O') { say('herWin', 'smug'); played('ttt', 'her'); }
    else { say('draw', 'calm'); played('ttt', 'draw'); }
  }
  let busy = false;
  function play(i) {
    if (over || busy || cells[i]) return;
    buzz();
    // how good was that? (only judged when it mattered: some cells were better or worse than others)
    const yours = options('X');
    const top = Math.max(...yours.map((o) => o.score));
    const low = Math.min(...yours.map((o) => o.score));
    const mine = yours.find((o) => o.move === i).score;
    cells[i] = 'X';
    draw();
    let w = winner(cells);
    if (w) return end(w);
    if (top !== low) brain.yourMove(mine === top ? (top - low >= 10 ? 1 : 0.8) : (mine - low) / (top - low) * 0.6, { missed: mine < top });
    busy = true;
    const choice = brain.choose(options('O'), { scale: 12 });
    face.src = `${FACES}face-thinking.png`;
    act('think', 1500);
    const wait = brain.hesitation(choice, (k) => { eye = k ?? -1; draw(); });
    setTimeout(() => {
      eye = -1;
      cells[choice.move] = 'O';
      busy = false;
      draw();
      w = winner(cells);
      if (w) return end(w);
      brain.afterHer(choice);
      brain.standing(Math.max(...options('O').map((o) => o.score)) / 10);
      face.src = `${FACES}face-${brain.face()}.png`;
      brain.yourTurn();
    }, wait + brain.thinkTime({ choice, many: 9 - cells.filter(Boolean).length }));
  }
  draw();
  brain.yourTurn();
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
  // she watches: fast taps, and when you're stuck (not on Difícil) a whispered safe cell
  const taps = [];
  let stuck = 0;
  const watch = () => {
    clearTimeout(stuck);
    if (level === 'hard') return;
    stuck = setTimeout(() => {
      if (over || !placed || current?.id !== 'mines') return;
      const near = cells.map((c, i) => i).filter((i) => !cells[i].open && !cells[i].flag && !cells[i].mine && around(i).some((k) => cells[k].open));
      if (!near.length) return;
      const at = pick(near);
      btns[at].classList.add('is-hint');
      setTimeout(() => btns[at].classList.remove('is-hint'), 2500);
      brain.line('hint', 'wink', { r: Math.floor(at / W2) + 1, c: (at % W2) + 1 }, 2);
    }, 22000);
  };
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
      clearTimeout(stuck);
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
      clearTimeout(stuck);
      say('cleared', 'love');
      played('mines', 'you');
      return;
    }
    const now = Date.now();
    taps.push(now);
    while (taps.length && now - taps[0] > 3500) taps.shift();
    if (taps.length >= 5) { taps.length = 0; brain.line('fast', 'surprised', {}, 1); }
    brain.standing(-cells.filter((k) => k.open).length / cells.filter((k) => !k.mine).length);
    watch();
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
  // what would bust her now, from the cards she hasn't seen yet (she counts them)
  const bustChance = () => {
    const unseen = deck.length;
    if (!unseen) return 1;
    return deck.filter((c) => value([...hers, c]) > 21).length / unseen;
  };
  // her call, like a player: she sees your cards, so with her head on she hits while she's behind
  // and weighs a tie; distracted or annoyed she plays the house's old rule (17), with nerves
  const wantsCard = () => {
    const h = value(hers);
    const y = value(mine);
    const risk = bustChance();
    if (Math.random() < brain.skill()) {
      if (h < y) return true;
      if (h > y) return false;
      return risk < 0.25 + brain.mood.confidence * 0.2; // a tie: only if it's fairly safe
    }
    if (brain.mood.tilt > 0.4 && h >= 15 && h < 17) return Math.random() < 0.5;
    return h < 17;
  };
  const herTurn = () => {
    hidden = false;
    draw();
    brain.stop();
    say('think', 'thinking');
    let first = true;
    const step = () => {
      const h = value(hers);
      if (h <= 21 && wantsCard()) {
        const risk = bustChance();
        if (risk > 0.45) brain.line('riskHit', 'focus', {}, first ? 1 : 0);
        else if (level === 'hard' && first && risk < 0.2) brain.line('counted', 'smug', {}, 0);
        first = false;
        hers.push(deck.pop());
        draw();
        setTimeout(step, 600 + Math.round(risk * 900) + Math.random() * 300); // a risky card: she takes her time
        return;
      }
      const y = value(mine);
      if (h === y && h <= 21) brain.line('safeStand', 'calm', {}, 0);
      if (h > 21) { say('herBust', 'cringe'); finish('you'); } else if (h > y) { say('herWin', 'smug'); finish('her'); } else if (h < y) { say('youWin', 'surprised'); finish('you'); } else { say('draw', 'calm'); finish('draw'); }
    };
    setTimeout(step, 700);
  };
  // reading you: a nudge when you're on the fence, and how you usually stand (she remembers)
  const yourCall = () => {
    const y = value(mine);
    if (y >= 12 && y <= 16) brain.line('tease21', 'wink', {}, 0);
    brain.yourTurn();
  };
  hit.addEventListener('click', () => {
    if (over) return;
    buzz();
    mine.push(deck.pop());
    draw();
    if (value(mine) > 21) { brain.stop(); say('bust', 'laugh'); finish('her'); } else if (value(mine) === 21) herTurn();
    else if (Math.random() < 0.3) say('hitLine', 'aside');
    else yourCall();
  });
  stand.addEventListener('click', () => {
    if (over) return;
    buzz();
    const y = value(mine);
    const usual = brain.habitOf('stand');
    brain.habit('stand', String(y));
    if (usual && usual.total >= 4 && usual.share >= 0.4 && Number(usual.value) === y && y <= 16) brain.line('stand15', 'smug', { x: y }, 2);
    over = true;
    hit.disabled = stand.disabled = true;
    herTurn();
  });
  draw();
  if (value(mine) === 21) { say('blackjack', 'wide'); setTimeout(herTurn, 600); }
  else setTimeout(yourCall, 2500);
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
  // every move she has and what it's worth to her (deeper the harder the level, and when she's focused)
  function think(st) {
    const depth = { easy: 1, normal: 2, hard: 3 }[level] + (level === 'normal' && brain.mood.focus > 0.7 ? 1 : 0);
    return legal(st).map((m) => ({ move: m, score: Math.max(-3000, Math.min(3000, search(apply(st, m), depth - 1, -Infinity, Infinity))) }));
  }
  // your moves, each with what it leaves you (her best answer, one move deep): to judge yours
  function yourOptions(st) {
    return legal(st).map((m) => ({ move: m, score: Math.max(-3000, Math.min(3000, search(apply(st, m), 1, -Infinity, Infinity))) }));
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
  let eye = null; // a move she's eyeing before she decides
  let mine = 0; // your moves so far
  const taken = { w: [], b: [] };
  function draw() {
    const check = inCheck(s) ? kingAt(s, s.turn) : -1;
    for (let i = 0; i < 64; i++) {
      const p = s.b[i];
      const d = squares[i];
      d.textContent = p ? GLYPH[p[1]] : '';
      d.className = `chess__sq${(Math.floor(i / 8) + (i % 8)) % 2 ? ' is-dark' : ''}${p ? ` is-${p[0]}` : ''}${i === chosen ? ' is-chosen' : ''}${targets.some((m) => m.to === i) ? ' is-target' : ''}${lastMove && (lastMove.from === i || lastMove.to === i) ? ' is-last' : ''}${eye && (eye.from === i || eye.to === i) ? ' is-eye' : ''}${i === check ? ' is-check' : ''}`;
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
      // how good was it: what it leaves you next to the best you had (lower = better for you)
      const opts = yourOptions(s);
      const best = Math.min(...opts.map((o) => o.score));
      const yours = opts.find((o) => o.move.from === t.from && o.move.to === t.to)?.score ?? best;
      const loss = yours - best;
      const second = opts.map((o) => o.score).sort((a, b) => a - b)[1] ?? best;
      // your opening: she remembers it
      if (mine === 0) {
        const key = `${t.from}-${t.to}`;
        const usual = brain.habitOf('open');
        if (usual && usual.total >= 3 && usual.share >= 0.6 && usual.value === key) brain.line('openRepeat', 'smug', {}, 2);
        brain.habit('open', key);
      }
      mine += 1;
      move(t);
      chosen = null;
      targets = [];
      draw();
      if (done()) return;
      if (inCheck(s)) say('inCheck', 'surprised');
      else if (took && Math.random() < 0.5) say('lost', 'angry');
      else if (mine > 1) brain.yourMove(loss <= 0 ? (second - best > 150 ? 1 : 0.8) : Math.max(0, 1 - loss / 300), { missed: loss > 60 });
      face.src = `${FACES}face-thinking.png`;
      act('think', 2400);
      setTimeout(herMove, 120);
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
    const choice = brain.choose(think(s), { scale: 300 });
    if (!choice) return done();
    const m = choice.move;
    const took = s.b[m.to] || m.enPassant;
    const wait = brain.hesitation(choice, (k) => { eye = k; draw(); });
    setTimeout(() => {
      eye = null;
      move(m);
      draw();
      if (done()) return;
      if (inCheck(s)) say('check', 'smug');
      else if (took && Math.random() < 0.5) say('took', 'grin');
      else brain.afterHer(choice);
      // how it's going: the pieces on the board, her side positive
      const material = evaluate(s) / 900;
      const was = brain.mood.confidence;
      brain.standing(Math.max(-1, Math.min(1, material)));
      if (material > 0.35 && was < 0.6 && Math.random() < 0.4) brain.line('material', 'smug', {}, 0);
      if (material < -0.35 && was > 0.4 && Math.random() < 0.4) brain.line('behind', 'cringe', {}, 0);
      face.src = `${FACES}face-${brain.face()}.png`;
      brain.yourTurn();
    }, wait + brain.thinkTime({ choice, many: choice.sorted.length }));
  }
  draw();
  brain.yourTurn();
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
  const h = Math.round(Math.min(w * 1.35, Math.max(320, window.innerHeight - cv.getBoundingClientRect().top - 110)));
  cv.width = w * dpr; cv.height = h * dpr;
  cv.style.width = `${w}px`; cv.style.height = `${h}px`;
  const g = cv.getContext('2d');
  g.scale(dpr, dpr);
  const pw = w * 0.24, ph = 10, r = 7;
  const mask = new Image();
  mask.src = `${FACES}capri-face.png`;
  const trail = [];
  // her, like a player (brain.js): she reads the ball only after a reaction time, guesses where it
  // will land (wall bounces included) roughly at first and better as it comes, gets tired on long
  // rallies unless she's in the zone, overreaches when annoyed, and smashes when she's confident
  const st = { me: w / 2, cap: w / 2, bx: w / 2, by: h / 2, vx: 0, vy: 0, mine: 0, hers: 0, wait: 0, over: false, react: 0, guess: 0, rally: 0, coming: false };
  const serve = (toward) => {
    const speed = h * { easy: 0.55, normal: 0.7, hard: 0.85 }[level];
    const a = (Math.random() - 0.5) * 0.9;
    st.bx = w / 2; st.by = h / 2;
    st.vx = Math.sin(a) * speed; st.vy = Math.cos(a) * speed * toward;
    st.wait = 0.8;
    st.rally = 0;
    st.coming = false;
  };
  const herY = 18 + ph;
  // where the ball will cross her line, folding the wall bounces
  const landing = () => {
    if (st.vy >= 0) return w / 2;
    const t = (st.by - herY) / -st.vy;
    let x = st.bx + st.vx * t;
    const lo = r, hi = w - r, span = hi - lo;
    x = ((x - lo) % (2 * span) + 2 * span) % (2 * span);
    return lo + (x > span ? 2 * span - x : x);
  };
  const point = (mine) => {
    buzz();
    if (mine) { st.mine += 1; you.textContent = `${W.you} ${st.mine}`; brain.event('yourPoint'); }
    else { st.hers += 1; her.textContent = `${W.her} ${st.hers}`; brain.event('herPoint'); }
    brain.event('calm');
    brain.standing((st.hers - st.mine) / 4);
    if (st.mine >= 5 || st.hers >= 5) {
      st.over = true;
      if (st.mine >= 5) { say('youWin', 'surprised'); played('pong', 'you'); }
      else { say('herWin', 'smug'); played('pong', 'her'); }
      return;
    }
    if (!brain.line(st.mine >= 4 && st.hers < 4 ? 'comeback' : st.hers >= 4 && st.mine < 4 ? 'confident' : 'none', brain.face(), {}, 1)) say(mine ? 'yourPoint' : 'herPoint', mine ? brain.face() : 'grin');
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
    // her smash: confident, she hits it harder now and then
    const smash = dir > 0 && brain.mood.confidence > 0.68 && Math.random() < 0.3 ? 1.18 : 1;
    const speed = Math.min(Math.hypot(st.vx, st.vy) * 1.06 * smash, h * 1.9);
    const a = off * 1.05;
    st.vx = Math.sin(a) * speed; st.vy = Math.cos(a) * speed * dir;
    st.by = y;
    st.rally += 1;
    brain.event('rally', st.rally);
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
        const myY = h - 18 - ph;
        if (st.vy > 0 && st.by + r >= myY && st.by < myY + ph) { if (hitPaddle(st.me, myY - r, -1)) buzz(); }
        if (st.vy < 0 && st.by - r <= herY && st.by > herY - ph) hitPaddle(st.cap, herY + r, 1);
        if (st.by > h + r) point(false);
        else if (st.by < -r) point(true);
      }
      // her paddle
      const sk = brain.skill();
      const mood = brain.mood;
      if (st.vy < 0 && !st.coming) {
        // it's coming her way: she notices after a moment, with a first rough guess
        st.coming = true;
        st.react = 0.32 - sk * 0.24 + Math.random() * 0.08;
        st.guess = (Math.random() - 0.5) * 2 * (1 - sk) * w * 0.45;
      }
      if (st.vy > 0) st.coming = false;
      let goal = w / 2;
      if (st.coming) {
        if (st.react > 0) { st.react -= dt; goal = st.cap; }
        else {
          const near = Math.max(0, Math.min(1, (st.by - herY) / (h * 0.85))); // 1 far … 0 at her line
          goal = landing() + st.guess * (0.25 + near * 0.75);
          if (mood.tilt > 0.4) goal += Math.sign(goal - st.cap) * mood.tilt * 18; // overreaching
        }
      }
      const tired = Math.min(0.3, st.rally * 0.014 * (1 - mood.focus));
      const max = h * (0.45 + sk * 0.75) * (1 - tired) * dt;
      st.cap += Math.max(-max, Math.min(max, goal - st.cap));
      st.cap = Math.max(pw / 2, Math.min(w - pw / 2, st.cap));
    }
    // a neon court: grid, her half in red, yours in cyan, her mask big and faint in the middle
    g.fillStyle = '#0b0a10';
    g.fillRect(0, 0, w, h);
    g.lineWidth = 1;
    for (let x = 0; x <= w; x += 28) { g.strokeStyle = 'rgba(0, 240, 255, 0.08)'; g.beginPath(); g.moveTo(x, 0); g.lineTo(x, h); g.stroke(); }
    for (let y = 0; y <= h; y += 28) { g.strokeStyle = y < h / 2 ? 'rgba(255, 32, 82, 0.1)' : 'rgba(0, 240, 255, 0.08)'; g.beginPath(); g.moveTo(0, y); g.lineTo(w, y); g.stroke(); }
    if (mask.complete && mask.naturalWidth) { g.globalAlpha = 0.1; g.drawImage(mask, w / 2 - w * 0.22, h / 2 - w * 0.22, w * 0.44, w * 0.44); g.globalAlpha = 1; }
    g.strokeStyle = 'rgba(255, 32, 82, 0.6)';
    g.setLineDash([10, 8]);
    g.beginPath(); g.moveTo(0, h / 2); g.lineTo(w, h / 2); g.stroke();
    g.setLineDash([]);
    g.shadowBlur = 16;
    // her paddle, with her mask on it
    g.fillStyle = GREEN; g.shadowColor = GREEN;
    g.fillRect(st.cap - pw / 2, 18, pw, ph);
    g.shadowBlur = 0;
    if (mask.complete && mask.naturalWidth) g.drawImage(mask, st.cap - 12, 18 + ph / 2 - 12, 24, 24);
    g.shadowBlur = 16;
    g.fillStyle = YELLOW; g.shadowColor = YELLOW;
    g.fillRect(st.me - pw / 2, h - 18 - ph, pw, ph);
    if (!st.over) {
      trail.push([st.bx, st.by]);
      if (trail.length > 8) trail.shift();
      trail.forEach(([x, y], i) => { g.globalAlpha = (i + 1) / trail.length * 0.35; g.fillStyle = st.vy < 0 ? GREEN : '#00f0ff'; g.beginPath(); g.arc(x, y, r * (0.5 + i / trail.length / 2), 0, Math.PI * 2); g.fill(); });
      g.globalAlpha = 1;
      g.fillStyle = YELLOW; g.shadowColor = YELLOW;
      g.beginPath(); g.arc(st.bx, st.by, r, 0, Math.PI * 2); g.fill();
    }
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
  const st = { mine: 0, hers: 0, seen: [], busy: false, said: null, lie: false };
  const beat = (x) => (x + 1) % 3; // what beats x
  // she guesses your next hand (no peeking): what you did after your last hand this game, and your
  // favourite hands from all your games against her
  const guess = () => {
    const h = st.seen;
    const smart = Math.max(0, Math.min(0.9, (brain.skill() - 0.2) * 1.15));
    if (Math.random() >= smart) return Math.floor(Math.random() * 3);
    const counts = [0, 0, 0];
    const past = brain.habitOf('hand')?.counts || {};
    for (let k = 0; k < 3; k++) counts[k] += (past[k] || 0) * 0.15;
    for (let i = 1; i < h.length; i++) if (h[i - 1] === h[h.length - 1]) counts[h[i]] += 1;
    h.forEach((x) => { counts[x] += 0.3; });
    if (!counts.some(Boolean)) return Math.floor(Math.random() * 3);
    return beat(counts.indexOf(Math.max(...counts)));
  };
  // her bluff: now and then she tells you what she'll throw. Whether it's true depends on you:
  // if you usually believe her (you play what beats it), she lies
  const announce = () => {
    st.said = null;
    if (st.over || Math.random() > 0.28) return;
    const trusting = brain.habitOf('believe');
    st.lie = trusting && trusting.total >= 2 ? trusting.counts.yes / trusting.total > 0.5 : Math.random() < 0.5;
    st.said = Math.floor(Math.random() * 3);
    brain.line('announce', 'wink', { x: W[HANDS[st.said][0]].toLowerCase() }, 2);
  };
  function throwHand(i) {
    if (st.busy || st.over) return;
    buzz();
    st.busy = true;
    let hers = guess();
    if (st.said != null) {
      // she said it: the truth, or the hand that beats the one you'd play if you believed her
      hers = st.lie ? beat(beat(st.said)) : st.said;
      brain.habit('believe', i === beat(st.said) ? 'yes' : 'no');
    }
    const said = st.said;
    st.said = null;
    st.seen.push(i);
    brain.habit('hand', i);
    // the same hand three times: she notices
    const n = st.seen.length;
    if (n >= 3 && st.seen[n - 1] === st.seen[n - 2] && st.seen[n - 2] === st.seen[n - 3]) brain.line('pattern', 'smug', { x: W[HANDS[i][0]].toLowerCase() }, 2);
    mineHand.textContent = HANDS[i][1];
    herHand.textContent = '…';
    btns.forEach((b, k) => b.classList.toggle('is-chosen', k === i));
    say('think', 'thinking');
    setTimeout(() => {
      herHand.textContent = HANDS[hers][1];
      herHand.classList.remove('is-pop'); void herHand.offsetWidth; herHand.classList.add('is-pop');
      const spoke = said != null && brain.line(st.lie ? 'lied' : 'honest', st.lie ? 'laugh' : 'calm', {}, 2);
      if (i === hers) { if (!spoke) say('rpsTie', 'calm'); }
      else if (beats(i, hers)) { st.mine += 1; you.textContent = `${W.you} ${st.mine}`; brain.event('yourPoint'); if (!spoke) say('rpsYours', brain.face()); }
      else { st.hers += 1; her.textContent = `${W.her} ${st.hers}`; brain.event('herPoint'); if (!spoke) say('rpsMine', 'grin'); }
      brain.standing((st.hers - st.mine) / 2);
      st.busy = false;
      if (st.mine < 3 && st.hers < 3) setTimeout(announce, 1400);
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


// ---------- Conecta 4 (1.11.0) ----------
// 7 columns, 6 rows; you drop yellow, she drops her mask. Her eye: minimax with a cut-off, deeper the
// harder the level, scored by the lines of four still open for each side.
function connect4(box) {
  const C = 7, R = 6;
  const b = Array(C * R).fill(0); // 1 you, 2 her; row 0 at the top
  const at = (c, r) => b[r * C + c];
  const grid = el('div', 'c4');
  const cells = [];
  for (let r = 0; r < R; r++) for (let c = 0; c < C; c++) {
    const d = el('button', 'c4__cell');
    d.addEventListener('click', () => mine(c));
    grid.append(d);
    cells.push(d);
  }
  box.append(grid);
  const depth = { easy: 2, normal: 4, hard: 6 }[level] || 4;
  let over = false, busy = false, eye = -1, last = -1;
  const drawB = () => b.forEach((v, i) => {
    cells[i].className = `c4__cell${v === 1 ? ' is-you' : v === 2 ? ' is-her' : ''}${i === last ? ' is-last' : ''}${!v && i % C === eye ? ' is-eye' : ''}`;
  });
  const free = (c) => { for (let r = R - 1; r >= 0; r--) if (!at(c, r)) return r; return -1; };
  const WIN4 = [];
  for (let r = 0; r < R; r++) for (let c = 0; c < C; c++) for (const [dc, dr] of [[1, 0], [0, 1], [1, 1], [1, -1]]) {
    const ce = c + dc * 3, re = r + dr * 3;
    if (ce < 0 || ce >= C || re < 0 || re >= R) continue;
    WIN4.push([0, 1, 2, 3].map((k) => (r + dr * k) * C + c + dc * k));
  }
  const winner = () => {
    for (const w of WIN4) { const v = b[w[0]]; if (v && w.every((i) => b[i] === v)) return v; }
    return b.every(Boolean) ? 3 : 0;
  };
  // how good the board is for her: open lines, weighted by how full they are
  const heur = () => {
    let s = 0;
    for (const w of WIN4) {
      let h = 0, y = 0;
      for (const i of w) { if (b[i] === 2) h++; else if (b[i] === 1) y++; }
      if (h && !y) s += h === 3 ? 12 : h === 2 ? 3 : 1;
      else if (y && !h) s -= y === 3 ? 14 : y === 2 ? 3 : 1;
    }
    return s;
  };
  const ORDER = [3, 2, 4, 1, 5, 0, 6];
  const search = (who, d, alpha, beta) => {
    const w = winner();
    if (w === 2) return 1000 + d;
    if (w === 1) return -1000 - d;
    if (w === 3) return 0;
    if (!d) return heur();
    let best = who === 2 ? -1e9 : 1e9;
    for (const c of ORDER) {
      const r = free(c);
      if (r < 0) continue;
      b[r * C + c] = who;
      const v = search(who === 2 ? 1 : 2, d - 1, alpha, beta);
      b[r * C + c] = 0;
      if (who === 2) { best = Math.max(best, v); alpha = Math.max(alpha, v); } else { best = Math.min(best, v); beta = Math.min(beta, v); }
      if (beta <= alpha) break;
    }
    return best;
  };
  const options = (who) => {
    const out = [];
    for (const c of ORDER) {
      const r = free(c);
      if (r < 0) continue;
      b[r * C + c] = who;
      const v = search(who === 2 ? 1 : 2, depth - 1, -1e9, 1e9);
      b[r * C + c] = 0;
      out.push({ move: c, score: who === 2 ? v : -v });
    }
    return out;
  };
  function end(w) {
    over = true;
    if (w === 1) { say('youWin', 'surprised'); played('c4', 'you'); }
    else if (w === 2) { say('herWin', 'smug'); played('c4', 'her'); }
    else { say('draw', 'calm'); played('c4', 'draw'); }
  }
  function drop(c, who) {
    const r = free(c);
    b[r * C + c] = who;
    last = r * C + c;
    drawB();
    return winner();
  }
  function mine(c) {
    if (over || busy || free(c) < 0) return;
    buzz();
    const yours = options(1);
    const top = Math.max(...yours.map((o) => o.score)), low = Math.min(...yours.map((o) => o.score));
    const sc = yours.find((o) => o.move === c).score;
    const w = drop(c, 1);
    if (w) return end(w);
    if (top !== low) brain.yourMove(sc === top ? 1 : Math.max(0, (sc - low) / (top - low)) * 0.7, { missed: sc < top });
    busy = true;
    const choice = brain.choose(options(2), { scale: 30 });
    face.src = `${FACES}face-thinking.png`;
    act('think', 1500);
    const wait = brain.hesitation(choice, (k) => { eye = k ?? -1; drawB(); });
    setTimeout(() => {
      eye = -1;
      busy = false;
      const w2 = drop(choice.move, 2);
      if (w2) return end(w2);
      brain.afterHer(choice);
      brain.standing(Math.max(-1, Math.min(1, heur() / 40)));
      face.src = `${FACES}face-${brain.face()}.png`;
      brain.yourTurn();
    }, wait + brain.thinkTime({ choice, many: 7 }));
  }
  drawB();
  brain.yourTurn();
}

// ---------- Memorama (1.11.0) ----------
// 16 cards, 8 pairs of her faces. You and her by turns: a pair = a point and you go again. She remembers
// the cards she has seen, not perfectly: the better she plays, the more of them she keeps.
function memory(box) {
  const KINDS = ['smug', 'laugh', 'love', 'wink', 'angry', 'surprised', 'sleepy', 'cringe', 'thinking', 'focus', 'bored', 'sad'];
  const kinds = [...KINDS].sort(() => Math.random() - 0.5).slice(0, 8);
  const deck = [...kinds, ...kinds].sort(() => Math.random() - 0.5);
  const state = deck.map(() => 'down'); // down | up | gone
  const seen = new Map(); // index -> kind she remembers
  const score = el('div', 'mines__info');
  const youS = el('span', '', `${W.you} 0`), herS = el('span', '', `${W.her} 0`);
  score.append(youS, herS);
  const grid = el('div', 'memo');
  const btns = deck.map((k, i) => {
    const d = el('button', 'memo__card');
    d.addEventListener('click', () => mine(i));
    grid.append(d);
    return d;
  });
  box.append(score, grid);
  let pts = { you: 0, her: 0 }, open = [], turn = 'you', busy = false, over = false;
  const drawM = () => deck.forEach((k, i) => {
    const st = state[i];
    btns[i].className = `memo__card is-${st}`;
    btns[i].style.backgroundImage = st === 'down' ? '' : `url(${FACES}face-${k}.png)`;
  });
  const keep = () => Math.random() < 0.35 + brain.skill() * 0.6;
  const remember = (i) => { if (keep()) seen.set(i, deck[i]); };
  const forgetGone = () => { for (const i of [...seen.keys()]) if (state[i] === 'gone') seen.delete(i); };
  function check() {
    const [a, c] = open;
    const pair = deck[a] === deck[c];
    setTimeout(() => {
      if (pair) {
        state[a] = state[c] = 'gone';
        pts[turn] += 1;
        youS.textContent = `${W.you} ${pts.you}`; herS.textContent = `${W.her} ${pts.her}`;
        brain.event(turn === 'her' ? 'herPoint' : 'yourPoint');
        say(turn === 'her' ? 'memoMine' : 'memoYours', turn === 'her' ? 'smug' : 'surprised');
      } else {
        state[a] = state[c] = 'down';
        if (turn === 'her') say('memoMiss', 'cringe');
        turn = turn === 'you' ? 'her' : 'you';
      }
      open = [];
      forgetGone();
      drawM();
      brain.standing((pts.her - pts.you) / 8);
      if (state.every((x) => x === 'gone')) {
        over = true;
        const r = pts.you > pts.her ? 'you' : pts.her > pts.you ? 'her' : 'draw';
        say(r === 'you' ? 'youWin' : r === 'her' ? 'herWin' : 'draw', r === 'you' ? 'surprised' : r === 'her' ? 'smug' : 'calm');
        return played('memo', r);
      }
      busy = false;
      if (turn === 'her') herTurn(); else brain.yourTurn();
    }, pair ? 600 : 1000);
  }
  function flip(i) {
    state[i] = 'up';
    open.push(i);
    remember(i);
    drawM();
  }
  function mine(i) {
    if (over || busy || turn !== 'you' || state[i] !== 'down') return;
    buzz();
    flip(i);
    if (open.length === 2) { busy = true; check(); }
  }
  function herTurn() {
    busy = true;
    face.src = `${FACES}face-thinking.png`;
    act('think', 1400);
    const down = state.map((s, i) => (s === 'down' ? i : -1)).filter((i) => i >= 0);
    // a pair she knows?
    const byKind = {};
    for (const [i, k] of seen) if (state[i] === 'down') (byKind[k] = byKind[k] || []).push(i);
    const known = Object.values(byKind).find((l) => l.length >= 2);
    let first = known ? known[0] : down.filter((i) => !seen.has(i))[0] ?? down[0];
    if (!known) { const unknown = down.filter((i) => !seen.has(i)); first = unknown.length ? unknown[Math.floor(Math.random() * unknown.length)] : down[Math.floor(Math.random() * down.length)]; }
    setTimeout(() => {
      flip(first);
      const k = deck[first];
      const match = [...seen].find(([i, kk]) => kk === k && i !== first && state[i] === 'down');
      const rest = state.map((s, i) => (s === 'down' ? i : -1)).filter((i) => i >= 0);
      const unknown = rest.filter((i) => !seen.has(i));
      const second = match ? match[0] : (unknown.length ? unknown : rest)[Math.floor(Math.random() * (unknown.length ? unknown : rest).length)];
      setTimeout(() => { flip(second); check(); }, 700 + Math.random() * 500);
    }, 700 + Math.random() * 600);
  }
  drawM();
  brain.yourTurn();
}

// ---------- Secuencia neural (1.11.0) ----------
// Four neon pads; she shows a sequence one longer each round, you repeat it. Her record to beat depends
// on the level; your best stays on this phone.
function sequence(box) {
  const goal = { easy: 6, normal: 9, hard: 13 }[level] || 9;
  const info = el('div', 'mines__info');
  const roundS = el('span', '', `${W.round} 0`);
  info.append(roundS, el('span', '', fillN(W.simonGoal, goal)));
  const pads = el('div', 'simon');
  const COLORS = ['green', 'red', 'cyan', 'yellow'];
  const btns = COLORS.map((c, i) => {
    const d = el('button', `simon__pad is-${c}`);
    d.addEventListener('click', () => press(i));
    pads.append(d);
    return d;
  });
  box.append(info, pads);
  const seq = [];
  let pos = 0, listening = false, over = false;
  const timers = [];
  stopGame = () => timers.forEach(clearTimeout);
  const later = (fn, ms) => timers.push(setTimeout(fn, ms));
  const flash = (i, ms = 360) => { btns[i].classList.add('is-lit'); later(() => btns[i].classList.remove('is-lit'), ms); };
  function round() {
    seq.push(Math.floor(Math.random() * 4));
    roundS.textContent = `${W.round} ${seq.length}`;
    listening = false;
    pos = 0;
    say('simonWatch', 'look');
    act('think');
    const step = Math.max(260, 620 - seq.length * 25);
    seq.forEach((v, k) => later(() => flash(v, step * 0.7), 700 + k * step));
    later(() => { listening = true; say('simonGo', 'smug'); act('watch'); brain.yourTurn(); }, 700 + seq.length * step);
  }
  function press(i) {
    if (!listening || over) return;
    buzz();
    flash(i, 220);
    if (i !== seq[pos]) {
      over = true;
      listening = false;
      const reached = seq.length - 1;
      say(fillN(pickOne(W.simonFail), seq.length), reached >= goal ? 'surprised' : 'smug');
      return later(() => played('simon', reached >= goal ? 'you' : 'her'), 900);
    }
    pos += 1;
    if (pos === seq.length) {
      listening = false;
      brain.event('yourPoint');
      if (seq.length === goal) brain.line('youGood', 'surprised', {}, 2); else if (Math.random() < 0.4) say('simonGood', 'look');
      later(round, 900);
    }
  }
  later(round, 600);
}
function fillN(s, n) { return String(s).replace('{n}', n); }
function pickOne(list) { return Array.isArray(list) ? list[Math.floor(Math.random() * list.length)] : list; }

// (1.11.0) opened from the app's Juegos tab: straight into that game
const asked = GAMES.find((g) => g.id === info.game);
if (asked) open(asked); else menu();

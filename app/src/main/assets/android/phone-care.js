// She looks after you and your phone: the battery (charged to the percent you chose, or running
// low), and a break now and then (after her desktop self's health breaks: two hours on the phone
// without putting it down five minutes, she tells you to rest your eyes). Android tells her the
// battery and when the screen goes off (CapriService.kt), no permission for either.
//
// Her config: batteryAlerts (on), batteryFull (100: when it's charged), batteryLow (15),
// breaks (on), breakHours (2).

import { on, send } from './native.js';

const REAL_BREAK_MS = 5 * 60 * 1000; // screen off this long: that was a break
const AGAIN_MS = 30 * 60 * 1000;     // "later", or no answer
const CRITICAL = 5;                  // and once more, right before it dies

const pick = (list) => list[Math.floor(Math.random() * list.length)];
const fill = (line, vars) => line.replace(/\{(\w+)\}/g, (_, k) => vars[k] ?? '');

const WORDS = {
  es: {
    full: ['¡Ya estás al {p}%! Ya puedes desconectarlo.', 'Batería al {p}%, choom. Desconéctalo antes de que se infle.', '{p}% de pila. Lista para otra ronda.'],
    low: ['Te queda {p}% de batería. Busca un cargador, choom.', 'Batería al {p}%. Yo que tú lo conectaba ya.', 'Solo {p}%… me estoy quedando sin energía. Cárgame.'],
    critical: ['¡{p}%! Esto se apaga en nada, conéctalo YA.', '{p}% y bajando. Si se apaga, me apago contigo.'],
    breakTime: '{h} h {m} min',
    breakLines: [
      'Llevas {time} pegado al teléfono sin descanso. Suéltalo cinco minutos y toma agua: yo lo cuido.',
      '{time} sin parar, choom. Mira algo lejos un rato, tus ojos te lo van a agradecer.',
      'Pausa. Ya son {time}. Estírate, parpadea, respira: la pantalla no se va a ir.',
    ],
    pause: 'Voy a descansar', later: 'Más tarde',
    paused: ['Ve. Aquí te espero.', 'Tómate tu tiempo, yo vigilo.'],
    laterLine: 'Va, te lo vuelvo a decir en media hora.',
  },
  en: {
    full: ["You're at {p}%! You can unplug it now.", 'Battery at {p}%, choom. Unplug it before it swells.', '{p}% charged. Ready for another round.'],
    low: ['{p}% battery left. Find a charger, choom.', "Battery at {p}%. If I were you I'd plug it in now.", 'Only {p}%… I\'m running out of juice. Charge me.'],
    critical: ['{p}%! This is about to die, plug it in NOW.', "{p}% and dropping. If it goes off, I go off with it."],
    breakTime: '{h} h {m} min',
    breakLines: [
      "You've been glued to your phone for {time} without a break. Put it down five minutes and drink some water: I'll watch it.",
      '{time} straight, choom. Look at something far away for a bit, your eyes will thank you.',
      "Break. It's been {time}. Stretch, blink, breathe: the screen isn't going anywhere.",
    ],
    pause: "I'll take a break", later: 'Later',
    paused: ["Go. I'll be here.", 'Take your time, I\'ve got it.'],
    laterLine: "Okay, I'll tell you again in half an hour.",
  },
  fr: {
    full: ['Tu es à {p} % ! Tu peux le débrancher.', 'Batterie à {p} %, choom. Débranche-le avant qu’elle gonfle.', '{p} % de batterie. Prêt pour un autre tour.'],
    low: ['Il te reste {p} % de batterie. Trouve un chargeur, choom.', 'Batterie à {p} %. À ta place, je le brancherais maintenant.', 'Plus que {p} %… je manque d’énergie. Recharge-moi.'],
    critical: ['{p} % ! Ça va s’éteindre, branche-le TOUT DE SUITE.', '{p} % et ça descend. S’il s’éteint, je m’éteins avec.'],
    breakTime: '{h} h {m} min',
    breakLines: [
      'Ça fait {time} que tu es collé à ton téléphone sans pause. Pose-le cinq minutes et bois de l’eau : je le garde.',
      '{time} non-stop, choom. Regarde au loin un moment, tes yeux te diront merci.',
      'Pause. Ça fait {time}. Étire-toi, cligne des yeux, respire : l’écran ne va pas partir.',
    ],
    pause: 'Je fais une pause', later: 'Plus tard',
    paused: ['Vas-y. Je t’attends ici.', 'Prends ton temps, je surveille.'],
    laterLine: 'OK, je te le redis dans une demi-heure.',
  },
  pt: {
    full: ['Já tá em {p}%! Pode desconectar.', 'Bateria em {p}%, choom. Desconecta antes que estufe.', '{p}% de bateria. Pronto pra outra rodada.'],
    low: ['Sobrou {p}% de bateria. Acha um carregador, choom.', 'Bateria em {p}%. Se eu fosse você, ligava já na tomada.', 'Só {p}%… tô ficando sem energia. Me carrega.'],
    critical: ['{p}%! Isso vai desligar já já, conecta AGORA.', '{p}% e caindo. Se desligar, eu desligo junto.'],
    breakTime: '{h} h {m} min',
    breakLines: [
      'Faz {time} que você tá grudado no celular sem descanso. Larga ele cinco minutos e bebe água: eu cuido dele.',
      '{time} sem parar, choom. Olha pra algo longe um pouco, seus olhos agradecem.',
      'Pausa. Já faz {time}. Se alonga, pisca, respira: a tela não vai sumir.',
    ],
    pause: 'Vou descansar', later: 'Mais tarde',
    paused: ['Vai. Te espero aqui.', 'Vai com calma, eu fico de olho.'],
    laterLine: 'Beleza, te falo de novo em meia hora.',
  },
  ru: {
    full: ['Уже {p}%! Можно отключать.', 'Батарея {p}%, чум. Отключи, пока не вздулась.', '{p}% заряда. Готова к новому раунду.'],
    low: ['Осталось {p}% батареи. Найди зарядку, чум.', 'Батарея {p}%. На твоём месте я бы уже поставила на зарядку.', 'Всего {p}%… у меня кончается энергия. Заряди меня.'],
    critical: ['{p}%! Сейчас выключится, подключи СРОЧНО.', '{p}% и падает. Выключится он — выключусь и я.'],
    breakTime: '{h} ч {m} мин',
    breakLines: [
      'Ты уже {time} в телефоне без перерыва. Отложи его на пять минут и выпей воды: я посторожу.',
      '{time} без остановки, чум. Посмотри вдаль немного, глаза скажут спасибо.',
      'Перерыв. Уже {time}. Потянись, поморгай, подыши: экран никуда не денется.',
    ],
    pause: 'Сделаю перерыв', later: 'Позже',
    paused: ['Иди. Я подожду.', 'Не спеши, я присмотрю.'],
    laterLine: 'Ладно, напомню через полчаса.',
  },
  vi: {
    full: ['Pin đã {p}%! Giờ rút sạc được rồi.', 'Pin {p}%, choom. Rút sạc trước khi nó phồng lên.', 'Đã sạc {p}%. Sẵn sàng cho vòng mới.'],
    low: ['Còn {p}% pin. Tìm sạc đi, choom.', 'Pin {p}%. Nếu là bạn mình sẽ cắm sạc ngay.', 'Chỉ còn {p}%… mình sắp cạn năng lượng. Sạc cho mình đi.'],
    critical: ['{p}%! Sắp tắt rồi, cắm sạc NGAY.', '{p}% và đang giảm. Nếu nó tắt, mình cũng tắt theo.'],
    breakTime: '{h} giờ {m} phút',
    breakLines: [
      'Bạn dán mắt vào điện thoại {time} không nghỉ rồi. Đặt xuống năm phút và uống nước đi: mình trông cho.',
      '{time} liền rồi, choom. Nhìn ra xa một chút, mắt bạn sẽ cảm ơn đấy.',
      'Nghỉ thôi. Đã {time} rồi. Duỗi người, chớp mắt, hít thở: màn hình không chạy đi đâu đâu.',
    ],
    pause: 'Mình nghỉ đây', later: 'Để sau',
    paused: ['Đi đi. Mình ở đây.', 'Cứ thong thả, để mình lo.'],
    laterLine: 'Được rồi, nửa tiếng nữa mình nhắc lại.',
  },
  ja: {
    full: ['{p}%になったよ！もう抜いて大丈夫。', 'バッテリー{p}%だよ、チューム。膨らむ前に抜いて。', '{p}%充電完了。次のラウンドの準備OK。'],
    low: ['バッテリー残り{p}%。充電器を探して、チューム。', 'バッテリー{p}%。わたしなら今すぐ充電する。', 'あと{p}%だけ…エネルギー切れそう。充電して。'],
    critical: ['{p}%！もうすぐ切れちゃう、今すぐ充電して。', '{p}%でまだ減ってる。切れたらわたしも一緒に消えちゃう。'],
    breakTime: '{h}時間{m}分',
    breakLines: [
      '休憩なしで{time}スマホに張りついてるよ。5分置いて水を飲んで。見ててあげるから。',
      '{time}ぶっ通しだよ、チューム。ちょっと遠くを見て。目が感謝するよ。',
      '休憩。もう{time}だよ。伸びをして、まばたきして、深呼吸。画面は逃げないから。',
    ],
    pause: '休憩する', later: 'あとで',
    paused: ['行ってらっしゃい。ここにいるね。', 'ゆっくりしてきて、任せて。'],
    laterLine: 'わかった、30分後にまた言うね。',
  },
  de: {
    full: ['Du bist bei {p} %! Du kannst ihn jetzt abstecken.', 'Akku bei {p} %, Choom. Steck ab, bevor er sich aufbläht.', '{p} % geladen. Bereit für die nächste Runde.'],
    low: ['Noch {p} % Akku. Such ein Ladegerät, Choom.', 'Akku bei {p} %. An deiner Stelle würde ich jetzt laden.', 'Nur noch {p} %… mir geht der Saft aus. Lad mich.'],
    critical: ['{p} %! Gleich ist er aus, lad JETZT.', '{p} % und fallend. Wenn er ausgeht, geh ich mit.'],
    breakTime: '{h} h {m} min',
    breakLines: [
      'Du klebst seit {time} ohne Pause an deinem Handy. Leg es fünf Minuten weg und trink Wasser: Ich pass drauf auf.',
      '{time} am Stück, Choom. Schau mal kurz in die Ferne, deine Augen werden’s dir danken.',
      'Pause. Es sind schon {time}. Streck dich, blinzle, atme: Der Bildschirm läuft nicht weg.',
    ],
    pause: 'Ich mach Pause', later: 'Später',
    paused: ['Geh nur. Ich bin hier.', 'Lass dir Zeit, ich hab’s im Griff.'],
    laterLine: 'Okay, ich sag’s dir in einer halben Stunde nochmal.',
  },
  it: {
    full: ['Sei al {p}%! Ora puoi staccarlo.', 'Batteria al {p}%, choom. Staccalo prima che si gonfi.', '{p}% carico. Pronto per un altro giro.'],
    low: ['Resta il {p}% di batteria. Trova un caricatore, choom.', 'Batteria al {p}%. Io al posto tuo lo collegherei adesso.', 'Solo il {p}%… sto finendo l’energia. Ricaricami.'],
    critical: ['{p}%! Sta per spegnersi, collegalo SUBITO.', '{p}% e in calo. Se si spegne, mi spengo anch’io.'],
    breakTime: '{h} h {m} min',
    breakLines: [
      'Sei incollato al telefono da {time} senza pause. Mettilo giù cinque minuti e bevi un po’ d’acqua: lo guardo io.',
      '{time} di fila, choom. Guarda qualcosa di lontano per un po’, i tuoi occhi ringrazieranno.',
      'Pausa. Sono già {time}. Stiracchiati, sbatti le palpebre, respira: lo schermo non scappa.',
    ],
    pause: 'Faccio una pausa', later: 'Più tardi',
    paused: ['Vai. Io sono qui.', 'Prenditi il tuo tempo, ci penso io.'],
    laterLine: 'Ok, te lo ricordo tra mezz’ora.',
  },
};

export function phoneCare({ cfg }) {
  const dev = window.dev;
  if (!dev?.capri) return;
  const { capri, panel } = dev;
  const t = () => WORDS[dev.brain?.lang] || WORDS.en;

  // her line: in her panel when she's out, else in her notification
  const tell = (line, { face = 'look', act = null, anim = null, ...more } = {}) => {
    const away = capri.hidden || capri.stowed || capri.dormant;
    if (away && !cfg().icon) return send('notify:wink', { line, face });
    capri.wake?.();
    // her animation for it (poses.js) when it's in, else her move
    if (anim && dev.poses?.has(anim)) dev.poses.play(anim, { times: 2 });
    else if (act) capri.perform?.(act, capri.towardCenter?.() || 1);
    panel.say({ kind: 'info', face, line, hold: 9000, ...more });
  };
  // not in the middle of her chat, nor in gamer mode (a game shouldn't be covered)
  const quiet = () => !dev.chat?.open && !cfg().gamer;

  // ---------- the battery ----------
  const said = { full: false, low: false, critical: false };
  on('battery:update', (b) => {
    if (!b || typeof b.percent !== 'number') return;
    const c = cfg();
    if (c.batteryAlerts === false) return;
    const p = Math.round(b.percent);
    const full = Math.min(100, Math.max(50, Number(c.batteryFull) || 100));
    const low = Math.min(50, Math.max(5, Number(c.batteryLow) || 15));
    // charging: what she said about it running low is over, and the other way round
    if (b.charging) {
      said.low = said.critical = false;
      if (!said.full && (p >= full || (full >= 100 && b.full))) {
        said.full = true;
        if (quiet()) tell(fill(pick(t().full), { p }), { face: 'laugh', act: 'victory', anim: 'charge' });
      }
      return;
    }
    said.full = false;
    if (p > low + 2) said.low = said.critical = false;
    if (!said.critical && p <= CRITICAL && low > CRITICAL) {
      said.critical = said.low = true;
      return tell(fill(pick(t().critical), { p }), { face: 'wide', act: 'alarm', anim: 'drained' }); // even in a game
    }
    if (!said.low && p <= low) {
      said.low = true;
      if (!cfg().gamer) tell(fill(pick(t().low), { p }), { face: 'aside', act: 'drained', anim: 'drained' });
    }
  });
  send('battery:now');

  // ---------- breaks ----------
  let since = Date.now(); // on the phone since (the screen on, without a real break)
  let due = 0;
  const every = () => Math.min(6, Math.max(0.5, Number(cfg().breakHours) || 2)) * 3600 * 1000;
  on('screen:on', (d) => {
    if (Number(d?.offMs) >= REAL_BREAK_MS) {
      since = Date.now();
      due = 0;
    }
  });
  setInterval(() => {
    if (cfg().breaks === false || document.hidden) return;
    const now = Date.now();
    if (!due) due = since + every();
    if (now < due || !quiet() || capri.busy || capri.dragging || panel.current) return;
    due = now + AGAIN_MS;
    remind();
  }, 60 * 1000);

  function remind() {
    const w = t();
    const minutes = Math.round((Date.now() - since) / 60000);
    const time = fill(w.breakTime, { h: Math.floor(minutes / 60), m: String(minutes % 60).padStart(2, '0') });
    tell(fill(pick(w.breakLines), { time }), {
      face: 'look', act: 'stretch', anim: 'stretch', hold: 20000,
      actions: [{ id: 'pause', label: w.pause, primary: true }, { id: 'later', label: w.later }],
      onAction: (id) => {
        if (id === 'later') {
          due = Date.now() + AGAIN_MS;
          panel.say({ kind: 'info', face: 'aside', line: w.laterLine, hold: 3500 });
          return;
        }
        panel.say({ kind: 'done', face: 'smug', line: pick(w.paused), hold: 4000 });
      },
    });
  }
}

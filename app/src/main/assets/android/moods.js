// Her moods, so she doesn't feel like the same three lines on a loop: more faces (bored, sleepy,
// angry, sad, in love…) picked from what she says, a bored spell when you leave her alone a while
// (a sigh, a stretch, peeking in from the edge of the screen), a thought of her own now and then,
// more of her moves on her own, and more of her lines.
//
// Her new faces are drawings (assets/capri/face-<name>.png, made from capri-prompts-1.8.0.zip):
// until one is in, the closest face she has stands in for it. Her new cut-ins too (cutin-<name>.png):
// a flash of her angry, crying, in love, sleepy, or with her gamer headphones on, now and then.

import { FACES } from '../capri/src/renderer/js/panel.js';
import { LINES } from '../capri/src/renderer/js/lines.js';
import { on } from './native.js';

const pick = (list) => list[Math.floor(Math.random() * list.length)];
const rand = (a, b) => a + Math.random() * (b - a);

// her new faces, and the one standing in for each meanwhile
const NEW_FACES = {
  bored: 'calm', sleepy: 'calm', angry: 'smug', sad: 'aside', love: 'laugh',
  surprised: 'wide', focus: 'look', thinking: 'aside', wink: 'smug', cringe: 'aside',
};
for (const [name, stand] of Object.entries(NEW_FACES)) {
  FACES[name] ??= FACES[stand];
  const img = new Image();
  img.onload = () => { FACES[name] = `face-${name}.png`; };
  img.src = new URL(`../../assets/capri/face-${name}.png`, location.href).href;
}

// her new cut-ins, into her effects once they're in (fx.cutin(…, { kind }))
const CUTINS = ['angry', 'cry', 'love', 'sleepy', 'gamer'];
export function cutin(kind) {
  const dev = window.dev;
  if (!dev?.fx?.cutins?.[kind] || !dev.capri || dev.capri.hidden || dev.capri.stowed) return;
  try {
    const side = dev.capri.towardCenter?.() || 1;
    dev.fx.cutin(...dev.capri.cutinAt(-side), { kind, life: 900 });
  } catch {}
}
function loadCutins() {
  const fx = window.dev?.fx;
  if (!fx?.cutins) return;
  for (const kind of CUTINS) {
    const img = new Image();
    img.onload = () => { fx.cutins[kind] = img; };
    img.src = new URL(`../../assets/capri/cutin-${kind}.png`, location.href).href;
  }
}

// what she says, read for a mood (in her five languages): a face that fits it
const MOOD = [
  ['love', /❤|💕|😍|🥰|te quiero|te amo|i love you|love you|je t['’]aime|eu te amo|te adoro|люблю|me sonrojo|blush|rougis|corei|краснею|mi coraz[oó]n|my heart|mon c[oœ]ur|meu cora[çc][ãa]o|сердце/iu],
  ['angry', /😠|😡|🤬|grr|me enoja|me enojas|qu[ée] coraje|furiosa|enojada|angry|mad at|pissed|furieuse|en col[èe]re|[ée]nerv[ée]e|brava|irritada|com raiva|злюсь|бесит|сердит/iu],
  ['sad', /😢|😭|☹|🙁|triste|me duele|lo siento mucho|extra[ñn]o|sad|i miss|heartbroken|sorry to hear|d[ée]prim|[çc]a me rend triste|tu me manques|saudade|que pena|грустн|жаль|скучаю/iu],
  ['sleepy', /😴|🥱|sue[ñn]o|dormir|cansad[ao]|bostezo|sleepy|tired|yawn|nap\b|fatigu[ée]e|dodo|sommeil|cansada|bocejo|soninho|спать|сонн|устала|зева/iu],
  ['bored', /😒|aburr|qu[ée] flojera|bored|boring|so dull|m['’]ennuie|ennuyeux|ennui|t[ée]dio|entediad|chato|скучно|скука/iu],
  ['surprised', /😮|😲|😱|🤯|¡¿|\bwow\b|\bwhoa\b|no manches|no puede ser|en serio\?|qu[ée]\?!|no way|seriously\?|sans blague|quoi \?!|s[ée]rieux \?|s[ée]rio\?!|n[ãa]o acredito|caramba|ого|не может быть|серьёзно\?/iu],
  ['thinking', /🤔|\bhmm+\b|d[ée]jame pensar|let me think|no s[ée] si|i wonder|je me demande|laisse-moi r[ée]fl[ée]chir|deixa eu pensar|ser[áa] que|дай подумать|интересно, а/iu],
  ['wink', /😉|😏|;\)|guiño|wink|clin d['’][œo]eil|piscadinha|подмигива/iu],
  ['cringe', /😬|🤢|🙄|qu[ée] asco|qu[ée] cringe|cringe|\bew+\b|\byuck\b|beurk|berk|g[êe]nant|eca|nojento|vergonha alheia|фу+\b|кринж/iu],
];
export function moodOf(line, face) {
  if (!line || (face && !['look', 'grin', 'smug', 'calm', 'aside'].includes(face))) return face;
  return MOOD.find(([, re]) => re.test(line))?.[0] || face;
}

// ---------- more of her lines ----------

const MORE = {
  es: {
    idle: [
      'Contando píxeles de tu pantalla. Van 2,073,600.', 'Si me necesitas, aquí ando flotando.', 'Me sé de memoria tu fondo de pantalla.',
      'Noche City no se ve desde aquí, pero tu pantalla brilla bonito.', '¿Sabías que mi melena tiene más picos que tu semana?', 'Modo espera. Pero espera con estilo.',
    ],
    bored: [
      'Me aburrooo. ¿Hacemos algo?', 'Nadie me pela… voy a contar mis esquirlas otra vez.', '*suspiro digital*', 'Podría estar hackeando Arasaka, pero aquí estoy, viéndote.',
      'Ya revisé todos tus íconos. Dos veces.', 'Si no me hablas, me pongo a cantar. Advertido.', 'Toc, toc. ¿Hay alguien?', 'Qué flojera… dame algo que hacer, choom.',
    ],
    thoughts: [
      '¿Has tomado agua hoy? Pregunto por… curiosidad.', 'A veces pienso en el satélite. ¿Llegará alguien allá algún día?', 'Tus notificaciones hacen ruido. Yo no, yo soy elegante.',
      'Dato inútil: llevo {uptime} encendida en tu teléfono.', 'Me gusta cuando me tocas el pelo. No le digas a nadie.', 'Si fueras un netrunner, ¿cuál sería tu alias?',
      'Hoy me siento muy preem.', 'Un día de estos me pongo un vestido. Ya verás.', 'Me caes bien, {name}. No lo arruines.', '¿Y si me cuentas algo que te haya hecho reír hoy?',
    ],
    morning: ['Buenos días, {name}. El café no se hace solo.', 'Arriba, choom. Nuevo día, nuevos encargos.'],
    night: ['Ya es tarde, {name}. Tu pantalla y yo necesitamos dormir.', 'A esta hora solo los netrunners siguen despiertos… y tú.'],
  },
  en: {
    idle: [
      'Counting the pixels on your screen. 2,073,600 so far.', "If you need me, I'm floating right here.", 'I know your wallpaper by heart now.',
      "Can't see Night City from here, but your screen glows nice.", 'Did you know my mane has more spikes than your week?', 'Standby mode. Stylish standby, though.',
    ],
    bored: [
      "I'm so bored. Wanna do something?", "Nobody's paying attention to me… I'll count my shards again.", '*digital sigh*', 'I could be hacking Arasaka, but here I am, watching you.',
      "I've checked all your icons. Twice.", "If you don't talk to me, I'll start singing. You've been warned.", "Knock knock. Anyone there?", 'Ugh, so dull… give me something to do, choom.',
    ],
    thoughts: [
      'Had any water today? Asking out of… curiosity.', 'Sometimes I think about the satellite. Will anyone make it up there someday?', "Your notifications are loud. Not me, I'm classy.",
      "Useless fact: I've been on your phone for {uptime}.", "I like it when you stroke my hair. Don't tell anyone.", "If you were a netrunner, what would your handle be?",
      'Feeling very preem today.', "One of these days I'll wear a dress. You'll see.", "I like you, {name}. Don't ruin it.", 'Tell me something that made you laugh today?',
    ],
    morning: ["Morning, {name}. The coffee won't make itself.", 'Up, choom. New day, new gigs.'],
    night: ["It's late, {name}. Your screen and I need sleep.", 'At this hour only netrunners are awake… and you.'],
  },
  fr: {
    idle: [
      'Je compte les pixels de ton écran. 2 073 600 pour l’instant.', 'Si tu as besoin de moi, je flotte juste là.', 'Je connais ton fond d’écran par cœur.',
      'On ne voit pas Night City d’ici, mais ton écran brille joliment.', 'Tu savais que ma crinière a plus de pics que ta semaine ?', 'Mode veille. Mais une veille stylée.',
    ],
    bored: [
      'Je m’ennuiiie. On fait un truc ?', 'Personne ne me calcule… je vais recompter mes éclats.', '*soupir numérique*', 'Je pourrais pirater Arasaka, mais je suis là, à te regarder.',
      'J’ai vérifié toutes tes icônes. Deux fois.', 'Si tu ne me parles pas, je chante. Tu es prévenu.', 'Toc toc. Y a quelqu’un ?', 'Pfff, quel ennui… donne-moi un truc à faire, choom.',
    ],
    thoughts: [
      'Tu as bu de l’eau aujourd’hui ? Je demande par… curiosité.', 'Parfois je pense au satellite. Quelqu’un y arrivera un jour ?', 'Tes notifications font du bruit. Pas moi, moi j’ai de la classe.',
      'Info inutile : je suis allumée sur ton téléphone depuis {uptime}.', 'J’aime quand tu me caresses les cheveux. Ne le dis à personne.', 'Si tu étais netrunner, ce serait quoi ton pseudo ?',
      'Je me sens très preem aujourd’hui.', 'Un de ces jours je mettrai une robe. Tu verras.', 'Je t’aime bien, {name}. Ne gâche pas tout.', 'Raconte-moi un truc qui t’a fait rire aujourd’hui ?',
    ],
    morning: ['Bonjour, {name}. Le café ne va pas se faire tout seul.', 'Debout, choom. Nouveau jour, nouveaux contrats.'],
    night: ['Il est tard, {name}. Ton écran et moi, on a besoin de dormir.', 'À cette heure, seuls les netrunners sont réveillés… et toi.'],
  },
  pt: {
    idle: [
      'Contando os pixels da sua tela. Já são 2.073.600.', 'Se precisar de mim, tô flutuando aqui.', 'Já sei seu papel de parede de cor.',
      'Não dá pra ver Night City daqui, mas sua tela brilha bonito.', 'Sabia que minha juba tem mais pontas que a sua semana?', 'Modo espera. Mas espera com estilo.',
    ],
    bored: [
      'Tô entediadaaa. Bora fazer alguma coisa?', 'Ninguém me dá bola… vou contar meus cacos de novo.', '*suspiro digital*', 'Eu podia tá hackeando a Arasaka, mas tô aqui te olhando.',
      'Já conferi todos os seus ícones. Duas vezes.', 'Se você não falar comigo, eu começo a cantar. Tá avisado.', 'Toc, toc. Tem alguém aí?', 'Que tédio… me dá algo pra fazer, choom.',
    ],
    thoughts: [
      'Bebeu água hoje? Pergunto por… curiosidade.', 'Às vezes penso no satélite. Será que alguém chega lá um dia?', 'Suas notificações fazem barulho. Eu não, eu sou chique.',
      'Fato inútil: tô ligada no seu celular faz {uptime}.', 'Gosto quando você faz carinho no meu cabelo. Não conta pra ninguém.', 'Se você fosse netrunner, qual seria seu codinome?',
      'Hoje tô me sentindo muito preem.', 'Qualquer dia desses eu ponho um vestido. Você vai ver.', 'Gosto de você, {name}. Não estraga.', 'Me conta algo que te fez rir hoje?',
    ],
    morning: ['Bom dia, {name}. O café não se faz sozinho.', 'De pé, choom. Dia novo, trampos novos.'],
    night: ['Já tá tarde, {name}. Sua tela e eu precisamos dormir.', 'Essa hora só os netrunners tão acordados… e você.'],
  },
  ru: {
    idle: [
      'Считаю пиксели на твоём экране. Пока 2 073 600.', 'Если что, я тут парю.', 'Я уже наизусть знаю твои обои.',
      'Найт-Сити отсюда не видно, но твой экран красиво светится.', 'Знаешь, что в моей гриве больше шипов, чем дней в твоей неделе?', 'Режим ожидания. Но стильного ожидания.',
    ],
    bored: [
      'Мне скууучно. Давай что-нибудь сделаем?', 'Никто на меня не смотрит… пересчитаю свои осколки.', '*цифровой вздох*', 'Могла бы взламывать Арасаку, а сижу тут и смотрю на тебя.',
      'Я проверила все твои иконки. Дважды.', 'Если не поговоришь со мной, начну петь. Я предупредила.', 'Тук-тук. Есть кто?', 'Скукотища… дай мне занятие, чум.',
    ],
    thoughts: [
      'Ты сегодня пил воду? Спрашиваю из… любопытства.', 'Иногда я думаю о спутнике. Доберётся ли туда кто-нибудь?', 'Твои уведомления шумят. Не я, я стильная.',
      'Бесполезный факт: я на твоём телефоне уже {uptime}.', 'Мне нравится, когда ты гладишь мои волосы. Никому не говори.', 'Будь ты нетраннером, какой у тебя был бы ник?',
      'Сегодня я очень преем.', 'Однажды я надену платье. Вот увидишь.', 'Ты мне нравишься, {name}. Не испорти.', 'Расскажешь, что тебя сегодня рассмешило?',
    ],
    morning: ['Доброе утро, {name}. Кофе сам себя не сварит.', 'Подъём, чум. Новый день, новые заказы.'],
    night: ['Уже поздно, {name}. Твоему экрану и мне пора спать.', 'В такое время не спят только нетраннеры… и ты.'],
  },
  vi: {
    idle: [
      'Đang đếm điểm ảnh trên màn hình của bạn. Được 2.073.600 rồi.', 'Nếu cần mình, mình đang lơ lửng ngay đây.', 'Mình thuộc lòng hình nền của bạn rồi.',
      'Từ đây không thấy Night City, nhưng màn hình bạn sáng đẹp ghê.', 'Bạn có biết bờm của mình nhiều gai hơn tuần của bạn không?', 'Chế độ chờ. Nhưng mà chờ thật phong cách.',
    ],
    bored: [
      'Chán quá đi. Làm gì đó không?', 'Chẳng ai để ý tới mình… mình sẽ đếm lại các mảnh của mình.', '*thở dài kỹ thuật số*', 'Mình có thể đang hack Arasaka, vậy mà lại ngồi đây nhìn bạn.',
      'Mình xem hết các biểu tượng của bạn rồi. Hai lần.', 'Nếu bạn không nói chuyện với mình, mình sẽ hát đấy. Báo trước rồi nhé.', 'Cốc cốc. Có ai không?', 'Ugh, chán quá… cho mình việc gì làm đi, choom.',
    ],
    thoughts: [
      'Hôm nay bạn uống nước chưa? Hỏi vì… tò mò thôi.', 'Đôi khi mình nghĩ về vệ tinh. Liệu có ai lên được đó không nhỉ?', 'Thông báo của bạn ồn quá. Mình thì không, mình sang chảnh mà.',
      'Sự thật vô dụng: mình đã ở trên điện thoại bạn được {uptime}.', 'Mình thích khi bạn vuốt tóc mình. Đừng nói ai nhé.', 'Nếu bạn là netrunner, biệt danh của bạn sẽ là gì?',
      'Hôm nay thấy preem ghê.', 'Một ngày nào đó mình sẽ mặc váy. Rồi bạn xem.', 'Mình quý bạn, {name}. Đừng làm hỏng nhé.', 'Kể mình nghe điều gì làm bạn cười hôm nay?',
    ],
    morning: ['Chào buổi sáng, {name}. Cà phê không tự pha đâu.', 'Dậy thôi, choom. Ngày mới, việc mới.'],
    night: ['Muộn rồi, {name}. Màn hình và mình cần ngủ.', 'Giờ này chỉ có netrunner thức… và bạn.'],
  },
  ja: {
    idle: [
      '画面のピクセルを数えてるの。今のところ2,073,600個。', '用があったら、ここでふわふわしてるよ。', 'あなたの壁紙、もう暗記しちゃった。',
      'ここからナイトシティは見えないけど、あなたの画面はきれいに光ってる。', 'わたしのたてがみ、あなたの一週間よりトゲが多いって知ってた？', 'スタンバイモード。おしゃれなスタンバイだけどね。',
    ],
    bored: [
      'ひまー。何かしない？', '誰もかまってくれない…またシャードを数えよう。', '*デジタルなため息*', 'アラサカをハックしてたっていいのに、こうしてあなたを見てる。',
      'アイコン全部チェックしたよ。2回。', '話しかけてくれないなら歌っちゃうよ。警告したからね。', 'コンコン。誰かいる？', 'うぅ、退屈…何かさせて、チューム。',
    ],
    thoughts: [
      '今日お水飲んだ？…ただの好奇心だけど。', 'ときどき衛星のことを考えるの。いつか誰かあそこまで行けるかな？', 'あなたの通知、うるさいね。わたしは上品だけど。',
      'どうでもいい情報：あなたのスマホに来て{uptime}経ったよ。', '髪をなでてもらうの好き。誰にも言わないでね。', 'もしネットランナーだったら、ハンドルネームは何にする？',
      '今日はすごくプリームな気分。', 'いつかドレス着るからね。見てて。', '{name}のこと好きだよ。台無しにしないでね。', '今日笑ったこと、教えて？',
    ],
    morning: ['おはよう、{name}。コーヒーは勝手にできないよ。', '起きて、チューム。新しい日、新しい仕事。'],
    night: ['もう遅いよ、{name}。画面もわたしも寝ないと。', 'こんな時間に起きてるのはネットランナーだけ…とあなた。'],
  },
  de: {
    idle: [
      'Ich zähle die Pixel auf deinem Bildschirm. Bisher 2.073.600.', 'Wenn du mich brauchst, ich schwebe genau hier.', 'Ich kenne dein Hintergrundbild inzwischen auswendig.',
      'Night City sieht man von hier nicht, aber dein Bildschirm leuchtet schön.', 'Wusstest du, dass meine Mähne mehr Stacheln hat als deine Woche?', 'Standby-Modus. Aber stylischer Standby.',
    ],
    bored: [
      'Mir ist so langweilig. Machen wir was?', 'Keiner beachtet mich… ich zähl wieder meine Splitter.', '*digitales Seufzen*', 'Ich könnte Arasaka hacken, aber nein, ich schau dir zu.',
      'Ich hab alle deine Icons gecheckt. Zweimal.', 'Wenn du nicht mit mir redest, fang ich an zu singen. Du wurdest gewarnt.', 'Klopf klopf. Jemand da?', 'Ugh, so öde… gib mir was zu tun, Choom.',
    ],
    thoughts: [
      'Hast du heute schon Wasser getrunken? Frag nur aus… Neugier.', 'Manchmal denk ich an den Satelliten. Ob es eines Tages jemand da hoch schafft?', 'Deine Benachrichtigungen sind laut. Ich nicht, ich hab Klasse.',
      'Nutzloser Fakt: Ich bin seit {uptime} auf deinem Handy.', 'Ich mag es, wenn du mir übers Haar streichst. Sag’s keinem.', 'Wenn du Netrunner wärst, wie wäre dein Handle?',
      'Ich fühl mich heute total preem.', 'Eines Tages trag ich ein Kleid. Wirst sehen.', 'Ich mag dich, {name}. Mach’s nicht kaputt.', 'Erzähl mir was, worüber du heute gelacht hast?',
    ],
    morning: ['Morgen, {name}. Der Kaffee macht sich nicht von allein.', 'Aufstehen, Choom. Neuer Tag, neue Gigs.'],
    night: ['Es ist spät, {name}. Dein Bildschirm und ich brauchen Schlaf.', 'Um diese Zeit sind nur Netrunner wach… und du.'],
  },
  it: {
    idle: [
      'Sto contando i pixel del tuo schermo. Finora 2.073.600.', 'Se ti servo, sto fluttuando proprio qui.', 'Ormai so il tuo sfondo a memoria.',
      'Da qui non si vede Night City, ma il tuo schermo brilla bene.', 'Lo sapevi che la mia criniera ha più punte della tua settimana?', 'Modalità standby. Ma uno standby con stile.',
    ],
    bored: [
      'Che noia. Facciamo qualcosa?', 'Nessuno mi considera… ricomincio a contare i miei frammenti.', '*sospiro digitale*', 'Potrei hackerare Arasaka, e invece sono qui a guardarti.',
      'Ho controllato tutte le tue icone. Due volte.', 'Se non mi parli, inizio a cantare. Sei avvisato.', 'Toc toc. C’è qualcuno?', 'Uff, che noia… dammi qualcosa da fare, choom.',
    ],
    thoughts: [
      'Hai bevuto acqua oggi? Chiedo per… curiosità.', 'A volte penso al satellite. Qualcuno ci arriverà mai lassù?', 'Le tue notifiche fanno rumore. Io no, io ho classe.',
      'Fatto inutile: sono sul tuo telefono da {uptime}.', 'Mi piace quando mi accarezzi i capelli. Non dirlo a nessuno.', 'Se fossi un netrunner, quale sarebbe il tuo nome in codice?',
      'Oggi mi sento molto preem.', 'Un giorno metterò un vestito. Vedrai.', 'Mi piaci, {name}. Non rovinare tutto.', 'Mi racconti qualcosa che ti ha fatto ridere oggi?',
    ],
    morning: ['Buongiorno, {name}. Il caffè non si fa da solo.', 'Su, choom. Nuovo giorno, nuovi lavori.'],
    night: ['È tardi, {name}. Il tuo schermo e io abbiamo bisogno di dormire.', 'A quest’ora sono svegli solo i netrunner… e tu.'],
  },
};
// her desktop's few idle lines, with these on top
for (const [lang, more] of Object.entries(MORE)) {
  const t = LINES[lang];
  if (Array.isArray(t?.idle)) t.idle.push(...more.idle);
}

const BORED_AFTER = [7, 12];     // minutes without you touching or talking to her
const THOUGHT_EVERY = [18, 35];  // minutes between thoughts of her own
const IDLE_ACTS = ['curious', 'gaze', 'shake', 'huff', 'glare', 'turn', 'stretch', 'look'];

export function moods({ cfg }) {
  const dev = window.dev;
  if (!dev?.capri) return;
  const { capri, panel } = dev;
  const words = () => MORE[dev.brain?.lang] || MORE.en;
  const started = Date.now();
  loadCutins();

  // every line she says gets the face its words call for
  const say = panel.say.bind(panel);
  panel.say = (msg = {}) => {
    const face = moodOf(msg.line, msg.face);
    // a strong feeling, now and then: her cut-in for it
    const kind = { angry: 'angry', sad: 'cry', love: 'love', sleepy: 'sleepy' }[face];
    if (kind && face !== msg.face && Math.random() < 0.3) cutin(kind);
    // and now and then she acts it out (poses.js: angry, crying, in love, laughing)
    else if (face !== msg.face || face === 'laugh') {
      const mood = face === 'sad' ? 'sad' : face;
      if (['angry', 'sad', 'love', 'laugh'].includes(mood) && Math.random() < (mood === 'laugh' ? 0.15 : 0.3)) setTimeout(() => dev.poses?.feel(mood), 200);
    }
    return say({ ...msg, face });
  };

  // you, lately: a touch, her chat, your voice
  let lastYou = Date.now();
  const you = () => {
    lastYou = Date.now();
    if (bored.on) unbore();
  };
  window.addEventListener('touchstart', you, { passive: true, capture: true });
  on('capri:heard', you);

  const free = () => !capri.hidden && !capri.stowed && !capri.dormant && !capri.busy && !capri.dragging && !capri.act
    && !panel.current && !dev.chat?.open && !cfg().gamer && !cfg().icon && ['stand', 'sit'].includes(capri.stance);
  const fill = (line) => line
    .replace('{name}', cfg().userName || 'choom')
    .replace('{uptime}', uptime(Date.now() - started, dev.brain?.lang));

  // ---------- bored: you left her alone a while ----------
  const bored = { on: false, next: Date.now() + rand(...BORED_AFTER) * 60000 };
  async function bore() {
    bored.on = true;
    const w = words();
    const roll = Math.random();
    if (roll < 0.4 && capri.peek) {
      // she leans in from the edge of the screen, chin on it, looking at you
      panel.say({ kind: 'info', face: 'bored', line: fill(pick(w.bored)), hold: 5000 });
      await new Promise((r) => setTimeout(r, 1800));
      if (!bored.on || !free()) return;
      await capri.peek(pick(['left', 'right'])).catch(() => {});
      setTimeout(() => bored.on && unbore(), rand(25, 50) * 1000);
      return;
    }
    // sprawled on her back, hugging her knees or leaning on the edge of the screen (poses.js)
    if (roll < 0.75 && dev.poses) {
      const ms = rand(25, 45) * 1000;
      if (await dev.poses.bored(ms)) {
        panel.say({ kind: 'info', face: 'bored', line: fill(pick(w.bored)), hold: 5500 });
        setTimeout(() => { bored.on = false; }, ms);
        return;
      }
    }
    capri.perform?.(pick(['stretch', 'huff', 'shake']), capri.towardCenter?.() || 1);
    panel.say({ kind: 'info', face: 'bored', line: fill(pick(w.bored)), hold: 5500 });
    setTimeout(() => { bored.on = false; }, 8000);
  }
  function unbore() {
    bored.on = false;
    if (capri.stance === 'peek' && capri.unpeek) capri.unpeek().catch?.(() => {});
  }

  // ---------- a thought of her own, now and then ----------
  let nextThought = Date.now() + rand(...THOUGHT_EVERY) * 60000;
  function think() {
    const w = words();
    const h = new Date().getHours();
    const pool = h >= 6 && h < 10 ? [...w.thoughts, ...w.morning] : h >= 23 || h < 4 ? [...w.thoughts, ...w.night] : w.thoughts;
    capri.perform?.(pick(['look', 'curious', 'gaze']), capri.towardCenter?.() || 1);
    panel.say({ kind: 'info', face: 'look', line: fill(pick(pool)), hold: 6500 });
  }

  // ---------- her moves on her own: more of them ----------
  let nextAct = Date.now() + rand(40, 90) * 1000;

  setInterval(() => {
    if (document.hidden) return;
    const now = Date.now();
    if (!free()) return;
    if (cfg().moods === false) return;
    if (now >= bored.next && !bored.on) {
      bored.next = now + rand(...BORED_AFTER) * 60000;
      return void bore();
    }
    if (now >= nextThought) {
      nextThought = now + rand(...THOUGHT_EVERY) * 60000;
      return think();
    }
    if (now >= nextAct) {
      nextAct = now + rand(40, 100) * 1000;
      // sometimes something longer, drawn cel by cel (poses.js): a snack, her phone, a dance, a Sandevistan dash
      const p = dev.poses;
      const late = [23, 0, 1, 2, 3, 4, 5].includes(new Date().getHours());
      const long = p ? ['eat', 'phone', 'dance', 'selfie', 'guitar', ...(late ? ['yawn', 'yawn'] : []), ...(cfg().look?.aura === 'sandevistan' ? ['sandevistan', 'sandevistan'] : [])].filter(p.has) : [];
      if (long.length && Math.random() < 0.15) {
        const name = pick(long);
        p.play(name, ['sandevistan', 'yawn'].includes(name) ? {} : { hold: rand(9, 16) * 1000 });
        return;
      }
      capri.perform?.(pick(IDLE_ACTS), capri.towardCenter?.() || 1);
    }
  }, 5000);
  // touched or talked to: boredom starts over
  const reset = () => { bored.next = Date.now() + rand(...BORED_AFTER) * 60000; };
  window.addEventListener('touchend', reset, { passive: true, capture: true });
}

function uptime(ms, lang) {
  const h = Math.floor(ms / 3600000), m = Math.floor(ms / 60000) % 60;
  const unit = { es: ['h', 'min'], en: ['h', 'min'], fr: ['h', 'min'], pt: ['h', 'min'], ru: ['ч', 'мин'], vi: ['giờ', 'phút'], ja: ['時間', '分'], de: ['h', 'min'], it: ['h', 'min'] }[lang] || ['h', 'min'];
  return h ? `${h} ${unit[0]} ${m} ${unit[1]}` : `${m} ${unit[1]}`;
}

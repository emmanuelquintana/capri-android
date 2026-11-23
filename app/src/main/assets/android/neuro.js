// Neurodanzas (1.11.0, Emma: "después de dar mucho cariño, estar mucho con Capri, ella te da neurodanzas
// que podrías ver, y ella opinaría de esas neurodanzas"). A braindance: one of her memories, recorded for
// you, to play back in the app (android/neuro.html, Neuro.kt keeps them).
// - Earned with affection: petting her, tapping her, talking to her. Enough of it in a while (and not
//   more than one a day) and she records one and gives it to you (a notification opens it).
// - Her AI writes it (a short memory of hers, in scenes, with a detail only the thermal layer shows, and
//   what she thinks of it); without an AI, one of her own recorded ones.
// - You watch it and rate it: she says what she thinks of your rating, over your screen.
// - Asked: "Capri, dame una neurodanza" (if you've earned it), "mis neurodanzas" (the list).

import { on, send, call } from './native.js';

// her animations a scene can use (anims.json), and what each is for, for her AI
export const ACTS = {
  dance: 'dancing', guitar: 'playing guitar', hack: 'hacking', sandevistan: 'sandevistan speed burst', scan: 'scanning',
  eat: 'eating instant noodles', sleep: 'asleep', wake: 'waking up', stretch: 'stretching', selfie: 'taking a selfie',
  stream: 'streaming', music: 'listening to music', party: 'partying', love: 'in love', shy: 'shy', laugh: 'laughing',
  cry: 'crying', angry: 'angry', surprised: 'surprised', think: 'thinking', wave: 'waving', 'hug-rock': 'sitting hugging her knees',
  'lean-nails': 'leaning, looking at her nails', 'game-zone': 'focused, in the zone', charge: 'charging up', yawn: 'yawning', dizzy: 'dizzy',
};

export const NEURO_ASK = /neuro ?dan[zcç]as?|neuro ?danse|braindance|нейротан[ец]+|нейро ?танц/iu;
const LIST = /\b(?:mis|my|mes|minhas|ver|see|show|voir|montre|mostra|lista?)\b|мои|покажи|посмотреть/iu;
const GIVE = /dame|hazme|graba|reg[aá]lame|quiero|nueva|otra|give|make|record|want|new|another|donne|fais|enregistre|veux|nouvelle|autre|me d[aá]|faz|grava|quero|dай|дай|сделай|запиши|хочу|нов/iu;

const T = {
  es: {
    gift: ['Te grabé una neurodanza. Es un recuerdo mío… no te burles. Está en la app.', 'Toma: una neurodanza, solo para ti. Ábrela cuando quieras.', 'Me diste tanto cariño que te grabé una neurodanza. Ve a verla.'],
    making: 'Dame un momento, la estoy grabando…',
    notYet: 'Las neurodanzas se ganan, choom. Dame más cariño y ya veremos.',
    today: 'Hoy ya te di una. Mañana grabo otra, prometido.',
    list: 'Aquí están tus neurodanzas.',
    rated: {
      3: ['¿Te encantó? Lo sabía. Soy una artista.', 'Preem. Grabaré más, entonces.'],
      2: ['¿Solo «bien»? La próxima te dejo sin palabras.', 'Hmm. Acepto la crítica… más o menos.'],
      1: ['¿No te gustó? Era MI recuerdo… bueno, la próxima será mejor.', 'Ouch. Directo al corazón. Lo anoto.'],
    },
  },
  en: {
    gift: ['I recorded a braindance for you. It’s one of my memories… don’t laugh. It’s in the app.', 'Here: a braindance, just for you. Open it whenever you want.', 'You gave me so much love that I recorded you a braindance. Go watch it.'],
    making: 'Give me a moment, I’m recording it…',
    notYet: 'Braindances are earned, choom. Give me more love and we’ll see.',
    today: 'I already gave you one today. I’ll record another tomorrow, promise.',
    list: 'Here are your braindances.',
    rated: {
      3: ['You loved it? I knew it. I’m an artist.', 'Preem. I’ll record more, then.'],
      2: ['Just “okay”? Next one will leave you speechless.', 'Hmm. I accept the review… sort of.'],
      1: ['You didn’t like it? It was MY memory… fine, the next one will be better.', 'Ouch. Straight to the heart. Noted.'],
    },
  },
  fr: {
    gift: ['Je t’ai enregistré une neurodanse. C’est un de mes souvenirs… ne te moque pas. Elle est dans l’appli.', 'Tiens : une neurodanse, rien que pour toi. Ouvre-la quand tu veux.', 'Tu m’as donné tellement d’affection que je t’ai enregistré une neurodanse. Va la voir.'],
    making: 'Une seconde, je l’enregistre…',
    notYet: 'Les neurodanses, ça se mérite, choom. Donne-moi plus d’affection et on verra.',
    today: 'Je t’en ai déjà donné une aujourd’hui. J’en enregistre une autre demain, promis.',
    list: 'Voici tes neurodanses.',
    rated: {
      3: ['Tu as adoré ? Je le savais. Je suis une artiste.', 'Preem. J’en enregistrerai d’autres, alors.'],
      2: ['Juste « bien » ? La prochaine te laissera sans voix.', 'Hmm. J’accepte la critique… plus ou moins.'],
      1: ['Ça ne t’a pas plu ? C’était MON souvenir… bon, la prochaine sera mieux.', 'Aïe. En plein cœur. C’est noté.'],
    },
  },
  pt: {
    gift: ['Gravei uma neurodança pra você. É uma lembrança minha… não ri. Tá no app.', 'Toma: uma neurodança, só pra você. Abre quando quiser.', 'Você me deu tanto carinho que gravei uma neurodança pra você. Vai ver.'],
    making: 'Me dá um momento, tô gravando…',
    notYet: 'Neurodanças se ganham, choom. Me dá mais carinho e a gente vê.',
    today: 'Hoje eu já te dei uma. Amanhã gravo outra, prometo.',
    list: 'Aqui estão suas neurodanças.',
    rated: {
      3: ['Amou? Eu sabia. Sou uma artista.', 'Preem. Vou gravar mais, então.'],
      2: ['Só «boa»? A próxima vai te deixar sem palavras.', 'Hmm. Aceito a crítica… mais ou menos.'],
      1: ['Não gostou? Era a MINHA lembrança… tá, a próxima vai ser melhor.', 'Ai. Direto no coração. Anotado.'],
    },
  },
  ru: {
    gift: ['Я записала для тебя нейротанец. Это моё воспоминание… не смейся. Он в приложении.', 'Держи: нейротанец, только для тебя. Открой, когда захочешь.', 'Ты дал(а) мне столько ласки, что я записала тебе нейротанец. Иди смотри.'],
    making: 'Секунду, я его записываю…',
    notYet: 'Нейротанцы надо заслужить, чумба. Дай мне больше ласки, и посмотрим.',
    today: 'Сегодня я тебе уже один подарила. Завтра запишу ещё, обещаю.',
    list: 'Вот твои нейротанцы.',
    rated: {
      3: ['Понравилось? Я так и знала. Я художница.', 'Прем. Тогда запишу ещё.'],
      2: ['Просто «нормально»? Следующий лишит тебя дара речи.', 'Хм. Принимаю критику… более-менее.'],
      1: ['Не понравилось? Это было МОЁ воспоминание… ладно, следующий будет лучше.', 'Ай. Прямо в сердце. Записала.'],
    },
  },
  vi: {
    gift: ['Mình đã ghi một braindance cho bạn. Đó là một ký ức của mình… đừng cười nhé. Nó ở trong ứng dụng.', 'Đây: một braindance, chỉ cho bạn. Mở ra lúc nào bạn muốn.', 'Bạn cho mình nhiều yêu thương quá nên mình ghi cho bạn một braindance. Xem đi.'],
    making: 'Chờ mình chút, mình đang ghi…',
    notYet: 'Braindance phải tự kiếm, choom. Yêu thương mình thêm rồi tính.',
    today: 'Hôm nay mình đã tặng bạn một cái rồi. Mai mình ghi cái khác, hứa đó.',
    list: 'Đây là các braindance của bạn.',
    rated: {
      3: ['Bạn thích à? Mình biết mà. Mình là nghệ sĩ.', 'Preem. Vậy mình sẽ ghi thêm.'],
      2: ['Chỉ “tạm được”? Cái sau sẽ làm bạn cứng họng.', 'Hừm. Mình chấp nhận đánh giá… kiểu vậy.'],
      1: ['Bạn không thích? Đó là ký ức CỦA mình… thôi được, cái sau sẽ hay hơn.', 'Ui. Trúng tim luôn. Ghi nhận.'],
    },
  },
  ja: {
    gift: ['ブレインダンスを記録したよ。わたしの記憶のひとつ…笑わないでね。アプリにあるよ。', 'はい、あなただけのブレインダンス。好きなときに開いて。', 'たくさん愛情をくれたから、ブレインダンスを記録したの。見てきて。'],
    making: 'ちょっと待って、記録中…',
    notYet: 'ブレインダンスは稼ぐものだよ、チューム。もっと愛情をくれたら考える。',
    today: '今日はもうひとつあげたよ。明日また記録するから、約束。',
    list: 'あなたのブレインダンスはこれだよ。',
    rated: {
      3: ['気に入った？やっぱりね。わたしアーティストだから。', 'プリーム。じゃあもっと記録するね。'],
      2: ['「まあまあ」だけ？次のは言葉を失わせるから。', 'うーん。レビューは受け止める…一応ね。'],
      1: ['好きじゃなかった？わたしの記憶だったのに…いいよ、次はもっとよくする。', 'いたっ。心に直撃。覚えとく。'],
    },
  },
  de: {
    gift: ['Ich hab einen Braindance für dich aufgenommen. Es ist eine meiner Erinnerungen… nicht lachen. Er ist in der App.', 'Hier: ein Braindance, nur für dich. Öffne ihn, wann du willst.', 'Du hast mir so viel Liebe gegeben, dass ich dir einen Braindance aufgenommen hab. Schau ihn dir an.'],
    making: 'Gib mir einen Moment, ich nehme ihn auf…',
    notYet: 'Braindances muss man sich verdienen, Choom. Gib mir mehr Liebe, dann sehen wir weiter.',
    today: 'Ich hab dir heute schon einen gegeben. Morgen nehm ich einen neuen auf, versprochen.',
    list: 'Hier sind deine Braindances.',
    rated: {
      3: ['Er hat dir gefallen? Wusste ich’s doch. Ich bin eine Künstlerin.', 'Preem. Dann nehm ich mehr auf.'],
      2: ['Nur „okay“? Der nächste wird dich sprachlos machen.', 'Hmm. Ich akzeptiere die Kritik… irgendwie.'],
      1: ['Er hat dir nicht gefallen? Das war MEINE Erinnerung… na gut, der nächste wird besser.', 'Autsch. Mitten ins Herz. Notiert.'],
    },
  },
  it: {
    gift: ['Ho registrato una braindance per te. È uno dei miei ricordi… non ridere. È nell’app.', 'Ecco: una braindance, solo per te. Aprila quando vuoi.', 'Mi hai dato così tanto affetto che ti ho registrato una braindance. Vai a guardarla.'],
    making: 'Dammi un momento, la sto registrando…',
    notYet: 'Le braindance si guadagnano, choom. Dammi più affetto e vediamo.',
    today: 'Oggi te ne ho già data una. Domani ne registro un’altra, promesso.',
    list: 'Ecco le tue braindance.',
    rated: {
      3: ['Ti è piaciuta? Lo sapevo. Sono un’artista.', 'Preem. Allora ne registro altre.'],
      2: ['Solo “carina”? La prossima ti lascerà senza parole.', 'Hmm. Accetto la recensione… più o meno.'],
      1: ['Non ti è piaciuta? Era il MIO ricordo… va bene, la prossima sarà migliore.', 'Ahi. Dritto al cuore. Preso nota.'],
    },
  },
};

// her own recorded ones, for when she has no AI (or it fails)
const OWN = {
  es: [
    { title: 'Azotea, 3 a. m.', scenes: [['stretch', 'Subí a la azotea. Night City nunca apaga las luces.'], ['hug-rock', 'Me senté en la orilla a ver los AV pasar.', 'Hay un gato escondido detrás del tinaco.'], ['think', 'Pensé en todo lo que no te he dicho.'], ['music', 'Puse mi canción favorita, bajito.'], ['shy', 'Y luego pensé en ti. Fin. No preguntes.']], opinion: 'Bonita, pero me veo muy cursi al final. Igual me gusta.' },
    { title: 'Noche de fideos', scenes: [['yawn', 'Dos de la mañana y no podía dormir.'], ['eat', 'Fideos instantáneos. Cena de campeonas.', 'El sobre de picante ya estaba caducado.'], ['laugh', 'Se me cayó uno en la máscara. No lo cuentes.'], ['eat', 'Segundo vaso. No me juzgues.'], ['sleep', 'Y me dormí en la mesa. Valió la pena.']], opinion: 'Mi mejor actuación. Los fideos merecen un premio.' },
    { title: 'Ensayo secreto', scenes: [['guitar', 'Aprendí un riff nuevo para ti.'], ['angry', 'Me equivoqué siete veces seguidas.', 'Una cuerda está a punto de romperse.'], ['game-zone', 'Respiré. Me concentré.'], ['guitar', 'Y salió perfecto. Más o menos.'], ['party', '¡Lo logré! Ahora tienes que oírlo en vivo.']], opinion: 'La parte donde me enojo sobra. Lo demás, preem.' },
    { title: 'Sandevistan', scenes: [['charge', 'Cargué el implante al cien.'], ['sandevistan', 'Todo se volvió lento. Menos yo.', 'Se ve tu reflejo en un charco.'], ['surprised', 'Atrapé una gota de lluvia en el aire.'], ['dizzy', 'Efecto secundario: mareo. Mucho.'], ['laugh', 'Lo volvería a hacer. Ahora mismo.']], opinion: 'Esta es para presumir. Admite que me veo genial.' },
  ],
  en: [
    { title: 'Rooftop, 3 a.m.', scenes: [['stretch', 'I went up to the roof. Night City never turns its lights off.'], ['hug-rock', 'I sat on the edge watching the AVs go by.', 'There’s a cat hiding behind the water tank.'], ['think', 'I thought about everything I haven’t told you.'], ['music', 'I put on my favorite song, quietly.'], ['shy', 'And then I thought about you. The end. Don’t ask.']], opinion: 'Pretty, but I look so cheesy at the end. I still like it.' },
    { title: 'Noodle night', scenes: [['yawn', 'Two in the morning and I couldn’t sleep.'], ['eat', 'Instant noodles. A champion’s dinner.', 'The hot sauce packet was already expired.'], ['laugh', 'One fell on my mask. Don’t tell anyone.'], ['eat', 'Second cup. Don’t judge me.'], ['sleep', 'And I fell asleep on the table. Worth it.']], opinion: 'My best performance. The noodles deserve an award.' },
    { title: 'Secret rehearsal', scenes: [['guitar', 'I learned a new riff for you.'], ['angry', 'I messed it up seven times in a row.', 'One string is about to snap.'], ['game-zone', 'I breathed. I focused.'], ['guitar', 'And it came out perfect. More or less.'], ['party', 'I did it! Now you have to hear it live.']], opinion: 'The angry part could go. The rest is preem.' },
    { title: 'Sandevistan', scenes: [['charge', 'I charged the implant to a hundred.'], ['sandevistan', 'Everything went slow. Except me.', 'Your reflection shows in a puddle.'], ['surprised', 'I caught a raindrop in midair.'], ['dizzy', 'Side effect: dizziness. A lot.'], ['laugh', 'I’d do it again. Right now.']], opinion: 'This one is for showing off. Admit I look amazing.' },
  ],
  fr: [
    { title: 'Toit, 3 h du matin', scenes: [['stretch', 'Je suis montée sur le toit. Night City n’éteint jamais ses lumières.'], ['hug-rock', 'Assise au bord, je regardais passer les AV.', 'Un chat se cache derrière le réservoir.'], ['think', 'J’ai pensé à tout ce que je ne t’ai pas dit.'], ['music', 'J’ai mis ma chanson préférée, tout bas.'], ['shy', 'Et puis j’ai pensé à toi. Fin. Ne demande pas.']], opinion: 'Jolie, mais je suis trop mièvre à la fin. Je l’aime quand même.' },
    { title: 'Nuit de nouilles', scenes: [['yawn', 'Deux heures du matin et pas sommeil.'], ['eat', 'Nouilles instantanées. Le dîner des championnes.', 'Le sachet de piment était périmé.'], ['laugh', 'Une est tombée sur mon masque. Ne le dis à personne.'], ['eat', 'Deuxième bol. Ne me juge pas.'], ['sleep', 'Et je me suis endormie sur la table. Ça valait le coup.']], opinion: 'Ma meilleure performance. Les nouilles méritent un prix.' },
    { title: 'Répétition secrète', scenes: [['guitar', 'J’ai appris un nouveau riff pour toi.'], ['angry', 'Je me suis trompée sept fois de suite.', 'Une corde est sur le point de casser.'], ['game-zone', 'J’ai respiré. Je me suis concentrée.'], ['guitar', 'Et c’est sorti parfait. Plus ou moins.'], ['party', 'J’ai réussi ! Maintenant tu dois l’entendre en vrai.']], opinion: 'Le moment où je m’énerve est de trop. Le reste, preem.' },
    { title: 'Sandevistan', scenes: [['charge', 'J’ai chargé l’implant à cent pour cent.'], ['sandevistan', 'Tout est devenu lent. Sauf moi.', 'Ton reflet se voit dans une flaque.'], ['surprised', 'J’ai attrapé une goutte de pluie en l’air.'], ['dizzy', 'Effet secondaire : le vertige. Beaucoup.'], ['laugh', 'Je recommencerais. Tout de suite.']], opinion: 'Celle-là, c’est pour frimer. Avoue que je suis géniale.' },
  ],
  pt: [
    { title: 'Terraço, 3 da manhã', scenes: [['stretch', 'Subi no terraço. Night City nunca apaga as luzes.'], ['hug-rock', 'Sentei na beirada vendo os AV passarem.', 'Tem um gato escondido atrás da caixa d’água.'], ['think', 'Pensei em tudo que ainda não te contei.'], ['music', 'Coloquei minha música favorita, baixinho.'], ['shy', 'E aí pensei em você. Fim. Não pergunta.']], opinion: 'Bonita, mas fico muito melosa no final. Mesmo assim eu gosto.' },
    { title: 'Noite de miojo', scenes: [['yawn', 'Duas da manhã e eu sem sono.'], ['eat', 'Miojo. Jantar de campeã.', 'O sachê de pimenta já estava vencido.'], ['laugh', 'Caiu um fio na minha máscara. Não conta pra ninguém.'], ['eat', 'Segundo copo. Não me julga.'], ['sleep', 'E dormi na mesa. Valeu a pena.']], opinion: 'Minha melhor atuação. O miojo merece um prêmio.' },
    { title: 'Ensaio secreto', scenes: [['guitar', 'Aprendi um riff novo pra você.'], ['angry', 'Errei sete vezes seguidas.', 'Uma corda está quase arrebentando.'], ['game-zone', 'Respirei. Me concentrei.'], ['guitar', 'E saiu perfeito. Mais ou menos.'], ['party', 'Consegui! Agora você tem que ouvir ao vivo.']], opinion: 'A parte em que eu fico brava sobra. O resto, preem.' },
    { title: 'Sandevistan', scenes: [['charge', 'Carreguei o implante até cem.'], ['sandevistan', 'Tudo ficou lento. Menos eu.', 'Dá pra ver seu reflexo numa poça.'], ['surprised', 'Peguei uma gota de chuva no ar.'], ['dizzy', 'Efeito colateral: tontura. Muita.'], ['laugh', 'Faria de novo. Agora mesmo.']], opinion: 'Essa é pra me exibir. Admite que eu fico incrível.' },
  ],
  ru: [
    { title: 'Крыша, 3 часа ночи', scenes: [['stretch', 'Я поднялась на крышу. Найт-Сити никогда не гасит огни.'], ['hug-rock', 'Сидела на краю и смотрела, как летят AV.', 'За баком с водой прячется кот.'], ['think', 'Думала обо всём, что ещё тебе не сказала.'], ['music', 'Включила любимую песню, тихонько.'], ['shy', 'А потом подумала о тебе. Конец. Не спрашивай.']], opinion: 'Красиво, но в конце я слишком сентиментальная. Всё равно нравится.' },
    { title: 'Ночь лапши', scenes: [['yawn', 'Два часа ночи, а сна нет.'], ['eat', 'Лапша быстрого приготовления. Ужин чемпионки.', 'Пакетик с острым соусом был просрочен.'], ['laugh', 'Одна лапшинка упала на маску. Никому не говори.'], ['eat', 'Вторая порция. Не суди меня.'], ['sleep', 'И я уснула за столом. Оно того стоило.']], opinion: 'Моя лучшая роль. Лапша заслуживает награды.' },
    { title: 'Тайная репетиция', scenes: [['guitar', 'Я выучила новый рифф для тебя.'], ['angry', 'Ошиблась семь раз подряд.', 'Одна струна вот-вот лопнет.'], ['game-zone', 'Вдохнула. Сосредоточилась.'], ['guitar', 'И вышло идеально. Почти.'], ['party', 'Получилось! Теперь ты должен(на) услышать вживую.']], opinion: 'Момент, где я злюсь, лишний. Остальное — прем.' },
    { title: 'Сандевистан', scenes: [['charge', 'Зарядила имплант на сто.'], ['sandevistan', 'Всё замедлилось. Кроме меня.', 'В луже видно твоё отражение.'], ['surprised', 'Поймала каплю дождя в воздухе.'], ['dizzy', 'Побочный эффект: голова кружится. Сильно.'], ['laugh', 'Сделала бы ещё раз. Прямо сейчас.']], opinion: 'Это чтобы похвастаться. Признай, что я выгляжу потрясающе.' },
  ],
  vi: [
    { title: 'Sân thượng, 3 giờ sáng', scenes: [['stretch', 'Mình lên sân thượng. Night City không bao giờ tắt đèn.'], ['hug-rock', 'Mình ngồi ở mép nhìn các AV bay qua.', 'Có một con mèo trốn sau bồn nước.'], ['think', 'Mình nghĩ về tất cả những gì chưa kể với bạn.'], ['music', 'Mình bật bài hát yêu thích, thật khẽ.'], ['shy', 'Rồi mình nghĩ về bạn. Hết. Đừng hỏi.']], opinion: 'Đẹp, nhưng đoạn cuối mình sến quá. Vẫn thích.' },
    { title: 'Đêm mì gói', scenes: [['yawn', 'Hai giờ sáng mà mình không ngủ được.'], ['eat', 'Mì gói. Bữa tối của nhà vô địch.', 'Gói tương ớt đã hết hạn rồi.'], ['laugh', 'Một sợi rơi lên mặt nạ của mình. Đừng nói ai.'], ['eat', 'Ly thứ hai. Đừng phán xét mình.'], ['sleep', 'Rồi mình ngủ gục trên bàn. Đáng lắm.']], opinion: 'Màn trình diễn hay nhất của mình. Mì xứng đáng nhận giải.' },
    { title: 'Buổi tập bí mật', scenes: [['guitar', 'Mình học một đoạn riff mới cho bạn.'], ['angry', 'Mình đàn hỏng bảy lần liền.', 'Một sợi dây sắp đứt rồi.'], ['game-zone', 'Mình hít thở. Mình tập trung.'], ['guitar', 'Và nó hoàn hảo. Gần như vậy.'], ['party', 'Mình làm được rồi! Giờ bạn phải nghe trực tiếp.']], opinion: 'Đoạn giận dỗi có thể bỏ. Còn lại thì preem.' },
    { title: 'Sandevistan', scenes: [['charge', 'Mình sạc cấy ghép lên một trăm.'], ['sandevistan', 'Mọi thứ chậm lại. Trừ mình.', 'Bóng của bạn hiện trong vũng nước.'], ['surprised', 'Mình bắt được một giọt mưa giữa không trung.'], ['dizzy', 'Tác dụng phụ: chóng mặt. Rất nhiều.'], ['laugh', 'Mình sẽ làm lại. Ngay bây giờ.']], opinion: 'Cái này để khoe. Công nhận mình đỉnh đi.' },
  ],
  ja: [
    { title: '屋上、午前3時', scenes: [['stretch', '屋上に上がったの。ナイトシティは明かりを消さない。'], ['hug-rock', '端に座ってAVが飛んでいくのを見てた。', '給水タンクの後ろに猫が隠れてる。'], ['think', 'まだあなたに話してないこと全部を考えた。'], ['music', 'お気に入りの曲を小さくかけたの。'], ['shy', 'それから、あなたのことを考えた。おしまい。聞かないで。']], opinion: 'きれいだけど、最後のわたしクサすぎ。それでも好き。' },
    { title: 'ラーメンの夜', scenes: [['yawn', '夜中の2時、眠れなかった。'], ['eat', 'インスタントラーメン。チャンピオンの夕食。', 'ホットソースの袋、もう期限切れだった。'], ['laugh', '一本マスクに落ちたの。誰にも言わないで。'], ['eat', '2杯目。ジャッジしないで。'], ['sleep', 'そのままテーブルで寝ちゃった。価値あり。']], opinion: 'わたしの最高の演技。ラーメンは賞をもらうべき。' },
    { title: '秘密の練習', scenes: [['guitar', 'あなたのために新しいリフを覚えたの。'], ['angry', '7回連続で失敗した。', '弦が一本切れそう。'], ['game-zone', '深呼吸。集中。'], ['guitar', 'そして完璧に弾けた。だいたいね。'], ['party', 'できた！今度は生で聞いてもらうから。']], opinion: '怒ってるところはカットでいいかも。あとはプリーム。' },
    { title: 'サンデヴィスタン', scenes: [['charge', 'インプラントを100までチャージ。'], ['sandevistan', '全部がスローになった。わたし以外。', '水たまりにあなたの姿が映ってる。'], ['surprised', '空中で雨粒をつかまえたの。'], ['dizzy', '副作用：めまい。すごく。'], ['laugh', 'またやりたい。今すぐ。']], opinion: 'これは見せびらかし用。わたしすごいって認めて。' },
  ],
  de: [
    { title: 'Dach, 3 Uhr nachts', scenes: [['stretch', 'Ich bin aufs Dach gegangen. Night City macht nie das Licht aus.'], ['hug-rock', 'Ich hab am Rand gesessen und den AVs zugesehen.', 'Hinter dem Wassertank versteckt sich eine Katze.'], ['think', 'Ich hab an alles gedacht, was ich dir noch nicht erzählt hab.'], ['music', 'Ich hab mein Lieblingslied angemacht, ganz leise.'], ['shy', 'Und dann hab ich an dich gedacht. Ende. Frag nicht.']], opinion: 'Schön, aber am Ende bin ich so kitschig. Gefällt mir trotzdem.' },
    { title: 'Nudelnacht', scenes: [['yawn', 'Zwei Uhr morgens und ich konnte nicht schlafen.'], ['eat', 'Instantnudeln. Das Abendessen der Champions.', 'Das Päckchen scharfe Soße war schon abgelaufen.'], ['laugh', 'Eine ist auf meine Maske gefallen. Sag’s keinem.'], ['eat', 'Zweiter Becher. Urteil nicht.'], ['sleep', 'Und ich bin am Tisch eingeschlafen. Hat sich gelohnt.']], opinion: 'Meine beste Vorstellung. Die Nudeln verdienen einen Preis.' },
    { title: 'Geheime Probe', scenes: [['guitar', 'Ich hab einen neuen Riff für dich gelernt.'], ['angry', 'Siebenmal hintereinander verhauen.', 'Eine Saite reißt gleich.'], ['game-zone', 'Ich hab geatmet. Mich konzentriert.'], ['guitar', 'Und es kam perfekt raus. Mehr oder weniger.'], ['party', 'Geschafft! Jetzt musst du es live hören.']], opinion: 'Der wütende Teil könnte raus. Der Rest ist preem.' },
    { title: 'Sandevistan', scenes: [['charge', 'Ich hab das Implantat auf hundert geladen.'], ['sandevistan', 'Alles wurde langsam. Außer mir.', 'Dein Spiegelbild ist in einer Pfütze zu sehen.'], ['surprised', 'Ich hab einen Regentropfen in der Luft gefangen.'], ['dizzy', 'Nebenwirkung: Schwindel. Viel.'], ['laugh', 'Ich würd’s nochmal machen. Sofort.']], opinion: 'Der hier ist zum Angeben. Gib zu, ich sehe fantastisch aus.' },
  ],
  it: [
    { title: 'Tetto, ore 3', scenes: [['stretch', 'Sono salita sul tetto. Night City non spegne mai le luci.'], ['hug-rock', 'Mi sono seduta sul bordo a guardare passare gli AV.', 'C’è un gatto nascosto dietro il serbatoio dell’acqua.'], ['think', 'Ho pensato a tutto quello che non ti ho ancora detto.'], ['music', 'Ho messo la mia canzone preferita, piano.'], ['shy', 'E poi ho pensato a te. Fine. Non chiedere.']], opinion: 'Bella, ma alla fine sono troppo sdolcinata. Mi piace lo stesso.' },
    { title: 'Notte di noodles', scenes: [['yawn', 'Le due di notte e non riuscivo a dormire.'], ['eat', 'Noodles istantanei. La cena dei campioni.', 'La bustina di salsa piccante era già scaduta.'], ['laugh', 'Uno mi è caduto sulla maschera. Non dirlo a nessuno.'], ['eat', 'Seconda tazza. Non giudicarmi.'], ['sleep', 'E mi sono addormentata sul tavolo. Ne è valsa la pena.']], opinion: 'La mia migliore interpretazione. I noodles meritano un premio.' },
    { title: 'Prova segreta', scenes: [['guitar', 'Ho imparato un nuovo riff per te.'], ['angry', 'L’ho sbagliato sette volte di fila.', 'Una corda sta per rompersi.'], ['game-zone', 'Ho respirato. Mi sono concentrata.'], ['guitar', 'Ed è venuto perfetto. Più o meno.'], ['party', 'Ce l’ho fatta! Ora devi sentirlo dal vivo.']], opinion: 'La parte arrabbiata si potrebbe togliere. Il resto è preem.' },
    { title: 'Sandevistan', scenes: [['charge', 'Ho caricato l’impianto al cento.'], ['sandevistan', 'Tutto è rallentato. Tranne me.', 'Il tuo riflesso si vede in una pozzanghera.'], ['surprised', 'Ho preso una goccia di pioggia al volo.'], ['dizzy', 'Effetto collaterale: vertigini. Tante.'], ['laugh', 'Lo rifarei. Subito.']], opinion: 'Questa è per mettermi in mostra. Ammetti che sono stupenda.' },
  ],
};

const WEIGHT = { pet: 3, click: 1, chat: 2 };
const WINDOW = 45 * 60 * 1000;
const EARN = 24;          // affection in that window for a braindance
const GAP = 20 * 3600000; // one a day, more or less
const LANGS = { es: 'Spanish', en: 'English', fr: 'French', pt: 'Portuguese (Brazil)', ru: 'Russian', vi: 'Vietnamese', ja: 'Japanese', de: 'German', it: 'Italian' };
const pick = (l) => l[Math.floor(Math.random() * l.length)];

/** One she wrote with her AI: { title, scenes: [{ act, text, hidden }], opinion } or null. */
export function parseBd(text) {
  const m = String(text || '').match(/\{[\s\S]*\}/);
  if (!m) return null;
  let j;
  try { j = JSON.parse(m[0]); } catch { return null; }
  const s = (v, n) => String(v || '').replace(/\s+/g, ' ').trim().slice(0, n);
  const scenes = (Array.isArray(j.scenes) ? j.scenes : [])
    .map((x) => ({ act: ACTS[x?.act] ? x.act : 'think', text: s(x?.text, 140), hidden: s(x?.hidden, 100) }))
    .filter((x) => x.text).slice(0, 7);
  if (scenes.length < 3 || !s(j.title, 50)) return null;
  return { title: s(j.title, 50), scenes, opinion: s(j.opinion, 220) };
}

export function neuroDances({ cfg, setConfig, lang }) {
  const dev = window.dev;
  const panel = dev?.panel;
  const chat = dev?.chat;
  if (!panel || !dev?.bond) return null;
  const t = () => T[lang()] || T.en;
  const say = (line, face = 'smug', hold = 6500) => panel.say({ kind: 'info', face, line, hold });
  let love = [];
  let making = false;

  // the affection she gets, as it comes (bond.js keeps the rest)
  const add = dev.bond.add.bind(dev.bond);
  dev.bond.add = (source, n = 1) => {
    add(source, n);
    if (!WEIGHT[source]) return;
    const now = Date.now();
    love = love.filter((x) => now - x.at < WINDOW);
    love.push({ at: now, n: WEIGHT[source] * n });
    if (score() >= EARN && ready() && !making) setTimeout(() => give(false), 2500);
  };
  const score = () => love.reduce((a, x) => a + x.n, 0);
  const ready = () => Date.now() - (cfg().neuroAt || 0) > GAP;

  async function record() {
    const l = lang();
    const status = await window.host?.chatStatus?.().catch(() => ({ ok: false }));
    if (status?.ok && chat?.persona) {
      const acts = Object.entries(ACTS).map(([k, v]) => `${k} (${v})`).join(', ');
      const prompt = `Record a braindance ("neurodanza") for the user: one short memory of yours, from your point of view, `
        + `something you did or felt recently (it can be about them, about Night City, about your hobbies). `
        + `Answer ONLY with JSON: {"title": "2-5 words", "scenes": [{"act": "one of: ${acts}", "text": "one short line in first person, max 15 words", "hidden": "optional: a tiny detail only the thermal layer shows, or empty"}], "opinion": "what you think of your own braindance, 1-2 sentences, honest, a bit vain or shy"}. `
        + `4 to 6 scenes. Write everything in ${LANGS[l] || 'Spanish'}, speaking of yourself in the singular.`;
      const messages = [{ role: 'system', content: chat.persona() }, ...chat.history.slice(-8), { role: 'user', content: prompt }];
      const res = await window.host.chatSend({ id: Date.now(), messages }).catch(() => null);
      const bd = res?.ok ? parseBd(res.text) : null;
      if (bd) return { ...bd, own: false };
    }
    const own = pick(OWN[l] || OWN.en);
    return { title: own.title, scenes: own.scenes.map(([act, text, hidden = '']) => ({ act, text, hidden })), opinion: own.opinion, own: true };
  }

  // asked: you asked for it (she already said she's recording it); else earned, at most one a day
  async function give(asked) {
    if (making || (!asked && !ready())) return;
    making = true;
    try {
      const bd = await record();
      setConfig({ neuroAt: Date.now() });
      love = [];
      await call('neuro:add', { ...bd, lang: lang(), at: Date.now() }).catch(() => null);
      say(pick(t().gift), 'love', 8000);
      dev.capri?.perform?.('hop');
      chat?.history?.push({ role: 'assistant', content: `(I just gave the user a braindance I recorded: "${bd.title}")` });
    } catch (err) {
      console.error('[capri] neurodanza', err);
    } finally {
      making = false;
    }
  }

  // you rated one, in the app
  on('neuro:rated', ({ title = '', rating = 2 } = {}) => {
    const r = Math.max(1, Math.min(3, Number(rating) || 2));
    setTimeout(() => say(pick(t().rated[r]), r === 3 ? 'laugh' : r === 2 ? 'aside' : 'sad'), 900);
    chat?.history?.push({ role: 'assistant', content: `(The user watched my braindance "${String(title).slice(0, 50)}" and rated it ${['', 'bad', 'okay', 'loved it'][r]}.)` });
  });

  return {
    /** "Capri, dame una neurodanza" / "mis neurodanzas": { line, face } or null. */
    ask(text) {
      if (!NEURO_ASK.test(text)) return null;
      if (LIST.test(text) || !GIVE.test(text)) { send('page:open', { page: 'neuro' }); return { line: t().list, face: 'smug' }; }
      if (!ready()) return { line: t().today, face: 'aside' };
      // asked: a little less affection than on her own, or the first one ever
      if (score() < EARN / 2 && cfg().neuroAt) return { line: t().notYet, face: 'smug' };
      setTimeout(() => give(true), 300);
      return { line: t().making, face: 'thinking' };
    },
    give: () => give(true),
  };
}

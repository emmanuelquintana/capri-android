// (1.12.0, community: "modo lectura, que me resuma lo que llevo") Capri reading with you: tell her which
// book ("estoy leyendo Dune") and where you are ("voy en la página 120"), and her AI knows it, so "resúmeme
// lo que llevo" or "¿quién era Stilgar?" gets an answer only up to your page, never a spoiler past it.
// Kept on this phone (localStorage capri.reading).

const KEY = 'capri.reading';

const START = /^(?:estoy leyendo|empec[eé] a leer|voy a leer|i'?m reading|i am reading|i started reading|je lis|je suis en train de lire|j'ai commencé|estou lendo|comecei a ler|я читаю|я начал[аи]? читать|tôi đang đọc|ich lese gerade|ich lese|sto leggendo|ho iniziato a leggere)\s+[«"“']?(.{2,80}?)[»"”']?[.!]?$/iu;
const START_JA = /^[「『]?(.{1,60}?)[」』]?を読んで(?:る|います|いる)/u;
const PAGE = /(?:voy en la|estoy en la|ya voy en la|i'?m on|i am on|je suis à la|estou na|я на|tôi đang ở|ich bin auf|sono a|sono alla)\s*(?:p[aá]gina|page|p[aá]g\.?|страниц[еа]|trang|seite|pagina)\s*(\d{1,5})/iu;
const PAGE_JA = /(\d{1,5})\s*ページ(?:目)?(?:まで|にいる|を読んで)/u;
const DONE = /(?:termin[eé] (?:de leer|el libro)|dej[eé] de leer|finished (?:reading|the book)|stopped reading|j'ai fini (?:le livre|de lire)|terminei (?:o livro|de ler)|я дочитал[аи]?|đọc xong|読み終わった|ich habe (?:das buch )?(?:fertig|ausgelesen)|ho finito (?:il libro|di leggere))/iu;
const MODE = /modo (?:de )?lectura|reading mode|mode lecture|modo (?:de )?leitura|режим чтения|chế độ đọc|読書モード|lesemodus|modalità lettura/iu;

const LINES = {
  es: { start: 'Anotado: estás leyendo «{book}». Dime en qué página vas y te resumo sin spoilers cuando quieras.', page: 'Página {n} de «{book}». Si quieres un resumen de lo que llevas, pídemelo.', done: '¡Terminaste «{book}»! ¿Qué te pareció?', on: 'Modo lectura: sigo contigo en «{book}», página {n}. Pídeme un resumen o pregúntame por un personaje.', none: 'Modo lectura: dime qué libro lees, por ejemplo «estoy leyendo Dune», y en qué página vas.', noBook: 'Primero dime qué libro lees: «estoy leyendo …».' },
  en: { start: 'Got it: you\'re reading "{book}". Tell me your page and I\'ll sum it up without spoilers whenever you want.', page: 'Page {n} of "{book}". Ask me for a recap of what you\'ve read anytime.', done: 'You finished "{book}"! What did you think?', on: 'Reading mode: I\'m with you in "{book}", page {n}. Ask me for a recap or about a character.', none: 'Reading mode: tell me which book, like "I\'m reading Dune", and your page.', noBook: 'First tell me which book: "I\'m reading …".' },
  fr: { start: 'Noté : tu lis « {book} ». Dis-moi ta page et je te résume sans spoilers quand tu veux.', page: 'Page {n} de « {book} ». Demande-moi un résumé quand tu veux.', done: 'Tu as fini « {book} » ! Tu en as pensé quoi ?', on: 'Mode lecture : je te suis dans « {book} », page {n}. Demande-moi un résumé ou un personnage.', none: 'Mode lecture : dis-moi quel livre, par exemple « je lis Dune », et ta page.', noBook: 'Dis-moi d\'abord quel livre : « je lis … ».' },
  pt: { start: 'Anotado: você está lendo «{book}». Me diga a página e eu resumo sem spoilers quando quiser.', page: 'Página {n} de «{book}». Peça um resumo do que já leu quando quiser.', done: 'Você terminou «{book}»! O que achou?', on: 'Modo leitura: estou com você em «{book}», página {n}. Peça um resumo ou pergunte de um personagem.', none: 'Modo leitura: me diga o livro, tipo «estou lendo Duna», e a página.', noBook: 'Primeiro me diga o livro: «estou lendo …».' },
  ru: { start: 'Записала: ты читаешь «{book}». Скажи страницу, и я перескажу без спойлеров, когда захочешь.', page: 'Страница {n} книги «{book}». Попроси пересказ, когда захочешь.', done: 'Ты дочитал(а) «{book}»! Как тебе?', on: 'Режим чтения: я с тобой в «{book}», страница {n}. Попроси пересказ или спроси о персонаже.', none: 'Режим чтения: скажи, какую книгу читаешь, например «я читаю Дюну», и страницу.', noBook: 'Сначала скажи, какую книгу: «я читаю …».' },
  vi: { start: 'Đã ghi: bạn đang đọc «{book}». Cho mình biết trang, mình sẽ tóm tắt không spoil khi bạn muốn.', page: 'Trang {n} của «{book}». Muốn tóm tắt thì cứ hỏi mình.', done: 'Bạn đọc xong «{book}» rồi! Thấy sao?', on: 'Chế độ đọc: mình đang cùng bạn ở «{book}», trang {n}. Hỏi mình tóm tắt hoặc về nhân vật nhé.', none: 'Chế độ đọc: nói mình nghe bạn đọc sách gì, ví dụ «tôi đang đọc Dune», và trang mấy.', noBook: 'Trước tiên nói mình biết sách gì: «tôi đang đọc …».' },
  ja: { start: '了解！「{book}」を読んでるんだね。ページを教えてくれたら、ネタバレなしで要約するよ。', page: '「{book}」の{n}ページだね。ここまでの要約がほしかったら言ってね。', done: '「{book}」読み終わったんだ！どうだった？', on: '読書モード：「{book}」の{n}ページで一緒にいるよ。要約やキャラのこと、聞いてね。', none: '読書モード：何の本か教えて。たとえば「デューンを読んでる」、それとページも。', noBook: 'まず何の本か教えて：「〇〇を読んでる」。' },
  de: { start: 'Notiert: Du liest „{book}“. Sag mir deine Seite und ich fasse ohne Spoiler zusammen, wann du willst.', page: 'Seite {n} von „{book}“. Frag mich jederzeit nach einer Zusammenfassung.', done: 'Du hast „{book}“ fertig! Wie fandest du es?', on: 'Lesemodus: Ich bin bei dir in „{book}“, Seite {n}. Frag nach einer Zusammenfassung oder einer Figur.', none: 'Lesemodus: Sag mir, welches Buch, z. B. „ich lese Dune“, und deine Seite.', noBook: 'Sag mir zuerst, welches Buch: „ich lese …“.' },
  it: { start: 'Segnato: stai leggendo «{book}». Dimmi la pagina e ti faccio un riassunto senza spoiler quando vuoi.', page: 'Pagina {n} di «{book}». Chiedimi un riassunto quando vuoi.', done: 'Hai finito «{book}»! Che ne pensi?', on: 'Modalità lettura: sono con te in «{book}», pagina {n}. Chiedimi un riassunto o di un personaggio.', none: 'Modalità lettura: dimmi che libro, tipo «sto leggendo Dune», e la pagina.', noBook: 'Prima dimmi che libro: «sto leggendo …».' },
};

const load = () => { try { return JSON.parse(localStorage.getItem(KEY) || 'null'); } catch { return null; } };
const save = (r) => { try { r ? localStorage.setItem(KEY, JSON.stringify(r)) : localStorage.removeItem(KEY); } catch {} };
const fill = (line, r) => line.replace('{book}', r?.book || '').replace('{n}', r?.page || '?');

export function reading(dev) {
  const chat = dev.chat;
  if (chat?.personal) {
    const personal = chat.personal.bind(chat);
    chat.personal = () => {
      const lines = personal();
      const r = load();
      if (r?.book) lines.push(`The user is reading the book "${r.book}"${r.page ? `, currently on page ${r.page}` : ''}. When they ask what has happened so far, for a recap, or about a character or event, answer only with what happens up to ${r.page ? `page ${r.page}` : 'where they are'}, and never reveal anything later in the book (no spoilers). If you don't know the book well, say so honestly instead of making things up.`);
      return lines;
    };
  }

  /** What she does for this, or null: { line, face } (asks.js). */
  function ask(said, lang) {
    const t = LINES[lang] || LINES.en;
    const text = String(said || '').trim();
    const started = START.exec(text) || START_JA.exec(text);
    if (started && !PAGE.test(text)) {
      const book = started[1].replace(/\s+/g, ' ').trim();
      const r = { book, page: 0, at: Date.now() };
      save(r);
      return { line: fill(t.start, r), face: 'laugh' };
    }
    const paged = PAGE.exec(text) || PAGE_JA.exec(text);
    if (paged) {
      const r = load();
      if (!r?.book) return { line: t.noBook, face: 'aside' };
      r.page = Number(paged[1]);
      r.at = Date.now();
      save(r);
      return { line: fill(t.page, r), face: 'smug' };
    }
    if (DONE.test(text)) {
      const r = load();
      if (!r?.book) return null;
      save(null);
      return { line: fill(t.done, r), face: 'laugh' };
    }
    if (MODE.test(text) && text.split(/\s+/).length <= 8) {
      const r = load();
      return { line: r?.book ? fill(t.on, r) : t.none, face: 'smug' };
    }
    return null;
  }

  return { ask, now: load };
}

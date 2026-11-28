// (1.13.0) The community store of her skins, in her Vestuario screen (closet.html): outfits people
// drew for her, approved by Emma, from the website (/api/skins). The list only shows small previews,
// loaded as you scroll to them; a skin's whole picture is downloaded only when you add it (by the
// app: Skins.kt checks it and keeps it), and then it's one more outfit in her wardrobe (wardrobe.js,
// "De la comunidad"). Each one says who drew it and whether with AI or by hand, and has hearts: one
// per installation (an id made up for that, not the phone's). Sorted by newest or by most hearts.
//
// Kotlin (ClosetActivity) gives, on `window.CapriCloset`: skins() the ones on this phone, skinAdd(entry)
// (answered by window.__capriSkinDone(id, error)), skinDrop(id), device(), test() (a beta: also the
// store's test skins).
import { textIn } from './wardrobe.js';

const SITE = 'https://capri-web-sdkn.vercel.app';

const WORDS = {
  es: { open: 'Tienda comunitaria', title: 'Tienda comunitaria', close: 'Cerrar', top: 'Más votadas', new: 'Más recientes', note: 'Skins hechas por la comunidad. Solo se descarga la que añades.', by: 'por', ia: 'Hecha con IA', mano: 'Dibujada a mano', add: 'Añadir', adding: 'Bajando…', wear: 'Ponérsela', worn: 'Puesta', drop: 'Quitar', empty: 'Aún no hay skins. ¡Pronto!', error: 'No se pudo abrir la tienda. Revisa tu conexión.', retry: 'Reintentar', failed: 'No se pudo bajar. Prueba otra vez.', hearts: 'corazones', mine: 'En tu vestuario', send: '¿Dibujaste una? Mándala en el Discord de Capri.', all: 'Todas', added: 'Añadida a su vestuario', heartsOff: 'Los corazones no están disponibles ahorita. Inténtalo más tarde.', heartFail: 'No se pudo guardar tu corazón.', noneHere: 'No hay skins en esta categoría todavía.', details: 'Ver', back: 'Volver', count: 'skins' },
  en: { open: 'Community store', title: 'Community store', close: 'Close', top: 'Most loved', new: 'Newest', note: 'Skins made by the community. Only the one you add is downloaded.', by: 'by', ia: 'Made with AI', mano: 'Hand-drawn', add: 'Add', adding: 'Downloading…', wear: 'Put it on', worn: 'Wearing', drop: 'Remove', empty: 'No skins yet. Soon!', error: 'Couldn’t open the store. Check your connection.', retry: 'Try again', failed: 'Couldn’t download it. Try again.', hearts: 'hearts', mine: 'In your wardrobe', send: 'Drew one? Send it on Capri’s Discord.', all: 'All', added: 'Added to her wardrobe', heartsOff: 'Hearts aren’t available right now. Try again later.', heartFail: 'Couldn’t save your heart.', noneHere: 'No skins in this category yet.', details: 'View', back: 'Back', count: 'skins' },
  fr: { open: 'Boutique communautaire', title: 'Boutique communautaire', close: 'Fermer', top: 'Les plus aimées', new: 'Les plus récentes', note: 'Des skins faites par la communauté. Seule celle que tu ajoutes est téléchargée.', by: 'par', ia: 'Faite avec l’IA', mano: 'Dessinée à la main', add: 'Ajouter', adding: 'Téléchargement…', wear: 'La lui mettre', worn: 'Portée', drop: 'Retirer', empty: 'Pas encore de skins. Bientôt !', error: 'Impossible d’ouvrir la boutique. Vérifie ta connexion.', retry: 'Réessayer', failed: 'Téléchargement impossible. Réessaie.', hearts: 'cœurs', mine: 'Dans ta garde-robe', send: 'Tu en as dessiné une ? Envoie-la sur le Discord de Capri.', all: 'Toutes', added: 'Ajoutée à sa garde-robe', heartsOff: 'Les cœurs ne sont pas disponibles pour l’instant. Réessaie plus tard.', heartFail: 'Impossible d’enregistrer ton cœur.', noneHere: 'Pas encore de skins dans cette catégorie.', details: 'Voir', back: 'Retour', count: 'skins' },
  pt: { open: 'Loja da comunidade', title: 'Loja da comunidade', close: 'Fechar', top: 'Mais votadas', new: 'Mais recentes', note: 'Skins feitas pela comunidade. Só a que você adiciona é baixada.', by: 'por', ia: 'Feita com IA', mano: 'Desenhada à mão', add: 'Adicionar', adding: 'Baixando…', wear: 'Vestir', worn: 'Vestida', drop: 'Remover', empty: 'Ainda não há skins. Em breve!', error: 'Não deu para abrir a loja. Confira sua conexão.', retry: 'Tentar de novo', failed: 'Não deu para baixar. Tente de novo.', hearts: 'corações', mine: 'No seu guarda-roupa', send: 'Desenhou uma? Mande no Discord da Capri.', all: 'Todas', added: 'Adicionada ao guarda-roupa dela', heartsOff: 'Os corações não estão disponíveis agora. Tente mais tarde.', heartFail: 'Não deu para salvar seu coração.', noneHere: 'Ainda não há skins nesta categoria.', details: 'Ver', back: 'Voltar', count: 'skins' },
  ru: { open: 'Магазин сообщества', title: 'Магазин сообщества', close: 'Закрыть', top: 'Самые любимые', new: 'Новые', note: 'Скины от сообщества. Скачивается только тот, что ты добавишь.', by: 'автор', ia: 'Сделано с ИИ', mano: 'Нарисовано от руки', add: 'Добавить', adding: 'Скачиваю…', wear: 'Надеть', worn: 'Надето', drop: 'Удалить', empty: 'Скинов пока нет. Скоро!', error: 'Не удалось открыть магазин. Проверь подключение.', retry: 'Ещё раз', failed: 'Не удалось скачать. Попробуй ещё раз.', hearts: 'сердец', mine: 'В твоём гардеробе', send: 'Нарисовал(а) скин? Отправь его в Discord Капри.', all: 'Все', added: 'Добавлено в её гардероб', heartsOff: 'Сердца сейчас недоступны. Попробуй позже.', heartFail: 'Не удалось сохранить сердце.', noneHere: 'В этой категории пока нет скинов.', details: 'Открыть', back: 'Назад', count: 'скинов' },
  vi: { open: 'Cửa hàng cộng đồng', title: 'Cửa hàng cộng đồng', close: 'Đóng', top: 'Được yêu thích nhất', new: 'Mới nhất', note: 'Skin do cộng đồng làm. Chỉ tải skin bạn thêm.', by: 'bởi', ia: 'Làm bằng AI', mano: 'Vẽ tay', add: 'Thêm', adding: 'Đang tải…', wear: 'Mặc cho cô ấy', worn: 'Đang mặc', drop: 'Gỡ', empty: 'Chưa có skin nào. Sắp có!', error: 'Không mở được cửa hàng. Kiểm tra kết nối.', retry: 'Thử lại', failed: 'Không tải được. Thử lại nhé.', hearts: 'tim', mine: 'Trong tủ đồ của bạn', send: 'Bạn vẽ một skin? Gửi lên Discord của Capri.', all: 'Tất cả', added: 'Đã thêm vào tủ đồ', heartsOff: 'Hiện chưa thả tim được. Thử lại sau nhé.', heartFail: 'Không lưu được tim của bạn.', noneHere: 'Chưa có skin trong mục này.', details: 'Xem', back: 'Quay lại', count: 'skin' },
  ja: { open: 'コミュニティストア', title: 'コミュニティストア', close: '閉じる', top: '人気順', new: '新着順', note: 'コミュニティが作ったスキン。追加したものだけダウンロードされます。', by: '作:', ia: 'AIで作成', mano: '手描き', add: '追加', adding: 'ダウンロード中…', wear: '着せる', worn: '着用中', drop: '削除', empty: 'まだスキンはありません。お楽しみに！', error: 'ストアを開けませんでした。接続を確認してね。', retry: '再試行', failed: 'ダウンロードできませんでした。もう一度試してね。', hearts: 'ハート', mine: 'ワードローブにあり', send: '描いた？ CapriのDiscordで送ってね。', all: 'すべて', added: 'ワードローブに追加しました', heartsOff: '今はハートを使えません。あとで試してね。', heartFail: 'ハートを保存できませんでした。', noneHere: 'このカテゴリにはまだスキンがありません。', details: '見る', back: '戻る', count: '件' },
  de: { open: 'Community-Shop', title: 'Community-Shop', close: 'Schließen', top: 'Beliebteste', new: 'Neueste', note: 'Skins aus der Community. Nur der, den du hinzufügst, wird geladen.', by: 'von', ia: 'Mit KI gemacht', mano: 'Handgezeichnet', add: 'Hinzufügen', adding: 'Wird geladen…', wear: 'Anziehen', worn: 'Getragen', drop: 'Entfernen', empty: 'Noch keine Skins. Bald!', error: 'Der Shop ließ sich nicht öffnen. Prüf deine Verbindung.', retry: 'Nochmal', failed: 'Download fehlgeschlagen. Versuch es nochmal.', hearts: 'Herzen', mine: 'In deinem Kleiderschrank', send: 'Einen gezeichnet? Schick ihn auf Capris Discord.', all: 'Alle', added: 'Zu ihrem Kleiderschrank hinzugefügt', heartsOff: 'Herzen sind gerade nicht verfügbar. Versuch es später.', heartFail: 'Dein Herz konnte nicht gespeichert werden.', noneHere: 'Noch keine Skins in dieser Kategorie.', details: 'Ansehen', back: 'Zurück', count: 'Skins' },
  it: { open: 'Negozio della community', title: 'Negozio della community', close: 'Chiudi', top: 'Più votate', new: 'Più recenti', note: 'Skin fatte dalla community. Si scarica solo quella che aggiungi.', by: 'di', ia: 'Fatta con l’IA', mano: 'Disegnata a mano', add: 'Aggiungi', adding: 'Scarico…', wear: 'Mettigliela', worn: 'Indossata', drop: 'Rimuovi', empty: 'Ancora nessuna skin. Presto!', error: 'Impossibile aprire il negozio. Controlla la connessione.', retry: 'Riprova', failed: 'Download non riuscito. Riprova.', hearts: 'cuori', mine: 'Nel tuo guardaroba', send: 'Ne hai disegnata una? Mandala sul Discord di Capri.', all: 'Tutte', added: 'Aggiunta al suo guardaroba', heartsOff: 'I cuori non sono disponibili ora. Riprova più tardi.', heartFail: 'Impossibile salvare il tuo cuore.', noneHere: 'Ancora nessuna skin in questa categoria.', details: 'Vedi', back: 'Indietro', count: 'skin' },
};

export function store({ bridge, closet, lang }) {
  const w = WORDS[lang] || WORDS.en;
  const mine = () => {
    try { return JSON.parse(bridge?.skins() || '[]') || []; } catch { return []; }
  };
  const device = (() => { try { return bridge?.device() || ''; } catch { return ''; } })();
  const test = (() => { try { return !!bridge?.test(); } catch { return false; } })();
  // (1.13.0b, Emma: "los filtros no sirven… necesitamos mejorar la experiencia") the list comes once; the order
  // and the category are chosen here, at once, with no wait
  let sort = 'new';
  let category = '';
  let skins = null; // the store's list (null: not yet / failed)
  let cats = [];
  let votes = true;
  let open1 = null; // the skin open in the detail sheet
  const busy = new Set(); // being downloaded
  const failed = new Set();

  // the button that opens it, at the top of her sheet's bar
  const opener = document.createElement('button');
  opener.type = 'button';
  opener.id = 'store-open';
  opener.textContent = `♥ ${w.open}`;
  opener.addEventListener('click', open);

  const panel = document.createElement('section');
  panel.className = 'store';
  panel.hidden = true;
  panel.setAttribute('role', 'dialog');
  panel.setAttribute('aria-label', w.title);
  panel.innerHTML = `
    <header class="store__head"><span class="store__title"></span><button type="button" class="store__close"></button></header>
    <p class="store__note"></p>
    <nav class="store__sort" role="tablist"><button type="button" role="tab" data-sort="new"></button><button type="button" role="tab" data-sort="top"></button></nav>
    <nav class="store__cats" aria-label="categories"></nav>
    <div class="store__list" aria-live="polite"></div>
    <div class="store__sheet" hidden><div class="store__shade"></div><article class="store__detail" role="dialog"></article></div>
    <p class="store__toast" role="status" hidden></p>`;
  panel.querySelector('.store__title').textContent = `// ${w.title}`;
  const closeBtn = panel.querySelector('.store__close');
  closeBtn.textContent = w.close;
  closeBtn.addEventListener('click', close);
  panel.querySelector('.store__note').textContent = w.note;
  for (const b of panel.querySelectorAll('[data-sort]')) {
    b.textContent = w[b.dataset.sort];
    b.addEventListener('click', () => { sort = b.dataset.sort; render(); list.scrollTop = 0; });
  }
  const catsEl = panel.querySelector('.store__cats');
  const list = panel.querySelector('.store__list');
  const sheet = panel.querySelector('.store__sheet');
  const detail = panel.querySelector('.store__detail');
  const toastEl = panel.querySelector('.store__toast');
  sheet.querySelector('.store__shade').addEventListener('click', closeDetail);
  document.body.append(panel);

  let toastTimer = 0;
  function toast(text) {
    toastEl.textContent = text;
    toastEl.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { toastEl.hidden = true; }, 2600);
  }

  function open() {
    panel.hidden = false;
    document.body.classList.add('is-shopping');
    if (!skins) load();
    else render();
  }
  function close() {
    closeDetail();
    panel.hidden = true;
    document.body.classList.remove('is-shopping');
    closet?.open?.(); // (her sheet again, with what you added or put on her)
  }

  let asked = 0;
  async function load() {
    const n = ++asked;
    list.replaceChildren(message('…'));
    try {
      const q = new URLSearchParams({ sort: 'new', lang, device });
      if (test) q.set('test', '1');
      const r = await fetch(`${SITE}/api/skins?${q}`, { cache: 'no-store' });
      if (!r.ok) throw new Error(`http ${r.status}`);
      const body = await r.json();
      if (n !== asked) return;
      skins = Array.isArray(body.skins) ? body.skins : [];
      cats = Array.isArray(body.categories) ? body.categories : [];
      votes = body.votes !== false;
      if (category && !cats.some((c) => c.id === category)) category = '';
      render();
    } catch (e) {
      if (n !== asked) return;
      bridge?.log?.(`store: ${e}`);
      skins = null;
      const m = message(w.error);
      m.append(document.createElement('br'), button(w.retry, load));
      list.replaceChildren(m);
    }
  }
  function message(text) {
    const p = document.createElement('p');
    p.className = 'store__msg';
    p.textContent = text;
    return p;
  }

  const shown = () => {
    const out = skins.filter((s) => !category || (s.category || 'otros') === category);
    return out.sort((a, b) => (sort === 'top'
      ? (b.hearts || 0) - (a.hearts || 0) || String(b.added).localeCompare(String(a.added))
      : String(b.added).localeCompare(String(a.added)) || (b.hearts || 0) - (a.hearts || 0)));
  };

  function render() {
    for (const b of panel.querySelectorAll('[data-sort]')) {
      b.classList.toggle('is-on', b.dataset.sort === sort);
      b.setAttribute('aria-selected', String(b.dataset.sort === sort));
    }
    if (!skins) return;
    // the categories: "All" and the ones that have skins
    catsEl.replaceChildren(...[{ id: '', name: w.all, count: skins.length }, ...cats].map((c) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'store__chip';
      b.classList.toggle('is-on', category === c.id);
      b.textContent = `${c.name} · ${c.count}`;
      b.addEventListener('click', () => { category = c.id; render(); list.scrollTop = 0; });
      return b;
    }));
    catsEl.hidden = !cats.length;
    if (!skins.length) return list.replaceChildren(message(w.empty));
    const items = shown();
    if (!items.length) return list.replaceChildren(message(w.noneHere));
    const have = new Set(mine().map((e) => e.id));
    const wearing = closet?.look?.().outfit;
    list.replaceChildren(...items.map((s) => card(s, have.has(s.id), wearing === `c-${s.id}`)));
    if (open1) showDetail(open1);
  }

  const howText = (s) => (s.method === 'mano' ? `✎ ${w.mano}` : `✦ ${w.ia}`);
  const catName = (s) => cats.find((c) => c.id === (s.category || 'otros'))?.name || '';

  function heartButton(s, big = false) {
    const heart = document.createElement('button');
    heart.type = 'button';
    heart.className = big ? 'store__heart is-big' : 'store__heart';
    const paint = () => {
      heart.classList.toggle('is-on', !!s.mine);
      heart.classList.toggle('is-off', !votes);
      heart.textContent = `${s.mine ? '♥' : '♡'} ${s.hearts || 0}`;
      heart.setAttribute('aria-label', `${s.hearts || 0} ${w.hearts}`);
      heart.setAttribute('aria-pressed', String(!!s.mine));
    };
    paint();
    heart.addEventListener('click', async (ev) => {
      ev.stopPropagation();
      if (!votes || !device) return toast(w.heartsOff);
      if (heart.disabled) return;
      const on = !s.mine;
      s.mine = on; // (at once; set right by the answer)
      s.hearts = Math.max(0, (s.hearts || 0) + (on ? 1 : -1));
      paint();
      heart.classList.remove('is-pop');
      void heart.offsetWidth;
      if (on) heart.classList.add('is-pop');
      heart.disabled = true;
      try {
        const r = await fetch(`${SITE}/api/skins/heart`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ id: s.id, device, on }) });
        if (!r.ok) throw new Error(`http ${r.status}`);
        const out = await r.json();
        s.hearts = out.hearts;
        s.mine = out.mine;
      } catch (e) {
        bridge?.log?.(`store heart: ${e}`);
        s.mine = !on;
        s.hearts = Math.max(0, (s.hearts || 0) + (on ? -1 : 1));
        toast(w.heartFail);
      }
      heart.disabled = false;
      render(); // (its place in "most loved", and its count everywhere)
    });
    return heart;
  }

  function actions(s, added, worn) {
    const row = document.createElement('div');
    row.className = 'store__actions';
    if (busy.has(s.id)) row.append(button(w.adding, null, true, 'is-main'));
    else if (!added) row.append(button(w.add, () => add(s), false, 'is-main'));
    else {
      row.append(worn ? button(`✓ ${w.worn}`, null, true) : button(w.wear, () => wear(s), false, 'is-main'));
      row.append(button(w.drop, () => drop(s)));
    }
    return row;
  }

  function card(s, added, worn) {
    const el = document.createElement('article');
    el.className = 'store__card';
    el.classList.toggle('is-mine', added);
    el.tabIndex = 0;
    const pic = document.createElement('img');
    pic.className = 'store__pic';
    pic.alt = '';
    pic.width = 120;
    pic.height = 210;
    pic.loading = 'lazy'; // (only the ones you scroll to)
    pic.decoding = 'async';
    pic.src = s.preview;
    const frame = document.createElement('div');
    frame.className = 'store__frame';
    frame.append(pic);
    if (added) {
      const tag = document.createElement('span');
      tag.className = 'store__tag';
      tag.textContent = worn ? `✓ ${w.worn}` : '✓';
      frame.append(tag);
    }
    const name = document.createElement('h3');
    name.className = 'store__name';
    name.textContent = textIn(s.name, lang) || s.id;
    const by = document.createElement('p');
    by.className = 'store__by';
    by.textContent = `${w.by} ${s.author || '?'}`;
    const how = document.createElement('span');
    how.className = `store__how is-${s.method === 'mano' ? 'mano' : 'ia'}`;
    how.textContent = howText(s);
    const foot = document.createElement('div');
    foot.className = 'store__foot';
    foot.append(how, heartButton(s));
    el.append(frame, name, by, foot, actions(s, added, worn));
    el.addEventListener('click', (ev) => { if (!ev.target.closest('button')) showDetail(s); });
    return el;
  }

  // a skin, big: its preview, who drew it and how, its hearts and what you can do with it
  function showDetail(s) {
    open1 = s;
    const have = new Set(mine().map((e) => e.id));
    const added = have.has(s.id);
    const worn = closet?.look?.().outfit === `c-${s.id}`;
    const pic = document.createElement('img');
    pic.className = 'store__big';
    pic.alt = '';
    pic.src = s.preview;
    const info = document.createElement('div');
    info.className = 'store__info';
    const name = document.createElement('h3');
    name.className = 'store__name is-big';
    name.textContent = textIn(s.name, lang) || s.id;
    const by = document.createElement('p');
    by.className = 'store__by';
    by.textContent = `${w.by} ${s.author || '?'}`;
    const tags = document.createElement('div');
    tags.className = 'store__tags';
    const how = document.createElement('span');
    how.className = `store__how is-${s.method === 'mano' ? 'mano' : 'ia'}`;
    how.textContent = howText(s);
    tags.append(how);
    if (catName(s)) {
      const c = document.createElement('span');
      c.className = 'store__how is-cat';
      c.textContent = catName(s);
      tags.append(c);
    }
    info.append(name, by, tags);
    if (s.desc) {
      const d = document.createElement('p');
      d.className = 'store__desc';
      d.textContent = textIn(s.desc, lang);
      info.append(d);
    }
    if (failed.has(s.id)) {
      const f = document.createElement('p');
      f.className = 'store__fail';
      f.textContent = w.failed;
      info.append(f);
    }
    const row = actions(s, added, worn);
    row.prepend(heartButton(s, true));
    info.append(row);
    const back = button(`‹ ${w.back}`, closeDetail, false, 'store__back');
    detail.replaceChildren(back, pic, info);
    sheet.hidden = false;
  }
  function closeDetail() {
    open1 = null;
    sheet.hidden = true;
  }

  function button(label, click, disabled = false, cls = '') {
    const b = document.createElement('button');
    b.type = 'button';
    b.className = `store__btn ${cls}`.trim();
    b.textContent = label;
    b.disabled = disabled;
    if (click) b.addEventListener('click', (ev) => { ev.stopPropagation(); click(); });
    return b;
  }

  // the app downloads it (and checks it); it answers here
  const waiting = new Map();
  window.__capriSkinDone = (id, error) => {
    waiting.get(id)?.(error || null);
    waiting.delete(id);
  };
  async function add(s) {
    busy.add(s.id);
    failed.delete(s.id);
    render();
    const error = await new Promise((resolve) => {
      waiting.set(s.id, resolve);
      try { bridge.skinAdd(JSON.stringify(s)); } catch (e) { resolve(String(e)); }
    });
    busy.delete(s.id);
    if (error) {
      bridge?.log?.(`store add ${s.id}: ${error}`);
      failed.add(s.id);
      toast(w.failed);
    } else {
      await closet?.refresh?.();
      toast(`✓ ${w.added}`);
    }
    render();
  }
  function wear(s) {
    closet?.wear({ outfit: `c-${s.id}` });
    render();
  }
  async function drop(s) {
    try { bridge.skinDrop(s.id); } catch {}
    await closet?.refresh?.();
    render();
  }

  // Android's back: the detail first, then the store
  const back = () => {
    if (!sheet.hidden) return closeDetail(), true;
    if (!panel.hidden) return close(), true;
    return false;
  };
  return { opener, open, close, back, get isOpen() { return !panel.hidden; } };
}

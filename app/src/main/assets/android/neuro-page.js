// Her neurodanzas, played back (1.11.0, neuro.html). See neuro.js for how she records them.

const bridge = window.CapriPage;
const info = (() => { try { return JSON.parse(bridge?.info?.() || '{}'); } catch { return {}; } })();
const LANGS = ['es', 'en', 'fr', 'pt', 'ru', 'vi', 'ja', 'de', 'it'];
const lang = LANGS.includes(info.lang) ? info.lang : 'es';
const $ = (id) => document.getElementById(id);
const esc = (s) => String(s ?? '').replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const ANIMS = '../capri/assets/capri/anims/';
const SCENE_MS = 5200;

const W = {
  es: { h1: 'Neurodanzas', intro: 'Recuerdos de Capri, grabados para ti. Mira cada escena, cambia a la capa térmica para encontrar detalles escondidos y dile qué te pareció.', none: 'Todavía no te ha grabado ninguna.<br>Dale mucho cariño (acaríciala, platica con ella) y te grabará una.', fresh: 'NUEVA', scenes: '{n} escenas', visual: 'Visual', thermal: 'Térmica', audio: 'Audio', clue: 'DETALLE OCULTO', clues: 'Detalles {n}/{t}', says: 'Capri opina', ask: '¿Y a ti qué te pareció?', r3: '❤ Me encantó', r2: '🙂 Bien', r1: '👎 No me gustó', thanks: '¡Se lo diré a Capri!', own: 'de su archivo' },
  en: { h1: 'Braindances', intro: 'Capri’s memories, recorded for you. Watch each scene, switch to the thermal layer to find hidden details, and tell her what you thought.', none: 'She hasn’t recorded one for you yet.<br>Give her lots of love (pet her, talk to her) and she will.', fresh: 'NEW', scenes: '{n} scenes', visual: 'Visual', thermal: 'Thermal', audio: 'Audio', clue: 'HIDDEN DETAIL', clues: 'Details {n}/{t}', says: 'Capri’s take', ask: 'And what did you think?', r3: '❤ Loved it', r2: '🙂 Okay', r1: '👎 Didn’t like it', thanks: 'I’ll tell Capri!', own: 'from her archive' },
  fr: { h1: 'Neurodanses', intro: 'Les souvenirs de Capri, enregistrés pour toi. Regarde chaque scène, passe en couche thermique pour trouver les détails cachés, et dis-lui ce que tu en as pensé.', none: 'Elle ne t’en a pas encore enregistré.<br>Donne-lui beaucoup d’affection (caresse-la, parle-lui) et elle le fera.', fresh: 'NOUVELLE', scenes: '{n} scènes', visual: 'Visuel', thermal: 'Thermique', audio: 'Audio', clue: 'DÉTAIL CACHÉ', clues: 'Détails {n}/{t}', says: 'L’avis de Capri', ask: 'Et toi, tu en as pensé quoi ?', r3: '❤ J’ai adoré', r2: '🙂 Bien', r1: '👎 Pas aimé', thanks: 'Je le dis à Capri !', own: 'de ses archives' },
  pt: { h1: 'Neurodanças', intro: 'Lembranças da Capri, gravadas pra você. Veja cada cena, mude pra camada térmica pra achar detalhes escondidos e diga o que achou.', none: 'Ela ainda não gravou nenhuma pra você.<br>Dê muito carinho (faça carinho, converse com ela) e ela grava.', fresh: 'NOVA', scenes: '{n} cenas', visual: 'Visual', thermal: 'Térmica', audio: 'Áudio', clue: 'DETALHE ESCONDIDO', clues: 'Detalhes {n}/{t}', says: 'A Capri acha', ask: 'E você, o que achou?', r3: '❤ Amei', r2: '🙂 Boa', r1: '👎 Não gostei', thanks: 'Vou contar pra Capri!', own: 'do arquivo dela' },
  ru: { h1: 'Нейротанцы', intro: 'Воспоминания Капри, записанные для тебя. Смотри каждую сцену, включай тепловой слой, чтобы найти скрытые детали, и скажи ей, как тебе.', none: 'Она ещё ничего тебе не записала.<br>Дай ей много ласки (гладь её, говори с ней), и она запишет.', fresh: 'НОВЫЙ', scenes: 'сцен: {n}', visual: 'Видео', thermal: 'Тепло', audio: 'Звук', clue: 'СКРЫТАЯ ДЕТАЛЬ', clues: 'Детали {n}/{t}', says: 'Мнение Капри', ask: 'А тебе как?', r3: '❤ Обожаю', r2: '🙂 Норм', r1: '👎 Не понравилось', thanks: 'Передам Капри!', own: 'из её архива' },
  vi: { h1: 'Braindance', intro: 'Ký ức của Capri, được ghi lại cho bạn. Xem từng cảnh, chuyển sang lớp nhiệt để tìm chi tiết ẩn, và kể cho cô ấy bạn nghĩ gì.', none: 'Cô ấy chưa ghi cái nào cho bạn.<br>Hãy yêu thương cô ấy thật nhiều (vuốt ve, trò chuyện) và cô ấy sẽ ghi.', fresh: 'MỚI', scenes: '{n} cảnh', visual: 'Hình ảnh', thermal: 'Nhiệt', audio: 'Âm thanh', clue: 'CHI TIẾT ẨN', clues: 'Chi tiết {n}/{t}', says: 'Capri nghĩ gì', ask: 'Còn bạn nghĩ sao?', r3: '❤ Rất thích', r2: '🙂 Tạm được', r1: '👎 Không thích', thanks: 'Mình sẽ kể cho Capri!', own: 'từ kho lưu trữ của cô ấy' },
  ja: { h1: 'ブレインダンス', intro: 'あなたのために記録されたカプリの記憶。各シーンを見て、サーマルレイヤーに切り替えて隠れたディテールを探し、感想を伝えてあげて。', none: 'まだ記録してくれていないみたい。<br>たくさん愛情をあげて（なでたり、話したり）。そうすれば記録してくれるよ。', fresh: 'NEW', scenes: '{n}シーン', visual: 'ビジュアル', thermal: 'サーマル', audio: 'オーディオ', clue: '隠れたディテール', clues: 'ディテール {n}/{t}', says: 'カプリの感想', ask: 'あなたはどう思った？', r3: '❤ 大好き', r2: '🙂 まあまあ', r1: '👎 好きじゃない', thanks: 'カプリに伝えておくね！', own: '彼女のアーカイブから' },
  de: { h1: 'Braindances', intro: 'Capris Erinnerungen, für dich aufgenommen. Sieh dir jede Szene an, schalte auf die Wärmeebene, um versteckte Details zu finden, und sag ihr, was du denkst.', none: 'Sie hat noch keinen für dich aufgenommen.<br>Gib ihr viel Liebe (streichle sie, rede mit ihr), dann tut sie es.', fresh: 'NEU', scenes: '{n} Szenen', visual: 'Visuell', thermal: 'Wärme', audio: 'Audio', clue: 'VERSTECKTES DETAIL', clues: 'Details {n}/{t}', says: 'Capris Meinung', ask: 'Und was hast du gedacht?', r3: '❤ Geliebt', r2: '🙂 Ganz okay', r1: '👎 Nicht gemocht', thanks: 'Ich sag’s Capri!', own: 'aus ihrem Archiv' },
  it: { h1: 'Braindance', intro: 'I ricordi di Capri, registrati per te. Guarda ogni scena, passa allo strato termico per trovare i dettagli nascosti e dille cosa ne pensi.', none: 'Non ne ha ancora registrato uno per te.<br>Dalle tanto affetto (accarezzala, parlale) e lo farà.', fresh: 'NUOVO', scenes: '{n} scene', visual: 'Visivo', thermal: 'Termico', audio: 'Audio', clue: 'DETTAGLIO NASCOSTO', clues: 'Dettagli {n}/{t}', says: 'Il parere di Capri', ask: 'E tu cosa ne pensi?', r3: '❤ Adorato', r2: '🙂 Carino', r1: '👎 Non mi è piaciuto', thanks: 'Lo dirò a Capri!', own: 'dal suo archivio' },
}[lang];

const list = (() => { try { return JSON.parse(bridge?.neuroList?.() || '[]'); } catch { return []; } })();
let meta = {};
fetch(`${ANIMS}anims.json`).then((r) => r.json()).then((j) => { meta = j; if (cur) show(at); }).catch(() => {});

$('h1').textContent = W.h1;
$('intro').textContent = W.intro;
document.documentElement.lang = lang;
for (const b of $('layers').children) b.textContent = W[b.dataset.layer];
for (const b of $('rate').children) b.textContent = W[`r${b.dataset.r}`];
$('vTitle').textContent = W.says;
$('vAsk').textContent = W.ask;
$('wave').innerHTML = Array.from({ length: 28 }, (_, i) => `<i style="animation-delay:${(i * 73) % 700}ms"></i>`).join('');

function drawList() {
  const out = $('list');
  if (!list.length) { out.innerHTML = `<p class="empty">${W.none}</p>`; return; }
  const day = new Intl.DateTimeFormat(lang, { day: 'numeric', month: 'short' });
  out.innerHTML = list.map((bd, i) => `<button class="bd cut" data-i="${i}">
    <span class="bd__chip cut">BD ${String(list.length - i).padStart(2, '0')}</span>
    <span class="bd__txt"><div class="bd__title">${esc(bd.title)}</div>
    <div class="bd__meta">${esc(day.format(new Date(Number(bd.at) || Date.now())))} · ${esc(W.scenes.replace('{n}', (bd.scenes || []).length))}${bd.rating ? ` · ${['', '👎', '🙂', '❤'][bd.rating]}` : ''}</div></span>
    ${bd.seen ? '' : `<span class="bd__new">${esc(W.fresh)}</span>`}</button>`).join('');
  out.querySelectorAll('.bd').forEach((b) => b.addEventListener('click', () => open(list[Number(b.dataset.i)])));
}

// ---- the player ----
let cur = null;
let at = 0;
let playing = true;
let started = 0;
let timer = 0;
let typing = 0;
let found = new Set();
let rated = 0;

function open(bd) {
  cur = bd;
  at = 0;
  found = new Set();
  rated = 0;
  playing = true;
  started = Date.now();
  document.body.classList.add('is-playing');
  $('rec').textContent = `BD · ${bd.title}`;
  $('verdict').classList.remove('is-on');
  layer('visual');
  const tl = $('timeline');
  tl.querySelectorAll('.timeline__mark').forEach((m) => m.remove());
  const n = bd.scenes.length;
  bd.scenes.forEach((s, i) => {
    const m = document.createElement('div');
    m.className = `timeline__mark${s.hidden ? ' is-clue' : ''}`;
    m.style.left = `${n === 1 ? 0 : (i / (n - 1)) * 100}%`;
    m.addEventListener('click', () => { at = i; show(i); });
    tl.append(m);
  });
  window.scrollTo(0, 0);
  show(0);
}

function show(i) {
  if (!cur) return;
  const scenes = cur.scenes;
  at = Math.max(0, Math.min(scenes.length - 1, i));
  const s = scenes[at];
  act(s.act);
  // her line, typed out
  clearInterval(typing);
  const cap = $('caption');
  const text = String(s.text || '');
  let k = 0;
  cap.textContent = '';
  typing = setInterval(() => { k += 2; cap.textContent = text.slice(0, k); if (k >= text.length) clearInterval(typing); }, 30);
  $('clue').dataset.label = W.clue;
  $('clue').textContent = s.hidden || '';
  $('clue').classList.toggle('has', Boolean(s.hidden));
  $('ping').classList.toggle('has', Boolean(s.hidden) && !found.has(at));
  if (s.hidden && $('screen').classList.contains('is-thermal')) found.add(at);
  clues();
  const n = scenes.length;
  $('fill').style.width = `${n === 1 ? 100 : (at / (n - 1)) * 100}%`;
  $('timeline').querySelectorAll('.timeline__mark').forEach((m, j) => m.classList.toggle('is-done', j <= at));
  clearTimeout(timer);
  if (playing) timer = setTimeout(step, SCENE_MS);
}

function step() {
  if (!cur) return;
  if (at < cur.scenes.length - 1) show(at + 1);
  else end();
}

function end() {
  playing = false;
  $('play').textContent = '↺';
  $('vText').textContent = cur.opinion || '…';
  $('verdict').classList.add('is-on');
  setTimeout(() => $('verdict').scrollIntoView({ behavior: 'smooth', block: 'end' }), 100);
  if (!cur.seen) { cur.seen = true; try { bridge?.neuroSeen?.(String(cur.id), 0); } catch {} }
}

function clues() {
  const total = cur.scenes.filter((s) => s.hidden).length;
  $('clues').textContent = total ? W.clues.replace('{n}', found.size).replace('{t}', total) : '';
}

function layer(name) {
  const sc = $('screen');
  sc.classList.toggle('is-thermal', name === 'thermal');
  sc.classList.toggle('is-audio', name === 'audio');
  for (const b of $('layers').children) b.classList.toggle('is-on', b.dataset.layer === name);
  if (name === 'thermal' && cur?.scenes[at]?.hidden) { found.add(at); $('ping').classList.remove('has'); clues(); try { bridge?.haptic?.(); } catch {} }
}

// her, doing the scene (her animations; the ones that aren't there: thinking)
let nowAct = '';
function act(name) {
  const body = $('body');
  const id = meta[name] ? name : meta.think ? 'think' : '';
  if (!id || id === nowAct) return;
  nowAct = id;
  const m = meta[id];
  const tall = m.kind === 'lie' || m.h < 330 ? 120 : 210;
  const k = tall / m.h;
  const w = Math.round(m.w * k);
  body.getAnimations?.().forEach((a) => a.cancel());
  Object.assign(body.style, { width: `${w}px`, height: `${tall}px`, backgroundImage: `url(${ANIMS}${id}.png)`, backgroundSize: `${w * m.n}px ${tall}px`, backgroundPosition: '0 0' });
  body.animate?.([{ backgroundPosition: '0 0' }, { backgroundPosition: `-${w * m.n}px 0` }], { duration: (m.n / (m.fps || 6)) * 1000, iterations: Infinity, easing: `steps(${m.n})` });
  body.classList.remove('is-new'); void body.offsetWidth; body.classList.add('is-new');
}

setInterval(() => {
  if (!cur) return;
  const s = Math.floor((Date.now() - started) / 1000);
  $('clock').textContent = `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;
}, 500);

$('layers').addEventListener('click', (e) => { const b = e.target.closest('button'); if (b) layer(b.dataset.layer); });
$('prev').addEventListener('click', () => show(at - 1));
$('next').addEventListener('click', () => (at < cur.scenes.length - 1 ? show(at + 1) : end()));
$('play').addEventListener('click', () => {
  if (!cur) return;
  if (!playing && $('verdict').classList.contains('is-on')) { $('verdict').classList.remove('is-on'); playing = true; $('play').textContent = '⏸'; started = Date.now(); return show(0); }
  playing = !playing;
  $('play').textContent = playing ? '⏸' : '▶';
  clearTimeout(timer);
  if (playing) timer = setTimeout(step, SCENE_MS);
});
$('rate').addEventListener('click', (e) => {
  const b = e.target.closest('button');
  if (!b || !cur || rated) return;
  rated = Number(b.dataset.r);
  cur.rating = rated;
  cur.seen = true;
  for (const x of $('rate').children) x.classList.toggle('is-on', x === b);
  $('vAsk').textContent = W.thanks;
  try { bridge?.neuroSeen?.(String(cur.id), rated); bridge?.haptic?.(); } catch {}
});
$('back').addEventListener('click', () => {
  if (document.body.classList.contains('is-playing')) {
    clearTimeout(timer); clearInterval(typing);
    cur = null; nowAct = '';
    document.body.classList.remove('is-playing');
    drawList();
  } else bridge?.done?.();
});

drawList();
// a new one (from her notification): straight into it
if (list[0] && !list[0].seen) open(list[0]);

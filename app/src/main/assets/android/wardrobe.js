// Her wardrobe: what you put on her. Outfits (a whole drawing of her in other clothes, in the same
// pose, so she moves in them as she does in her own), things on top (a crown, cat ears, glasses, a
// halo…), the colors of her mane and her mask, an aura, how see-through she is. Easy to use: one
// sheet, four tabs, a tap puts it on her and a second tap takes it off. Opened from her quick menu
// (holding her) and from her menu.
//
// It all goes into her picture itself (the texture her shader draws, capri-full.png's 240x420), so
// a hat tilts with her head and a dress swings with her legs. Outfit drawings come later (the
// prompts in capri-prompts-1.8.0.zip): until assets/capri/wardrobe/outfit-<id>.png is there, an
// outfit shows a padlock, "soon". Things on top are drawn here, or come from
// assets/capri/wardrobe/acc-<id>.png when that drawing is in.
//
// And one thing that isn't in any list: ask her to marry you (wedding.js).

import { FACES } from '../capri/src/renderer/js/panel.js';

const WARDROBE = new URL('../../assets/capri/wardrobe/', location.href).href;

// ---------- what there is ----------

// [id, es, en, fr, pt, ru]
const OUTFITS = [
  ['default', 'Su traje', 'Her suit', 'Sa tenue', 'O traje dela', 'Её костюм'],
  ['jinete', 'Jinete del Oeste', 'Western jockey', 'Jockey de l’Ouest', 'Jóquei do Oeste', 'Жокей Запада'],
  ['escolar', 'Uniforme escolar', 'School uniform', 'Uniforme scolaire', 'Uniforme escolar', 'Школьная форма'],
  ['piloto', 'Piloto de mecha', 'Mecha pilot', 'Pilote de mecha', 'Piloto de mecha', 'Пилот меха'],
  ['cazadora', 'Cazadora de demonios', 'Demon hunter', 'Chasseuse de démons', 'Caçadora de demônios', 'Охотница на демонов'],
  ['ninja', 'Ninja', 'Ninja', 'Ninja', 'Ninja', 'Ниндзя'],
  ['netrunner', 'Netrunner', 'Netrunner', 'Netrunner', 'Netrunner', 'Нетраннер'],
  ['chaqueta', 'Chaqueta amarilla', 'Yellow jacket', 'Veste jaune', 'Jaqueta amarela', 'Жёлтая куртка'],
  ['gotica', 'Gótica', 'Gothic', 'Gothique', 'Gótica', 'Готика'],
  ['kimono', 'Yukata', 'Yukata', 'Yukata', 'Yukata', 'Юката'],
  ['pijama', 'Pijama', 'Pajamas', 'Pyjama', 'Pijama', 'Пижама'],
  ['bruja', 'Bruja', 'Witch', 'Sorcière', 'Bruxa', 'Ведьма'],
  ['santa', 'Navidad', 'Christmas', 'Noël', 'Natal', 'Рождество'],
  ['novia', 'Vestido de novia', 'Wedding dress', 'Robe de mariée', 'Vestido de noiva', 'Свадебное платье', 'secret'],
];

// where things go on her picture (sprite px): her mask is small and tilted up to the right
const MASK = { x: 122, y: 131, rot: -0.32 };
const ACCESSORIES = [
  // [id, slot, es, en, fr, pt, ru, secret?]
  ['crown', 'head', 'Corona', 'Crown', 'Couronne', 'Coroa', 'Корона'],
  ['catears', 'head', 'Orejas de gato', 'Cat ears', 'Oreilles de chat', 'Orelhas de gato', 'Кошачьи ушки'],
  ['horns', 'head', 'Cuernos', 'Horns', 'Cornes', 'Chifres', 'Рога'],
  ['halo', 'head', 'Aureola', 'Halo', 'Auréole', 'Auréola', 'Нимб'],
  ['bow', 'head', 'Moño', 'Bow', 'Nœud', 'Laço', 'Бант'],
  ['flower', 'head', 'Flor', 'Flower', 'Fleur', 'Flor', 'Цветок'],
  ['santahat', 'head', 'Gorro navideño', 'Santa hat', 'Bonnet de Noël', 'Gorro de Natal', 'Новогодняя шапка'],
  ['witchhat', 'head', 'Sombrero de bruja', 'Witch hat', 'Chapeau de sorcière', 'Chapéu de bruxa', 'Шляпа ведьмы'],
  ['headphones', 'face', 'Audífonos', 'Headphones', 'Casque', 'Fones', 'Наушники'],
  ['glasses', 'face', 'Lentes de sol', 'Sunglasses', 'Lunettes de soleil', 'Óculos escuros', 'Очки'],
  ['visor', 'face', 'Visor', 'Visor', 'Visière', 'Visor', 'Визор'],
  ['scarf', 'neck', 'Bufanda', 'Scarf', 'Écharpe', 'Cachecol', 'Шарф'],
  ['choker', 'neck', 'Gargantilla', 'Choker', 'Ras-de-cou', 'Gargantilha', 'Чокер'],
  ['veil', 'head', 'Velo de novia', 'Bridal veil', 'Voile de mariée', 'Véu de noiva', 'Фата', 'secret'],
  ['bouquet', 'hand', 'Ramo', 'Bouquet', 'Bouquet', 'Buquê', 'Букет', 'secret'],
];

// her mane's and her mask's colors: a hue (0..1), or a gray
const MANES = [
  ['default', null, '#fc0346'], ['violet', 0.76, '#8a3cff'], ['blue', 0.62, '#2a5bff'], ['cyan', 0.5, '#00e5ff'],
  ['gold', 0.12, '#ffc21a'], ['green', 0.36, '#1fe36a'], ['silver', 'gray', '#d7d7e3'], ['shadow', 'dark', '#2a2236'],
];
const MASKS = [
  ['default', null, '#08f467'], ['cyan', 0.5, '#00e5ff'], ['violet', 0.76, '#8a3cff'], ['gold', 0.12, '#ffc21a'], ['white', 'gray', '#f2f2f7'], ['red', 0.98, '#ff1744'],
];
const AURAS = [['none', null], ['red', '#ff1744'], ['green', '#08f467'], ['cyan', '#00e5ff'], ['violet', '#8a3cff'], ['gold', '#ffc21a'], ['white', '#ffffff']];
const ALPHAS = [1, 0.85, 0.7, 0.55, 0.4];

const WORDS = {
  es: { title: 'Vestuario', outfits: 'Conjuntos', things: 'Accesorios', colors: 'Colores', effects: 'Efectos', soon: 'pronto', secret: 'secreto', mane: 'Melena', mask: 'Máscara', aura: 'Aura', alpha: 'Transparencia', gamerAlpha: 'En modo gamer', done: 'Listo', reset: 'Quitar todo', none: 'Nada', tap: 'Toca para ponérselo; otra vez para quitárselo.' },
  en: { title: 'Wardrobe', outfits: 'Outfits', things: 'Accessories', colors: 'Colors', effects: 'Effects', soon: 'soon', secret: 'secret', mane: 'Mane', mask: 'Mask', aura: 'Aura', alpha: 'See-through', gamerAlpha: 'In gamer mode', done: 'Done', reset: 'Take it all off', none: 'None', tap: 'Tap to put it on her; again to take it off.' },
  fr: { title: 'Garde-robe', outfits: 'Tenues', things: 'Accessoires', colors: 'Couleurs', effects: 'Effets', soon: 'bientôt', secret: 'secret', mane: 'Crinière', mask: 'Masque', aura: 'Aura', alpha: 'Transparence', gamerAlpha: 'En mode gamer', done: 'OK', reset: 'Tout enlever', none: 'Rien', tap: 'Touche pour le lui mettre ; encore pour l’enlever.' },
  pt: { title: 'Guarda-roupa', outfits: 'Roupas', things: 'Acessórios', colors: 'Cores', effects: 'Efeitos', soon: 'em breve', secret: 'segredo', mane: 'Juba', mask: 'Máscara', aura: 'Aura', alpha: 'Transparência', gamerAlpha: 'No modo gamer', done: 'Pronto', reset: 'Tirar tudo', none: 'Nada', tap: 'Toque para colocar nela; de novo para tirar.' },
  ru: { title: 'Гардероб', outfits: 'Наряды', things: 'Аксессуары', colors: 'Цвета', effects: 'Эффекты', soon: 'скоро', secret: 'секрет', mane: 'Грива', mask: 'Маска', aura: 'Аура', alpha: 'Прозрачность', gamerAlpha: 'В игровом режиме', done: 'Готово', reset: 'Снять всё', none: 'Нет', tap: 'Нажми, чтобы надеть; ещё раз — снять.' },
};
const LANG_INDEX = { es: 1, en: 2, fr: 3, pt: 4, ru: 5 };

// ---------- drawing what goes on top (until its own drawing is in) ----------

const INK = '#0a0418';
const VIOLET = '#7b3cff';
// a filled polygon with her ink outline and the violet copy a little off it
function shard(g, pts, fill, { line = 1.2, echo = true, alpha = 1 } = {}) {
  const path = () => {
    g.beginPath();
    pts.forEach(([x, y], i) => (i ? g.lineTo(x, y) : g.moveTo(x, y)));
    g.closePath();
  };
  g.save();
  g.globalAlpha = alpha;
  if (echo) {
    g.save();
    g.translate(1.2, 1.2);
    path();
    g.strokeStyle = VIOLET;
    g.lineWidth = line;
    g.stroke();
    g.restore();
  }
  path();
  g.fillStyle = fill;
  g.fill();
  g.strokeStyle = INK;
  g.lineWidth = line;
  g.lineJoin = 'miter';
  g.stroke();
  g.restore();
}
const onMask = (g, fn) => {
  g.save();
  g.translate(MASK.x, MASK.y);
  g.rotate(MASK.rot);
  fn(g);
  g.restore();
};

const DRAW = {
  crown: (g) => shard(g, [[96, 30], [100, 10], [108, 22], [118, 4], [128, 22], [136, 10], [140, 30]], '#ffc21a'),
  catears: (g) => {
    shard(g, [[64, 46], [74, 10], [90, 40]], INK);
    shard(g, [[70, 42], [75, 20], [84, 38]], '#08f467', { echo: false, line: 0.6 });
    shard(g, [[150, 36], [168, 6], [176, 42]], INK);
    shard(g, [[156, 34], [167, 16], [171, 38]], '#08f467', { echo: false, line: 0.6 });
  },
  horns: (g) => {
    shard(g, [[86, 44], [70, 8], [78, 6], [98, 38]], '#1a1024');
    shard(g, [[150, 38], [168, 2], [176, 6], [162, 44]], '#1a1024');
  },
  halo: (g) => {
    g.save();
    g.strokeStyle = '#fff36a';
    g.lineWidth = 3.2;
    g.shadowColor = '#ffe14d';
    g.shadowBlur = 8;
    g.beginPath();
    g.ellipse(120, 10, 34, 6, -0.12, 0, Math.PI * 2);
    g.stroke();
    g.restore();
  },
  bow: (g) => {
    shard(g, [[140, 112], [124, 100], [126, 122]], '#ffffff');
    shard(g, [[140, 112], [158, 98], [156, 124]], '#ffffff');
    shard(g, [[136, 108], [144, 108], [144, 116], [136, 116]], '#ff1744');
  },
  flower: (g) => {
    const cx = 100, cy = 118;
    for (let i = 0; i < 5; i++) {
      const a = (i / 5) * Math.PI * 2;
      shard(g, [[cx, cy], [cx + Math.cos(a - 0.35) * 12, cy + Math.sin(a - 0.35) * 12], [cx + Math.cos(a) * 15, cy + Math.sin(a) * 15], [cx + Math.cos(a + 0.35) * 12, cy + Math.sin(a + 0.35) * 12]], '#ffffff', { echo: i === 0 });
    }
    shard(g, [[cx - 3, cy - 3], [cx + 3, cy - 3], [cx + 3, cy + 3], [cx - 3, cy + 3]], '#ffc21a', { echo: false });
  },
  santahat: (g) => {
    shard(g, [[80, 40], [118, 2], [168, 18], [176, 40], [150, 30]], '#e0102f');
    shard(g, [[76, 36], [156, 28], [158, 44], [78, 50]], '#ffffff');
    shard(g, [[170, 34], [180, 30], [184, 40], [174, 46]], '#ffffff');
  },
  witchhat: (g) => {
    shard(g, [[66, 52], [176, 34], [180, 46], [70, 62]], '#1a1024');
    shard(g, [[96, 48], [122, 2], [132, 0], [150, 38]], '#1a1024');
    shard(g, [[98, 46], [150, 38], [151, 44], [99, 52]], '#08f467', { echo: false, line: 0.6 });
  },
  glasses: (g) => onMask(g, (m) => {
    shard(m, [[-15, -4], [-2, -6], [-3, 3], [-13, 4]], INK, { line: 0.8 });
    shard(m, [[2, -6], [16, -6], [14, 3], [3, 3]], INK, { line: 0.8 });
    m.fillStyle = '#ffffff';
    m.fillRect(-10, -4, 3, 1.4);
    m.fillRect(6, -4, 3, 1.4);
    m.fillStyle = INK;
    m.fillRect(-2, -4, 4, 1.4);
  }),
  visor: (g) => onMask(g, (m) => {
    shard(m, [[-20, -6], [20, -7], [22, 1], [-18, 3]], 'rgba(8,244,103,0.55)', { line: 0.8 });
    m.fillStyle = 'rgba(255,255,255,0.7)';
    for (let x = -16; x < 18; x += 5) m.fillRect(x, -3, 2.5, 0.8);
  }),
  headphones: (g) => onMask(g, (m) => {
    m.save();
    m.strokeStyle = INK;
    m.lineWidth = 2.6;
    m.beginPath();
    m.moveTo(-17, -2);
    m.lineTo(-14, -20);
    m.lineTo(14, -22);
    m.lineTo(18, -3);
    m.stroke();
    m.restore();
    shard(m, [[-24, -6], [-15, -8], [-14, 6], [-23, 7]], '#08f467', { line: 0.9 });
    shard(m, [[15, -8], [24, -7], [23, 6], [14, 6]], '#08f467', { line: 0.9 });
  }),
  scarf: (g) => {
    shard(g, [[100, 144], [138, 144], [140, 156], [98, 156]], '#e0102f');
    shard(g, [[124, 154], [134, 154], [138, 196], [126, 192]], '#e0102f');
    g.fillStyle = '#08f467';
    g.fillRect(100, 148, 38, 2);
  },
  choker: (g) => {
    shard(g, [[108, 147], [130, 145], [131, 149], [109, 151]], INK, { echo: false, line: 0.6 });
    shard(g, [[117, 149], [122, 149], [120, 154]], '#ff1744', { echo: false, line: 0.5 });
  },
  veil: (g) => {
    // sheer, falling from her tiara down both sides of her mane, wide
    shard(g, [[112, 112], [80, 120], [36, 190], [20, 262], [70, 246], [96, 210], [112, 160]], '#ffffff', { alpha: 0.5, line: 0.8 });
    shard(g, [[132, 108], [168, 116], [212, 186], [222, 258], [176, 246], [148, 206], [136, 156]], '#ffffff', { alpha: 0.5, line: 0.8 });
    shard(g, [[100, 116], [150, 104], [150, 112], [104, 124]], '#ffffff', { alpha: 0.7, echo: false, line: 0.6 });
    onMask(g, (m) => {
      shard(m, [[-12, -12], [-8, -18], [-4, -13], [0, -21], [4, -13], [8, -18], [12, -12]], '#e8e8f4', { line: 0.7 });
      m.fillStyle = '#08f467';
      m.fillRect(-1.2, -18, 2.4, 2.4);
    });
  },
  bouquet: (g) => {
    shard(g, [[22, 216], [30, 232], [36, 214]], '#1fa85a', { echo: false });
    for (const [x, y] of [[20, 206], [30, 200], [38, 208], [26, 212], [34, 214]]) {
      shard(g, [[x - 5, y], [x, y - 5], [x + 5, y], [x, y + 5]], '#ff1744', { echo: false, line: 0.6 });
    }
    shard(g, [[28, 214], [34, 214], [36, 226], [26, 226]], '#ffffff', { echo: false, line: 0.5 });
  },
};

// ---------- the picture ----------

function loadImage(src) {
  return new Promise((resolve) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => resolve(null);
    img.src = src;
  });
}

const rgb2hsv = (r, g, b) => {
  const max = Math.max(r, g, b), min = Math.min(r, g, b), d = max - min;
  let h = 0;
  if (d) h = max === r ? ((g - b) / d) % 6 : max === g ? (b - r) / d + 2 : (r - g) / d + 4;
  return [(h / 6 + 1) % 1, max ? d / max : 0, max];
};
const hsv2rgb = (h, s, v) => {
  const f = (n) => {
    const k = (n + h * 6) % 6;
    return v - v * s * Math.max(0, Math.min(k, 4 - k, 1));
  };
  return [f(5), f(3), f(1)];
};
// her mane (crimson to red) and her mask (green) to other colors, already as bright as her shader
// makes her red and her green (it only brightens those two)
function recolor(g, w, h, mane, mask) {
  if (mane == null && mask == null) return;
  const img = g.getImageData(0, 0, w, h);
  const d = img.data;
  for (let i = 0; i < d.length; i += 4) {
    if (d[i + 3] < 8) continue;
    const [hu, s, v] = rgb2hsv(d[i] / 255, d[i + 1] / 255, d[i + 2] / 255);
    if (v < 0.1 || s < 0.06) continue;
    const hx = hu < 0.5 ? hu + 1 : hu;
    const isRed = hx > 0.78 && hx < 1.1;
    const isGreen = hu > 0.22 && hu < 0.52 && s > 0.15;
    const to = isRed ? mane : isGreen ? mask : undefined;
    if (to == null) continue;
    let out;
    if (to === 'gray') out = hsv2rgb(0, 0, Math.min(1, v * 1.6 + 0.15));
    else if (to === 'dark') out = hsv2rgb(0.75, 0.35, Math.min(1, v * 0.55 + 0.05));
    else out = hsv2rgb(to, Math.max(s, 0.86), Math.min(1, v * 1.5 + 0.1));
    d[i] = out[0] * 255;
    d[i + 1] = out[1] * 255;
    d[i + 2] = out[2] * 255;
  }
  g.putImageData(img, 0, 0);
}

export function wardrobe({ cfg, setConfig, onToggle }) {
  const dev = window.dev;
  const capri = dev?.capri;
  if (!capri?.sprite) return null;
  const original = capri.sprite.image;
  const have = {}; // drawings that are in: { 'outfit-jinete': Image, 'acc-crown': Image }
  const lang = () => (WORDS[dev.brain?.lang] ? dev.brain.lang : 'en');
  const t = () => WORDS[lang()];
  const look = () => ({ outfit: 'default', things: [], mane: 'default', mask: 'default', aura: 'none', alpha: 1, gamerAlpha: 0.55, ...(cfg().look || {}) });
  const unlocked = () => new Set(cfg().unlocked || []);

  // which drawings are in (once)
  const probing = Promise.all([
    ...OUTFITS.slice(1).map(([id]) => loadImage(`${WARDROBE}outfit-${id}.png`).then((img) => { if (img) have[`outfit-${id}`] = img; })),
    ...ACCESSORIES.map(([id]) => loadImage(`${WARDROBE}acc-${id}.png`).then((img) => { if (img) have[`acc-${id}`] = img; })),
  ]);

  // her picture with all of it, sent to her shader
  let composing = Promise.resolve();
  function apply() {
    composing = composing.then(async () => {
      await probing;
      const l = look();
      const base = have[`outfit-${l.outfit}`] || original;
      const w = original.naturalWidth || original.width, h = original.naturalHeight || original.height;
      const c = document.createElement('canvas');
      c.width = w;
      c.height = h;
      const g = c.getContext('2d', { willReadFrequently: true });
      g.drawImage(base, 0, 0, w, h);
      recolor(g, w, h, MANES.find(([id]) => id === l.mane)?.[1] ?? null, MASKS.find(([id]) => id === l.mask)?.[1] ?? null);
      // behind her face first (veil), then the rest
      const order = ['veil', ...ACCESSORIES.map(([id]) => id).filter((id) => id !== 'veil')];
      for (const id of order) {
        if (!l.things.includes(id)) continue;
        const pic = have[`acc-${id}`];
        if (pic) drawPicture(g, id, pic);
        else DRAW[id]?.(g);
      }
      upload(c);
      style();
    }).catch((err) => console.warn('[capri] wardrobe', err));
    return composing;
  }
  // a drawing of a thing (assets): fitted to where the drawn one goes
  function drawPicture(g, id, pic) {
    const box = { head: [70, 0, 110, 60], face: [100, 112, 46, 30], neck: [96, 140, 46, 60], hand: [8, 192, 40, 44] }[ACCESSORIES.find(([a]) => a === id)?.[1] || 'head'];
    const k = Math.min(box[2] / pic.naturalWidth, box[3] / pic.naturalHeight);
    const pw = pic.naturalWidth * k, ph = pic.naturalHeight * k;
    g.drawImage(pic, box[0] + (box[2] - pw) / 2, box[1] + (box[3] - ph) / 2, pw, ph);
  }
  function upload(canvas) {
    const r = capri.sprite;
    const gl = r.gl;
    r.image = canvas; // (after a GPU reset, this is the picture that goes back)
    const tex = gl.getParameter(gl.TEXTURE_BINDING_2D);
    if (!tex) return;
    gl.bindTexture(gl.TEXTURE_2D, tex);
    gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, false);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, canvas);
    // touches find her where she is now (a long dress, a hat)
    try {
      capri.alphaMap = capri.buildAlphaMap(canvas);
    } catch {}
  }
  // an aura and how see-through she is: on her canvas itself
  function style() {
    const el = document.getElementById('sprite');
    if (!el) return;
    const l = look();
    const aura = AURAS.find(([id]) => id === l.aura)?.[1];
    el.style.filter = aura ? `drop-shadow(0 0 6px ${aura}) drop-shadow(0 0 2px ${aura})` : '';
    const alpha = cfg().gamer ? Math.min(l.alpha, l.gamerAlpha) : l.alpha;
    el.style.opacity = alpha < 1 ? String(alpha) : '';
  }

  function set(patch) {
    setConfig({ look: { ...look(), ...patch } });
    apply();
  }
  function toggleThing(id) {
    const l = look();
    const slot = ACCESSORIES.find(([a]) => a === id)?.[1];
    if (l.things.includes(id)) return set({ things: l.things.filter((x) => x !== id) });
    // one per place, except the head can have a hat and a flower… keep it simple: one per place, and the veil with anything
    const keep = l.things.filter((x) => x === 'veil' || id === 'veil' || ACCESSORIES.find(([a]) => a === x)?.[1] !== slot);
    set({ things: [...keep, id] });
  }

  // ---------- her sheet ----------
  let el = null, shade = null, tab = 'outfits';
  function open() {
    if (!el) build();
    render();
    el.hidden = false;
    shade.hidden = false;
    onToggle?.();
  }
  function close() {
    if (!el || el.hidden) return;
    el.hidden = true;
    shade.hidden = true;
    onToggle?.();
  }
  function build() {
    shade = document.createElement('div');
    shade.className = 'cmenu-shade wardrobe-shade';
    shade.hidden = true;
    shade.addEventListener('click', close);
    el = document.createElement('section');
    el.className = 'wardrobe';
    el.hidden = true;
    el.setAttribute('role', 'dialog');
    document.body.append(shade, el);
  }
  function render() {
    const w = t();
    const l = look();
    const open = unlocked();
    el.replaceChildren();
    const head = document.createElement('header');
    head.className = 'wardrobe__head';
    head.innerHTML = '<span class="wardrobe__title"></span><button type="button" class="wardrobe__done"></button>';
    head.querySelector('.wardrobe__title').textContent = `// ${w.title}`;
    const done = head.querySelector('.wardrobe__done');
    done.textContent = w.done;
    done.addEventListener('click', close);
    el.append(head);

    const tabs = document.createElement('nav');
    tabs.className = 'wardrobe__tabs';
    for (const [id, label] of [['outfits', w.outfits], ['things', w.things], ['colors', w.colors], ['effects', w.effects]]) {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'wardrobe__tab';
      b.classList.toggle('is-on', tab === id);
      b.textContent = label;
      b.addEventListener('click', () => { tab = id; render(); });
      tabs.append(b);
    }
    el.append(tabs);

    const body = document.createElement('div');
    body.className = 'wardrobe__body';
    const tile = ({ label, on = false, locked = null, swatch = null, paint = null, click }) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'wardrobe__tile';
      b.classList.toggle('is-on', on);
      b.classList.toggle('is-locked', Boolean(locked));
      const pic = document.createElement('span');
      pic.className = 'wardrobe__pic';
      if (swatch) pic.style.background = swatch;
      if (paint) pic.append(paint);
      const name = document.createElement('span');
      name.className = 'wardrobe__name';
      name.textContent = label;
      b.append(pic, name);
      if (locked) {
        const tag = document.createElement('span');
        tag.className = 'wardrobe__lock';
        tag.textContent = `🔒 ${locked}`;
        b.append(tag);
      } else b.addEventListener('click', click);
      return b;
    };
    const grid = (title) => {
      if (title) {
        const h = document.createElement('div');
        h.className = 'wardrobe__label';
        h.textContent = title;
        body.append(h);
      }
      const g = document.createElement('div');
      g.className = 'wardrobe__grid';
      body.append(g);
      return g;
    };

    if (tab === 'outfits') {
      const g = grid(null);
      for (const row of OUTFITS) {
        const [id] = row;
        const secret = row[6] === 'secret';
        if (secret && !open.has(id)) continue;
        const ready = id === 'default' || have[`outfit-${id}`];
        g.append(tile({
          label: row[LANG_INDEX[lang()]],
          on: l.outfit === id,
          locked: ready ? null : w.soon,
          paint: thumb(ready ? (have[`outfit-${id}`] || original) : null),
          click: () => { set({ outfit: id }); render(); },
        }));
      }
    } else if (tab === 'things') {
      const hint = document.createElement('p');
      hint.className = 'wardrobe__hint';
      hint.textContent = w.tap;
      body.append(hint);
      const g = grid(null);
      for (const row of ACCESSORIES) {
        const [id] = row;
        if (row[7] === 'secret' && !open.has(id)) continue;
        g.append(tile({
          label: row[LANG_INDEX[lang()] + 1],
          on: l.things.includes(id),
          paint: thingThumb(id),
          click: () => { toggleThing(id); render(); },
        }));
      }
    } else if (tab === 'colors') {
      const g1 = grid(w.mane);
      for (const [id, , swatch] of MANES) g1.append(tile({ label: '', on: l.mane === id, swatch, click: () => { set({ mane: id }); render(); } }));
      const g2 = grid(w.mask);
      for (const [id, , swatch] of MASKS) g2.append(tile({ label: '', on: l.mask === id, swatch, click: () => { set({ mask: id }); render(); } }));
    } else {
      const g1 = grid(w.aura);
      for (const [id, color] of AURAS) g1.append(tile({ label: id === 'none' ? w.none : '', on: l.aura === id, swatch: color ? `radial-gradient(circle, ${color} 0 35%, transparent 70%)` : null, click: () => { set({ aura: id }); render(); } }));
      const g2 = grid(w.alpha);
      for (const a of ALPHAS) g2.append(tile({ label: `${Math.round(a * 100)}%`, on: Math.abs(l.alpha - a) < 0.01, click: () => { set({ alpha: a }); render(); } }));
      const g3 = grid(w.gamerAlpha);
      for (const a of [1, 0.7, 0.55, 0.4, 0.25]) g3.append(tile({ label: `${Math.round(a * 100)}%`, on: Math.abs(l.gamerAlpha - a) < 0.01, click: () => { set({ gamerAlpha: a }); render(); } }));
      const reset = document.createElement('button');
      reset.type = 'button';
      reset.className = 'wardrobe__reset';
      reset.textContent = w.reset;
      reset.addEventListener('click', () => { set({ outfit: 'default', things: [], mane: 'default', mask: 'default', aura: 'none', alpha: 1 }); render(); });
      body.append(reset);
    }
    el.append(body);
  }
  // a small picture of her in an outfit (or a padlock's silhouette)
  function thumb(img) {
    const c = document.createElement('canvas');
    c.width = 60;
    c.height = 96;
    const g = c.getContext('2d');
    if (img) g.drawImage(img, 0, 0, img.naturalWidth || img.width, img.naturalHeight || img.height, 2, 0, 56, 96);
    else {
      g.globalAlpha = 0.25;
      g.drawImage(original, 2, 0, 56, 96);
      g.globalCompositeOperation = 'source-in';
      g.fillStyle = '#9a96a8';
      g.fillRect(0, 0, 60, 96);
    }
    return c;
  }
  // a thing, on its own, as it goes on her
  function thingThumb(id) {
    const c = document.createElement('canvas');
    c.width = 60;
    c.height = 60;
    const g = c.getContext('2d');
    const box = { head: [60, 0, 130, 70], face: [96, 106, 52, 44], neck: [90, 136, 60, 70], hand: [4, 190, 48, 50] }[ACCESSORIES.find(([a]) => a === id)?.[1] || 'head'];
    if (id === 'veil') box.splice(0, 4, 10, 96, 220, 172);
    const k = Math.min(60 / box[2], 60 / box[3]);
    g.translate((60 - box[2] * k) / 2, (60 - box[3] * k) / 2);
    g.scale(k, k);
    g.translate(-box[0], -box[1]);
    const pic = have[`acc-${id}`];
    if (pic) drawPicture(g, id, pic);
    else DRAW[id]?.(g);
    return c;
  }

  // gamer mode on or off: how see-through she is changes
  apply();
  return {
    open, close, apply, style,
    get isOpen() { return Boolean(el && !el.hidden); },
    // the easter egg's prize (wedding.js): unlocked, and on her
    unlock(ids) {
      const now = unlocked();
      ids.forEach((id) => now.add(id));
      setConfig({ unlocked: [...now] });
    },
    wear(patch) { set(patch); },
    look,
    has: (id) => Boolean(have[id]),
    ready: () => probing,
    FACES,
  };
}

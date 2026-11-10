// Her quick menu: what you hold her for. A finger kept on her opens it: talk, look at your screen,
// a few of her moves, her modes as switches, and the way to all her settings (menu.js). Over a
// dimmed screen (blurred too, where the phone can: Overlay.kt), so it reads over any app.

const LABELS = {
  es: {
    title: 'Capri', moves: 'Movimientos', modes: 'Modos', all: 'Todos los ajustes', app: 'Abrir la app',
    chat: 'Hablar', look: 'Mira mi pantalla', live: 'Ver en vivo', teleport: 'Teleport', satellite: 'Satélite', hide: 'Guardarla', show: 'Sacarla',
    hop: 'Brincar', spin: 'Girar', stretch: 'Estirarse', victory: 'Victoria', zap: 'Rayo', split: 'Split',
    ghost: 'Fantasma', icon: 'Ícono', float: 'Sin gravedad', gamer: 'Gamer',
    ghostHow: 'tus toques la atraviesan (menos su cara)', iconHow: 'solo su máscara', floatHow: 'se queda donde la pones', gamerHow: 'chiquita, para jugar', liveHow: 'comenta lo que ves',
  },
  en: {
    title: 'Capri', moves: 'Moves', modes: 'Modes', all: 'All settings', app: 'Open the app',
    chat: 'Talk', look: 'Look at my screen', live: 'Watch live', teleport: 'Teleport', satellite: 'Satellite', hide: 'Put her away', show: 'Bring her out',
    hop: 'Hop', spin: 'Spin', stretch: 'Stretch', victory: 'Victory', zap: 'Zap', split: 'Split',
    ghost: 'Ghost', icon: 'Icon', float: 'No gravity', gamer: 'Gamer',
    ghostHow: 'your taps go through her (not her face)', iconHow: 'just her mask', floatHow: 'stays where you put her', gamerHow: 'tiny, for playing', liveHow: 'comments on what you see',
  },
  fr: {
    title: 'Capri', moves: 'Mouvements', modes: 'Modes', all: 'Tous les réglages', app: 'Ouvrir l’appli',
    chat: 'Parler', look: 'Regarde mon écran', live: 'En direct', teleport: 'Téléport', satellite: 'Satellite', hide: 'La ranger', show: 'La sortir',
    hop: 'Sauter', spin: 'Tourner', stretch: 'S’étirer', victory: 'Victoire', zap: 'Éclair', split: 'Grand écart',
    ghost: 'Fantôme', icon: 'Icône', float: 'Sans gravité', gamer: 'Gamer',
    ghostHow: 'tes touchers la traversent (sauf son visage)', iconHow: 'juste son masque', floatHow: 'reste où tu la poses', gamerHow: 'toute petite, pour jouer', liveHow: 'commente ce que tu vois',
  },
  pt: {
    title: 'Capri', moves: 'Movimentos', modes: 'Modos', all: 'Todos os ajustes', app: 'Abrir o app',
    chat: 'Conversar', look: 'Olha minha tela', live: 'Ver ao vivo', teleport: 'Teleporte', satellite: 'Satélite', hide: 'Guardar', show: 'Mostrar',
    hop: 'Pular', spin: 'Girar', stretch: 'Espreguiçar', victory: 'Vitória', zap: 'Raio', split: 'Espacate',
    ghost: 'Fantasma', icon: 'Ícone', float: 'Sem gravidade', gamer: 'Gamer',
    ghostHow: 'seus toques passam por ela (menos o rosto)', iconHow: 'só a máscara', floatHow: 'fica onde você põe', gamerHow: 'pequenininha, pra jogar', liveHow: 'comenta o que você vê',
  },
  ru: {
    title: 'Капри', moves: 'Движения', modes: 'Режимы', all: 'Все настройки', app: 'Открыть приложение',
    chat: 'Поговорить', look: 'Посмотри на экран', live: 'Смотреть вживую', teleport: 'Телепорт', satellite: 'Спутник', hide: 'Убрать', show: 'Вернуть',
    hop: 'Прыжок', spin: 'Кружиться', stretch: 'Потянуться', victory: 'Победа', zap: 'Молния', split: 'Шпагат',
    ghost: 'Призрак', icon: 'Иконка', float: 'Без гравитации', gamer: 'Игровой',
    ghostHow: 'касания проходят сквозь неё (кроме лица)', iconHow: 'только маска', floatHow: 'остаётся, где поставишь', gamerHow: 'крошечная, для игр', liveHow: 'комментирует, что видишь',
  },
};

// little line drawings for the big buttons, in her HUD's stroke
const ICONS = {
  chat: '<path d="M4 5h16v10H9l-5 4z"/>',
  look: '<path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
  teleport: '<path d="M4 20 20 4M14 4h6v6M4 10V4h6"/>',
  satellite: '<path d="m13 7 4 4M5 15l4 4M9 11l4 4M15 5l4 4-6 6-4-4z"/><path d="M3 21c0-3 1-5 3-6"/>',
  hide: '<path d="M4 7h16v13H4zM2 4h20v3H2zM10 11h4"/>',
};
const icon = (name) => `<svg viewBox="0 0 24 24" aria-hidden="true">${ICONS[name] || ''}</svg>`;

export class QuickMenu {
  // `host`: { cfg(), lang(), sync(), setConfig(patch), command(name, payload), perform(name), full(at), openApp(), blur(on) }
  constructor(host) {
    this.host = host;
    this.el = document.createElement('nav');
    this.el.className = 'cmenu qmenu';
    this.el.hidden = true;
    this.el.setAttribute('role', 'menu');
    this.shade = document.createElement('div');
    this.shade.className = 'cmenu-shade qmenu-shade';
    this.shade.hidden = true;
    this.shade.addEventListener('click', () => this.close());
    const add = () => document.body.append(this.shade, this.el);
    if (document.body) add();
    else document.addEventListener('DOMContentLoaded', add, { once: true });
  }

  get isOpen() {
    return !this.el.hidden;
  }

  open(at) {
    this.at = at || null;
    this.render();
    this.el.hidden = false;
    this.shade.hidden = false;
    this.host.blur(true);
  }

  close({ keepBlur = false } = {}) {
    if (this.el.hidden) return;
    this.el.hidden = true;
    this.shade.hidden = true;
    if (!keepBlur) this.host.blur(false);
  }

  render() {
    const t = LABELS[this.host.lang()] || LABELS.en;
    const cfg = this.host.cfg();
    const visible = cfg.visible !== false;
    this.el.replaceChildren();

    const head = document.createElement('div');
    head.className = 'qmenu__head';
    head.innerHTML = '<span class="qmenu__title"></span><span class="qmenu__sync"></span>';
    head.querySelector('.qmenu__title').textContent = `// ${t.title}`;
    head.querySelector('.qmenu__sync').textContent = this.host.sync() || '';
    this.el.append(head);

    // what you most often hold her for: big buttons
    const big = document.createElement('div');
    big.className = 'qmenu__big';
    const action = (id, label, run, close = true) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'qmenu__btn';
      b.innerHTML = `${icon(id)}<span></span>`;
      b.querySelector('span').textContent = label;
      b.addEventListener('click', () => {
        if (close) this.close();
        run();
      });
      return b;
    };
    big.append(
      action('chat', t.chat, () => this.host.command('chat')),
      action('look', t.look, () => this.host.command('look')),
      action('teleport', t.teleport, () => this.host.command('teleport')),
      action('satellite', t.satellite, () => this.host.command('satellite')),
      action('hide', visible ? t.hide : t.show, () => this.host.toggleVisible()),
    );
    this.el.append(big);

    // a few of her moves, played right away (the menu stays, for another)
    this.el.append(this.label(t.moves));
    const moves = document.createElement('div');
    moves.className = 'qmenu__chips';
    for (const name of ['hop', 'spin', 'stretch', 'victory', 'zap', 'split']) {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'qmenu__chip';
      b.textContent = t[name];
      b.addEventListener('click', () => {
        this.close();
        setTimeout(() => this.host.perform(name), 120);
      });
      moves.append(b);
    }
    this.el.append(moves);

    // her modes, as switches
    this.el.append(this.label(t.modes));
    const modes = document.createElement('div');
    modes.className = 'qmenu__modes';
    const toggle = (key, label, how, on, set, closeAfter = false) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'qmenu__mode';
      b.setAttribute('role', 'menuitemcheckbox');
      b.setAttribute('aria-checked', String(on));
      b.classList.toggle('is-on', on);
      b.innerHTML = '<span class="qmenu__box"></span><span class="qmenu__mode-text"><b></b><small></small></span>';
      b.querySelector('b').textContent = label;
      b.querySelector('small').textContent = how;
      b.addEventListener('click', () => {
        set(!on);
        if (closeAfter && !on) this.close();
        else setTimeout(() => this.isOpen && this.render(), 60);
      });
      modes.append(b);
    };
    toggle('live', t.live, t.liveHow, Boolean(cfg.watchScreen), (v) => this.host.command('watchScreen', { on: v }), true);
    toggle('ghost', t.ghost, t.ghostHow, Boolean(cfg.ghost), (v) => this.host.setConfig({ ghost: v }));
    toggle('icon', t.icon, t.iconHow, Boolean(cfg.icon), (v) => this.host.setConfig({ icon: v, ...(v && { visible: true }) }), true);
    toggle('float', t.float, t.floatHow, Boolean(cfg.float), (v) => this.host.setConfig({ float: v }));
    toggle('gamer', t.gamer, t.gamerHow, Boolean(cfg.gamer), (v) => this.host.setConfig({ gamer: v }), true);
    this.el.append(modes);

    // and everything else
    const foot = document.createElement('div');
    foot.className = 'qmenu__foot';
    const all = document.createElement('button');
    all.type = 'button';
    all.className = 'qmenu__all';
    all.textContent = `${t.all} ›`;
    all.addEventListener('click', () => {
      this.close({ keepBlur: true });
      this.host.full(this.at);
    });
    const app = document.createElement('button');
    app.type = 'button';
    app.className = 'qmenu__app';
    app.textContent = `${t.app} ↗`;
    app.addEventListener('click', () => {
      this.close();
      this.host.openApp();
    });
    foot.append(all, app);
    this.el.append(foot);
    this.place();
  }

  label(text) {
    const l = document.createElement('div');
    l.className = 'qmenu__label';
    l.textContent = text;
    return l;
  }

  // centered across the screen, a little above the middle: one thumb reaches all of it
  place() {
    const r = this.el.getBoundingClientRect();
    const w = r.width || 320;
    const h = Math.min(r.height || 480, innerHeight - 32);
    const x = Math.max(12, (innerWidth - w) / 2);
    const y = Math.max(16, Math.min(innerHeight - h - 16, (innerHeight - h) * 0.42));
    this.el.style.left = `${Math.round(x)}px`;
    this.el.style.top = `${Math.round(y)}px`;
  }
}

// Two of her (1.10.0): "pulsas con dos dedos sobre Capri y arrastras cada dedo hacia un lado
// diferente para dividirla, como en el episodio 6". Two fingers on her pulled apart (touch.js sends
// `capri:split`), or "Capri, divídete": a copy of her steps out, glitching, and lives beside her.
// The two share the work: she keeps talking with you, chatting and looking at your screen; her twin
// dances to your music, walks about, hops when you tap it and says its own lines. Drag the twin back
// onto her (or tap it twice, or "Capri, júntense") and they're one again.
//
// The twin is drawn plainly (2D, her picture as it is now, outfit and both arms), so it costs next to
// nothing; it isn't a second WebGL picture.

import { SPRITE } from '../capri/src/renderer/js/sprite.js';
import { bothArms } from './arms.js';
import { on, send } from './native.js';

const LINES = {
  es: {
    split: ['¡Dos de nosotras! Ahora sí, el doble de problemas.', 'Ta-da. Una platica contigo, la otra se divierte.', 'Mitad y mitad, choom.'],
    tap: ['¿Qué? Yo soy la buena.', 'Ella habla, yo bailo.', 'No le digas que soy más bonita.', 'Estoy de guardia.', '¿Me extrañaste? Estoy aquí al lado.'],
    music: ['¡Esa me la sé!', 'Ella que hable, yo bailo.'],
    merge: ['Otra vez una. Se sentía raro.', 'De vuelta en casa.', 'Ya, juntas otra vez.'],
    already: 'Ya somos dos. ¿Quieres tres? Ni lo sueñes.',
    alone: 'Ya soy una sola.',
  },
  en: {
    split: ['Two of us! Double the trouble.', 'Ta-da. One talks with you, the other has fun.', 'Half and half, choom.'],
    tap: ['What? I’m the nice one.', 'She talks, I dance.', 'Don’t tell her I’m cuter.', 'I’m on watch.', 'Missed me? I’m right here.'],
    music: ['I know this one!', 'Let her talk, I’ll dance.'],
    merge: ['One again. That felt weird.', 'Back home.', 'There, together again.'],
    already: 'There are two of us already. Three? Dream on.',
    alone: 'I’m just one already.',
  },
  fr: {
    split: ['Deux de nous ! Le double d’ennuis.', 'Ta-da. L’une te parle, l’autre s’amuse.', 'Moitié-moitié, choom.'],
    tap: ['Quoi ? C’est moi la gentille.', 'Elle parle, je danse.', 'Lui dis pas que je suis plus mignonne.', 'Je monte la garde.', 'Je t’ai manqué ? Je suis juste là.'],
    music: ['Je la connais !', 'Qu’elle parle, moi je danse.'],
    merge: ['Une seule à nouveau. Bizarre.', 'De retour à la maison.', 'Voilà, réunies.'],
    already: 'On est déjà deux. Trois ? Même pas en rêve.',
    alone: 'Je suis déjà une seule.',
  },
  pt: {
    split: ['Duas de nós! O dobro de confusão.', 'Ta-da. Uma conversa com você, a outra se diverte.', 'Meio a meio, choom.'],
    tap: ['Que foi? Eu sou a boazinha.', 'Ela fala, eu danço.', 'Não conta pra ela que eu sou mais bonita.', 'Tô de guarda.', 'Sentiu saudade? Tô aqui do lado.'],
    music: ['Essa eu conheço!', 'Ela que fale, eu danço.'],
    merge: ['Uma de novo. Foi estranho.', 'De volta pra casa.', 'Pronto, juntas de novo.'],
    already: 'Já somos duas. Três? Nem sonhando.',
    alone: 'Eu já sou uma só.',
  },
  ru: {
    split: ['Нас двое! Вдвое больше проблем.', 'Та-да. Одна болтает с тобой, другая веселится.', 'Пополам, чум.'],
    tap: ['Что? Я хорошая.', 'Она болтает, я танцую.', 'Не говори ей, что я милее.', 'Я на посту.', 'Скучал? Я тут, рядом.'],
    music: ['Знаю эту!', 'Пусть она болтает, я потанцую.'],
    merge: ['Снова одна. Было странно.', 'Дома.', 'Всё, снова вместе.'],
    already: 'Нас и так двое. Трое? Даже не мечтай.',
    alone: 'Я и так одна.',
  },
};
const pick = (list) => list[Math.floor(Math.random() * list.length)];

export const SPLIT_ASK = /div[ií]dete|div[ií]danse|sep[aá]rate en dos|hazte dos|clónate|cl[oó]nate|split|clone yourself|make two of you|divise-toi|d[ée]double-toi|divide-se|se divide|se clona|раздвойся|раздвоись|разделись/iu;
export const MERGE_ASK = /j[uú]ntense|[uú]nanse|vuelve(?:n)? a ser una|j[uú]ntate|merge|become one|back together|fusionnez|fusionne-toi|juntem-se|se junta(?:m)?|voltem a ser uma|соединитесь|объединитесь|снова одна/iu;

export function twins({ lang, cfg }) {
  const capri = window.dev?.capri;
  if (!capri) return null;
  const t = () => LINES[lang()] || LINES.en;
  let twin = null; // { el, say, x, y, dir, state, until, vx, drag, last, hop }
  let musicOn = false;
  on('phone:activity', (s = {}) => { musicOn = Boolean(s.sound); });

  const scale = () => capri.scale || 1;
  const size = () => ({ w: (SPRITE.width + SPRITE.pad * 2) * scale(), h: (SPRITE.height + SPRITE.pad * 2) * scale() });
  const picture = () => capri.sprite?.image;

  function draw() {
    const { el } = twin;
    const { w, h } = size();
    const dpr = Math.min(window.devicePixelRatio || 1, window.capriLite ? 1.25 : 2);
    if (el.width !== Math.round(w * dpr)) {
      el.width = Math.round(w * dpr);
      el.height = Math.round(h * dpr);
      el.style.width = `${w}px`;
      el.style.height = `${h}px`;
      twin.drawn = null;
    }
    const img = picture();
    if (!img || twin.drawn === img) return;
    twin.drawn = img;
    const g = el.getContext('2d');
    const k = el.width / (SPRITE.width + SPRITE.pad * 2);
    g.clearRect(0, 0, el.width, el.height);
    g.filter = 'brightness(1.3) saturate(1.2)'; // (as bright as her shader shows her)
    g.drawImage(bothArms(img), SPRITE.pad * k, SPRITE.pad * k, SPRITE.width * k, SPRITE.height * k);
    g.filter = 'none';
  }

  // where its feet are → where its canvas goes
  function place() {
    const { el, x, y, dir } = twin;
    const s = scale();
    const left = x - (SPRITE.pad + SPRITE.feet.x) * s;
    const top = y - (SPRITE.pad + SPRITE.feet.y) * s + (twin.bob || 0);
    el.style.transform = `translate(${left.toFixed(1)}px, ${top.toFixed(1)}px)${dir < 0 ? ' scaleX(-1)' : ''}`;
    el.style.transformOrigin = `${(SPRITE.pad + SPRITE.feet.x) * s}px 0`;
    if (!twin.say.hidden) {
      const head = y - 360 * s;
      twin.say.style.left = `${Math.max(8, Math.min(innerWidth - twin.say.offsetWidth - 8, x - twin.say.offsetWidth / 2))}px`;
      twin.say.style.top = `${Math.max(8, head - twin.say.offsetHeight - 8)}px`;
    }
  }

  function speak(line, ms = 3500) {
    if (!twin || !line) return;
    twin.say.textContent = line;
    twin.say.hidden = false;
    clearTimeout(twin.sayTimer);
    twin.sayTimer = setTimeout(() => { if (twin) twin.say.hidden = true; }, ms);
    place();
  }

  function hop() {
    if (twin) twin.hop = performance.now();
  }

  // its life: stands about, walks somewhere, dances when music plays
  let lastT = 0;
  function tick(now) {
    if (!twin) return;
    requestAnimationFrame(tick);
    if (now - lastT < 33) return; // ~30 fps is plenty
    const dt = Math.min(0.1, (now - (lastT || now)) / 1000);
    lastT = now;
    if (capri.hidden || capri.stowed || cfg().icon) return merge(false); // she went away: so does her twin
    draw();
    const s = scale();
    const floor = capri.floor ?? innerHeight;
    if (!twin.drag) {
      twin.y = floor;
      if (now > twin.until) {
        const r = Math.random();
        if (musicOn) {
          twin.state = 'dance';
          twin.until = now + 4000;
          if (Math.random() < 0.15) speak(pick(t().music), 2500);
        } else if (r < 0.45) {
          const margin = 80 * s;
          twin.target = margin + Math.random() * Math.max(10, innerWidth - margin * 2);
          twin.state = 'walk';
          twin.until = now + 9000;
        } else {
          twin.state = 'idle';
          twin.until = now + 2500 + Math.random() * 4000;
        }
      }
      if (twin.state === 'walk') {
        const d = twin.target - twin.x;
        twin.dir = d < 0 ? 1 : -1; // (her picture faces left as drawn: flipped to go right)
        const step = 70 * s * dt;
        if (Math.abs(d) <= step) { twin.x = twin.target; twin.state = 'idle'; twin.until = now + 3000; }
        else twin.x += Math.sign(d) * step;
      }
      const calm = matchMedia('(prefers-reduced-motion: reduce)').matches;
      const since = now - (twin.hop || -1e9);
      if (!calm && since < 450) twin.bob = -Math.sin((since / 450) * Math.PI) * 26 * s; // a hop
      else if (calm) twin.bob = 0;
      else if (twin.state === 'dance') { twin.bob = -Math.abs(Math.sin(now / 160)) * 10 * s; if (Math.floor(now / 640) % 2) twin.dir = -twin.dir || 1; }
      else if (twin.state === 'walk') twin.bob = -Math.abs(Math.sin(now / 120)) * 4 * s;
      else twin.bob = Math.sin(now / 700) * 1.5 * s;
    } else twin.bob = 0;
    place();
  }

  // a finger on the twin: tap (a line), two taps (back into her), drag (dropped on her: one again)
  function bindTouch(el) {
    el.addEventListener('touchstart', (e) => {
      const p = e.touches[0];
      twin.drag = { x: p.clientX, y: p.clientY, dx: twin.x - p.clientX, dy: twin.y - p.clientY, moved: false, at: performance.now() };
      e.stopPropagation();
    }, { passive: true });
    el.addEventListener('touchmove', (e) => {
      const d = twin?.drag;
      if (!d) return;
      const p = e.touches[0];
      if (!d.moved && Math.hypot(p.clientX - d.x, p.clientY - d.y) < 10) return;
      d.moved = true;
      twin.x = p.clientX + d.dx;
      twin.y = Math.min(capri.floor ?? innerHeight, p.clientY + d.dy);
      if (e.cancelable) e.preventDefault();
      e.stopPropagation();
    }, { passive: false });
    const end = (e) => {
      const d = twin?.drag;
      if (!d) return;
      twin.drag = null;
      e.stopPropagation();
      if (d.moved) {
        // dropped on her: one again
        if (Math.abs(twin.x - capri.pos.x) < 70 * scale()) merge(true);
        else { twin.state = 'idle'; twin.until = performance.now() + 2000; }
        return;
      }
      const now = performance.now();
      if (now - (twin.tapAt || 0) < 350) return merge(true); // two taps
      twin.tapAt = now;
      hop();
      send('overlay:haptic');
      speak(pick(t().tap));
    };
    el.addEventListener('touchend', end, { passive: true });
    el.addEventListener('touchcancel', end, { passive: true });
  }

  /** Two of her: the twin steps out toward `toward` (a screen x), or the roomier side. */
  function split(toward = null) {
    if (twin) return false;
    if (capri.hidden || capri.stowed || cfg().icon || !picture()) return false;
    const el = document.createElement('canvas');
    el.className = 'capri-twin is-in';
    const say = document.createElement('div');
    say.className = 'twin-say';
    say.hidden = true;
    document.body.append(el, say);
    const side = toward != null ? Math.sign(toward - capri.pos.x) || 1 : capri.pos.x > innerWidth / 2 ? -1 : 1;
    twin = { el, say, x: capri.pos.x, y: capri.pos.y, dir: capri.facing || 1, state: 'walk', until: performance.now() + 6000, bob: 0 };
    twin.target = Math.max(60, Math.min(innerWidth - 60, capri.pos.x + side * (160 * scale() + 60)));
    bindTouch(el);
    draw();
    place();
    capri.flash?.([[0.21, 1, 0.48], [0.08, 0.06, 0.11]]);
    send('overlay:haptic');
    lastT = 0;
    requestAnimationFrame(tick);
    setTimeout(() => speak(pick(t().split)), 500);
    send('log', { level: 'I', text: 'two of her (split)' });
    return true;
  }

  /** One again (with a glitch and a line if `show`). */
  function merge(show = true) {
    if (!twin) return false;
    const { el, say } = twin;
    clearTimeout(twin.sayTimer);
    twin = null;
    say.remove();
    el.classList.add('is-out');
    setTimeout(() => el.remove(), 260);
    if (show) {
      capri.flash?.([[0.21, 1, 0.48], [0.08, 0.06, 0.11]]);
      window.dev?.panel?.say({ kind: 'chat', face: 'laugh', line: pick(t().merge), hold: 3500 });
    }
    return true;
  }

  // two fingers on her pulled apart (touch.js)
  window.addEventListener('capri:split', (e) => {
    const { a, b } = e.detail || {};
    const far = a && b ? (Math.abs(a.x - capri.pos.x) > Math.abs(b.x - capri.pos.x) ? a.x : b.x) : null;
    split(far);
  });

  return {
    split,
    merge,
    get on() { return Boolean(twin); },
    rect() { // where touches should reach the twin (touch.js): its body, not its whole square
      if (!twin) return null;
      const s = scale();
      return [twin.x - 70 * s, twin.y - 380 * s + (twin.bob || 0), twin.x + 70 * s, twin.y + 4];
    },
    sayRect() {
      if (!twin || twin.say.hidden) return null;
      const r = twin.say.getBoundingClientRect();
      return [r.left, r.top, r.right, r.bottom];
    },
    lines: t,
  };
}

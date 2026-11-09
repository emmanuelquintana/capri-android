// Movimiento de la página: la intro del héroe (los mismos dibujos de la intro de la app, a 24 cuadros por
// segundo) y la escena fija de sus caras. Con «reducir movimiento» todo queda quieto y visible.
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

const root = document.documentElement;
const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
const EASE = 'expo.out';
const FPS = 24;

// Nav con fondo en cuanto bajas
const nav = document.querySelector<HTMLElement>('[data-nav]');
const onScroll = () => nav?.classList.toggle('is-solid', window.scrollY > 24);
onScroll();
window.addEventListener('scroll', onScroll, { passive: true });

// ── Su intro ──────────────────────────────────────────────────────────────
const capri = document.querySelector<HTMLElement>('[data-capri]');
const frames = new Map<string, HTMLImageElement>();
document.querySelectorAll<HTMLImageElement>('[data-frame]').forEach((img) => frames.set(img.dataset.frame!, img));
let current = 'grab';
const show = (name: string) => {
  if (name === current || !frames.has(name)) return;
  frames.get(current)?.classList.remove('is-on');
  frames.get(name)!.classList.add('is-on');
  current = name;
};

// «hola», con la boca como en la app (fx.js SAY_HI), y el saludo con la mano (fx.js WAVE)
const SAY_HI = ['mouth1', 'mouth1', 'mouth3', 'mouth3', 'mouth3', 'mouth3', 'mouth2', 'mouth2', 'mouth1', 'mouth1', 'grab'];
const WAVE = ['smear', 'smear', 'wave1', 'wave1', 'wave1', 'wave3', 'wave3', 'wave3', 'wave2', 'wave2', 'wave2', 'wave3', 'wave3', 'wave3', 'wave1', 'wave1', 'wave1', 'wave3', 'wave3', 'wave3', 'wave2', 'wave2', 'wave2', 'smear', 'smear', 'happy', 'happy', 'happy', 'happy', 'happy', 'happy', 'happy', 'happy', 'happy', 'happy', 'grab'];

const sequence = (names: string[]) => {
  const tl = gsap.timeline();
  names.forEach((n, i) => tl.call(show, [n], i / FPS));
  return tl;
};

const hi = document.querySelector<HTMLElement>('[data-hi]');
const greet = () => {
  const tl = gsap.timeline();
  tl.add(sequence(SAY_HI), 0);
  if (hi) {
    tl.fromTo(hi, { opacity: 0, scale: 0.85, rotate: -6 }, { opacity: 1, scale: 1, rotate: -3, duration: 0.35, ease: 'back.out(2)' }, 2 / FPS);
    tl.to(hi, { opacity: 0, duration: 0.25, ease: 'power2.out' }, 1.6);
  }
  tl.add(sequence(WAVE), (SAY_HI.length + 2) / FPS);
  return tl;
};

// las otras imágenes, en cuanto la primera se ve (para que el saludo no espere a la red)
const warm = () => frames.forEach((img) => { img.loading = 'eager'; img.decode?.().catch(() => {}); });

if (reduce) {
  root.classList.remove('intro');
} else {
  const title = document.querySelectorAll('[data-hero-title] .line > span');
  const fades = document.querySelectorAll('[data-hero-fade]');
  const mane = document.querySelector('[data-mane]');
  warm();

  const intro = gsap.timeline({ delay: 0.1, onStart: () => root.classList.remove('intro') });
  intro
    .fromTo(capri, { yPercent: 28, y: 0, scale: 0.94, opacity: 0 }, { yPercent: 0, y: 0, scale: 1, opacity: 1, duration: 1.1, ease: EASE, transformOrigin: '50% 100%' }, 0)
    .fromTo(mane, { scaleY: 0.55, opacity: 0 }, { scaleY: 1, opacity: 1, duration: 1.2, ease: EASE, transformOrigin: '50% 100%' }, 0.08)
    .fromTo(title, { yPercent: 110, y: 0, opacity: 1 }, { yPercent: 0, y: 0, opacity: 1, duration: 1, ease: EASE, stagger: 0.08 }, 0.12)
    .fromTo(fades, { y: 18, opacity: 0 }, { y: 0, opacity: 1, duration: 0.8, ease: EASE, stagger: 0.07 }, 0.42)
    .add(greet(), 1.15);

  // tócala y te vuelve a saludar
  let busy: gsap.core.Timeline | null = null;
  capri?.addEventListener('click', () => {
    if (busy?.isActive() || intro.isActive()) return;
    busy = greet();
    gsap.fromTo(capri, { scale: 0.985 }, { scale: 1, duration: 0.5, ease: 'elastic.out(1, 0.5)', transformOrigin: '50% 100%' });
  });
}

// ── Sus caras, mientras bajas ─────────────────────────────────────────────
const moods = document.querySelector<HTMLElement>('[data-moods]');
if (moods && !reduce) {
  const faces = gsap.utils.toArray<HTMLElement>('[data-face]', moods);
  const lines = gsap.utils.toArray<HTMLElement>('[data-mood]', moods);
  const dots = gsap.utils.toArray<HTMLElement>('[data-dot]', moods);
  const stage = moods.querySelector<HTMLElement>('.moods__stage');

  const tl = gsap.timeline({
    defaults: { ease: 'power2.inOut' },
    scrollTrigger: {
      trigger: stage,
      start: 'top top',
      end: () => `+=${window.innerHeight * 2.4}`,
      pin: true,
      scrub: 0.6,
      snap: { snapTo: 'labelsDirectional', duration: { min: 0.2, max: 0.6 }, ease: 'power2.out' },
      onUpdate: (self) => {
        const i = Math.min(dots.length - 1, Math.round(self.progress * (dots.length - 1)));
        dots.forEach((d, j) => d.classList.toggle('is-on', j <= i));
      },
    },
  });
  tl.addLabel('m0');
  for (let i = 1; i < lines.length; i++) {
    const at = `m${i - 1}+=0.2`;
    tl.to(lines[i - 1], { opacity: 0, y: -24, duration: 0.3, ease: 'power2.in' }, at)
      .to(faces[i - 1], { opacity: 0, scale: 0.97, duration: 0.5 }, at)
      .fromTo(faces[i], { opacity: 0, scale: 1.05, clipPath: 'polygon(6% 0, 100% 4%, 94% 0%, 0 0%)' }, { opacity: 1, scale: 1, clipPath: 'polygon(6% 0, 100% 4%, 94% 100%, 0 95%)', duration: 0.6 }, `${at}+=0.1`)
      .fromTo(lines[i], { opacity: 0, y: 28 }, { opacity: 1, y: 0, duration: 0.4, ease: 'power2.out' }, `${at}+=0.32`)
      .addLabel(`m${i}`, '+=0.2');
  }
}

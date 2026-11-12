// A scroll bar you can see on her menus (1.9.0, from the community: "una barra de desplazamiento para
// ver las demás opciones con la pantalla en horizontal"). Android's WebView only shows its own for a
// moment while it scrolls, so in a game, sideways, nobody knew there was more below. Drawn as the
// panel's own background (it doesn't scroll with what's in it): a thin bar on the right, in her
// frame's color, only while there's more than fits.

const watched = new WeakSet();

export function scrollCue(el) {
  if (!el) return;
  const draw = () => {
    const { scrollTop, scrollHeight, clientHeight } = el;
    const more = scrollHeight > clientHeight + 4;
    el.classList.toggle('has-scroll', more);
    if (!more) return;
    const h = Math.max(28, (clientHeight * clientHeight) / scrollHeight);
    const y = (scrollTop / (scrollHeight - clientHeight)) * (clientHeight - h - 8) + 4;
    el.style.setProperty('--bar-h', `${Math.round(h)}px`);
    el.style.setProperty('--bar-y', `${Math.round(y)}px`);
  };
  if (!watched.has(el)) {
    watched.add(el);
    el.addEventListener('scroll', draw, { passive: true });
    try { new ResizeObserver(draw).observe(el); } catch {}
  }
  requestAnimationFrame(draw);
}

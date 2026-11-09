// La web se construye con la última versión publicada, pero si sale una nueva antes de volver a
// construirla, aquí se pone al día sola: versión, tamaño y enlace de descarga.
const REPO = 'emmanuelquintana/capri-android';

const shown = document.querySelector<HTMLElement>('[data-version]')?.textContent?.trim();

fetch(`https://api.github.com/repos/${REPO}/releases/latest`, { headers: { Accept: 'application/vnd.github+json' } })
  .then((r) => (r.ok ? r.json() : null))
  .then((rel) => {
    if (!rel || rel.draft || rel.prerelease) return;
    const version = String(rel.tag_name).replace(/^v/, '');
    if (!version || version === shown) return;
    const apk = (rel.assets as { name: string; size: number; browser_download_url: string }[]).find((a) => a.name.endsWith('.apk'));
    if (!apk) return;
    document.querySelectorAll('[data-version]').forEach((el) => (el.textContent = version));
    document.querySelectorAll('[data-size]').forEach((el) => (el.textContent = `${(apk.size / 1048576).toFixed(1).replace('.', ',')} MB`));
    document.querySelectorAll<HTMLAnchorElement>('[data-download]').forEach((a) => (a.href = apk.browser_download_url));
  })
  .catch(() => {});

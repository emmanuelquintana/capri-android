// Las versiones publicadas de Capri, sacadas de GitHub Releases cuando se construye la web.
// Solo cuenta lo publicado: nada de borradores, versiones de prueba ni la rama `next`.

export const REPO = 'emmanuelquintana/capri-android';
export const REPO_URL = `https://github.com/${REPO}`;
export const RELEASES_URL = `${REPO_URL}/releases`;

export interface Release {
  version: string; // 1.6.2
  tag: string; // v1.6.2
  url: string; // su página en GitHub
  date: string; // ISO
  apk: string | null; // descarga directa del APK
  apkSize: number | null; // bytes
  notes: string[]; // cada punto del registro de cambios, en HTML ya seguro
}

// Por si GitHub no contesta al construir: la última versión publicada que conocemos.
const FALLBACK: Release[] = [
  {
    version: '1.6.2',
    tag: 'v1.6.2',
    url: `${RELEASES_URL}/tag/v1.6.2`,
    date: '2026-11-09T09:31:28Z',
    apk: `${RELEASES_URL}/download/v1.6.2/Capri-Android-1.6.2.apk`,
    apkSize: 3464169,
    notes: [
      '<strong>Únete a la comunidad:</strong> el Discord de la comunidad y el TikTok de Emma, al final de <em>Ajustes</em> y al final del menú de Capri. La primera vez que abras la app te los muestra una sola vez.',
    ],
  },
];

const esc = (s: string) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

// Markdown en línea de las notas: **negrita**, *cursiva*, `código` y [enlaces](https://…).
export const inline = (md: string) =>
  esc(md)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/(^|[^*])\*(?!\s)(.+?)\*/g, '$1<em>$2</em>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
    .replace(/\[([^\]]+)\]\((https:\/\/[^)\s]+)\)/g, '<a href="$2" rel="noopener">$1</a>');

// Cada «- punto» de la nota de la versión (con sus líneas de continuación), hasta el «---» del pie.
export function parseNotes(body: string): string[] {
  const main = body.split(/\n-{3,}\s*\n/)[0];
  const items: string[] = [];
  for (const line of main.split('\n')) {
    if (/^\s*-\s+/.test(line)) items.push(line.replace(/^\s*-\s+/, ''));
    else if (line.trim() && items.length) items[items.length - 1] += ' ' + line.trim();
  }
  return items.map(inline);
}

interface GhRelease {
  tag_name: string;
  html_url: string;
  published_at: string;
  draft: boolean;
  prerelease: boolean;
  body: string | null;
  assets: { name: string; size: number; browser_download_url: string }[];
}

export const toRelease = (r: GhRelease): Release => {
  const apk = r.assets.find((a) => a.name.endsWith('.apk'));
  return {
    version: r.tag_name.replace(/^v/, ''),
    tag: r.tag_name,
    url: r.html_url,
    date: r.published_at,
    apk: apk?.browser_download_url ?? null,
    apkSize: apk?.size ?? null,
    notes: parseNotes(r.body ?? ''),
  };
};

let cache: Promise<Release[]> | null = null;

export function getReleases(): Promise<Release[]> {
  cache ??= (async () => {
    try {
      const headers: Record<string, string> = { Accept: 'application/vnd.github+json', 'User-Agent': 'capri-web' };
      if (process.env.GITHUB_TOKEN) headers.Authorization = `Bearer ${process.env.GITHUB_TOKEN}`;
      const res = await fetch(`https://api.github.com/repos/${REPO}/releases?per_page=10`, { headers });
      if (!res.ok) throw new Error(`GitHub ${res.status}`);
      const list = ((await res.json()) as GhRelease[]).filter((r) => !r.draft && !r.prerelease).map(toRelease);
      if (!list.length) throw new Error('sin versiones');
      return list;
    } catch (e) {
      console.warn(`[capri-web] no pude leer las versiones de GitHub (${(e as Error).message}); uso la ${FALLBACK[0].version}`);
      return FALLBACK;
    }
  })();
  return cache;
}

export const mb = (bytes: number | null) => (bytes ? `${(bytes / 1048576).toFixed(1).replace('.', ',')} MB` : '');

export const fecha = (iso: string) =>
  new Date(iso).toLocaleDateString('es-MX', { day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' });

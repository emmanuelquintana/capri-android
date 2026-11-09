# La web de Capri

La página de Capri para Android: qué hace, descarga de la última versión, lo nuevo, cómo instalarla, el Discord y
donaciones. Hecha con [Astro](https://astro.build) y [GSAP](https://gsap.com), sin nada que corra en un servidor.

## Cómo se mantiene al día

- **Versión, descarga y novedades** salen de GitHub Releases al construir la web: solo lo publicado (nada de
  borradores, versiones de prueba ni la rama `next`).
- Si sale una versión nueva y la web todavía no se vuelve a construir, el botón de descarga y la versión se ponen al
  día solos en el navegador.
- Para que también cambien las novedades, vuelve a desplegar en Vercel (o conecta un *Deploy Hook*).

## Desplegar en Vercel

1. En Vercel: **Add New → Project** e importa `emmanuelquintana/capri-android`.
2. En **Root Directory** elige `web`. Lo demás lo detecta solo (Astro, `npm run build`, carpeta `dist`).
3. **Deploy.**

Opcional, en *Settings → Environment Variables*:

- `SITE_URL`: el dominio final si le pones uno propio (por ejemplo `https://capri.app`), para el mapa del sitio,
  la URL canónica y la imagen al compartir. Sin ella usa el dominio de producción de Vercel.
- `GITHUB_TOKEN`: solo si GitHub llega a limitar las consultas al construir (no hace falta normalmente).

## En tu máquina

```sh
cd web
npm install
npm run dev      # http://localhost:4321
npm run build    # la web lista en dist/
```

El arte de Capri sale de la app (`app/src/main/assets/capri/assets/capri`), igual que la intro del inicio: son
los mismos dibujos, cuadro a cuadro.

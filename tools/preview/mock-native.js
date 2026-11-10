// A stand-in for HostBridge.kt, so the Android page runs in a desktop browser (tools/preview/run.mjs).
// It answers like the phone would, keeps her settings in memory, and fetches with the browser's own fetch.
(() => {
  const state = { config: null, memory: {}, log: [] };
  const reply = (id, ok, value) => id && setTimeout(() => window.__capri.reply(id, ok, JSON.stringify(value ?? null)), 0);
  const emit = (channel, value) => window.__capri.emit(channel, JSON.stringify(value ?? null));
  const answers = {
    'config:load': () => state.config,
    'config:save': (cfg) => { state.config = cfg; },
    'memory:load': () => state.memory,
    'memory:save': (m) => { state.memory = m; },
    'device:info': () => ({ lang: navigator.language, ram: 8 * 1024 ** 3, sdk: 34, model: 'preview', termux: false }),
    'sounds:list': () => [],
    'keys:seal': ({ value }) => `plain:${value}`,
    'keys:open': ({ value }) => String(value || '').replace(/^plain:/, ''),
    'hooks:copy': () => ({ ok: true }),
    'pc:cpu': () => null,
    'notes:allowed': () => true,
    'media:now': () => ({}),
    'media:control': ({ action }) => { state.media = action; return true; },
    'media:open': () => true,
    'note:open': () => true,
    'clipboard:read': () => state.clipboard || '',
    'look:snap': () => ({ image: 'iVBORw0KGgo=' }),
    'look:live': () => true,
    'look:changed': () => 0,
    'apps:list': () => [],
    'apps:media': () => true,
    'open:url': () => true,
    'apps:launchables': () => [{ name: 'WhatsApp', kind: 'app', id: 'com.whatsapp' }, { name: 'Spotify', kind: 'app', id: 'com.spotify.music' }, { name: 'YouTube', kind: 'app', id: 'com.google.android.youtube' }],
    'apps:launch': () => true,
    'pc:do': (r) => (r.action === 'info' ? ({ battery: { percent: 64, charging: false }, disk: { drive: 'Android', free: 21e9, total: 64e9 }, ram: { total: 6e9, used: 3e9 } }[r.what] ?? null) : r.action === 'settings' ? true : r.action === 'volume' ? true : null),
    'voice:say': () => true,
    'voice:torch': ({ on }) => on !== false,
  };
  const aborts = new Map();
  window.CapriNative = {
    invoke(id, method, json) {
      const args = JSON.parse(json);
      state.log.push(method);
      const fn = answers[method];
      if (!fn) return reply(id, false, { error: `not in preview: ${method}` });
      try {
        reply(id, true, fn(args));
      } catch (err) {
        reply(id, false, { error: err.message });
      }
    },
    fetchStart(id, url, method, headers, body) {
      const abort = new AbortController();
      aborts.set(id, abort);
      fetch(url, { method, headers: JSON.parse(headers), body: body || undefined, signal: abort.signal })
        .then(async (res) => {
          emit('fetch:head', { id, status: res.status, headers: Object.fromEntries(res.headers) });
          const reader = res.body.getReader();
          for (;;) {
            const { done, value } = await reader.read();
            if (done) break;
            let bin = '';
            for (const b of value) bin += String.fromCharCode(b);
            emit('fetch:chunk', { id, data: btoa(bin) });
          }
          emit('fetch:end', { id });
        })
        .catch((err) => emit('fetch:error', { id, error: err.message }));
    },
    fetchAbort(id) {
      aborts.get(id)?.abort();
    },
  };
  window.__mock = { state, emit };
})();

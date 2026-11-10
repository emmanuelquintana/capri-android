// fetch() for the desktop's main-process modules, done by Kotlin (NativeFetch.kt): no CORS (Ollama on
// 127.0.0.1 in Termux, any OpenAI-compatible server), and a body that streams as Node's does
// (`for await (const chunk of res.body)`), whatever the WebView's version.

import { on } from './native.js';

const streams = new Map(); // id -> { head, push, end, fail }
let seq = 0;

const b64 = (s) => Uint8Array.from(atob(s), (c) => c.charCodeAt(0));

on('fetch:head', ({ id, status, headers }) => streams.get(id)?.head(status, headers));
on('fetch:chunk', ({ id, data }) => streams.get(id)?.push(b64(data)));
on('fetch:end', ({ id }) => streams.get(id)?.end());
on('fetch:error', ({ id, error }) => streams.get(id)?.fail(new Error(error || 'network error')));

function abortError() {
  return Object.assign(new Error('This operation was aborted'), { name: 'AbortError' });
}

// Models that "think" first, out of sight, before their first word: asked to think little, she
// answers sooner instead of sitting on "let me think…" (and her chat giving up on them after 40 s).
// The free online AI's only model; Gemini, whose Flash models think by default (Gemini 3: a lot).
// The tries, best first: a server that doesn't take one says so (400), and the next one goes.
// The last is always the request as it was.
function variants(url, body) {
  if (body == null) return [body];
  const plain = String(body);
  let json;
  try {
    json = JSON.parse(plain);
  } catch {
    return [plain];
  }
  if (!json || typeof json !== 'object' || json.reasoning_effort) return [plain];
  let efforts = [];
  if (/text\.pollinations\.ai\/openai\/chat\/completions/.test(url)) efforts = ['low'];
  else if (/generativelanguage\.googleapis\.com\/.*\/chat\/completions/.test(url)) {
    const model = String(json.model || '');
    efforts = /2\.5-flash/.test(model) ? ['none', 'low'] : /flash/.test(model) ? ['minimal', 'low'] : ['low'];
  }
  return [...efforts.map((reasoning_effort) => JSON.stringify({ ...json, reasoning_effort })), plain];
}

export async function nativeFetch(url, init = {}) {
  const tries = variants(String(url), init.body);
  for (let i = 0; ; i++) {
    const res = await rawFetch(url, { ...init, body: tries[i] });
    if (i === tries.length - 1 || ![400, 422].includes(res.status)) return res;
    await res.text().catch(() => '');
  }
}

function rawFetch(url, init = {}) {
  const id = `f${++seq}`;
  const signal = init.signal;
  if (signal?.aborted) return Promise.reject(abortError());
  const headers = {};
  if (init.headers) for (const [k, v] of Object.entries(init.headers)) headers[k.toLowerCase()] = String(v);

  return new Promise((resolve, reject) => {
    const chunks = [];
    let waiting = null; // the reader waiting for the next chunk
    let done = false;
    let failure = null;
    let settled = false;

    const wake = () => {
      const w = waiting;
      waiting = null;
      w?.();
    };
    const finish = () => {
      streams.delete(id);
      signal?.removeEventListener('abort', onAbort);
    };
    const onAbort = () => {
      window.CapriNative?.fetchAbort(id);
      failure = abortError();
      done = true;
      finish();
      if (!settled) {
        settled = true;
        reject(failure);
      }
      wake();
    };
    signal?.addEventListener('abort', onAbort, { once: true });

    async function* body() {
      for (;;) {
        if (chunks.length) {
          yield chunks.shift();
          continue;
        }
        if (failure) throw failure;
        if (done) return;
        await new Promise((r) => { waiting = r; });
      }
    }
    const all = async () => {
      const parts = [];
      for await (const c of body()) parts.push(c);
      const out = new Uint8Array(parts.reduce((n, p) => n + p.length, 0));
      let at = 0;
      for (const p of parts) {
        out.set(p, at);
        at += p.length;
      }
      return out;
    };

    streams.set(id, {
      head(status, h) {
        if (settled) return;
        settled = true;
        const lower = Object.fromEntries(Object.entries(h || {}).map(([k, v]) => [k.toLowerCase(), v]));
        const text = async () => new TextDecoder().decode(await all());
        resolve({
          ok: status >= 200 && status < 300,
          status,
          url,
          headers: { get: (k) => lower[String(k).toLowerCase()] ?? null },
          body: { [Symbol.asyncIterator]: body },
          text,
          json: async () => JSON.parse(await text()),
          arrayBuffer: async () => (await all()).buffer,
        });
      },
      push(bytes) {
        chunks.push(bytes);
        wake();
      },
      end() {
        done = true;
        finish();
        wake();
      },
      fail(err) {
        failure = err;
        done = true;
        finish();
        if (!settled) {
          settled = true;
          reject(Object.assign(new TypeError('fetch failed'), { cause: err }));
        }
        wake();
      },
    });

    try {
      window.CapriNative.fetchStart(id, String(url), String(init.method || 'GET'), JSON.stringify(headers),
        init.body == null ? '' : String(init.body));
    } catch (err) {
      streams.get(id)?.fail(err);
    }
  });
}

// Her long-term memory: after a chat, she asks her AI once for what's worth remembering about you
// (what you like and don't, the people you talk about, what's going on in your life, how it went)
// and keeps it as a short list on this phone (memory.json), with a line on what you talked about
// last. Her later chats have it in mind, so she picks up where you left off instead of starting
// from nothing. Your "remember that…" (custom.js's memories) stays apart, as it was.
//
// One extra question to her AI per chat, never while you're talking with her (the free AI answers
// one message every 45 s: it waits until she has been quiet a while). Off in the app's Settings;
// "Forget" in her menu (or in Settings) empties it.

const MAX_FACTS = 60; // (1.10.0: was 40, she remembers more)
const MAX_PHRASES = 10;
const FACT_CHARS = 160;
const IDLE_MS = 3 * 60 * 1000;      // quiet this long after her last answer: she notes it down
const AFTER_CLOSE_MS = 20 * 1000;   // or this long after you close her chat
const AFTER_CLOSE_FREE_MS = 60 * 1000; // (the free AI: past its 45 s)
const LANGUAGE = { es: 'Spanish', en: 'English', fr: 'French', pt: 'Portuguese', ru: 'Russian' };

const key = (m) => String(m?.content || '').slice(0, 200);

export function longMemory({ cfg, readMemory, saveMemory }) {
  const dev = window.dev;
  const chat = dev?.chat;
  if (!chat) return;
  let timer = null;
  let running = null; // the chat id of the note-taking under way, to stop it if you come back

  const enabled = () => cfg().longMemory !== false && !['claude'].includes(cfg().provider);
  const facts = () => (Array.isArray(readMemory().facts) ? readMemory().facts : []);

  // what she has in mind about you, for every answer of hers (her chat, out loud, her looks)
  const personal = chat.personal.bind(chat);
  chat.personal = () => {
    const lines = personal();
    if (!enabled()) return lines;
    const known = facts();
    const last = readMemory().last;
    if (known.length) {
      lines.push(`What you remember about the user from earlier chats (it's true: bring it up naturally when it fits, like a friend who remembers, never list it or say you have notes):\n${known.map((f) => `- ${f}`).join('\n')}`);
    }
    if (last?.text) lines.push(`Your last chat with them was ${ago(last.at)}: ${last.text}`);
    // (1.10.0) the way you two talk: their expressions and your inside jokes, used now and then
    const phrases = Array.isArray(readMemory().phrases) ? readMemory().phrases : [];
    if (phrases.length) lines.push(`Expressions and inside jokes you two use (use one now and then when it fits naturally, like a friend who talks like them; never all at once, never forced): ${phrases.map((p) => `"${p}"`).join(', ')}`);
    // (1.10.0, checkin.js) how they said they were, when she asked
    const feeling = readMemory().feeling;
    if (feeling?.text && Date.now() - feeling.at < 3 * 86400000) lines.push(`When you asked how they were (${ago(feeling.at)}), they said: ${feeling.text}. Keep it in mind, and ask how they are now if it fits.`);
    return lines;
  };

  function schedule(ms) {
    clearTimeout(timer);
    timer = setTimeout(run, ms);
  }

  // her answers (typed or out loud) and your closing her chat set the time to note things down
  const remember = chat.remember.bind(chat);
  chat.remember = (message) => {
    remember(message);
    if (enabled()) schedule(IDLE_MS);
  };
  const close = chat.close.bind(chat);
  chat.close = () => {
    const was = chat.open;
    close();
    if (was && enabled() && pending().length) schedule(cfg().provider === 'pollinations' ? AFTER_CLOSE_FREE_MS : AFTER_CLOSE_MS);
  };
  const start = chat.start.bind(chat);
  chat.start = (...args) => {
    // you're back: her note-taking waits (it would hold up her answer to you)
    if (running) window.host.chatStop(running);
    return start(...args);
  };

  // the conversation since she last noted things down
  function pending() {
    const history = chat.history || [];
    const seen = readMemory().seen;
    let from = seen ? history.map(key).lastIndexOf(seen) + 1 : 0;
    if (from <= 0 && seen) from = Math.max(0, history.length - 24);
    return history.slice(from).filter((m) => ['user', 'assistant'].includes(m.role) && typeof m.content === 'string').slice(-30);
  }

  async function run() {
    timer = null;
    if (!enabled() || running) return;
    if (chat.open || chat.turn) return schedule(60 * 1000);
    const talk = pending();
    const said = talk.filter((m) => m.role === 'user');
    // a "hi" and little else: nothing to note down yet
    if (said.length < 2 && said.reduce((n, m) => n + m.content.length, 0) < 60) return;
    const status = await window.host.chatStatus().catch(() => ({ ok: false }));
    if (!status.ok) return;
    const lang = LANGUAGE[dev.brain?.lang] || 'English';
    const name = cfg().userName || 'the user';
    const known = facts();
    const messages = [
      { role: 'system', content: `You keep the long-term notes of Capri, a playful digital companion, about ${name}, the person she chats with. You only answer with JSON.` },
      {
        role: 'user',
        content: [
          `Notes so far:\n${known.length ? known.map((f) => `- ${f}`).join('\n') : '(none)'}`,
          `New conversation, oldest first ("User" is ${name}):\n${talk.map((m) => `${m.role === 'user' ? 'User' : 'Capri'}: ${m.content.slice(0, 600)}`).join('\n')}`,
          `Expressions so far: ${(Array.isArray(readMemory().phrases) ? readMemory().phrases : []).map((p) => `"${p}"`).join(', ') || '(none)'}`,
          'Update the notes. Answer with JSON only, nothing else: {"facts": ["..."], "phrases": ["..."], "last": "..."}',
          `- facts: what's worth remembering in later chats, about the user only (not Capri): their likes and dislikes, people they mention (name and who they are to them), pets, studies or work, plans and dates coming up and how things went, what matters to them, how they like to be treated. One short line each, in ${lang}. Keep the old notes unless this conversation changes or contradicts them; merge repeats; at most ${MAX_FACTS}, the most important first. Small talk and jokes are not facts.`,
          `- phrases: the user's own catchphrases, slang, nicknames and inside jokes you two share (their exact words, short), so Capri can talk like them. Keep the old ones unless they stopped using them; at most ${MAX_PHRASES}. Never anything offensive about someone.`,
          '- Never note passwords, keys, codes, card or bank numbers, exact addresses, or anything the user asked to forget or keep secret.',
          `- last: one sentence on what you talked about this time, in ${lang}, written to Capri (like "They told you…").`,
        ].join('\n\n'),
      },
    ];
    const id = Date.now() + 1;
    running = id;
    const res = await window.host.chatSend({ id, messages }).catch(() => null);
    running = null;
    if (!res?.ok) {
      // the free AI was busy, or you came back: another go a while later
      if (!chat.open) schedule(IDLE_MS);
      return;
    }
    const notes = parse(res.text);
    if (!notes) return;
    const kept = notes.facts.length || !known.length ? notes.facts : known; // an empty answer never wipes them
    saveMemory({
      facts: kept,
      ...(notes.phrases.length ? { phrases: notes.phrases } : {}),
      last: notes.last ? { text: notes.last, at: Date.now() } : readMemory().last || null,
      seen: key(talk.at(-1)),
    });
  }

  return { run, pending };
}

// The JSON in her AI's answer (some wrap it in ```json, or add a word around it): checked and trimmed.
function parse(text) {
  const s = String(text || '');
  const a = s.indexOf('{');
  const b = s.lastIndexOf('}');
  if (a < 0 || b <= a) return null;
  try {
    const json = JSON.parse(s.slice(a, b + 1));
    const facts = (Array.isArray(json.facts) ? json.facts : [])
      .filter((f) => typeof f === 'string')
      .map((f) => f.replace(/\s+/g, ' ').trim().slice(0, FACT_CHARS))
      .filter((f) => f.length > 2);
    const unique = [...new Map(facts.map((f) => [f.toLowerCase(), f])).values()].slice(0, MAX_FACTS);
    const last = typeof json.last === 'string' ? json.last.replace(/\s+/g, ' ').trim().slice(0, 240) : '';
    const phrases = [...new Set((Array.isArray(json.phrases) ? json.phrases : []).filter((p) => typeof p === 'string')
      .map((p) => p.replace(/\s+/g, ' ').trim().slice(0, 60)).filter((p) => p.length > 1))].slice(0, MAX_PHRASES);
    return { facts: unique, last, phrases };
  } catch {
    return null;
  }
}

function ago(at) {
  const days = Math.floor((startOfDay(Date.now()) - startOfDay(at)) / 86400000);
  if (!Number.isFinite(days) || days <= 0) return 'earlier today';
  if (days === 1) return 'yesterday';
  if (days < 14) return `${days} days ago`;
  return `on ${new Date(at).toLocaleDateString('en', { day: 'numeric', month: 'long' })}`;
}
function startOfDay(t) {
  const d = new Date(t);
  d.setHours(0, 0, 0, 0);
  return d.getTime();
}

// Her setup (setup.js) on a phone: no terminal anywhere.
//
// On the desktop the first choice is Ollama on this PC, which she installs with winget. A phone has
// neither, so here the first choice is the free online AI (Pollinations, one tap), then a key
// (Groq and Gemini are free, the page to get one opens in the browser), then the Ollama of your PC
// over Wi-Fi, by its address. Her panels and the rest of setup.js are the desktop's, unchanged:
// this only swaps the steps that would need a terminal, and the lines that talk about a PC.

import { Setup } from '../capri/src/renderer/js/setup.js';
import { fmt } from '../capri/src/renderer/js/util.js';

// the services that take a key: the desktop's, and NVIDIA's (free too, many models)
const KEYED = ['groq', 'gemini', 'nvidia', 'openrouter', 'openai', 'mistral', 'deepseek'];
const NAMES = { groq: 'Groq', gemini: 'Gemini', nvidia: 'NVIDIA', openrouter: 'OpenRouter', openai: 'OpenAI', mistral: 'Mistral', deepseek: 'DeepSeek' };

const LINES = {
  es: {
    whoHow: 'Lo más fácil: en línea, gratis y sin cuenta. Un toque y listo.\nSi quieres algo mejor: una clave gratis de Groq, Gemini o NVIDIA, o el Ollama de tu PC por Wi-Fi.',
    why: { noOllama: 'No encuentro el Ollama de tu PC.', asleep: 'No encuentro el Ollama de tu PC.', noModel: 'El Ollama de tu PC aún no tiene ningún modelo.' },
    online: 'Gratis, sin cuenta', local: 'Ollama de mi PC',
    pasteHow: 'Toca «¿Dónde la consigo?», crea la clave en el navegador, cópiala y pégala aquí. La guardo cifrada en este teléfono.',
    readyHow: 'Tócame cuando quieras charlar.',
    pcLine: '¿Cuál es la IP de tu PC?',
    pcHow: 'En tu PC: abre Ollama, en sus ajustes activa «Expose Ollama to the network», y escribe aquí la IP del PC. Los dos en el mismo Wi-Fi.',
    pcNote: 'La IP sale en el PC en Configuración › Red. Algo como 192.168.1.20.',
    pcBad: 'No contesta en esa dirección.',
    whichHow: 'Groq, Gemini y NVIDIA tienen opción gratis: una cuenta, una clave, y listo. NVIDIA tiene muchos modelos para elegir.',
  },
  en: {
    whoHow: 'Easiest: online, free, no account. One tap and done.\nFor something better: a free key from Groq, Gemini or NVIDIA, or your PC’s Ollama over Wi-Fi.',
    why: { noOllama: 'I can’t find your PC’s Ollama.', asleep: 'I can’t find your PC’s Ollama.', noModel: 'Your PC’s Ollama has no model yet.' },
    online: 'Free, no account', local: 'My PC’s Ollama',
    pasteHow: 'Tap “Where do I get one?”, make the key in the browser, copy it and paste it here. I keep it encrypted on this phone.',
    readyHow: 'Tap me whenever you want to chat.',
    pcLine: 'What’s your PC’s IP?',
    pcHow: 'On your PC: open Ollama, turn on “Expose Ollama to the network” in its settings, and type the PC’s IP here. Both on the same Wi-Fi.',
    pcNote: 'The PC shows its IP in Settings › Network. Something like 192.168.1.20.',
    pcBad: 'Nothing answers at that address.',
    whichHow: 'Groq, Gemini and NVIDIA have a free tier: an account, a key, and you’re set. NVIDIA has lots of models to pick from.',
  },
  pt: {
    whoHow: 'O mais fácil: online, grátis e sem conta. Um toque e pronto.\nPra algo melhor: uma chave grátis do Groq, do Gemini ou da NVIDIA, ou o Ollama do seu PC pelo Wi-Fi.',
    why: { noOllama: 'Não encontro o Ollama do seu PC.', asleep: 'Não encontro o Ollama do seu PC.', noModel: 'O Ollama do seu PC ainda não tem nenhum modelo.' },
    online: 'Grátis, sem conta', local: 'Ollama do meu PC',
    pasteHow: 'Toque em “Onde eu consigo?”, crie a chave no navegador, copie e cole aqui. Eu guardo ela criptografada neste celular.',
    readyHow: 'Me toque quando quiser conversar.',
    pcLine: 'Qual é o IP do seu PC?',
    pcHow: 'No seu PC: abra o Ollama, ative “Expose Ollama to the network” nas configurações dele e digite aqui o IP do PC. Os dois no mesmo Wi-Fi.',
    pcNote: 'O IP aparece no PC em Configurações › Rede. Algo como 192.168.1.20.',
    pcBad: 'Nada responde nesse endereço.',
    whichHow: 'O Groq, o Gemini e a NVIDIA têm plano grátis: uma conta, uma chave, e pronto. A NVIDIA tem muitos modelos para escolher.',
  },
  fr: {
    whoHow: 'Le plus simple : en ligne, gratuit et sans compte. Un geste et c’est fait.\nPour mieux : une clé gratuite de Groq, Gemini ou NVIDIA, ou l’Ollama de ton PC par le Wi-Fi.',
    why: { noOllama: 'Je ne trouve pas l’Ollama de ton PC.', asleep: 'Je ne trouve pas l’Ollama de ton PC.', noModel: 'L’Ollama de ton PC n’a pas encore de modèle.' },
    online: 'Gratuit, sans compte', local: 'L’Ollama de mon PC',
    pasteHow: 'Touche « Où la trouver ? », crée la clé dans le navigateur, copie-la et colle-la ici. Je la garde chiffrée sur ce téléphone.',
    readyHow: 'Touche-moi quand tu veux parler.',
    pcLine: 'Quelle est l’IP de ton PC ?',
    pcHow: 'Sur ton PC : ouvre Ollama, active « Expose Ollama to the network » dans ses réglages, et tape ici l’IP du PC. Les deux sur le même Wi-Fi.',
    pcNote: 'Le PC affiche son IP dans Paramètres › Réseau. Quelque chose comme 192.168.1.20.',
    pcBad: 'Rien ne répond à cette adresse.',
    whichHow: 'Groq, Gemini et NVIDIA ont une offre gratuite : un compte, une clé, et c’est parti. NVIDIA a plein de modèles au choix.',
  },
  ru: {
    whoHow: 'Проще всего: онлайн, бесплатно и без аккаунта. Одно касание, и готово.\nЕсли хочешь лучше: бесплатный ключ Groq, Gemini или NVIDIA, или Ollama на твоём ПК по Wi-Fi.',
    why: { noOllama: 'Не нахожу Ollama на твоём ПК.', asleep: 'Не нахожу Ollama на твоём ПК.', noModel: 'В Ollama на твоём ПК пока нет модели.' },
    online: 'Бесплатно, без аккаунта', local: 'Ollama на моём ПК',
    pasteHow: 'Нажми «Где его взять?», создай ключ в браузере, скопируй и вставь сюда. Я храню его зашифрованным на этом телефоне.',
    readyHow: 'Коснись меня, когда захочешь поболтать.',
    pcLine: 'Какой IP у твоего ПК?',
    pcHow: 'На ПК: открой Ollama, включи в настройках «Expose Ollama to the network» и введи здесь IP ПК. Оба в одной сети Wi-Fi.',
    pcNote: 'IP видно на ПК в Параметры › Сеть. Что-то вроде 192.168.1.20.',
    pcBad: 'По этому адресу никто не отвечает.',
    whichHow: 'У Groq, Gemini и NVIDIA есть бесплатный вариант: аккаунт, ключ, и готово. У NVIDIA много моделей на выбор.',
  },
  vi: {
    whoHow: 'Dễ nhất: trực tuyến, miễn phí, không cần tài khoản. Một chạm là xong.\nTốt hơn: khóa miễn phí từ Groq, Gemini hoặc NVIDIA, hoặc Ollama trên PC qua Wi-Fi.',
    why: { noOllama: 'Mình không tìm thấy Ollama trên PC của bạn.', asleep: 'Mình không tìm thấy Ollama trên PC của bạn.', noModel: 'Ollama trên PC của bạn chưa có mô hình.' },
    online: 'Miễn phí, không cần tài khoản', local: 'Ollama trên PC của mình',
    pasteHow: 'Chạm “Lấy ở đâu?”, tạo khóa trong trình duyệt, sao chép và dán vào đây. Mình giữ nó được mã hóa trên điện thoại này.',
    readyHow: 'Chạm vào mình bất cứ khi nào bạn muốn trò chuyện.',
    pcLine: 'IP của PC bạn là gì?',
    pcHow: 'Trên PC: mở Ollama, bật “Expose Ollama to the network” trong cài đặt của nó, rồi gõ IP của PC vào đây. Cả hai cùng một Wi-Fi.',
    pcNote: 'PC hiện IP trong Cài đặt › Mạng. Kiểu như 192.168.1.20.',
    pcBad: 'Không có gì trả lời ở địa chỉ đó.',
    whichHow: 'Groq, Gemini và NVIDIA có gói miễn phí: một tài khoản, một khóa, là xong. NVIDIA có rất nhiều mô hình để chọn.',
  },
  ja: {
    whoHow: 'いちばん簡単：オンライン、無料、アカウント不要。ワンタップで完了。\nもっと良くするなら：Groq、Gemini、NVIDIAの無料キー、またはWi-Fi経由でPCのOllama。',
    why: { noOllama: 'PCのOllamaが見つからないよ。', asleep: 'PCのOllamaが見つからないよ。', noModel: 'PCのOllamaにまだモデルがないよ。' },
    online: '無料・アカウント不要', local: 'PCのOllama',
    pasteHow: '「どこで手に入る？」をタップして、ブラウザでキーを作って、コピーしてここに貼り付けて。このスマホに暗号化して保存するね。',
    readyHow: '話したいときはいつでもタップしてね。',
    pcLine: 'PCのIPは？',
    pcHow: 'PCで：Ollamaを開いて、設定で「Expose Ollama to the network」をオンにして、PCのIPをここに入力。両方同じWi-Fiでね。',
    pcNote: 'PCのIPは設定 › ネットワークに表示されるよ。192.168.1.20みたいなの。',
    pcBad: 'そのアドレスでは何も応答がないよ。',
    whichHow: 'Groq、Gemini、NVIDIAには無料枠があるよ：アカウントとキーがあればOK。NVIDIAはモデルがたくさん選べる。',
  },
  de: {
    whoHow: 'Am einfachsten: online, kostenlos, ohne Konto. Ein Tipp und fertig.\nFür mehr: ein kostenloser Schlüssel von Groq, Gemini oder NVIDIA, oder Ollama auf deinem PC über WLAN.',
    why: { noOllama: 'Ich finde das Ollama deines PCs nicht.', asleep: 'Ich finde das Ollama deines PCs nicht.', noModel: 'Das Ollama deines PCs hat noch kein Modell.' },
    online: 'Kostenlos, ohne Konto', local: 'Ollama auf meinem PC',
    pasteHow: 'Tipp auf „Wo bekomme ich einen?“, erstell den Schlüssel im Browser, kopier ihn und füg ihn hier ein. Ich speichere ihn verschlüsselt auf diesem Handy.',
    readyHow: 'Tipp mich an, wann immer du reden willst.',
    pcLine: 'Welche IP hat dein PC?',
    pcHow: 'Auf deinem PC: Öffne Ollama, schalte in den Einstellungen „Expose Ollama to the network“ ein und gib hier die IP des PCs ein. Beide im selben WLAN.',
    pcNote: 'Der PC zeigt seine IP unter Einstellungen › Netzwerk. Etwa 192.168.1.20.',
    pcBad: 'An dieser Adresse antwortet nichts.',
    whichHow: 'Groq, Gemini und NVIDIA haben ein kostenloses Kontingent: ein Konto, ein Schlüssel, fertig. NVIDIA hat viele Modelle zur Auswahl.',
  },
  it: {
    whoHow: 'Il modo più facile: online, gratis, senza account. Un tocco e fatto.\nPer qualcosa di meglio: una chiave gratuita di Groq, Gemini o NVIDIA, oppure Ollama sul tuo PC via Wi-Fi.',
    why: { noOllama: 'Non trovo Ollama sul tuo PC.', asleep: 'Non trovo Ollama sul tuo PC.', noModel: 'Ollama sul tuo PC non ha ancora un modello.' },
    online: 'Gratis, senza account', local: 'Ollama sul mio PC',
    pasteHow: 'Tocca “Dove la trovo?”, crea la chiave nel browser, copiala e incollala qui. La conservo criptata su questo telefono.',
    readyHow: 'Toccami quando vuoi parlare.',
    pcLine: 'Qual è l’IP del tuo PC?',
    pcHow: 'Sul PC: apri Ollama, attiva “Expose Ollama to the network” nelle sue impostazioni e scrivi qui l’IP del PC. Entrambi sullo stesso Wi-Fi.',
    pcNote: 'Il PC mostra il suo IP in Impostazioni › Rete. Qualcosa come 192.168.1.20.',
    pcBad: 'A quell’indirizzo non risponde niente.',
    whichHow: 'Groq, Gemini e NVIDIA hanno un piano gratuito: un account, una chiave e sei a posto. NVIDIA ha tanti modelli tra cui scegliere.',
  },
};

const base = Object.getOwnPropertyDescriptor(Setup.prototype, 't').get;

Object.defineProperty(Setup.prototype, 't', {
  configurable: true,
  get() {
    const t = base.call(this);
    const a = LINES[this.brain.lang] || LINES.en;
    return { ...t, ...a, why: { ...t.why, ...a.why } };
  },
});

// Who answers her: the free online one first, a key, or your PC's Ollama. True once she can talk.
Setup.prototype.choose = async function choose(status) {
  const t = this.t;
  let why = '';
  if (status && !status.ok) {
    if (status.code === 'nokey') return this.paste(status.provider);
    if (status.code === 'nourl') return this.address();
    why = status.provider === 'ollama' ? t.why.noOllama : `${t.failed} ${status.error}`;
  }
  this.capri.perform('curious', this.capri.towardCenter());
  const id = await this.guide.ask({
    face: 'smug',
    line: t.who,
    quote: [why, t.whoHow].filter(Boolean).join('\n'),
    actions: [
      { id: 'online', label: t.online, primary: true },
      { id: 'key', label: t.key },
      { id: 'local', label: t.local },
      { id: 'later', label: t.later },
    ],
  });
  if (id === 'online') return this.use('pollinations');
  if (id === 'key') return this.pickKeyed();
  if (id === 'local') return this.ollama();
  return false;
};

// Ollama awake: on a phone that's your PC's, found by its address. True once it answers.
Setup.prototype.ollamaUp = async function ollamaUp(error = '') {
  const t = this.t;
  if (!error && (await this.host.ollamaState()).running) return true;
  const got = await this.guide.askText({
    face: error ? 'wide' : 'look',
    line: t.pcLine,
    quote: error ? `${t.pcBad} ${error}` : t.pcHow,
    placeholder: '192.168.1.20',
    tag: 'IP',
    note: t.pcNote,
    actions: [{ id: 'other', label: t.other }],
  });
  if (got?.action === 'other') {
    await this.choose(null);
    return false;
  }
  if (!got?.text) return false;
  this.working(t.checking);
  const state = await this.host.setOllamaHost(got.text);
  return state.running ? true : this.ollamaUp(state.host);
};

// The key steps, with NVIDIA among the services (the desktop's list doesn't have it).
Setup.prototype.key = async function key() {
  let ready = false;
  await this.guide.scene(async () => {
    const { provider } = await this.host.getConfig();
    if (KEYED.includes(provider)) ready = await this.paste(provider);
    else if (provider === 'custom') ready = await this.address();
    else ready = await this.pickKeyed();
  });
  if (ready) this.onReady?.();
};

Setup.prototype.pickKeyed = async function pickKeyed() {
  const t = this.t;
  const id = await this.guide.ask({
    face: 'smug',
    line: t.which,
    quote: t.whichHow,
    actions: [...KEYED.map((p, i) => ({ id: p, label: NAMES[p], primary: i === 0 })), { id: 'back', label: t.back }],
  });
  if (id === 'back') return this.choose(null);
  return KEYED.includes(id) ? this.paste(id) : false;
};

Setup.prototype.paste = async function paste(provider, error = '') {
  const t = this.t;
  const got = await this.guide.askText({
    face: error ? 'wide' : 'look',
    line: fmt(t.paste, { name: NAMES[provider] || provider }),
    quote: error ? `${t.keyBad} ${error}` : t.pasteHow,
    secret: true,
    placeholder: t.placeholder,
    tag: t.keyTag,
    note: t.keyNote,
    actions: [{ id: 'page', label: t.where }, { id: 'other', label: t.other }],
    onAction: (id) => id === 'page' && (this.host.keyPage(provider), true),
  });
  if (got?.action === 'other') return this.choose(null);
  if (!got?.text) return false;
  this.working(t.checking);
  const res = await this.host.useProvider({ provider, key: got.text });
  return res.ok ? this.done() : this.paste(provider, res.error);
};

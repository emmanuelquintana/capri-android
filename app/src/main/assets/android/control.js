// Capri using your phone for you (1.10.0, Emma: "controlar el celular"), out loud ("Capri, …",
// listen.js) or in her chat (asks.js): "ve atrás", "ve al inicio", "abre las notificaciones",
// "apps recientes", "baja" / "sube", "toca Ajustes", "escribe aquí hola", "captura de pantalla".
// Android does it through ControlService.kt, only if you turned it on (Accessibility); off, she
// says where. She never acts in bank apps nor types into passwords (it says why: "control:miss").

import { call, send, on } from './native.js';

// what you say → what she does; the text after "toca" / "escribe aquí" is what she looks for or types
const ASKS = [
  ['back', /^(?:ve |vete |regresa |vuelve |volver |go |va |volta |vai )?(?:atr[aá]s|back|en arri[eè]re|retour|pra tr[aá]s|para tr[aá]s|назад)(?: por favor| please| stp| por favor)?$|^(?:regresa|vuelve|go back|reviens|volta)$/iu],
  ['home', /^(?:ve |vete |vamos |go |va |vai |volta )?(?:al |a la |to the |to |à l['’]|à |pro |para o |ao |на )?(?:inicio|pantalla de inicio|pantalla principal|escritorio|home(?: screen)?|[ée]cran d['’]accueil|accueil|tela inicial|in[ií]cio|главн(?:ый|ую) экран|домой)$/iu],
  ['recents', /(?:apps?|aplicaciones|applications|aplicativos) recientes|recent apps|multitarea|multitasking|apps? recentes|applications r[ée]centes|недавние(?: приложения)?/iu],
  ['notifications', /^(?:abre|baja|mu[eé]strame|ens[eé][ñn]ame|open|show(?: me)?|pull down|ouvre|montre(?:-moi)?|abra|abre|mostra|открой|покажи)\s+(?:el panel de |las |mis |my |the |les |mes |as |minhas |o painel de )?(?:notificaciones|notifications|notifica[cç][õo]es|уведомления)$/iu],
  ['quick', /^(?:abre|baja|open|show|ouvre|abra|abre|открой)\s+(?:los |the |les |as |o )?(?:ajustes r[aá]pidos|quick settings|r[ée]glages rapides|configura[cç][õo]es r[aá]pidas|быстрые настройки)$/iu],
  ['screenshot', /^(?:toma |haz |saca |take |fais |prends |tira |faz |сделай )?(?:una |un |a |une |um |uma )?(?:captura(?: de pantalla| de tela)?|screenshot|capture d['’][ée]cran|print|скриншот)$/iu],
  ['down', /^(?:baja|bajale|b[aá]jale|desliza (?:hacia )?abajo|despl[aá]zate (?:hacia )?abajo|scroll(?: down)?|haz scroll|d[ée]file(?: vers le bas)?|descends|desce|rola (?:pra |para )?baixo|листай(?: вниз)?|прокрути(?: вниз)?)(?: m[aá]s| more| plus| mais| ещё)?$/iu],
  ['up', /^(?:sube|s[uú]bele|desliza (?:hacia )?arriba|despl[aá]zate (?:hacia )?arriba|scroll up|remonte|monte|d[ée]file vers le haut|sobe|rola (?:pra |para )?cima|прокрути вверх|листай вверх)(?: m[aá]s| more| plus| mais| ещё)?$/iu],
];
// "toca Ajustes", "pulsa en Enviar", "tap Settings", "touche Paramètres", "toca em Configurações", "нажми Настройки"
const TAP = /^(?:toca|t[oó]cale|pulsa|presiona|dale(?: clic| click)?(?: a)?|haz (?:clic|click)|tap|click|press|hit|touche|appuie|clique|toque|clica|aperta|нажми|тапни)\s+(?:en |sobre |on |sur |em |no |na |на )?(?:el |la |los |las |the |le |les |o |a |os |as |кнопку )?(?:bot[oó]n |button |bouton |botão )?[«"“']?(.+?)[»"”']?[.!]?$/iu;
// "escribe aquí hola", "type here hello", "écris ici salut", "escreve aqui oi", "напиши здесь привет"
const TYPE = /^(?:escribe|teclea|pon|type|write|[ée]cris|tape|escreve|digita|напиши|введи|напечатай)\s+(?:aqu[ií]|ah[ií]|ac[aá]|here|there|ici|l[aà]|aqui|a[ií]|здесь|тут)[:,]?\s+[«"“']?(.+?)[»"”']?$/iu;
const SONG = /canci|m[uú]sica|music|song|chanson|guitarra|guitar|piano|песн|музык/iu;

const T = {
  es: {
    ok: { back: 'Atrás.', home: 'Al inicio.', recents: 'Tus apps recientes.', notifications: 'Tus notificaciones.', quick: 'Ajustes rápidos.', screenshot: 'Listo, captura.', down: 'Bajando.', up: 'Subiendo.', tap: 'Toco «{x}».', type: 'Escrito.' },
    off: 'Para eso necesito permiso: actívame en Accesibilidad › Capri usa tu celular. Te abro la pantalla.',
    miss: { bank: 'Ahí no: es una app de banco o de pagos. Eso lo haces tú.', password: 'Contraseñas no. Esa escríbela tú.', locked: 'Primero desbloquea el celular.', notFound: 'No veo «{x}» en tu pantalla.', noField: 'No encuentro dónde escribir. Toca el campo primero.', nothing: 'No veo nada que pueda tocar ahí.', failed: 'No pude. Android no me dejó.', old: 'Tu Android no me deja hacer eso.', unknown: 'No sé hacer eso todavía.' },
  },
  en: {
    ok: { back: 'Back.', home: 'Home.', recents: 'Your recent apps.', notifications: 'Your notifications.', quick: 'Quick settings.', screenshot: 'Done, screenshot.', down: 'Scrolling down.', up: 'Scrolling up.', tap: 'Tapping “{x}”.', type: 'Typed.' },
    off: 'I need permission for that: turn on Accessibility › Capri uses your phone. Opening it for you.',
    miss: { bank: 'Not there: that’s a bank or payments app. You do that one.', password: 'No passwords. Type that one yourself.', locked: 'Unlock your phone first.', notFound: 'I don’t see “{x}” on your screen.', noField: 'I can’t find where to type. Tap the field first.', nothing: 'I don’t see anything to tap there.', failed: 'I couldn’t. Android didn’t let me.', old: 'Your Android doesn’t let me do that.', unknown: 'I don’t know how to do that yet.' },
  },
  fr: {
    ok: { back: 'Retour.', home: 'Accueil.', recents: 'Tes apps récentes.', notifications: 'Tes notifications.', quick: 'Réglages rapides.', screenshot: 'C’est fait, capture.', down: 'Je descends.', up: 'Je remonte.', tap: 'Je touche « {x} ».', type: 'C’est écrit.' },
    off: 'Il me faut la permission : active Accessibilité › Capri utilise ton téléphone. Je t’ouvre l’écran.',
    miss: { bank: 'Pas là : c’est une app de banque ou de paiement. Ça, c’est toi.', password: 'Pas les mots de passe. Celui-là, écris-le toi.', locked: 'Déverrouille d’abord ton téléphone.', notFound: 'Je ne vois pas « {x} » sur ton écran.', noField: 'Je ne trouve pas où écrire. Touche le champ d’abord.', nothing: 'Je ne vois rien à toucher là.', failed: 'Je n’ai pas pu. Android ne m’a pas laissée.', old: 'Ton Android ne me laisse pas faire ça.', unknown: 'Je ne sais pas encore faire ça.' },
  },
  pt: {
    ok: { back: 'Voltei.', home: 'Início.', recents: 'Seus apps recentes.', notifications: 'Suas notificações.', quick: 'Configurações rápidas.', screenshot: 'Pronto, print.', down: 'Descendo.', up: 'Subindo.', tap: 'Tocando em «{x}».', type: 'Escrito.' },
    off: 'Preciso de permissão pra isso: ativa em Acessibilidade › Capri usa seu celular. Vou abrir a tela pra você.',
    miss: { bank: 'Aí não: é um app de banco ou de pagamento. Isso é com você.', password: 'Senha não. Essa você digita.', locked: 'Desbloqueia o celular primeiro.', notFound: 'Não vejo «{x}» na sua tela.', noField: 'Não acho onde escrever. Toca no campo primeiro.', nothing: 'Não vejo nada pra tocar aí.', failed: 'Não consegui. O Android não deixou.', old: 'Seu Android não me deixa fazer isso.', unknown: 'Ainda não sei fazer isso.' },
  },
  ru: {
    ok: { back: 'Назад.', home: 'На главный.', recents: 'Твои недавние приложения.', notifications: 'Твои уведомления.', quick: 'Быстрые настройки.', screenshot: 'Готово, скриншот.', down: 'Листаю вниз.', up: 'Листаю вверх.', tap: 'Нажимаю «{x}».', type: 'Написала.' },
    off: 'Для этого нужно разрешение: включи Спецвозможности › Капри управляет телефоном. Открываю.',
    miss: { bank: 'Только не там: это банк или платёжное приложение. Это делай сам(а).', password: 'Пароли — нет. Его введи сам(а).', locked: 'Сначала разблокируй телефон.', notFound: 'Не вижу «{x}» на экране.', noField: 'Не нахожу, куда писать. Сначала нажми на поле.', nothing: 'Не вижу, что там нажать.', failed: 'Не получилось. Android не дал.', old: 'Твой Android не даёт мне это сделать.', unknown: 'Я пока так не умею.' },
  },
};

let isOn = false;
let lang = 'es';
let wired = false;
export function wire() {
  if (wired) return;
  wired = true;
  call('control:state').then((v) => { isOn = !!v; }).catch(() => {});
  on('control:state', (p) => { isOn = !!p?.on; });
  // what went wrong, after she said she'd do it
  on('control:miss', (p) => {
    const t = T[lang] || T.en;
    const line = (t.miss[p?.why] || t.miss.failed).replace('{x}', p?.what || '');
    window.dev?.panel?.say({ kind: 'info', face: p?.why === 'bank' || p?.why === 'password' ? 'smug' : 'aside', line, hold: 6000 });
  });
}

/** "Capri, ve atrás"…: what she does, or null when it isn't one of these. */
export function controlAsk(text, language) {
  wire();
  lang = language;
  const said = String(text || '').trim().replace(/^(?:capri|капри)[,!]?\s+/iu, '').replace(/^(?:por favor|please|s['’]il te pla[iî]t|пожалуйста)[,]?\s+/iu, '').replace(/[.!¡¿?]+$/u, '').trim();
  if (!said || said.split(/\s+/).length > 12) return null;
  let what = ASKS.find(([, re]) => re.test(said))?.[0];
  let arg = '';
  if (!what) {
    const typed = TYPE.exec(said);
    if (typed) { what = 'type'; arg = typed[1]; }
  }
  if (!what && !SONG.test(said)) {
    const tap = TAP.exec(said);
    if (tap && tap[1].split(/\s+/).length <= 5) { what = 'tap'; arg = tap[1]; }
  }
  if (!what) return null;
  const t = T[language] || T.en;
  if (!isOn) {
    call('control:state').then((v) => { isOn = !!v; }).catch(() => {});
    send('control:settings');
    return { line: t.off, face: 'aside' };
  }
  // a moment, so her chat or her answer isn't what gets the tap
  setTimeout(() => send('control:do', { what, text: arg }), 700);
  return { line: t.ok[what].replace('{x}', arg), face: 'smug' };
}

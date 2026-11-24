// (1.12.0, community: "que a Capri possa falar as notícias do dia, tipo um Jarvis do Tony Stark") "Capri, ¿qué
// noticias hay?": the day's top headlines from Google News, in your language and country, said by her. Only the
// headlines' list is asked for (no account, nothing of yours sent); a tap on her panel opens the full page.

import { nativeFetch } from './fetch.js';

export const NEWS_ASK = /(?:^|[^\p{L}])(?:noticias|las noticias|qu[ée] (?:hay de )?nuevo en el mundo|news|headlines|what'?s happening|nouvelles|actualit[ée]s|infos du jour|not[ií]cias|новости|tin tức|ニュース|nachrichten|neuigkeiten|notizie)(?![\p{L}])/iu;

const FEEDS = {
  es: 'hl=es-419&gl=MX&ceid=MX:es-419', en: 'hl=en-US&gl=US&ceid=US:en', fr: 'hl=fr&gl=FR&ceid=FR:fr', pt: 'hl=pt-BR&gl=BR&ceid=BR:pt-419',
  ru: 'hl=ru&gl=RU&ceid=RU:ru', vi: 'hl=vi&gl=VN&ceid=VN:vi', ja: 'hl=ja&gl=JP&ceid=JP:ja', de: 'hl=de&gl=DE&ceid=DE:de', it: 'hl=it&gl=IT&ceid=IT:it',
};
const SAY = {
  es: { intro: 'Lo más sonado de hoy:', none: 'No pude traer las noticias. ¿Tienes internet?' },
  en: { intro: 'Today’s top stories:', none: 'I couldn’t get the news. Are you online?' },
  fr: { intro: 'Les gros titres du jour :', none: 'Je n’ai pas pu récupérer les infos. Tu as internet ?' },
  pt: { intro: 'As principais de hoje:', none: 'Não consegui pegar as notícias. Você tá com internet?' },
  ru: { intro: 'Главное за сегодня:', none: 'Не получилось загрузить новости. Есть интернет?' },
  vi: { intro: 'Tin nổi bật hôm nay:', none: 'Mình không lấy được tin. Bạn có mạng không?' },
  ja: { intro: '今日の主なニュース：', none: 'ニュースを取れなかった。ネットにつながってる？' },
  de: { intro: 'Die Top-Meldungen von heute:', none: 'Ich konnte die Nachrichten nicht laden. Bist du online?' },
  it: { intro: 'Le notizie principali di oggi:', none: 'Non sono riuscita a prendere le notizie. Sei online?' },
};

/** The first [n] headlines, without the source's name at the end ("… - El Universal"). */
export async function headlines(lang, n = 4) {
  const q = FEEDS[lang] || FEEDS.en;
  const res = await nativeFetch(`https://news.google.com/rss?${q}`, { signal: AbortSignal.timeout(12000) });
  if (!res.ok) throw new Error(`news ${res.status}`);
  const xml = new DOMParser().parseFromString(await res.text(), 'text/xml');
  return [...xml.querySelectorAll('item > title')].map((e) => e.textContent.replace(/\s+[-–|]\s+[^-–|]{2,60}$/, '').trim()).filter(Boolean).slice(0, n);
}

/** What she says: her intro and the headlines, one per line. */
export async function newsLine(lang) {
  const s = SAY[lang] || SAY.en;
  try {
    const list = await headlines(lang);
    if (!list.length) return { ok: false, line: s.none };
    return { ok: true, line: `${s.intro}\n${list.map((h) => `• ${h}.`).join('\n')}`, url: `https://news.google.com/?${FEEDS[lang] || FEEDS.en}` };
  } catch {
    return { ok: false, line: s.none };
  }
}

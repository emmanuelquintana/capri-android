# Journal des modifications (français)

Le même que CHANGELOG.md (en espagnol, l’original), en français : l’appli l’affiche quand elle est en français.

## 1.10.0

- **Fini l’écran blanc quand elle danse :** quand Capri dansait, écoutait de la musique ou se reposait, sur beaucoup
  de téléphones elle devenait un bloc blanc, puis tout l’écran blanchissait et l’appli se fermait. Ses animations
  utilisent maintenant bien moins de mémoire graphique, et si ton téléphone plante quand même deux fois pendant
  l’une d’elles, elle reste immobile (sans animations) quelques jours au lieu de se fermer.
- **Joue avec elle :** échecs, démineur, 21 et morpion, sans internet. Elle joue contre toi et dit ce qu’elle en
  pense. Dans l’onglet « Toi », ou dis-lui « Capri, on joue ».
- **Historique du chat :** dans « Toi », tout ce que tu lui as dit et ce qu’elle a répondu, par jour, avec une
  recherche. Ça reste sur ton téléphone et tu peux l’effacer.
- **Ta question n’est plus annulée :** si tu touches en dehors de son chat pendant qu’elle réfléchit, elle continue.
  Quand la réponse est prête, elle te la montre dans sa bulle, avec un bouton pour continuer à parler.
- **Messages par WhatsApp :** « Capri, dis à Ana sur WhatsApp que j’arrive ». Elle ouvre WhatsApp (ou Telegram,
  Messenger, SMS…) avec le message écrit ; tu choisis à qui et tu l’envoies.
- **Son rayon :** quand tu désinstalles une appli, Capri lui tire dessus et la désintègre.
- **Elle s’habille seule (optionnel) :** dans « Personnaliser », toutes les 6 h, 12 h ou chaque jour, elle choisit une
  autre tenue et te la montre. Ses couleurs ne changent jamais.
- **Économie de batterie (optionnel) :** dans « Toi », choisis 15, 20 ou 30 %. À ce niveau et sans charger, ce qui
  consomme le plus (ses oreilles, son regard en direct, ses animations) se repose jusqu’à ce que tu la charges.
- **Mode icône avec plus d’expressions :** elle rit, fait un clin d’œil, s’étonne, se fâche, tombe amoureuse,
  réfléchit avec des petits points, bouge avec ta musique, a le tournis si tu la traînes trop et s’endort si tu la
  laisses seule.
- **Elle se souvient mieux de toi :** elle garde plus de choses, apprend tes expressions et les utilise de temps en
  temps, et te demande parfois comment tu vas.
- **Pendant que tu écris :** elle s’allonge sur le clavier ; en jeu, elle est sur son téléphone.
- **Sa voix peut être coupée :** dans « Personnaliser › Sa voix ». Elle t’écoute toujours et répond par écrit.
- **Micro :** corrigé « erreur 12 » sur certains Samsung sous Android 16.
- **Regarder ton écran :** si ton IA ne voit pas les images, elle en utilise une autre des tiennes qui le peut
  (Gemini, Groq…).
- **Groq :** ne choisit plus par erreur un modèle vocal qui demandait d’accepter des conditions.
- **Notifications et barre de musique :** sur certains Xiaomi, elles n’apparaissaient qu’après avoir ouvert un jeu
  lourd. Elles se reconnectent maintenant toutes seules.

## 1.9.1

- **Moins de fermetures et d’écrans blancs :** sur les téléphones avec une puce graphique modeste (Redmi, Honor,
  realme…) Capri se fermait ou restait blanche. En **mode léger** elle est maintenant dessinée sans 3D, et si ton
  téléphone perd son image deux fois de suite, il passe tout seul à ce dessin pendant quelques jours.
- **OpenRouter :** regarder des photos et des vidéos n’échoue plus avec « quelque chose a échoué ». Si tu n’as plus
  de crédits, elle te le dit clairement.
- **Mode gamer avec le cloud gaming :** détecte Boosteroid, GeForce NOW, Xbox Cloud, Steam Link, Moonlight, Parsec
  et d’autres.
- **Elle jette un œil à tes notifications :** elle n’apparaît plus à chaque fois et ne reste plus bloquée ; elle
  passe une fois et se repose un moment.
- **Micro :** plus de « La reconnaissance vocale a échoué (erreur 5) » en boucle, elle se souvient de la façon dont
  ton téléphone écoute, et plus de bip à chaque fois que le micro s’allume et s’éteint (le son multimédia est coupé
  un instant seulement ; jamais tes appels, alarmes ni notifications).
- **Avec une souris :** si tu utilises ton téléphone avec une souris, tu peux maintenant cliquer sur Capri.

## 1.9.0

- **Signaler un bug :** dans Réglages, raconte-nous ce qui s’est passé et ça arrive directement à Emma sur Discord,
  avec la version et le journal interne d’erreurs de Capri (sans ton nom, tes messages ni tes clés).
- **Son menu à l’horizontale :** avec le téléphone couché (en jouant, par exemple), il tient sur deux colonnes et a
  une barre qui montre qu’il y a plus en bas.
- **Garde-robe :** l’aura Sandevistan n’a plus l’air découpée (trois traînées nettes), les options de Transparence
  te montrent le rendu, et sur certains téléphones son aperçu n’apparaît plus comme un carré blanc.
- **Tes cours et ton agenda :** dans l’onglet « Toi », note les jours où tu as cours et l’heure, et une heure avant
  sa notification spéciale arrive, avec son visage et ses couleurs. Si tu veux, elle lit aussi ton agenda (en
  lecture seule, elle ne change jamais rien) et te rappelle ce qui approche : la veille au soir et une heure avant.
  Si elle est sur ton écran, elle te le dit aussi. Rien ne sort de ton téléphone.
- **Son icône dans tes notifications :** la barre du haut montre maintenant son petit visage au lieu d’un carré
  blanc, sur sa tuile de réglages rapides aussi.
- **Partage-lui des choses :** dans n'importe quelle appli, *Partager → Montrer à Capri* avec une image, une vidéo ou
  un texte. Elle ouvre son chat et te dit ce qu'elle en pense, comme une amie à qui tu l'envoies ; puis vous en
  parlez. D'une vidéo, elle voit trois moments. Son IA doit pouvoir voir les images.
- **Elle traduit ton écran :** « Capri, traduis ça » (ou « …en espagnol », « …en japonais ») dans son chat ou à voix
  haute : elle regarde ton écran et t'écrit le message, le post ou la page dans la langue demandée, ou dans la tienne.
- **Des conseils pour tes jeux :** « Capri, un conseil » ou « qu'est-ce que je fais ? » : elle regarde ta partie et
  te dit quelque chose d'utile pour ce moment. En mode gamer, avec son regard en direct activé, elle t'en donne un
  toutes les quelques minutes (désactivable dans Réglages).
- **Mode streaming :** quand tu es en live (TikTok, Twitch, YouTube, Kick…), elle lit sur ton écran les commentaires
  de tes viewers et leur répond à voix haute, comme ta co-animatrice. Aucun compte, aucune connexion à ces applis.
  Dans Réglages ou « Capri, mode streaming ».
- **Son accent :** dans Réglages › Parle-lui, choisis comment elle parle dans ta langue : France ou Québec ;
  Mexique, Espagne, Argentine… ; américain ou britannique ; Brésil ou Portugal.
- **Elle réagit toute seule à ce que tu fais :** avec de la musique ou une vidéo, elle écoute, danse ou joue de la
  guitare ; avec TikTok ou YouTube ouvert, elle grignote pendant que tu regardes ; quand tu joues, elle s'allonge pour
  te regarder. Pour la musique, plus besoin d'aucune autorisation ; pour savoir quelle appli est ouverte, elle
  utilise l'« Accès aux données d'utilisation » si tu l'as déjà donné.
- **On voit qu'elle t'écoute :** quand tu dis « Capri » et que tu lui parles, de petites barres apparaissent près de
  sa tête et bougent avec ta voix.
- **Discord à portée de main :** le bouton Discord est maintenant juste sous celui qui la lance.
- **Elle ne reste plus bloquée au démarrage :** sur certains téléphones, son image se perdait et laissait un carré gris
  avec un visage triste. Maintenant elle se rétablit toute seule, et si le téléphone ne peut pas la dessiner comme
  d'habitude, elle est dessinée plus simplement au lieu de ne pas apparaître.
- **Moins de « serveurs saturés » :** avec l'IA gratuite, elle attend son tour au lieu d'abandonner, et si la tienne est
  pleine, une autre dont tu lui as donné la clé répond, pour qu'elle te réponde toujours.
- **Ton anniversaire :** de temps en temps elle te demande quand c'est (ou dis-le-lui : « mon anniversaire c'est le
  3 mai »), et ce jour-là elle te fête : confettis, elle te le souhaite à voix haute et fait sa fête.
- **Effet Cyberpsychose :** dans sa Garde-robe › Effets, des copies rouges d'elle qui tremblent autour d'elle, comme
  le chrome qui prend le dessus.
- **Sa voix ne se déclenche plus quand tu bouges le téléphone :** avec le filtre strict, elle ignore ce qu'elle
  entend pendant que le téléphone bouge (son frottement, pas toi), et son nom seul doit être bien clair.
- **Si sa page se fige** (un panneau à moitié dessiné, plus rien ne bouge), Android lui en donne une nouvelle et elle
  revient.
- **Elle répond plus vite :** elle garde la connexion avec son IA ouverte entre deux messages et la prépare quand tu
  ouvres son chat, donc ton message part sans attendre ; et si un serveur bloque, elle essaie un autre modèle plus tôt.
- **Changer d’onglet ne saccade plus :** l’app ne relit plus tous ses réglages à chaque changement, et la transition
  est plus courte et plus fluide.
- **Mode léger pour les vieux téléphones :** dans Réglages › Performances. Il s’active tout seul sur les téléphones
  avec peu de mémoire ou un vieil Android : elle est dessinée avec moins de pixels et ses menus perdent le flou, donc
  plus fluide. Et si l’« Android System WebView » du téléphone est trop ancien pour elle, l’app te prévient et
  t’emmène le mettre à jour.
- **Moins de cases grises :** sa fenêtre utilise beaucoup moins de mémoire graphique, ce qui laissait des parties
  d’elle en gris et la faisait se fermer et se rouvrir toute seule.
- **Le micro ne s’allume et ne s’éteint plus sans arrêt :** sur Android 13 ou plus récent, elle écoute en continu (le
  micro reste allumé, stable) au lieu de recommencer toutes les quelques secondes ; sur les autres, elle se repose un
  peu quand il n’y a rien à entendre.
- **Filtre « Très strict » plus fin :** elle ignore les voix lointaines, comme la télé ou quelqu’un à l’autre bout de
  la pièce.
- **Réglages rangés en trois onglets :** **Toi** (comment elle t’appelle, ton anniversaire, ce dont elle se souvient
  et comment elle prend soin de toi), **Personnaliser** (sa garde-robe, son apparence et son comportement à l’écran, et
  sa voix) et **Réglages** (le général : autorisations, langue, son IA, son ouïe, ses modes et les performances). Les
  autorisations n’ont plus d’onglet : elles sont en haut des Réglages.
- **Ton anniversaire dans l’app :** dans l’onglet Toi, choisis la date sans attendre qu’elle te la demande.
- **Nouvelles animations :** elle écoute la main à l’oreille, salue en mode streaming, souffle les bougies de son
  gâteau et fait sa fête pour ton anniversaire.
- **Statistiques anonymes :** une fois par jour, l’app signale « quelqu’un utilise Capri » avec un nombre au hasard
  (pas celui de ton téléphone), la version, la langue et la version d’Android, pour savoir combien de personnes
  l’utilisent. Rien de personnel. Ça se désactive dans les Réglages.
- **Mode fantôme rien que pour elle :** tes touches traversent Capri, mais ses cartes de notifications et de musique
  restent touchables. Pour qu’elles soient traversées aussi, active Personnaliser › « Ses cartes aussi ».
- **Elle passe la tête dans tes notifications (optionnel) :** quand tu tires le volet des notifications ou les
  réglages rapides, Capri passe la tête par le côté et te salue. À activer dans Personnaliser avec l’Accessibilité
  (désactivé par défaut) ; elle sait seulement que le volet s’est ouvert, elle ne lit et ne touche rien. Attention :
  certaines banques ne s’ouvrent pas avec l’Accessibilité activée.
- **Widget Capri :** mets-le sur ton écran d’accueil, à ses couleurs et avec son visage (il change quand elle est
  éteinte ou rangée) : la lancer, lui parler, qu’elle regarde ton écran, l’habiller, la ranger ou la sortir, d’un geste.
- **Elle t’entend quand tu mets une vidéo en pause :** elle mettait jusqu’à 3 secondes à réécouter après une pause
  sur YouTube et ratait le début de ta phrase ; maintenant elle écoute dès que le son s’arrête.

## 1.8.2

- **Ses animations aussi avec une tenue :** si tu lui avais mis une tenue, elle ne dansait plus sur ta musique, ne
  s’allongeait plus pour te regarder jouer et ne réagissait à rien. Maintenant si : l’animation la montre dans sa
  tenue à elle, et ta tenue revient à la fin.
- **Plus légère :** elle ne charge plus toutes ses animations et toutes ses tenues au démarrage, seulement chacune
  quand elle sert. Sur certains téléphones ça lui faisait manquer de mémoire : après une discussion l’écran clignotait,
  Capri disparaissait et sa réponse restait à moitié. Et si sa page tombe quand même, elle attend un peu avant de
  revenir au lieu de redémarrer en boucle.

## 1.8.1

- **La garde-robe, aussi comme écran de l’app :** *Réglages → Ouvrir sa garde-robe* ouvre maintenant son propre
  écran, avec Capri sur une scène en haut et les tenues, accessoires et effets en dessous. Chaque changement se voit
  sur elle tout de suite et, si elle est sur ton écran, elle change là aussi. Si tu préfères l’habiller comme avant,
  par-dessus ton écran, c’est toujours là : *L’habiller sur ton écran* (dans Réglages, sur cet écran, dans son menu
  rapide ou en lui disant « Capri, habille-toi »). À toi de choisir.
- **Corrigé :** dans *Réglages → Lui parler*, les boutons du filtre de bruit, de ta façon de parler et de sa voix ne
  faisaient rien quand Capri était sur ton écran. Ils changent maintenant quand tu les touches.

## 1.8.0

- **Sa garde-robe :** nouveau bouton *L’habiller* dans le menu rapide (aussi dans *Réglages → Garde-robe* ou en lui
  disant « Capri, habille-toi »). 12 tenues complètes : jockey de l’Ouest, uniforme scolaire, pilote de mecha,
  chasseuse de démons, ninja, netrunner, veste jaune, gothique, yukata, pyjama, sorcière et Noël. 25
  accessoires à combiner (couronne, oreilles de chat, cornes, auréole, chapeau de cowboy, bonnet de Noël, casque,
  lunettes, visière, écharpe, katana, manette, ballon cœur…), une aura et
  sa transparence. Pendant que tu l’habilles, elle monte sur le panneau pour que tu voies chaque
  changement sur elle ; elle bouge avec tout ça et c’est enregistré. Les *Réglages* de l’app la présentent, avec un
  bouton pour l’ouvrir.
- **Cyberpunk: Edgerunners :** Capri vient de la deuxième saison, alors la garde-robe a sa section de la série, avec
  des tenues inspirées des deux saisons (David, Lucy, Rebecca, Maine, Dorio, Kiwi, Pilar, Falco, Faraday, Adam
  Smasher, Gloria, Sasha, D., Talia Yang, Kingsley, Roman Carax, Avida et Kyoko), des accessoires de la série (la
  croix de David, le masque de Kiwi, le monofil de Lucy, le sac de Roman) et
  l’effet **Sandevistan** : des images rémanentes vertes, cyan et violettes derrière elle.
- **Un mode gamer plus confortable :** quand tu l’actives, elle s’allonge sur le ventre en haut de ton écran et bat
  des jambes en te regardant jouer, et tu peux la rendre semi-transparente seulement pendant que tu joues (*Garde-robe
  → Effets*). Touche-la et elle se relève.
- **Plus vivante :** de nouveaux visages (fâchée, triste, amoureuse, pensive, concentrée, qui s’ennuie, gênée, clin
  d’œil) selon ce qu’elle dit, de nouveaux flashs quand quelque chose la touche, et de nouvelles poses : quand elle
  s’ennuie, elle s’étale sur le dos, s’assoit en serrant ses genoux ou s’appuie au bord de l’écran ; elle dort roulée
  en boule et te fait coucou quand tu l’appelles. Elle partage aussi une pensée de temps en temps et a beaucoup plus
  de phrases et de mouvements, donc elle se répète bien moins.
- **Animée image par image, comme un anime :** elle salue, danse, rit, pleure, tape du pied, rougit, tombe
  amoureuse, réfléchit pendant que son IA te répond, serre une pile pendant que ton téléphone charge, tombe à plat
  quand la batterie est faible, mange, regarde son téléphone, s’étire au réveil et fait un sprint Sandevistan. En mode
  gamer, elle s’allonge en haut de ton écran et joue parfois avec toi.
  Elle scanne aussi ton écran avec sa visière pendant que son IA le regarde, salue et disparaît en glitch quand tu
  la caches, a le tournis si tu la lances fort, hoche la tête sur ta musique, s’étire avec toi pendant une pause,
  bâille tard le soir, pirate quand elle ouvre quelque chose pour toi, sursaute à tes notifications, fait des selfies
  et de l’air guitar.
- **Elle prend soin de toi et de ton téléphone :** elle te prévient quand ton téléphone est chargé et quand la
  batterie est faible, au pourcentage que tu choisis (*Réglages → Elle prend soin de toi* ou son menu → *Ton
  téléphone*). Et si tu es collé à l’écran depuis des heures, elle te dit de reposer tes yeux (toutes les 1, 2 ou 3
  heures, ou jamais).
- **Elle ne parle plus d’elle-même quand elle regarde ton écran :** avant d’envoyer la capture, elle s’efface, elle,
  son panneau et ses textes, donc elle ne commente que ce qu’il y a sur ton écran.
- **Quelle IA peut voir ton écran :** les services et modèles qui voient les images ont maintenant un 👁 dans l’appli
  et dans son menu. Si celui que tu as choisi ne voit pas, ou n’a plus de quota, elle en essaie toute seule un autre
  qui voit.
- **« L’IA est saturée », beaucoup moins :** quand un modèle n’a plus de quota, elle le laisse se reposer et en
  utilise un autre au lieu d’insister. S’ils se reposent tous, elle te dit jusqu’à quand.
- **Des commandes vocales qui font vraiment quelque chose :** l’heure et la date, mettre en pause ou lancer la
  musique, la météo, les captures, les réglages (Wi‑Fi, données, Bluetooth, localisation…), appeler quelqu’un, ses
  mouvements (« Capri, danse », « saute », « tourne »…) et « Capri, qu’est-ce que tu sais faire ». Si quelque chose
  n’est pas possible, elle le dit au lieu de rester à réfléchir.
- **Sa voix et ses oreilles, à ton goût :** dans *Réglages → Lui parler*, tu choisis son filtre anti-bruit (utile avec
  du bruit de fond : au plus strict, elle ne réagit que si tu dis « Capri » d’abord et clairement), combien de temps
  elle attend que tu finisses de parler, et le ton et la vitesse de sa voix.
- **L’attraper ne rate plus :** quand tu la maintiens, un anneau apparaît et ça vibre ; si tu lâches, le menu rapide
  s’ouvre, et si tu bouges le doigt, tu la déplaces (avant, le menu du système apparaissait parfois ou tu ne pouvais
  pas la bouger sans toucher *Parler*). En modes fantôme et gamer, son visage répond toujours au toucher.
- **« Revoir l’accueil » ne casse plus rien :** il demande confirmation et garde l’IA et la clé que tu avais déjà.
- **Corrigé :** l’erreur « messages… content must be a string » en regardant ton écran avec un modèle Groq qui ne voit
  pas les images.
- Et un secret. Demande-lui quelque chose que tu ne lui as jamais demandé. 💍

## 1.7.1

- **Menu rapide :** en la gardant appuyée, un menu court s’ouvre avec ce que tu utilises le plus : parler, regarder
  ton écran, téléport, satellite, la ranger, quelques mouvements (sauter, tourner, s’étirer…) et ses modes en
  interrupteurs (en direct, fantôme, icône, sans gravité, gamer). De là, *Tous les réglages*.
- **Des réglages plus clairs :** le menu complet est rangé par sections (son IA, elle, ton téléphone, plus) et
  l’écran derrière s’assombrit (et se floute sous Android 12 ou plus récent), pour se lire au-dessus de n’importe
  quelle appli.
- **Modèle d’IA en deux étapes :** d’abord le service (Gemini, Groq, NVIDIA…), puis son modèle à l’intérieur, sans
  la longue liste mélangée.
- **Son panneau n’apparaît plus sans fond sur les téléphones plus anciens :** sur certains (avec un WebView Android
  antérieur à la version 111), ses panneaux et ses cartes étaient transparents, avec le texte noir sur ton fond
  d’écran. Maintenant ses couleurs s’affichent partout.
- **Avec une clé Gemini, elle répond bien plus vite :** les modèles Flash de Gemini « réfléchissent » avant de
  répondre, parfois si longtemps qu’elle restait sur « Laisse-moi réfléchir… » et que son chat abandonnait.
  Maintenant elle leur demande de moins réfléchir, comme avec l’IA gratuite.

## 1.7.0

- **Mode icône :** il ne reste à l’écran que son masque vert, les yeux ouverts, et elle continue à commenter de là
  (ce qu’elle voit sur ton écran, des encouragements en mode gamer, son chat, une phrase de temps en temps). Fais-le
  glisser du doigt là où il te gêne le moins ; touche-le pour ouvrir son menu. Tu peux ainsi la garder grande et,
  quand tu veux, qu’elle ne prenne presque pas de place. S’active dans *Réglages*, dans son menu ou en disant
  « Capri, mode icône » (et « Capri, quitte le mode icône »).
- **Sans gravité :** elle reste exactement là où tu la poses, même en l’air. Elle ne part pas se promener, ne saute
  pas sur tes notifications, et si tu la lances elle ne s’envole pas, donc elle ne se fâche plus. Idéal pour la
  laisser dans un coin ou à côté de tes chats. Dans *Réglages* ou son menu ; elle se souvient d’où tu l’as laissée.
- **Elle se souvient de toi :** après une discussion, Capri note ce qui compte pour toi (ce que tu aimes, les gens dont
  tu lui parles, ce qui t’arrive, comment ça s’est passé) et s’en souvient la fois suivante : vous reprenez là où vous
  en étiez au lieu de repartir de zéro. Tu le vois et l’effaces dans *Réglages → Ce qu’elle retient de toi* (une
  chose ou tout), et tu peux aussi le désactiver là. Ça reste seulement sur ton téléphone ; pour le noter, elle
  demande à son IA une fois par discussion, quand tu ne lui parles plus (avec l’IA gratuite, elle attend son tour pour
  ne pas te faire attendre).
- **IA de NVIDIA :** tu peux maintenant lui donner une clé NVIDIA gratuite (build.nvidia.com), avec plein de modèles
  au choix. Elle en prend un rapide pour discuter et, pour voir ton écran, un modèle NVIDIA qui voit les images. Si un
  modèle n’existe plus, elle passe toute seule au suivant.
- **Plus de langues :** dans *Réglages → Idioma · Language* choisis Automatique, Español, English, Français, Português
  ou Русский, pour l’appli et pour ce qu’elle dit. Les commandes vocales (« Capri, … ») marchent aussi dans ces
  langues.
- **Nouvel onglet Communauté** dans la barre du bas de l’appli : le Discord et le TikTok y sont maintenant (avant, ils
  étaient à la fin des *Réglages*), et c’est là qu’iront les nouveautés de la communauté.
- **Elle commente ce que tu regardes :** avec « Voir mon écran en direct » activé, quand tu changes de vidéo sur
  TikTok, Reels ou YouTube, elle donne son avis sur ce qu’elle voit (au plus une fois toutes les 50 secondes).
- **Elle voit ton écran avec plus d’IA :** si ta clé vient d’un service dont le modèle de chat ne voit pas les images
  (Groq, par exemple), elle regarde avec un modèle du même service qui, lui, voit.
- **Elle ne disparaît plus quand une notification arrive :** elle saute là où la carte s’arrête, pas là où son
  animation commence.
- **Le clavier ne se ferme plus tout seul** quand tu lui écris : elle ne bouge pas tant que son chat est ouvert.
- **Elle se branche sur les téléphones plus anciens :** sur certains sous Android 8 (comme le Sony Xperia XA1), la
  configuration de son IA échouait avec « Can't create handler inside thread that has not called Looper.prepare() ».
  C’est réglé.
- **Des erreurs compréhensibles :** au lieu de messages techniques comme « messages content must be a string » ou
  « signal perdu », elle te dit avec des mots simples ce qui s’est passé.
- **Chat vocal :** si l’IA met du temps à répondre, elle dit à voix haute qu’elle réfléchit, au lieu de rester muette.

## 1.6.2

- **Rejoins la communauté :** le Discord de la communauté et le TikTok d’Emma, à la fin des *Réglages* et à la fin du
  menu de Capri. La première fois que tu ouvres l’appli, elle te les montre une seule fois.

## 1.6.1

- **Le mode gamer automatique remarque enfin quand tu quittes le jeu :** avant, il attendait 20 secondes et parfois
  ne s’en rendait pas compte (par exemple si, en sortant, le téléphone ne disait pas quelle appli restait devant, ou
  avec les outils de jeu de Samsung). Maintenant il remarque aussi quand le jeu s’en va, ignore les panneaux flottants
  comme le volet des notifications ou Game Booster, et se désactive 10 secondes après ta sortie.
- *Réglages → Journal* note quelle appli elle voit devant et quand le mode gamer s’active ou se désactive, au cas où
  un jeu ne serait pas bien détecté.

## 1.6.0

- **Mode gamer (une idée de Capri elle-même) :** pour quand tu joues. Elle se fait toute petite dans un coin, ne se
  touche plus (tes touchers vont au jeu), ses alertes et sa barre de musique restent à l’écart, elle n’écoute plus et
  ne se promène plus. De temps en temps, elle t’encourage. Active-le dans *Réglages → Mode gamer*, dans son menu, en
  disant « Capri, mode gamer » ou en l’écrivant dans son chat. Il se désactive pareil (dans son chat : « désactive
  le mode gamer ») ou en la redémarrant, et quand tu as fini, elle te demande comment ça s’est passé.
- **Mode gamer automatique (facultatif) :** dans *Réglages → Mode gamer → Toute seule quand un jeu s’ouvre*, il
  s’active quand tu ouvres un jeu et se désactive quand tu le quittes (si tu le désactives en pleine partie, elle
  respecte ton choix). Il faut l’autorisation « Accès aux données d’utilisation » d’Android, qui montre seulement
  quelle appli est ouverte. Désactivé par défaut : certaines applis bancaires et antivirus se méfient de cette
  autorisation.

## 1.5.1

- **Tes vidéos et ta musique ne se mettent plus en pause :** avec « Lui parler » activé, chaque fois que Capri se
  remettait à écouter, le téléphone lui donnait le son et mettait en pause ce que tu regardais. Maintenant, pendant
  une vidéo ou de la musique, elle n’écoute pas (*Réglages → Lui parler* l’indique) et reprend toute seule à la fin.
  Pour la musique, utilise sa barre ou écris-lui.
- **Bouton « Coller » pour ta clé d’IA :** dans son panneau, l’appui long ne permet pas de coller. Maintenant il y a
  un bouton *Coller* qui met ce que tu as copié et le vérifie aussitôt. Dans l’appli, il y a aussi *Coller la clé*.
- **Une clé mise depuis l’appli ne redemande plus d’en mettre une :** si son panneau te demandait la clé et que tu
  l’as mise depuis l’appli, le panneau disparaît tout seul.
- **Barre de musique à ton goût :** dans *Réglages → Barre de musique* choisis *Toujours* (elle reste pendant la
  musique), *Un moment* (comme avant) ou *Jamais*.
- **Alertes plus haut :** les cartes de tes notifications n’apparaissent plus au milieu de l’écran, mais en haut,
  juste là où Capri peut encore grimper.
- **Capri ne se perd plus :** parfois, elle se penchait depuis le bord du haut, que la barre d’état cache sur un
  téléphone, et on croyait qu’elle avait disparu. Maintenant elle ne se penche que sur les côtés, et ne reste jamais
  cachée.
- **Sa notification, plus propre :** elle ne répète plus chaque notification ou chanson que tu reçois (Android les
  affiche déjà), elle dit « Sur ton écran. Touche ici pour ouvrir mon menu. », et sa dernière phrase disparaît au
  bout de 10 minutes.

## 1.5.0

- **Capri ne part plus en flèche :** quand tu la touchais ou la lâchais après l’avoir déplacée, elle s’envolait
  parfois comme si tu l’avais lancée. Maintenant, elle n’est lancée que si tu la jettes vraiment d’un geste rapide ;
  si tu la lâches simplement, elle tombe en douceur. Et quand tu la lances, c’est avec moins de force.
- **Choisis sa taille :** dans *Réglages → Capri sur ton écran*, de mini à énorme.
- **Mode fantôme :** Capri reste sur ton écran mais ne se touche pas et ne gêne pas : tes touchers passent à l’appli
  d’en dessous, comme un shimeji fantôme. S’active dans *Réglages* ou son menu.
- **Plus de mouvement :** elle ne reste plus plantée là. Choisis à quel point elle est agitée dans *Réglages →
  Mouvement* : calme (comme avant), normale ou agitée. Elle marche, saute, tourne, s’étire et regarde autour d’elle
  toute seule.
- **Capri voit ton écran :** dis-lui « Capri, regarde mon écran » (ou touche *Regarde mon écran maintenant* dans
  Réglages, ou dans son menu) et elle commente ce qu’elle voit. Avec *Regarder en direct*, elle continue de regarder
  et réagit toute seule de temps en temps, ou quand tu changes d’appli, jusqu’à ce que tu l’arrêtes. Android te
  demande à chaque fois qu’elle commence à regarder, et tu verras l’icône de diffusion pendant qu’elle regarde. Les
  captures ne sont pas enregistrées : elles vont seulement à son IA. Les applis protégées (banques, mots de passe)
  lui apparaissent en noir. Il faut une IA qui voit les images, comme Gemini (sa clé est gratuite) ; l’IA gratuite
  ne voit pas, et Capri te le dit.
- **Le chat ne gèle plus :** on sait enfin pourquoi c’était lent : l’IA gratuite ne permet qu’une réponse toutes les
  45 secondes par téléphone, et si tu écrivais avant, elle attendait ou échouait. Maintenant, Capri attend son tour
  et redemande toute seule, demande à l’IA de moins réfléchir pour répondre plus vite, et en attendant te dit qu’elle
  réfléchit au lieu de rester muette. Si tu veux des réponses instantanées, donne-lui une clé Gemini gratuite.
- **Elle choisit seule un meilleur modèle :** elle préfère les modèles rapides et vraiment faits pour discuter, et
  garde la liste quelques minutes en mémoire pour ne pas redemander à chaque fois.
- **Ta clé d’IA ne s’efface plus :** parfois, si l’IA était lente ou échouait une fois, Capri jetait la clé et disait
  qu’elle n’avait pas d’IA. Maintenant, elle ne l’enlève que si elle est vraiment invalide.
- **Elle parle plus comme Capri :** dans le chat, elle répond comme quelqu’un avec qui tu discutes, pas comme une
  assistante : elle réagit, plaisante, donne son avis et répond à ce que tu lui as vraiment dit. En espagnol, elle
  parle comme une Latina, avec un peu d’argot mexicain.

## 1.4.1

- **Plus de zones mortes à côté de Capri :** après une téléportation, toucher près d’elle (pas sur elle) ne faisait
  rien, ni Capri ni l’appli d’en dessous, jusqu’à ce que tu la déplaces. Sa zone tactile suit maintenant la silhouette
  de son corps au lieu d’un grand carré autour d’elle, et pendant qu’elle se téléporte, elle ne bloque rien.
- **Mise à jour depuis l’appli sur plus de téléphones :** sur certains (comme Samsung avec Android 16), l’installation
  échouait avec « Self update is blocked ». Maintenant la nouvelle version s’ouvre sur l’écran d’installation
  d’Android, comme si tu l’ouvrais depuis le navigateur. Si ça échoue encore, sa page s’ouvre pour la télécharger.

## 1.4.0

- **Cartes animées :** les cartes de tes notifications arrivent en glissant avec un glitch et repartent pareil, au
  lieu d’apparaître et disparaître d’un coup. Une petite barre rouge en bas montre combien de temps il leur reste.
- **La barre de musique n’est plus toujours là :** elle apparaît quand une chanson commence, change, ou que tu la mets
  en pause ou la relances, et s’en va toute seule au bout de 15 secondes. Si tu la touches, elle reste un peu plus.
  En pause, elle part au bout de 12 secondes.
- **Nouvel onglet « Commandes » :** tout ce que tu peux demander à Capri, à voix haute ou par écrit dans son chat, avec
  des exemples.
- **Mise à jour depuis l’appli, à nouveau :** *Mettre à jour* télécharge et installe la nouvelle version sur place,
  sans passer par le navigateur (Android te demande de confirmer). La première fois, il te demande de laisser Capri
  installer des applis.
- **Avis du système quand il y a une nouvelle version :** l’appli vérifie toutes les 3 heures, même Capri éteinte, et
  t’envoie une notification. En la touchant, l’appli s’ouvre et la mise à jour commence.
- **Nouveautés repliées :** chaque version s’affiche fermée, avec son nombre de changements ; touche-la pour la lire.
- **« Lui parler » plus fluide :** quand tu dis seulement « Capri », elle te répond dans son panneau sans parler, pour
  ne pas couvrir ce que tu dis ensuite, et elle attend un peu plus que tu finisses ta phrase.

# Journal des modifications (français)

Le même que CHANGELOG.md (en espagnol, l’original), en français : l’appli l’affiche quand elle est en français.

## 1.12.0

- **De nouveau sur les téléphones 32 bits :** depuis la 1.11.0, l’app ne s’installait pas sur les téléphones 32 bits
  (armeabi-v7a, comme le TECNO Spark Go 1S et beaucoup d’Android Go) : l’installateur disait que le processeur
  n’était pas compatible. Maintenant le même APK s’installe partout. Sur ces téléphones sa voix pour les réponses
  (le moteur) n’est pas disponible, mais ses phrases enregistrées et la voix du téléphone, si.
- **Sa voix répond beaucoup plus vite :**
  - Pendant qu’elle dit une phrase, elle prépare déjà la suivante. Avant, elle attendait d’avoir fini de parler pour
    commencer la suivante, donc les longues réponses prenaient deux fois plus de temps.
  - Elle commence à parler dès que la première partie de la phrase est prête, sans attendre la phrase entière.
  - Son moteur commence à se charger dès que tu lui écris ou lui parles, pendant que son IA réfléchit, et pas à la fin.
  - Il n’est plus déchargé chaque fois que tu quittes l’app, et il reste chargé plus longtemps sur les téléphones
    avec beaucoup de RAM (jusqu’à 15 minutes) : plus besoin de le recharger à chaque réponse.
  - Il utilise les cœurs rapides du processeur (jusqu’à 6) sur les téléphones qui en ont.
  - Elle se souvient des phrases déjà dites : celles qu’elle répète sortent tout de suite.
  - Quand une phrase ne se terminait pas, le moteur générait longtemps avant d’abandonner ; maintenant il s’arrête plus tôt.
- **Le pourcentage de sa voix dans le chat :** sa réponse s’affiche écrite tout de suite et en dessous tu vois où en
  est sa voix (*Génération de la voix 42 %*), puis *Elle parle*. Dans l’onglet Chat et dans sa bulle.
- **Elle ne reste plus muette :** si tu la touchais ou la déplaçais pendant qu’elle préparait la voix de sa réponse,
  sa phrase de caresse annulait toute la réponse. Maintenant sa réponse passe d’abord. Et pendant qu’elle prépare sa
  voix, elle continue de t’écouter.
- **Ses phrases enregistrées, plus souvent :** elles sonnent plus souvent quand tu la caresses, la déplaces ou la
  lances, et maintenant aussi quand elle te salue, quand elle s’ennuie, à l’ouverture et à la fermeture de son chat,
  quand internet coupe, quand tu joues et pour tes rappels.
- **Réglages de la voix :** chaque précision indique maintenant laquelle est la plus rapide. *Maximale* (BF16) est la
  plus fidèle mais sur un téléphone elle prend 2 à 3 fois plus de temps que *Légère* : si la vitesse compte, prends
  *Légère* ou *Haute*.
- **Elle parle pendant que son IA écrit encore :** dès que son IA termine la première phrase, sa voix commence à la
  dire pendant que le reste s’écrit ; elle n’attend plus la réponse entière. Dans l’onglet Chat, tu vois la réponse
  s’écrire en direct.
- **Son moteur arrive prêt :** en se chargeant, il fait un petit échauffement silencieux, pour que sa première réponse
  ne traîne pas.
- **Quatre nouvelles langues : vietnamien, japonais, allemand et italien.** Toute l’appli, ses répliques, les jeux,
  le mode sommeil, la garde-robe, les commandes vocales (« Capri, … », en japonais aussi « カプリ ») et les réponses
  de son IA. Choisis-la dans Réglages › Langue. Sa propre voix pour les réponses parle espagnol et anglais pour
  l’instant ; dans ces langues, elle utilise la voix du téléphone.
- **Elle ne reste plus bloquée à réfléchir dans le chat :** si tu écrivais pendant qu’elle répondait encore, elle
  disait « Une seconde, je réfléchis encore au message d’avant » puis plus rien. Maintenant chaque message attend son
  tour et reçoit sa réponse. Si son IA s’arrête à mi-réponse, elle garde ce qu’elle a déjà écrit.
- **L’IA gratuite ne se bloque plus elle-même :** elle ne traite qu’une requête à la fois par téléphone. Ses notes
  sur toi et ses neurodanses attendent maintenant la fin de ton message (et s’effacent si tu écris).
- **Plusieurs clés API :** sur l’écran de son IA, tu peux coller plusieurs clés du même service, séparées par des
  virgules, ou en ajouter une plus tard. Quand l’une n’a plus de quota (comme Gemini gratuit), elle passe tout de
  suite à la suivante. Et quand un modèle n’a plus de quota, elle passe au suivant sans attendre.
- **Elle te prévient de tes messages :** de temps en temps, quand une notification arrive, elle te le dit avec sa
  voix enregistrée.
- **Sa voix démarre encore plus tôt :** si la première phrase de sa réponse est longue, elle commence par sa
  première partie (jusqu’à la virgule).
- **Écoute passive, comme « Dis Siri » :** son oreille utilisait la reconnaissance vocale du téléphone, qui sur
  beaucoup de téléphones n’écoute que quelques secondes : le micro s’allumait et s’éteignait toute la journée, avec
  son bip. Maintenant un petit modèle vocal, dans le téléphone, n’écoute que son prénom en continu : pas de bips,
  pas de micro qui clignote, rien ne sort du téléphone. Il se télécharge une fois par langue (environ 40 Mo) en
  Wi-Fi, ou depuis **Réglages › Réglages de voix › Écoute passive**.
- **Elle se réveille avec sa voix :** si elle dort et que tu l’appelles, elle se réveille en le disant avec une de
  ses phrases enregistrées.
- **Sa voix, plus rapide sur les téléphones puissants :** avec 8 Go de RAM ou plus, le moteur ne décharge et ne
  recharge plus des parties du modèle après chaque phrase. Sans précision choisie, elle prend la plus rapide ; si
  celle que tu as choisie est plus lente qu’elle ne parle, ses réponses en utilisent une plus rapide installée. Sa
  première phrase est plus courte et sa voix se charge dès que tu ouvres le chat.
- **Elle ne s’arrête plus après la première phrase :** elle continue avec le reste, et si la fin change, elle
  reprend là où elle en était.
- **Quelle IA lui répond, dans le chat :** en haut de l’onglet Chat, tu vois quelle IA répond et tu peux en changer
  d’un toucher (ou Ollama sur ton PC).
- **Phrases de message et de notification qui sonnent vraiment :** la première notification est toujours annoncée,
  même avec l’app ouverte.
- **Menu plus léger :** Communauté et Commandes vocales sont réunies dans un seul onglet.
- **Sa voix dans le journal :** les rapports disent combien de temps sa voix a mis à sonner.
- **Gemini, rapide à nouveau :** Google a fermé `gemini-2.5-flash` aux nouvelles clés et l’appli l’essayait encore en
  premier à chaque message. Désormais un modèle disparu n’est plus utilisé et elle passe directement à celui que
  Google indique.
- **Spotify et la précision de sa voix sont enregistrés :** avec Capri allumée, choisir Spotify ou une précision
  dans les réglages de voix ne s’enregistrait pas. Corrigé.
- **Jeux :** la difficulté se change dans chaque jeu (bouton ◆), aussi depuis l’onglet Jeux. Au Puissance 4, vous
  commencez chacun votre tour.
- **Écrans complets bien placés :** dans la Garde-robe et les jeux, le texte ne passe plus sous l’heure et la
  batterie, et sa silhouette du coin n’est plus derrière les boutons de navigation.
- **Elle n’apparaît plus par-dessus les jeux** quand sa page redémarrait pendant une partie.
- **Garde-robe :** correction d’une erreur qui laissait la liste des Accessoires vide.
- **Confirmation avant d’effacer ses souvenirs**, et **modifie tes cours** en les touchant.
- **Elle comprend son nom dit un peu autrement** (« Capi », « Cabi »…), et si tu demandes quelque chose sans son
  nom, elle te rappelle parfois : « Dis mon nom d’abord ».
- **Ton compte :** connecte-toi avec Google ou ton e-mail (dans « Toi ») et sa mémoire de toi, tes réglages, tes cours
  et ton chat sont gardés dans le cloud. Sur un nouveau téléphone ou après réinstallation, elle se souvient de toi.
  Tes clés d'IA ne quittent jamais ton téléphone.
- **Mode lecture :** dis-lui « je lis Dune » et « je suis à la page 120 », puis demande un résumé ou un personnage :
  elle répond seulement jusqu'à ta page, sans spoilers.
- **Des fichiers pour elle :** partage un PDF, un Word, un texte ou un audio (ou le 📎 du chat) et elle le lit ou
  l'écoute avec toi.
- **« Capri, quelle est cette chanson ? » :** elle écoute quelques secondes et te dit laquelle (avec une clé Gemini).
- **Elle répond à tes messages :** « Capri, réponds à Ana que j'arrive » répond depuis la notification.
- **Deux nouveaux widgets :** « Capri mini » et « Ta journée avec Capri » (date, batterie, prochain cours, boutons).
- **Les infos du jour :** « Capri, quoi de neuf ? » et elle te lit les titres.
- **Secoue ton téléphone** et elle a le tournis (désactivable dans les Réglages).
- **Attente avant de voir ton écran :** 0, 2, 4 ou 6 secondes, dans Modes.
- **Chercher dans ton chat :** le bouton ⌕ de l'onglet Chat.
- **Plus de bras en double :** certaines tenues dessinaient un second bras immobile. Corrigé.
- **Plus de tenue mélangée dans ses animations :** avec une tenue, elle ne repasse plus à ses habits habituels en
  pleine animation. Elle ne joue que celles dessinées pour cette tenue.
- **Skins de la communauté :** dans Personnaliser › « Crée ta propre Capri », télécharge son modèle, dessine une tenue et
  envoie-la. Si Emma et le serveur Discord l'approuvent, elle arrive dans une mise à jour avec ton nom.
- **Sa vraie voix :** avec le moteur de sa voix mais sans son profil, elle parlait avec la voix du téléphone. Le profil
  arrive maintenant avec le moteur, et s'il manque, elle le télécharge.
- **Écrire dans tes apps :** « Capri, écris ici … » avec son chat ouvert ne ferme plus ton clavier.
- **Seulement ta voix :** « Seulement ma voix » apprend ta voix les 3 premières fois, puis ignore son nom dit par
  d'autres voix (la télé, les gens autour). Tout reste sur ton téléphone.
- **Menu « Quelle IA lui répond ? » à son style :** le choix de l'IA du chat ressemble maintenant au reste de l'app.
- **Neurodanses avec du son :** ambiance (bourdonnement, parasites, glitchs) et elle les raconte de sa voix.
- **La déplacer à nouveau :** sur certains téléphones (Xiaomi), l'app croyait le clavier encore ouvert et empêchait de la
  bouger. Corrigé.
- **Télécharger sa voix sans erreur :** si la connexion coupe, le téléchargement reprend où il en était.
- **Sa voix n'est plus coupée au milieu :** les longues réponses sont dites en entier, aussi avec la voix du téléphone.
- **Trois nouveaux mini-jeux :** Réflexes, Devine le nombre et Plus ou moins, dans l'onglet Jeux.
- **Choisis la voix du téléphone :** dans Réglages de la voix › Voix du téléphone, écoute les voix de ton téléphone et
  choisis la plus proche de la sienne, pour quand sa propre voix n'est pas là.
- **Elle ne reste plus muette :** si son moteur de voix se bloquait, elle se taisait à toutes les réponses suivantes ;
  maintenant elle répond avec la voix du téléphone en attendant. Et si son IA échoue en pleine réponse, elle ne reste
  plus sur « Elle parle · 90 % » pendant une minute.
- **Sa voix dans le cloud (Fish Audio) :** dans Réglages de la voix, ajoute ta propre clé Fish Audio et elle parle avec
  sa voix en temps réel à chaque réponse, dès une demi-seconde, pendant que son IA écrit encore. Ta clé reste sur ton
  téléphone. Sans internet ou sans crédit, elle reprend toute seule sa voix du téléphone.
- **Ses phrases avec sa nouvelle voix :** ses 202 phrases enregistrées (espagnol et anglais) ont maintenant sa voix
  Fish Audio. Elles jouent toujours tout de suite et sans internet. Si tu avais déjà sa voix, les nouvelles se
  téléchargent toutes seules.
- **Réglages de la voix mieux rangés :** un menu court. En haut tu actives ou coupes sa voix, et chaque chose a son
  écran : Télécharger sa voix, Voix dans le cloud (ta clé), Comment elle sonne et Ses oreilles. Avant Réglages avancés,
  un nouvel interrupteur **Voix : cloud ou locale** choisit la voix qui te répond.
- **Réglages personnels en écrans :** ton prénom en haut et le reste chacun sur son écran.
- **App réorganisée :** elle s’ouvre sur Nouveautés ; nouvel onglet **Fonctions** (mode sommeil, tes cours, ses
  soins) ; l’onglet Commandes n’a que les commandes ; suivre sur TikTok sous Faire un don ; Écran et Modes séparés ; ton
  profil avec photo ou icône en haut des Réglages ; et ses clés enregistrées se voient (cachées).
- **Son chat répond plus vite :** quand son IA est saturée, celle qui a répondu passe en premier quelques minutes.
- **Ses phrases enregistrées aussi dans le chat :** pendant qu'elle réfléchit, de temps en temps elle dit une de ses phrases.

## 1.11.7

- **Son moteur de voix ne jette plus la réponse :** quand une phrase atteignait le maximum que le moteur peut
  générer, la réponse, déjà faite, était jetée et on n’entendait rien après l’attente la plus longue. Maintenant la
  limite suit la longueur de la phrase, et ce qu’il a fait s’entend. Pendant qu’il génère, il n’est plus annulé.
- **Maintenant elle parle vraiment :** ce qu’elle te répond dans son chat (sa bulle et l’onglet Chat) est aussi dit
  à voix haute, pas seulement quand tu lui parles avec « Capri, … ». Avant, ces réponses étaient seulement écrites,
  et c’est là que presque tout le monde lui parle. Ça se coupe dans Réglages de la voix (*Lire à voix haute ses
  réponses du chat*). Réponses longues : leurs premières phrases.
- **Ses phrases aussi en vibreur :** téléphone en vibreur (comme presque tout le monde), ses phrases (quand tu la
  caresses, la lances, qu’elle te salue) ne sonnaient pas. Maintenant si ; elles se taisent seulement en mode
  silencieux ou avec le volume multimédia à 0.

## 1.11.6

- **« Impossible : busy » en téléchargeant la voix la plus fidèle :** ça arrivait si tu touchais télécharger pendant
  qu’un autre téléchargement de sa voix était en cours, et ça cachait aussi la barre de celui en cours, comme s’il avait
  échoué. Maintenant on te dit d’attendre qu’il finisse (avec où il en est), et l’autre garde sa barre et sa notification.

## 1.11.5

- **Sa voix pour les réponses, maintenant dès Android 10 :** son moteur de voix (expérimental) marche désormais sur
  Android 10 et 11, pas seulement dès Android 12. Le reste de sa voix (ses phrases, la télécharger, l’écouter)
  marchait déjà dès Android 8. Rien ne change sur Android 12 ou plus récent.

## 1.11.4

- **Le menu pilule ne cache plus là où tu lui écris :** dans le chat, la ligne pour écrire restait sous le menu sur
  beaucoup de téléphones (Android 14 ou plus ancien). Maintenant elle est toujours au-dessus, même avec une grande police.

## 1.11.3

- **Tu vois où en est chaque téléchargement :** quand tu télécharges sa voix, ses moteurs ou une mise à jour, une
  notification montre l’avancement, puis si c’est fini (ou pourquoi pas). Les Réglages de la voix montrent aussi le
  pourcentage.
- **Tutoriel « Comment lui donner sa voix ? » :** pas à pas, avec le reste assombri : où la télécharger, comment
  l’écouter et comment faire que toutes ses réponses aient sa voix. Dans les Réglages de la voix et les Nouveautés.
- **« Écouter sa voix » répond toujours :** ça sonne maintenant même si Capri n’est pas lancée, et si ça ne peut pas
  sonner, l’app te dit pourquoi (pas encore téléchargée, volume multimédia à 0, sa voix coupée).
- **Le comparateur A/B de sa voix disparaît :** il embrouillait et ne sonnait pas.

## 1.11.2

- **« Télécharger sa voix » marche enfin :** en téléchargeant ses données de voix (environ 8 Mo), on voyait
  « Impossible : npy… » et son profil ne s’installait pas. L’app ne savait pas lire le format de son profil approuvé ;
  maintenant si, sans modifier le fichier. Ça arrivait sur tous les téléphones, Android 10 aussi. Si ça a échoué,
  touche à nouveau « Télécharger sa voix ».

## 1.11.1

- **Sa voix fidèle se télécharge enfin sur les téléphones de 6 et 8 Go :** l’app mesurait mal la mémoire (Android
  indique un peu moins que ce qu’a le téléphone) et bloquait les moteurs Haute et Maximale sur des téléphones qui
  peuvent les faire tourner. Maintenant : Légère dès 3 Go de RAM, Haute dès 4 Go et Maximale dès 6 Go. Si ça ne
  rentre vraiment pas, l’app te dit pourquoi.
- **Rappel :** son moteur de voix (expérimental) demande Android 12 ou plus récent. Sur Android 8 à 11, ses phrases
  enregistrées sonnent bien avec sa voix ; le reste utilise la voix du téléphone ou seulement le texte.

## 1.11.0

- **Le chat est le cœur de l’appli :** en ouvrant Capri, tu arrives directement sur son chat, pour lui parler
  tranquillement, avec tout ton historique (aussi ce que tu lui écris dans sa bulle). Elle répond avec sa mémoire,
  son humeur et ses pouvoirs. Si elle dormait, elle se réveille toute seule. Ce que tu peux lui demander est derrière
  le bouton « ? ».
- **Les jeux ont leur onglet** dans le menu pilule, chaque jeu à un toucher.
- **3 nouveaux jeux :** Puissance 4, Memory (avec ses visages) et Séquence neurale. Dans les deux premiers, elle joue
  contre toi et se souvient, comme dans les autres.
- **Capri flottante s’écarte pendant que tu joues** dans l’appli, pour ne pas cacher le jeu (optionnel, dans Jeux).
  Elle te regarde quand même depuis le coin.
- **Ses anciennes animations reviennent,** dont celle où elle mange des nouilles instantanées. Celles redessinées en
  1.10.0 s’en vont (oui, aussi celle du six seven). Seule reste celle allongée sur le clavier, plus calme.
- **8 nouvelles tenues :** Soubrette, Soubrette néon, Astronaute, Idol, Sweat gamer, Rockeuse punk, Charra et Hiver,
  dans sa Garde-robe. Et son animation allongée sur le clavier est redessinée, en 4 images calmes.
- **Mode sommeil :** Capri dort avec toi. Dans Réglages › Réglages personnels › Mode sommeil (ou « Capri, mode sommeil, réveille-moi à 7 h ») elle
  se blottit sur ton écran, compte combien de fois tu l’allumes la nuit et te gronde quand tu le fais. Avec un réveil,
  elle te réveille et ne s’arrête pas tant que tu ne l’éteins pas (avec « 5 minutes de plus »). Le matin, elle te dit combien tu as dormi.
- **Mode réaction :** dis-lui « Capri, regardons des vidéos ensemble » et, pendant que tu regardes une vidéo, elle la
  regarde avec toi, donne son avis de temps en temps et le note dans son chat pour s’en souvenir. « Désactive le mode réaction »
  l’éteint. Elle doit pouvoir voir ton écran.
- **Neurodanses :** donne-lui beaucoup d’affection (caresse-la, parle-lui) et de temps en temps elle t’enregistre une
  neurodanse : un de ses souvenirs, scène par scène, à regarder dans l’appli (Réglages › Réglages personnels › Neurodanses). Passe en couche
  thermique pour trouver des détails cachés. À la fin, elle te dit ce qu’elle en pense, et toi aussi. Aussi :
  « Capri, donne-moi une neurodanse ».
- **Elle parle au singulier :** plus de « nous » ni « on adore ». Elle parle d’elle comme d’une seule personne.
- **Bouton FAIRE UN DON** sous Démarrer Capri, Discord et TikTok : ouvre la fenêtre pour la soutenir sur PayPal.
- **Sa pilule dans la barre (Android 16+) :** à côté de l’heure, avec son humeur, et dans la Now Bar des Samsung,
  avec des boutons pour discuter, ouvrir son menu ou la cacher. Ça dépend du téléphone ; désactivable dans Réglages.
- **Mieux sur Huawei et Honor :** dans Réglages, une carte pour la laisser tourner en arrière-plan (sinon le
  téléphone la ferme), lever sa restriction de batterie et installer une voix. Les autorisations ouvrent le bon écran
  sur ces téléphones, et les liens de boutique vont vers AppGallery s’il n’y a pas de Play Store.
- **Nouveau lien Discord.**
- **Nouveaux menus :** le menu en pilule a maintenant Chat, Nouveautés, Communauté, Jeux, Personnaliser, Commandes et
  Réglages. Les commandes vocales ont leur propre page (aussi derrière le « ? » du chat). Les Réglages sont un menu
  d’écrans : Réglages personnels, Réglages de la voix, Autorisations, Langue et son IA, Écran et modes, Performances
  et téléphone, et Signaler un bug.
- **Visite guidée des nouveautés :** quand tu ouvres l’appli après la mise à jour, Capri te demande si tu veux
  découvrir les nouveautés et te guide pas à pas, le reste assombri. À revoir dans Nouveautés › Voir les nouveautés.
- **Capri Voice (sa voix) :** Capri parle maintenant avec sa propre voix, en espagnol latino et en anglais. Ses
  phrases enregistrées (quand tu la caresses, la lances, reviens, lui dis au revoir ou bonne nuit) se jouent hors
  ligne sans charger de modèle, avec le texte exact de ce qu’elle dit.
- **Télécharge-la à la mise à jour :** la première fois que tu ouvres l’appli après la mise à jour, Capri te demande
  si tu veux télécharger sa voix (environ 8 Mo : ses phrases et son profil). Elle vient du canal public de Capri sur
  GitHub et se vérifie fichier par fichier. Si tu dis « plus tard », c’est dans Réglages › Réglages de la voix.
- **Toutes ses réponses avec sa voix (optionnel, expérimental) :** avec son moteur de voix installé, tout ce qu’elle
  répond (réponses d’IA, avis) est dit avec sa voix, avec le même profil approuvé que sur PC. Le moteur s’ajoute ou se
  retire dans Réglages de la voix, en trois précisions : Légère (environ 880 Mo), Haute (environ 1,3 Go) et Maximale
  (environ 2,2 Go) ; plus de précision, c’est plus proche du PC et plus de mémoire. Il faut Android 12 ou plus récent,
  un téléphone 64 bits et assez de mémoire libre (l’appli te dit laquelle convient). Sans moteur, ses réponses
  utilisent la voix du téléphone ou le texte seul. Sur les téléphones modestes, elle peut mettre du temps à commencer
  à parler.
- Modes de sa voix (Auto, Léger, Équilibré, Fidélité max, Personnalisé), comparateur A/B et options avancées
  (importer, vérifier, supprimer) dans Réglages › Réglages de la voix.

## 1.10.1

- **La deuxième Capri ne tourne plus sur elle-même :** quand tu la divises et que la musique joue, sa jumelle se
  retournait sans arrêt comme si elle ne savait pas où aller. Maintenant elle danse et se retourne en rythme, de
  temps en temps.
- **Micro sur Huawei (et autres) :** Capri disait ne pas avoir la permission du micro alors qu’elle l’avait. C’était
  l’app vocale du téléphone qui ne l’avait pas. Maintenant elle essaie un autre service de reconnaissance et, si
  aucun ne peut, elle te dit quelle app a besoin de la permission.
- **Spotify joue la chanson :** si tu as choisi Spotify, « mets telle chanson » la lance vraiment, au lieu de juste
  la chercher.
- **S’installe sur Huawei et Honor :** l’APK porte aussi la signature classique que certains installateurs (dont
  ceux de Huawei) demandent. Si ça ne s’installe toujours pas, le site explique quoi désactiver (le « Mode pur »).
- **Soutenir Capri (facultatif) :** de temps en temps, jamais les premiers jours, un message propose de la soutenir
  sur PayPal. Avec « Ne plus afficher », il ne revient plus.
- **Animations plus propres :** dans plusieurs (danser, guitare, rire, s’étirer, écouter, certaines des jeux…)
  Capri glissait de côté d’une image à l’autre. Maintenant elle reste en place. Là où sa crinière était coupée net,
  elle s’estompe en douceur.

## 1.10.0

- **Son nouveau visage :** toutes ses animations, poses et expressions sont redessinées avec son vrai masque (la
  plaque verte et ses grands yeux), et deux nouvelles : allongée sur ton clavier et sur son téléphone quand tu joues.
- **Il ne lui manque plus un bras :** en mode léger, sur la grande image du vestiaire et dans ses vignettes,
  elle n’avait qu’un bras. Maintenant elle a ses deux mains avec toutes ses tenues, dans tous les modes.
- **Le chat ne bouge plus pendant que tu écris :** le clavier change un peu de taille en suggérant des mots, et ça
  déplaçait son chat.
- **Banques :** beaucoup d’applis bancaires ne marchent pas avec une autre appli par-dessus. Si tu lui as donné
  « Accès à l’utilisation », Capri se cache toute seule pendant que tu utilises ta banque et revient après.
- **Tes chansons sur Spotify (optionnel) :** dans « Personnaliser », choisis si les chansons demandées passent sur
  YouTube ou Spotify.
- **Plus de réponses vides :** si l’IA ne répond rien, Capri redemande ou prend un autre modèle.
- **Elle parle au singulier :** elle ne dit presque plus « on ».
- **Micro :** sur les téléphones qui écoutent quelques secondes à la fois, il s’allume et s’éteint moins dans le
  silence.
- **Regarder ton écran avec Groq :** si Groq n’a pas de modèle qui voit, elle prend une autre de tes IA.
- **Fini l’écran blanc quand elle danse :** quand Capri dansait, écoutait de la musique ou se reposait, sur beaucoup
  de téléphones elle devenait un bloc blanc, puis tout l’écran blanchissait et l’appli se fermait. Ses animations
  utilisent maintenant bien moins de mémoire graphique, et si ton téléphone plante quand même deux fois pendant
  l’une d’elles, elle reste immobile (sans animations) quelques jours au lieu de se fermer.
- **Joue avec elle :** échecs, démineur, 21, morpion, ping-pong et pierre-feuille-ciseaux, sans internet. Elle joue contre toi et dit ce qu’elle en
  pense. Dans l’onglet « Toi », ou dis-lui « Capri, on joue ».
- **Sa mémoire ne s’efface plus :** si Capri enregistrait quelque chose avant d’avoir fini de lire ce qu’elle se
  rappelait de toi, elle effaçait tout. Maintenant elle attend de l’avoir lu, et garde une copie de secours.
- **Elle surgit de nulle part :** sa tuile des réglages rapides ou le bouton de sa notification la font apparaître
  avec un glitch et un salut, même pendant une vidéo ou un jeu. Si tu actives « Elle utilise ton téléphone pour
  toi », le bouton d’accessibilité d’Android aussi.
- **Jeux cyberpunk :** l’écran des jeux est maintenant en néon de Night City, avec Capri qui te regarde depuis le coin,
  son masque comme pion au morpion, sur ses cartes et sur sa raquette de ping-pong, et un tampon à la fin de chaque
  partie.
- **Capri joue comme une vraie rivale :** elle ne répond plus instantanément ni toujours pareil. Elle a une humeur
  (sûre d’elle, agacée, dans la zone) qui se voit sur son visage et dans son jeu. Elle réfléchit plus quand le coup
  est difficile, hésite parfois et change d’avis, se trompe comme une personne (et parfois s’en rend compte),
  commente tes bons coups et tes erreurs, te presse si tu traînes et devient sérieuse si tu la bats souvent. Elle
  retient tes habitudes d’une partie à l’autre (ton ouverture aux échecs, quand tu restes au 21, ta main préférée) et
  tient le score contre toi. À pierre-feuille-ciseaux, elle t’annonce parfois ce qu’elle va jouer… et ça peut être un
  mensonge. Au ping-pong, elle réagit avec un temps de retard, se fatigue dans les longs échanges et smashe quand elle
  est sûre d’elle. Au démineur, elle te donne un indice si tu bloques.
- **Deux Capri :** écarte deux doigts sur elle (ou dis-lui « Capri, divise-toi ») et elles sont deux. L’autre marche,
  danse sur ta musique et te parle si tu la touches. Touche-la deux fois ou pose-la sur Capri pour les réunir.
- **Elle utilise ton téléphone pour toi (optionnel) :** « Capri, retour », « écran d’accueil », « ouvre les
  notifications », « descends », « touche Paramètres » ou « écris ici salut ». S’active dans Accessibilité
  (« Personnaliser » t’explique comment). Elle n’agit que quand tu le demandes, jamais dans les apps de banque ou de
  paiement, et n’écrit jamais de mots de passe.
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

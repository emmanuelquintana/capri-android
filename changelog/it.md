# Changelog (Italiano)

Lo stesso di CHANGELOG.md (spagnolo, l’originale), in italiano: l’app lo mostra quando è in italiano.

## 1.12.0

- **Di nuovo sui telefoni a 32 bit:** dalla 1.11.0 l’app non si installava sui telefoni a 32 bit (armeabi-v7a, come
  il TECNO Spark Go 1S e molti Android Go): l’installer diceva che il processore non era compatibile. Ora lo stesso
  APK si installa su tutti. Su quei telefoni la sua voce per le risposte (il motore) non è disponibile, ma le sue
  frasi registrate e la voce del telefono sì.
- **La sua voce risponde molto più veloce:**
  - Mentre dice una frase, sta già creando la successiva. Prima aspettava di finire di parlare, quindi le risposte
    lunghe ci mettevano il doppio.
  - Inizia a parlare appena ha la prima parte della frase, senza aspettare la frase intera.
  - Il suo motore inizia a caricarsi appena le scrivi o le parli, mentre la sua IA pensa, non alla fine.
  - Non viene più scaricato dalla memoria ogni volta che esci dall’app, e resta caricato più a lungo sui telefoni con
    tanta RAM (fino a 15 minuti).
  - Usa i core veloci del processore (fino a 6) sui telefoni che li hanno.
  - Ricorda le frasi che ha già detto, così quelle ripetute escono subito.
- **La percentuale della sua voce in chat:** la risposta appare scritta subito e sotto vedi quanta voce è pronta
  (*Creo la sua voce 42%*), poi *Sta parlando*. Nella scheda Chat e nel suo fumetto.
- **Non resta più zitta:** se la toccavi o la spostavi mentre preparava la voce, la sua frase delle carezze annullava
  tutta la risposta. Ora la risposta viene prima. E mentre prepara la voce, continua ad ascoltarti.
- **Le sue frasi registrate, più spesso:** quando la accarezzi, la sposti o la lanci, e ora anche quando ti saluta,
  quando si annoia, quando si apre e si chiude la chat, se salta internet, quando giochi e nei promemoria.
- **Parla mentre la sua IA sta ancora scrivendo:** appena la sua IA finisce la prima frase, la sua voce inizia a
  dirla mentre il resto viene scritto. Nella scheda Chat vedi la risposta scriversi dal vivo.
- **Il motore arriva pronto:** quando si carica fa un piccolo riscaldamento silenzioso, così la prima risposta non
  tarda.
- **Quattro nuove lingue: vietnamita, giapponese, tedesco e italiano.** Tutta l’app, le sue frasi, i giochi, la
  modalità sonno, il guardaroba, i comandi vocali («Capri, …») e le risposte della sua IA. Sceglila in Impostazioni ›
  Lingua. La sua voce per le risposte per ora parla spagnolo e inglese; in italiano usa la voce del telefono.
- **Non resta più bloccata a pensare nella chat:** se le scrivevi mentre stava ancora rispondendo, diceva «Dammi un
  secondo» e poi niente. Ora ogni messaggio aspetta il suo turno e riceve la sua risposta. Se la sua IA si ferma a
  metà, tiene quello che ha già scritto.
- **L’IA gratuita non si intralcia più da sola:** accetta una richiesta alla volta per telefono. Le sue note su di te
  e le sue braindance ora aspettano che il tuo messaggio finisca (e si fanno da parte se scrivi).
- **Più chiavi API:** nella schermata della sua IA puoi incollare più chiavi dello stesso servizio, separate da
  virgole, o aggiungerne un’altra dopo. Quando una finisce la quota (come Gemini gratuito), passa subito alla
  successiva.
- **Ti avvisa dei tuoi messaggi:** ogni tanto, quando arriva una notifica, te lo dice con la sua voce registrata.
- **La sua voce parte ancora prima:** se la prima frase è lunga, inizia dalla sua prima parte (fino alla virgola).
- **Ascolto passivo, come «Ehi Siri»:** prima usava il riconoscimento vocale del telefono, che su molti telefoni
  ascolta solo pochi secondi: il microfono si accendeva e spegneva tutto il giorno con un bip. Ora un piccolo
  modello vocale dentro il telefono ascolta solo il suo nome, di continuo: niente bip, niente microfono che
  lampeggia, niente esce dal telefono. Si scarica una volta per lingua (circa 40 MB) con il Wi-Fi, o da
  **Impostazioni › Impostazioni voce › Ascolto passivo**.
- **Si sveglia con la sua voce:** se dorme e la chiami, si sveglia dicendolo con una delle sue frasi registrate.
- **La sua voce è più veloce sui telefoni potenti:** con 8 GB di RAM o più, il motore non scarica e ricarica più
  parti del modello dopo ogni frase. La prima frase è più corta e la voce inizia a caricarsi appena apri la chat.
- **Non si ferma più alla prima frase:** continua con il resto.
- **Quale IA risponde, nella chat:** in alto nella scheda Chat vedi quale IA risponde e la cambi con un tocco.
- **Le frasi di messaggio e notifica suonano davvero:** la prima notifica viene sempre annunciata, anche con l’app aperta.
- **Menu più leggero:** Community e Comandi vocali ora sono un’unica scheda.
- **Gemini di nuovo veloce:** Google ha chiuso `gemini-2.5-flash` alle chiavi nuove e l’app lo provava ancora per
  primo a ogni messaggio. Ora un modello chiuso non viene più usato e passa subito a quello indicato da Google.
- **Spotify e la precisione della sua voce si salvano:** con Capri accesa, scegliere Spotify o una precisione nelle
  impostazioni della voce non veniva salvato. Corretto.
- **Giochi:** la difficoltà si cambia dentro ogni gioco (pulsante ◆), anche dalla scheda Giochi. A Forza 4 si
  alterna chi comincia.
- **Schermi interi al posto giusto:** nel Guardaroba e nei giochi il testo non finisce più sopra ora e batteria, e la
  sua figura nell’angolo non resta dietro i tasti di navigazione.
- **Non appare più sopra i giochi** se la sua pagina si riavviava durante la partita.
- **Guardaroba:** corretto un errore che lasciava vuota la lista degli Accessori.
- **Conferma prima di cancellare i suoi ricordi**, e **modifica le lezioni** toccandole.
- **Capisce il suo nome detto un po’ diverso** («Capi», «Cabi»…), e se chiedi qualcosa senza il suo nome, ogni tanto
  ti ricorda: «Prima di’ il mio nome».
- **Il tuo account:** accedi con Google o con l'email (in «Tu») e la sua memoria di te, le impostazioni, le lezioni e la
  chat vengono salvate nel cloud. Su un telefono nuovo si ricorda di te. Le tue chiavi IA non lasciano mai il telefono.
- **Modalità lettura:** dille «sto leggendo Dune» e «sono a pagina 120», poi chiedi un riassunto o di un personaggio:
  risponde solo fino alla tua pagina, senza spoiler.
- **File per lei:** condividi un PDF, un Word, un testo o un audio (o il 📎 della chat) e lo legge o lo ascolta con te.
- **«Capri, che canzone è?»:** ascolta qualche secondo e te lo dice (con una chiave Gemini).
- **Risponde ai tuoi messaggi:** «Capri, rispondi ad Ana che arrivo» risponde dalla notifica.
- **Due nuovi widget:** «Capri mini» e «La tua giornata con Capri».
- **Notizie del giorno:** «Capri, che novità ci sono?» e ti legge i titoli.
- **Scuoti il telefono** e le gira la testa (si può spegnere nelle Impostazioni).
- **Attesa prima di guardare lo schermo:** 0, 2, 4 o 6 secondi, in Modalità.
- **Cerca nella chat:** il pulsante ⌕ nella scheda Chat.
- **Niente più braccia doppie:** alcuni outfit disegnavano un secondo braccio fermo. Risolto.
- **Niente più vestiti mischiati nelle animazioni:** con un outfit non torna più ai vestiti di sempre a metà
  animazione. Fa solo le animazioni disegnate per quell'outfit.


# Changelog (Italiano)

Lo stesso di CHANGELOG.md (spagnolo, l’originale), in italiano: l’app lo mostra quando è in italiano.

## 1.11.8

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

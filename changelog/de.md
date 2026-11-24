# Changelog (Deutsch)

Dasselbe wie CHANGELOG.md (Spanisch, das Original), auf Deutsch: Die App zeigt es, wenn sie auf Deutsch ist.

## 1.12.0

- **Wieder auf 32-Bit-Handys:** Seit 1.11.0 ließ sich die App auf 32-Bit-Handys (armeabi-v7a, wie dem TECNO Spark
  Go 1S und vielen Android-Go-Geräten) nicht installieren: Der Installer meldete einen inkompatiblen Prozessor. Jetzt
  installiert sich dieselbe APK auf allen. Auf diesen Handys gibt es ihre Stimme für Antworten (die Engine) nicht,
  aber ihre aufgenommenen Sätze und die Stimme des Handys schon.
- **Ihre Stimme antwortet viel schneller:**
  - Während sie einen Satz sagt, erzeugt sie schon den nächsten. Vorher wartete sie, bis sie fertig gesprochen hatte,
    sodass lange Antworten doppelt so lange dauerten.
  - Sie fängt an zu sprechen, sobald der erste Teil des Satzes fertig ist.
  - Ihre Engine lädt, sobald du ihr schreibst oder mit ihr sprichst, während ihre KI nachdenkt, nicht erst am Ende.
  - Sie wird nicht mehr jedes Mal aus dem Speicher geworfen, wenn du die App verlässt, und bleibt auf Handys mit viel
    RAM länger geladen (bis zu 15 Minuten).
  - Sie nutzt die schnellen Prozessorkerne (bis zu 6) auf Handys, die sie haben.
  - Sie merkt sich Sätze, die sie schon gesagt hat, also kommen Wiederholungen sofort.
- **Der Fortschritt ihrer Stimme im Chat:** Ihre Antwort erscheint sofort als Text, darunter siehst du, wie weit ihre
  Stimme ist (*Ihre Stimme entsteht 42 %*), dann *Spricht*. Im Chat-Tab und in ihrer Blase.
- **Sie verstummt nicht mehr:** Wenn du sie berührt oder bewegt hast, während sie die Stimme vorbereitete, hat ihr
  Streichel-Satz die ganze Antwort abgebrochen. Jetzt geht ihre Antwort vor. Und während sie die Stimme vorbereitet,
  hört sie dir weiter zu.
- **Ihre aufgenommenen Sätze öfter:** beim Streicheln, Bewegen und Werfen, und jetzt auch beim Begrüßen, wenn ihr
  langweilig ist, beim Öffnen und Schließen ihres Chats, wenn das Internet weg ist, beim Spielen und bei Erinnerungen.
- **Sie spricht, während ihre KI noch schreibt:** Sobald ihre KI den ersten Satz fertig hat, fängt ihre Stimme an,
  ihn zu sagen, während der Rest geschrieben wird. Im Chat-Tab siehst du die Antwort live entstehen.
- **Ihre Engine kommt startklar:** Beim Laden macht sie ein kurzes, stilles Aufwärmen, damit die erste Antwort nicht
  länger dauert.
- **Vier neue Sprachen: Vietnamesisch, Japanisch, Deutsch und Italienisch.** Die ganze App, ihre Sätze, Spiele,
  Schlafmodus, Kleiderschrank, Sprachbefehle („Capri, …“) und die Antworten ihrer KI. Wähle sie unter Einstellungen ›
  Sprache. Ihre eigene Stimme für Antworten spricht vorerst Spanisch und Englisch; auf Deutsch nutzt sie die Stimme
  des Handys.
- **Sie bleibt im Chat nicht mehr beim Nachdenken hängen:** Wenn du schriebst, während sie noch antwortete, sagte
  sie „Gib mir eine Sekunde“ und dann nichts. Jetzt wartet jede Nachricht, bis sie dran ist, und bekommt ihre
  Antwort. Bricht ihre KI mitten in der Antwort ab, behält sie, was schon geschrieben war.
- **Die kostenlose KI blockiert sich nicht mehr selbst:** Sie nimmt pro Handy nur eine Anfrage gleichzeitig an. Ihre
  Notizen über dich und ihre Braindances warten jetzt, bis deine Nachricht fertig ist (und weichen, wenn du schreibst).
- **Mehrere API-Schlüssel:** Auf dem Bildschirm ihrer KI kannst du mehrere Schlüssel desselben Dienstes einfügen,
  durch Kommas getrennt, oder später einen hinzufügen. Ist bei einem das Kontingent aufgebraucht (wie beim
  kostenlosen Gemini), nimmt sie sofort den nächsten.
- **Sie sagt dir Bescheid:** Ab und zu, wenn eine Benachrichtigung kommt, sagt sie es dir mit ihrer aufgenommenen
  Stimme.
- **Ihre Stimme setzt noch früher ein:** Ist der erste Satz lang, beginnt sie mit seinem ersten Teil (bis zum Komma).
- **Passives Zuhören wie bei „Hey Siri“:** Bisher nutzte sie die Spracherkennung des Handys, die auf vielen Geräten
  nur ein paar Sekunden zuhört: Das Mikrofon ging den ganzen Tag piepend an und aus. Jetzt hört ein kleines
  Sprachmodell direkt im Handy ständig nur auf ihren Namen: keine Pieptöne, kein blinkendes Mikrofon, nichts
  verlässt das Handy. Es wird einmal pro Sprache (etwa 40 MB) über WLAN geladen oder unter **Einstellungen ›
  Stimmeinstellungen › Passives Zuhören**.
- **Sie wacht mit ihrer Stimme auf:** Schläft sie und du rufst sie, wacht sie mit einer aufgenommenen Phrase auf.
- **Ihre Stimme ist auf starken Handys schneller:** Ab 8 GB RAM entlädt und lädt die Engine nach jedem Satz keine
  Modellteile mehr neu. Der erste Satz ist kürzer, und ihre Stimme lädt, sobald du den Chat öffnest.
- **Sie hört nicht mehr nach dem ersten Satz auf:** Sie spricht den Rest weiter.
- **Welche KI antwortet, direkt im Chat:** Oben im Chat-Tab siehst du die KI und wechselst sie mit einem Tippen.
- **Phrasen für Nachrichten und Benachrichtigungen erklingen wirklich:** Die erste Benachrichtigung wird immer angesagt,
  auch bei geöffneter App.
- **Schlankeres Menü:** Community und Sprachbefehle sind jetzt ein Tab.
- **Gemini wieder schnell:** Google hat `gemini-2.5-flash` für neue Schlüssel geschlossen, und die App hat es trotzdem
  bei jeder Nachricht zuerst versucht. Jetzt wird ein geschlossenes Modell nicht mehr benutzt, und sie springt direkt
  zu dem, das Google nennt.
- **Spotify und die Genauigkeit ihrer Stimme werden gespeichert:** mit eingeschalteter Capri wurde die Wahl von
  Spotify oder einer Genauigkeit in den Stimmeinstellungen nicht gespeichert. Behoben.
- **Spiele:** die Schwierigkeit lässt sich in jedem Spiel ändern (◆-Knopf), auch vom Spiele-Tab aus. Bei Vier
  gewinnt fangt ihr abwechselnd an.
- **Vollbilder richtig platziert:** im Kleiderschrank und in den Spielen liegt Text nicht mehr über Uhr und Akku,
  und ihre Figur in der Ecke nicht mehr hinter den Navigationstasten.
- **Sie erscheint nicht mehr über den Spielen**, wenn ihre Seite während des Spiels neu startete.
- **Kleiderschrank:** ein Fehler, der die Accessoire-Liste leer ließ, ist behoben.
- **Bestätigung vor dem Löschen ihrer Erinnerungen**, und **Kurse bearbeiten** durch Antippen.
- **Sie versteht ihren Namen, auch etwas anders gesagt** („Capi“, „Cabi“…), und wenn du ohne ihren Namen fragst,
  erinnert sie dich ab und zu: „Sag zuerst meinen Namen“.
- **Dein Konto:** Melde dich mit Google oder E-Mail an (unter „Du“), und ihre Erinnerungen an dich, deine
  Einstellungen, Kurse und dein Chat werden in der Cloud gespeichert. Auf einem neuen Handy erinnert sie sich an dich.
  Deine KI-Schlüssel verlassen dein Handy nie.
- **Lesemodus:** Sag „ich lese Dune“ und „ich bin auf Seite 120“, dann frag nach einer Zusammenfassung oder einer
  Figur: Sie antwortet nur bis zu deiner Seite, ohne Spoiler.
- **Dateien für sie:** Teile ein PDF, Word, einen Text oder eine Audiodatei (oder 📎 im Chat), und sie liest oder hört
  mit dir.
- **„Capri, welches Lied ist das?“:** Sie hört ein paar Sekunden zu und sagt es dir (mit einem Gemini-Schlüssel).
- **Sie antwortet auf Nachrichten:** „Capri, antworte Ana, dass ich gleich komme“ antwortet aus der Benachrichtigung.
- **Zwei neue Widgets:** „Capri mini“ und „Dein Tag mit Capri“.
- **Nachrichten des Tages:** „Capri, was gibt's Neues?“, und sie liest die Schlagzeilen.
- **Schüttle dein Handy**, und ihr wird schwindlig (in den Einstellungen abschaltbar).
- **Wartezeit vor dem Bildschirmfoto:** 0, 2, 4 oder 6 Sekunden, unter Modi.
- **Im Chat suchen:** die Taste ⌕ im Chat-Tab.
- **Keine doppelten Arme mehr:** Manche Outfits zeichneten einen zweiten, starren Arm. Behoben.
- **Keine gemischte Kleidung mehr in Animationen:** In einem Outfit wechselt sie nicht mehr mitten in der Animation
  zu ihrer üblichen Kleidung. Es laufen nur Animationen, die für dieses Outfit gezeichnet sind.
- **Skins aus der Community:** Zeichne ein Outfit auf Capris Vorlage (im Discord) und schick es über Einstellungen ›
  Melden › „Skin senden“. Nimmt Emma es an, kommt es mit deinem Namen.
- **Ihre echte Stimme:** Mit der Stimm-Engine, aber ohne ihr Profil, sprach sie mit der Handystimme. Jetzt kommt das
  Profil mit der Engine, und fehlt es, lädt sie es selbst.
- **Tippen in deinen Apps:** „Capri, schreib hier …“ bei offenem Chat nimmt dir die Tastatur nicht mehr weg.
- **Nur deine Stimme:** „Nur meine Stimme“ lernt deine Stimme bei den ersten 3 Rufen und ignoriert dann ihren Namen in
  anderen Stimmen (Fernseher, Leute um dich). Alles bleibt auf deinem Handy.


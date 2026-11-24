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

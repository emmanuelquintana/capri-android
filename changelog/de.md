# Changelog (Deutsch)

Dasselbe wie CHANGELOG.md (Spanisch, das Original), auf Deutsch: Die App zeigt es, wenn sie auf Deutsch ist.

## 1.11.8

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

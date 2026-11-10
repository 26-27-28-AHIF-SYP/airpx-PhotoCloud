## 1. Teilen: QR-Code, Link & Kontakt-Berechtigung

**Szenario A: Der schnelle Event-Upload (QR-Code)**
*   **Kontext:** Sascha ist auf dem Sommercamp und will, dass alle Betreuer Fotos in ein Album laden.
*   **Ablauf:**
    1.  Sascha erstellt links ein neues Album "Sommercamp 2024".
    2.  Er klickt das Album an. In der Mitte erscheint statt den Albuminhalten eine Anzeige für "Album teilen", "Erste Fotos hinzufügen" etc.
    3.  Er wählt den Link/Button "Album Teilen". Es Öffnete sich ein Fenster in dem er das Teilen anpassen kann (Per Link, QR-Code, Nutzerfreigabe). Sascha wählt "Per QR-Code".
    4.  Ein QR-Code wird generiert. Er zeigt ihn den Kollegen. Diese scannen ihn und landen direkt in diesem Album.

**Szenario B: Privater Link mit Passwort**
*   **Kontext:** Markus will Urlaubsfotos mit den Großeltern teilen, die keinen Account haben.
*   **Ablauf:**
    1.  Markus wählt das Album "Urlaub" links aus (Rechtsklick) und klickt "Teilen" im Dropdown.
    2.  Es öffnet sich wieder ein Fenster für die Konfiguraton des Teilens.
    3.  Er aktiviert "Passwortschutz" und setzt ein Ablaufdatum (z.B. 30 Tage). Unter "Berechtigungen" hakt er "Ansehen" und "Herunterladen".
    4.  Er kopiert den Link. Die Großeltern können die Fotos ansehen und herunterladen, aber nicht bearbeiten oder andere sehen.

**Szenario C: Teilen via Kontakt (Account-Berechtigung)**
*   **Kontext:** Sabrina will mit einem Kollegen an einem Projekt arbeiten.
*   **Ablauf:**
    1.  Sabrina wählt das Album "Kunde XY" aus (Rechtsklick) und klickt "Teilen/Freigeben" im Dropdown.
    2.  Es öffnet sich wieder ein Fenster für die Konfiguraton des Teilens.
    3.  Sie tippt den Namen des Kollegen ein (aus ihren Kontakten oder der internen User-Liste).
    4.  Sie setzt die Berechtigung auf "Bearbeiten" (damit er auch Fotos hinzufügen kann).
    5.  Der Kollege sieht das Album nun in seiner eigenen linken Navigationsleiste unter "Geteilt mit mir".

---

### 2. Bilder Hochladen: Sync vs. Manuell

**Szenario A: Der Einstieg – Ordner-Sync (One-Time Setup)**
*   **Kontext:** Simon installiert die Desktop-App zum ersten Mal und hat seine Fotos noch chaotisch auf der Festplatte verteilt.
*   **Ablauf
    1.  Beim ersten Start der Desktop-App fragt ein Wizard: "Welche lokalen Ordner möchtest du hochladen?"
    2.  Simon wählt `D:\Bilder\2023` und `E:\Fotos\Reisen`.
    3.  Die App erstellt in der Cloud-Struktur (linke Spalte) automatisch die Ordner/Alben "2023" und "Reisen".
    4.  Es wird keine Verknüpfung hergestellt. Ändert Simon lokal einen Dateinamen, ändert er sich nicht auch in der Cloud. Es ist ein einseitiger Upload.

**Szenario B: Manuelles Hochladen ("Von Gerät")**
*   **Kontext:** Sabrina hat ein paar fertige Bearbeitungen auf ihrem Laptop, die sie schnell in die Cloud schieben will.
*   **Ablauf:**
    1.  Sie öffnet die Web-Oberfläche.
    2.  Sie zieht die Dateien per Drag & Drop in die mittlere Spalte (in den aktuell offenen Ordner).
    3.  Alternativ: Klick auf den Button "Hochladen" oben rechts -> "Dateien auswählen" (evtl. auch im Dropdown bei Rechtsklick).
    4.  Die Bilder erscheinen sofort im Grid. Je nach Einstellung läuft im Hintergrund die KI-Analyse (OCR, Objekterkennung) an. Bei entsprechender Einstellung wird das auch erst zu bestimmten Uhrzeiten gemacht (zb. von 3-6 Uhr), wenn der Server ohnehin wenig zu tun hat.

---

### 3. Tags festlegen: Manuell vs. KI (Hintergründig)

**Szenario A: Manuelle Tags (Nutzer-Kontrolle)**
*   **Kontext:** Markus möchte ein Foto seines Sohnes mit dem Tag "Schulabschluss" versehen, damit er es später leicht findet.
*   **Ablauf:**
    1.  Markus klickt ein Foto in der mittleren Spalte an.
    2.  Je nach Ansichtseinstellungenie zeigt die rechte Spalte (unter "Einstellungen", "Kontakte" etc.) die Details oder auch verkürzt unter jedem Bild im Album in der Mitte.
    3.  Unter "Tags" gibt es ein Eingabefeld. Er tippt "Schulabschluss" ein und drückt Enter.
    4.  Das Tag wird sofort gespeichert und ist über die Suche auffindbar.

**Szenario B: KI-Tags (Automatisch & Editierbar)**
*   **Kontext:** Die KI hat ein Bild analysiert.
*   **Ablauf:**
    1.  Sabrina öffnet ein Bild.
    2.  In der rechten Spalte sieht sie unter "Tags" (eingeklappt oder als kleine Icons): `Katze`, `Sofa`, `Innenraum`.
    4.  Sie sieht einen falschen Tag "Hund" (obwohl es eine Katze ist). Sie klickt auf das kleine "X" neben dem Tag, um ihn zu entfernen.
    5.  KI-Features sind nicht aufgezwungen. Beim Setup wird, neben der Option einen API-Key oder einen Port einer lokalen KI anzugeben, auch die Option angezeigt, alle KI-Features auszuschalten.

---

### 4. Die "Intelligente Suche"

**Szenario: Natürlichsprachliche Suche**
*   **Kontext:** Simon sucht ein bestimmtes Foto von seiner letzten Wanderung, weiß aber nicht genau, wann es war.
*   **Ablauf:**
    1.  Simon klickt in der **oberen Leiste der mittleren Spalte** in die Suchleiste.
    2.  Er tippt ein: *"Sonnenuntergang am Berg mit Zelt"*.
    3.  Da die KI bereits Objekte (`Berg`, `Zelt`) und Szenen (`Sonnenuntergang`) erkannt hat, filtert die mittlere Spalte sofort die relevanten Bilder.
    4.  Er muss keine Ordner durchwühlen. Das Ergebnis ist eine Mischung aus KI-Objekterkennung und Metadaten (Ort/Datum).
    5.  Wenn er die Suche löscht, sieht er wieder den normalen Ordnerinhalt.


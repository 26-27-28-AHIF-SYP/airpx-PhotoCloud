### 1. High Priority (Must-Haves)
*Diese Features wurden in der Umfrage stark nachgefragt oder sind kritische Schmerzpunkte.*

| Feature aus Brainstorm | Umfrage-Evidenz | Begründung |
| :--- | :--- | :--- |
| **Verschlüsselung (E2EE)** | 7/16 (43,8%) wünschen E2EE; 8/16 (50%) sagen Datenschutz ist "Sehr wichtig"; 5/16 haben Datenschutzbedenken. | Datenschutz ist das Top-Thema. Ohne Verschlüsselung wird die Zielgruppe (die Nextcloud/Immich nutzt) nicht wechseln. |
| **KI-Texterkennung (OCR)** | 8/16 (50%) – **Das beliebteste KI-Feature**. | Wird explizit am häufigsten genannt. Wichtig für das Durchsuchen von Dokumenten, Screenshots etc. |
| **Personenerkennung / Gesichtserkennung** | 7/16 (43,8%) wollen Personen erkennen; 10/16 (62,5%) sind offen für KI-Analyse. | Klassisches Feature, das erwartet wird. Wichtig: Lokale Verarbeitung bevorzugen (siehe Punkt 3). |
| **Gemeinsame Alben & Teilen** | 12/16 (80% von 15 Antworten) wollen gemeinsame Alben; 10/16 wollen, dass andere Fotos hinzufügen können. | "Alben teilen" ist ein Kernfeature. Die Option "Andere können Fotos hinzufügen" ist sehr stark nachgefragt. |
| **Privatsphäre beim Teilen** | 9/16 wollen private Alben; 5/16 wollen passwortgeschützte Links. | "Alben privat lassen" während des Teilens ist essenziell. |
| **Automatischer Sync** | 5/16 (31,3%) finden automatisches Backup "Sehr wichtig"; 7/16 (43,8%) nutzen es bereits. | Manuelles Sync ist für die Mehrheit keine Option. Es muss "einfach funktionieren". |
| **Self-Hosted / Lokale Kontrolle** | 1x Nextcloud, 1x Immich, 1x Synology genannt; Kommentare fordern "Datenschutz" und "Vertrauen". | Eure Zielgruppe kennt sich aus. Self-hosted ist ein USP gegenüber Google/iCloud. |

### 2. Medium Priority (Should-Haves)
*Wichtige Features, die die Usability stark erhöhen, aber nicht unbedingt das K.O.-Kriterium sind.*

| Feature aus Brainstorm | Umfrage-Evidenz | Begründung |
| :--- | :--- | :--- |
| **KI-Suche (Objekte/Katzen)** | 6/16 (37,5%) wollen Objekte erkennen; 5/16 wollen KI-Fragen stellen. | Nützlich, aber weniger kritisch als OCR oder Personen. |
| **Duplikaterkennung (KI-gestützt)** | 4/16 wollen Duplikate entfernen; 1x explizit "Vorschlag, nicht auto-löschen". | Wichtig für Aufräumen, aber Nutzer wollen Kontrolle (nicht automatisch löschen). |
| **Ortsbasierte Sortierung** | 7/16 (43,8%) wollen Fotos nach Orten sortieren. | Standard-Feature in fast allen Clouds. Erwartet, aber nicht einzigartig. |
| **2FA (Zwei-Faktor-Auth)** | 6/16 (37,5%) wünschen es sich. | Sicherheitsstandard. Muss dabei sein, ist aber "Hygienefaktor". |
| **Bildbearbeitung (Basic)** | Nur 1/16 will automatische Verbesserung. | **Achtung:** Keine hohe Priorität für KI-Bearbeitung. Fokus auf Basis-Tools legen, nicht auf KI-Magie. |
| **QR-Code zum Hochladen/Teilen** | *Nicht direkt abgefragt.* | Könnte ein nettes "Delighter"-Feature sein, besonders für Events ("Fotos hochladen ohne Account"). Aber kein Must-Have laut Umfrage. |

### 3. Low Priority / Nice-to-Have
*Features, die entweder kaum nachgefragt wurden oder technisch komplex sind mit geringem Mehrwert für diese spezifische Gruppe.*

| Feature aus Brainstorm | Umfrage-Evidenz | Begründung |
| :--- | :--- | :--- |
| **Öffentliche Profile / Alben öffentlich machen** | Kaum Nachfrage nach "Social Media"-Aspekten (Likes/Kommentare nur 2x). | Nutzer wollen Fotos *teilen*, nicht unbedingt eine öffentliche Galerie wie Instagram. |
| **KI-Rückblicke (Memories)** | *Nicht direkt abgefragt.* | Google Photos macht das gut, aber in der Umfrage nicht explizit gefordert. Kann später kommen. |
| **SSO (Single Sign-On)** | *Nicht abgefragt.* | Für Self-Hosted/Private Nutzer eher sekundär. |
| **Permission Tags (Feingranulare Rechte)** | Indirekt durch "Passwortschutz für Alben" (8/16) abgedeckt. | Komplexe Rechtesysteme sind oft overkill. Passwortschutz pro Album reicht zunächst. |
| **Statische vs. Dynamische Alben** | *Nicht abgefragt.* | Technisches Implementierungsdetail. Nutzer wollen einfach "Ordner/Alben". |

### Kritische Erkenntnisse & Empfehlungen

#### 1. Datenschutz & Lokale KI ist der Schlüssel
*   **Erkenntnis:** 6/16 (37,5%) sagen, es ist "Sehr wichtig", dass KI **lokal auf dem Gerät** läuft. Weitere 5/16 (31,3%) finden es "Wichtig".
*   **Empfehlung:** Wenn wir KI-Features (OCR, Gesichtserkennung) anbieten, **müssen** diese client-seitig oder self-hosted laufen. Wir senden keine Bilder zur Analyse an externe APIs. Das ist unser größter Vorteil gegenüber Google Photos.

#### 2. Organisation ist ein Schmerzpunkt
*   **Erkenntnis:** 5/16 (31,3%) stören sich an "Schwieriger Organisation". 8/16 (50%) sortieren ihre Fotos **kaum**.
*   **Empfehlung:** Die KI soll nicht nur suchen, sondern **automatisch strukturieren**.
    *   Auto-Alben nach Ereignissen/Orten (5/16 Wunsch).
    *   Duplikate vorschlagen zum Löschen (aber nicht automatisch!).
    *   Die App muss die Arbeit abnehmen, da die Nutzer faul beim Sortieren sind.

#### 3. "Kostenlos" & "Speicherplatz" sind Barrieren
*   **Erkenntnis:** In den offenen Antworten wird "kostenlos" und "genug Speicherplatz" mehrfach genannt.
*   **Empfehlung:** Da wir self-hosted sind, ist das unser Modell. Werben mit: **"Dein Speicher, deine Regeln, keine Abo-Falle."**

#### 4. Was man weglassen oder hinten anstellen kann
*   **Soziale Features:** Likes, Kommentare, öffentliche Profile. Das ist nicht gefragt.
*   **KI-Bildverbesserung:** Kaum Interesse.
*   **Komplexe Freigabe-Workflows:** Einfache Links mit Passwort reichen erstmal. QR-Codes sind nett, aber nicht prioritär.

### Fazit für unser Feature-Set

Wir behalten diese Features bei und priorisieren sie so:

1.  **Core:** Self-hosted, E2EE Verschlüsselung, Auto-Sync, Accounts/2FA.
2.  **Organization (KI):** OCR (Texterkennung), Personenerkennung (lokal!), Duplikat-Erkennung (mit Vorschlag), Orts-Sortierung.
3.  **Sharing:** Gemeinsame Alben, Private Alben, Passwortschutz für Links, "Andere können uploaden".
4.  **Nice-to-have:** QR-Code Upload, Objektiv-Erkennung (Katze etc.), Bildbearbeitung (Basic).

**Streichen oder später machen:** Öffentliche Profile, Social-Likes, komplexe Permission-Tags (erstmal simpel halten), SSO (wenn nicht B2B).

Die Umfrage zeigt klar: Die Nutzer wollen eine **sichere, private, automatisch organisierte** Lösung, die ihnen die lästige Sortierarbeit abnimmt, ohne ihre Daten an Big Tech zu verkaufen.

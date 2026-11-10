# Umfrageauswertung: Foto-Cloud (airpx)

Basierend auf den Ergebnissen des Google Forms (n = 16). Bei dieser kleinen Stichprobe sind die Ergebnisse eher als Tendenzen denn als belastbare Statistik zu verstehen.

## Wer hat teilgenommen?

- **13 von 16 (81 %)** speichern ihre Fotos bereits in einer Cloud. Die häufigsten Anbieter sind iCloud (7) und OneDrive (6), danach Google Photos (3). Selbst gehostete Lösungen (Nextcloud, Immich, Synology) wurden jeweils einmal genannt.
- Das Fotovolumen ist groß: 7 Personen haben 5.000–50.000 Fotos, 1 Person mehr als 50.000.
- Alle möchten per **Smartphone** zugreifen, und 15 von 16 zusätzlich per **Laptop/PC**. Das bestätigt den Umfang Android + Web. Tablet (6) und Smart-TV (2) spielen eine kleine Rolle.

## Schmerzpunkte

- Die Zufriedenheit mit den aktuellen Diensten ist überwiegend gut: 9 sind zufrieden oder sehr zufrieden, 4 neutral und 3 unzufrieden.
- Die häufigsten Kritikpunkte, jeweils 5 Nennungen, sind Datenschutzbedenken, Kosten und schwierige Organisation der Fotos. Es folgen zu wenig Speicherplatz (3) und langsamer Upload/Download (2).
- **8 von 16 sortieren ihre Fotos kaum**, und 7 sortieren nur nach Datum. Das ist das stärkste Argument für KI-Sortierung und -Suche.

## Datenschutz und Sicherheit (das stärkste Alleinstellungsmerkmal)

- Datenschutz ist **14 von 16** wichtig oder sehr wichtig, 8 nennen ihn „sehr wichtig".
- **11 von 16** finden es wichtig oder sehr wichtig, dass KI direkt auf dem Gerät läuft und Fotos nicht an externe Anbieter gesendet werden. Das stützt die lokale/selbst gehostete KI-Option im Deployment-Diagramm.
- Meistgewünschte Sicherheitsfunktionen:
  - Passwortschutz für einzelne Alben (8)
  - Ende-zu-Ende-Verschlüsselung (7)
  - PIN- oder biometrische Sperre (7)
  - Automatische Backups (7)
  - Anzeige des Speicherortes der Daten (7)
  - Zwei-Faktor-Authentifizierung (6)
- Der Passwortschutz pro Album liegt vorn und passt gut zum Berechtigungsmodell pro Album.

## KI-Funktionen

- **10 von 16** würden eine KI nutzen, die Fotos automatisch analysiert und sortiert, 6 würden das nicht tun. Sechs Personen haben außerdem „Keine KI-Funktionen" gewählt, daher sollte KI **optional (Opt-in)** sein.
- Meistgewünschte Funktionen:
  - Texterkennung (8)
  - Gesichtserkennung (7)
  - Sortierung nach Orten (7)
  - Objekterkennung (6)
  - KI-Fotosuche (5)
  - Gruppierung nach Ereignissen (5)
  - Erkennung ähnlicher Fotos (5)
  - Automatisch Alben erstellen (5)
  - Doppelte Fotos erkennen und entfernen (4)
- Eine Person wünschte sich, dass Duplikate **vorgeschlagen, aber nicht automatisch gelöscht** werden. Die Duplikaterkennung sollte also ein Prüfschritt sein und keine automatische Aktion.
- Automatische Bildverbesserung wurde nur einmal genannt (1 von 16), KI-Bildbearbeitung hat daher offenbar niedrige Priorität.

## Teilen

- 8 teilen regelmäßig, 7 selten, Teilen ist also für fast alle relevant.
- Meistgewünscht:
  - Gemeinsame Alben (12)
  - Andere können Fotos zum Album hinzufügen (10)
  - Private Alben (9)
  - Passwortgeschützte Links (5)
  - Fotos über einen Link teilen (5)
  - Ablaufdatum für Links (4)
- Kommentare und Likes sind kaum relevant (jeweils 2 Nennungen) und könnten verschoben oder gestrichen werden.
- Die Berechtigung **Hinzufügen** und gemeinsame Alben sind klar bestätigt. Niemand hat feingranulares Ansehen/Speichern/Löschen gewünscht, die Oberfläche dafür sollte daher einfach bleiben.

## Was für einen Wechsel nötig wäre (Freitext)

- **Kostenloser oder günstiger Speicher** wurde 4-mal genannt, die häufigste Antwort.
- Weitere Antworten: Datenschutz, Geschwindigkeit, eine zuverlässige Handy-App, Vertrauen in Stabilität und Sicherheit sowie Übersichtlichkeit.
- Genannte fehlende Funktionen: KI-Kategorisierung, automatische Ordner nach Orten, Übertragung auf ein NAS und das Aufnahmedatum bei jedem Bild.

## Folgerungen für die Roadmap

1. Die aktuelle Priorisierung (Alben, Berechtigungen, UI, Accounts, Suche, KI) passt zur Umfrage. Gemeinsame Alben mit „Hinzufügen"-Recht sind das am stärksten bestätigte Kernfeature.
2. Passwortschutz pro Album und Ende-zu-Ende-Verschlüsselung verdienen einen höheren Platz, da sie vor 2FA liegen.
3. Bei der KI zuerst Texterkennung, Ortssortierung und Gesichtserkennung umsetzen, lokal oder als Opt-in.
4. Kosten und Speicherplatz sind die größte Wechselhürde, was für den Self-Hosting-Ansatz spricht.
5. QR-Code-Teilen, Kommentare, Likes und KI-Bearbeitung wurden nicht abgefragt oder kaum gewünscht und sind gute Kandidaten zum Streichen, falls die Zeit knapp wird.

## Einschränkungen

- Die Stichprobe ist klein und vermutlich auf das Umfeld des Teams verzerrt, eine Auswahlverzerrung ist wahrscheinlich.
- Mehrfachantworten wurden an Kommas getrennt, wodurch mindestens eine KI-Antwort mitten im Satz geteilt wurde („nur Vorschlag"). Sie wurde als ein einzelner Kommentar gezählt.
- Die Ergebnisse wurden nicht pro Person aufgeschlüsselt, zum Beispiel ob die KI-Skeptiker auch die Befürworter von Geräte-lokaler KI sind. Diese Kreuzauswertung wäre ein sinnvoller nächster Schritt.

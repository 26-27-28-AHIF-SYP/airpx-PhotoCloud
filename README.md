# Projektantrag - airpx (PhotoCloud)

## 1. Ausgangslage

### 1.1. Ist-Situation
- Es gibt bereits zahlreiche Cloud-Speicher- und Foto-Sharing-Dienste.
- Viele dieser Dienste werden von großen internationalen Unternehmen betrieben. Dadurch haben Nutzer nur begrenzte Kontrolle darüber, wie ihre Fotos und Metadaten gespeichert, verarbeitet und möglicherweise für die Analyse oder das Training von KI-Systemen verwendet werden.
- Es werden oft eingeschränkte oder unübersichtliche Berechtigungsmodelle geboten.
- Klassischer lokale Speicherung (Festplatte, USB-Stick) ist weit verbreitet.

### 1.2. Verbesserungspotenziale
- Automatisches Organisieren und Auffinden von Fotos durch KI, statt manueller Sortierung
- Feingranulare, pro Album bzw. pro Freund einstellbare Freigaben (View/Save/Add/Delete) statt "Alles-oder-nichts"-Freigaben
- Bei klassischer lokaler Speicherung die Daten nicht von überall verfügbar zu haben, das eigene system mit einem Cloudservice verbinden.

## 2. Zielsetzung

- Entwicklung einer benutzerfreundlichen Photo-Cloud mit moderner und ansprechender Benutzeroberfläche für Mobile (Android) und Web
- Implementierung eines Album-Systems inklusive verschachtelter Alben, das Freigabe-Einstellungen vom übergeordneten Album erbt bzw. überschreiben kann
- Umsetzung eines granularen Berechtigungssystems (View, Save, Add, Delete) pro Album bzw. pro Freund
- Integration von KI-Funktionen:
  - Automatische Sortierung nach Datum, Ort, Ereignis und Personen
  - Suche über Meta-Tags (Datum, Ort, Kamera-Infos)
  - Bild- und Texterkennung (Objekte, Szenen, Gesichter, Text in Bildern wie Schilder oder Dokumente)
- Umsetzung von Kernfunktionen für Mobile und Web: Alben durchsuchen, Fotos hochladen/aufnehmen, geteilte Alben von Freunden ansehen, KI-gestützte Suche
- Zusatzfunktionen Mobile App: Offline-Zugriff auf gespeicherte/heruntergeladene Fotos, Push-Benachrichtigungen (geteilte Alben, Freundschaftsanfragen, Kommentare), Auto-Backup des Kamera-Rolls
- Zusatzfunktion Web App: Login synchronisiert mit dem Mobile-Account, In-Browser-Benachrichtigungen

## 3. Chancen und Risiken
- **S**trenghts
  - Kombination aus Fotoverwaltung und KI - Funktionen
  - Benutzerfreundliche Verwaltung von Fotos und verschachtelten Alben
  - Individuell einstellbare Berechtigungen
  - Gemeinsame Nutzung von Fotos über Web und Mobile
  - Praxisnahes Projekt mit klar erkennbarem Nutzen für die Zielgruppe
- **W**eaknesses:
  - Hoher Entwicklungsaufwand 
  - Komplexität durch KI-Funktionen (Sortierung, Erkennung) noch ungewohnt
  - Hoher Speicher- und Rechenbedarf bei großen Foto-Sammlungen
  - Begrenzte Entwicklungszeit und Erfahrung mit Cloud-Architektur und Bilderkennung
  - Verschachtelte Alben mit vererbten Berechtigungen sind anspruchsvoll
- **O**pportunities
  - Sammeln von Erfahrungen mit Cloud-Architektur, Mobile- und Web-Entwicklung
  - Vertiefung im Bereich KI/Bilderkennung
  - Potenzial für eine spätere Weiterentwicklung zu einem eigenständigen Produkt
  
- **T**hreats:
  - Datenschutz/Sicherheit bei sensiblen Foto- und Nutzerdaten
  - Aufwand der KI-Integration wird unterschätzt
  - Steigende Speicher- und Betriebskosten bei großen Datenmengen
  - Fehlerhafte KI-Ergebnisse, beispielsweise falsche Personen- oder Objekterkennung
  - Abhängigkeit von externen Diensten, Bibliotheken oder KI-Modellen
  - Rechtliche Anforderungen an Datenschutz, Einwilligung und die Verarbeitung personenbezogener Daten
  - Möliche Zeitverzögungen

## 4. Planung
Priorisierung der geplanten Features:
1. Album- und Foto-Verwaltung (Upload, verschachtelte Alben)
2. Berechtigungssystem (View/Save/Add/Delete)
3. Entwicklung der Benutzeroberfläche (Mobile & Web)
4. Benutzeraccount-System (inkl. Login-Sync Mobile/Web)
5. Suche und Filterung 
6. KI-gestützte Sortierung, Suche und Bild-/Texterkennung


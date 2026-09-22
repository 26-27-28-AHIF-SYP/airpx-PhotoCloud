# Projektantrag - airpx (PhotoCloud)

## 1. Ausgangslage

### 1.1. Ist-Situation
- HTL Leonding 4./5. Klasse Syp-Projekt
- Entwicklung von airpx, einer Photo-Cloud, als Software-Projekt
- 

### 1.2. Verbesserungspotenziale
- Es gibt bereits zahlreiche Cloud-Speicher- und Foto-Sharing-Dienste, welche jedoch oft eingeschränkte oder unübersichtliche Berechtigungsmodelle bieten
- Gegenüber klassischer lokaler Speicherung (Festplatte, USB-Stick) bietet airpx Verfügbarkeit von überall und auf mehreren Geräten (Mobile & Web)
- Automatisches Organisieren und Auffinden von Fotos durch KI, statt manueller Sortierung
- Feingranulare, pro Album bzw. pro Freund einstellbare Freigaben (View/Save/Add/Delete) statt "Alles-oder-nichts"-Freigaben

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
  - Klar definiertes, aus der Praxis bekanntes Feature-Set (README als Basis)
- **W**eaknesses:
  - Komplexität durch KI-Funktionen (Sortierung, Erkennung) noch ungewohnt
  - Verschachtelte Alben mit vererbten Berechtigungen sind anspruchsvoll
- **O**pportunities
  - Sammeln von Erfahrungen mit Cloud-Architektur, Mobile- und Web-Entwicklung
  - Vertiefung im Bereich KI/Bilderkennung
- **T**hreats:
  - Datenschutz/Sicherheit bei sensiblen Foto- und Nutzerdaten
  - Aufwand der KI-Integration wird unterschätzt

## 4. Planung
- Priorisierung der geplanten Features:
  1. Entwicklung der Benutzeroberfläche (Mobile & Web)
  2. Album- und Foto-Verwaltung (Upload, verschachtelte Alben)
  3. Berechtigungssystem (View/Save/Add/Delete)
  4. Benutzeraccount-System (inkl. Login-Sync Mobile/Web)
  5. KI-Funktionen (Sortierung, Suche, Bild-/Texterkennung)
  6. Testen

  Milestones
  1. Ostern: funktionierender Grundsatz (Upload, einfache Alben, grobe Benutzeroberfläche für Mobile & Web)
  2. Mai: Berechtigungssystem, verschachtelte Alben, erste KI-Funktionen + anschauliches UI/UX
  3. Juni: Polishing, Fertigstellung, eventuelle Erweiterungen (z. B. weitere KI-Features)
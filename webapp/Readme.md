# AirPX PhotoCloud – Dashboard

Die Web-App ist aktuell bewusst auf eine einzelne, responsive Dashboard-Seite
beschränkt. Sie verwendet React mit Vite; es gibt keine zusätzlichen
Frontend-Routen. Der Schalter „Nutzer / Admin“ wechselt für die Mockup-
Vorschau zwischen dem normalen Dashboard und einer Admin-Übersicht. Er
simuliert keine Anmeldung oder echte Rollen-/Zugriffsprüfung.

## Lokal starten

```bash
npm install
npm run dev
```

Die Übersicht zeigt zuletzt hinzugefügte Medien und neue Fotos aus geteilten
Alben. Dabei wird jeweils das geteilte Album samt Absender angezeigt.
Benachrichtigungen erscheinen beim Profil. „Teilen“ zeigt alle geteilten Alben;
unter „Fotos“ ist die vollständige Galerie verfügbar. Verschachtelte Alben
filtern die Galerie auf das ausgewählte Album.

Das Admin-Dashboard ist auf einen kleinen privaten Host ausgerichtet: Es zeigt
laufende Uploads und Synchronisierungen, aktive Share-Links, Nutzerstatus sowie
CPU-, RAM-, Speicher- und Netzwerkauslastung. Alle Kennzahlen, Jobs, Nutzer,
Share-Links und Statusanzeigen sind Mockup-Daten im Frontend. Uploads erscheinen
nur in der aktuellen Browser-Sitzung und werden nicht in eine Cloud übertragen.
Automatische Handy-Synchronisierung, echte Admin-Funktionen,
Profileinstellungen und weitere Aktionen benötigen noch eine Backend-Anbindung.

## Research: Technologie und Synchronisierung

- **Web oder Desktop:** React mit Vite ist für die responsive Fotoübersicht und
  Verwaltung geeignet und bereits festgelegt. Eine Desktop-App (z. B. mit
  Electron oder Tauri) würde eine separate Installation erfordern und bietet
  hier wenig Mehrwert: Fotos entstehen üblicherweise am Handy, die Übersicht
  funktioniert im Browser.
- **Technologien und Anbindung:** Die Web-App nutzt React/Vite. Web- und
  Mobile-App sollen dieselbe REST-API über HTTPS verwenden (JSON für Metadaten,
  Datei-Uploads für Fotos). Das Backend wird aktuell folgende Technologien verwenden; Quarkus, Java
  und PostgreSQL.
- **Automatische Synchronisierung:** Sie gehört in die mobile App. Diese kann
  neue Fotos im Hintergrund erkennen, Uploads lokal vormerken und bei fehlender
  Verbindung erneut versuchen.

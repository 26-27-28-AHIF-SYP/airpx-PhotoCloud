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

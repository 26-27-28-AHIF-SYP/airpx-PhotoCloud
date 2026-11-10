# AirPX PhotoCloud – Dashboard

Die Web-App ist aktuell bewusst auf eine einzelne, responsive Dashboard-Seite
beschränkt. Sie verwendet React mit Vite; es gibt keine zusätzlichen
Frontend-Routen.

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

Die Beispielbilder, Albumdaten und Benachrichtigung sind lokal im Frontend
hinterlegt. Uploads erscheinen nur in der aktuellen Browser-Sitzung und werden
nicht in eine Cloud übertragen. Automatische Handy-Synchronisierung,
Profileinstellungen und weitere Aktionen benötigen noch eine Backend-Anbindung.

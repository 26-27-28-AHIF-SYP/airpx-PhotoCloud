# AirPX PhotoCloud – Dashboard

Die Web-App ist aktuell bewusst auf eine einzelne, responsive Dashboard-Seite
beschränkt. Sie verwendet React mit Vite; es gibt keine zusätzlichen
Frontend-Routen.

## Lokal starten

```bash
npm install
npm run dev
```

Die Übersicht zeigt die zuletzt hinzugefügten Fotos, kürzlich geteilte Alben und
neue Fotos aus geteilten Alben. Unter „Fotos“ ist die vollständige Galerie
verfügbar; verschachtelte Alben filtern die Galerie auf das ausgewählte Album.
Die Beispielbilder und Albumdaten sind lokal im Frontend hinterlegt. Uploads
erscheinen nur in der aktuellen Browser-Sitzung und werden nicht in eine Cloud
übertragen. Automatische Handy-Synchronisierung, Profileinstellungen und weitere
Aktionen benötigen noch eine Backend-Anbindung.

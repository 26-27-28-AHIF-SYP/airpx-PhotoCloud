# AirPX PhotoCloud – Dashboard

Die Web-App ist aktuell bewusst auf eine einzelne, responsive Dashboard-Seite
beschränkt. Sie verwendet React mit Vite; es gibt keine zusätzlichen
Frontend-Routen. Die Registrierung, Anmeldung und Fotoliste werden vom Backend geladen. Der
Schalter „Nutzer / Admin“ bleibt eine Mockup-Vorschau und simuliert keine
echten Rollen oder Zugriffsrechte.

## Lokal starten

```bash
npm install
npm run dev
```

Vite proxies relative `/api` requests to `http://localhost:8080` during local
development. The hosted Nginx container uses the same `/api` path and proxies
it to the backend on the Compose network. Set `VITE_API_BASE_URL` in `.env` to
override the default `/api` base URL.

The registration calls `POST /api/auth/register`; after a successful
registration, the user can sign in with the new account. Login calls
`POST /api/auth/login` and keeps the bearer token in memory until logout or
page refresh. The gallery calls `GET /api/pictures` and shows
the default page of up to 20 results. The API currently returns metadata and
the original-image route has no thumbnail variant, so gallery thumbnails
remain mock images. Favorites, album membership, sharing, and admin data are
also still mock data.

Die Übersicht zeigt zuletzt hinzugefügte Medien. Geteilte Alben samt Absender
werden weiterhin mit Mock-Daten angezeigt.
Benachrichtigungen erscheinen beim Profil. „Teilen“ zeigt alle geteilten Alben;
unter „Fotos“ ist die geladene Fotoseite verfügbar. Verschachtelte Alben
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

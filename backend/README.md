## PhotoCloud Backend

Ein Quarkus-Backend für die AirPX PhotoCloud.

### Starten

Voraussetzungen: Java 21 oder neuer und Maven.

```bash
mvn clean package
```
```bash
docker compose up --build
```
Der Server läuft danach auf `http://localhost:8080`.

### Endpoints

- `GET /api` - prüft, ob der Server laeuft
- `GET /api/albums` - listet alle Alben

Die Daten werden in einer PostgreSQL Datenkbank gespeichert und bleiben im Volume erhalten.

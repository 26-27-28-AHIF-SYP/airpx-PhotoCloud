# PhotoCloud backend

Quarkus 3.40 / Java 21 REST API backed by PostgreSQL. Docker Compose persists
PostgreSQL data and uploaded files in named Docker volumes.

## Configure and run

Copy `.env.example` to `.env`, set local database and bootstrap credentials,
then run from `backend/`:

```sh
./mvnw package -DskipTests
docker compose --env-file .env up --build
```

On first startup, Compose initializes PostgreSQL from `POSTGRES_DB`,
`POSTGRES_USER`, and `POSTGRES_PASSWORD`. Flyway applies the schema migrations
from `src/main/resources/db/migration/`; Hibernate validates the resulting
schema. Development mode instead uses an ephemeral PostgreSQL Dev Service and
recreates its schema on startup.

Re-run the Maven package command after backend source changes before rebuilding
the image. The Dockerfile copies the already packaged `target/quarkus-app/`;
`./mvnw test` alone does not refresh that runnable image input.

Do not commit `.env`. The default development API base URL is
`http://localhost:8080/api`. Photo files are stored under
`PHOTO_STORAGE_DIRECTORY` (Compose default `/var/lib/photocloud/photos`) and
persist in the `picture-data` volume. `PHOTO_MAX_BODY_SIZE` controls Quarkus's
maximum request size (default `100M`).

## Endpoints

- `GET /api` — health response.
- `GET /api/albums` — list albums.
- `POST /api/auth/login` — authenticate with email and password; returns a
  bearer token.
- `POST /api/auth/register` — register a account.
- `POST /api/pictures/missing` — authenticated batch check. JSON body:
  `{ "hashes": ["<sha256>"] }`; response contains `missingHashes`.
- `POST /api/pictures` — authenticated multipart upload. Fields: `file`,
  `filename`, `capturedAt`, `sha256`, `size`, `mimeType`, optional `albumId`.
  The server accepts image MIME types, verifies size and SHA-256, generates the
  disk filename, and associates the picture with the authenticated account.
- `GET /api/pictures` — authenticated metadata list for the current account.

All picture routes require `Authorization: Bearer <token>`. An optional album ID
must belong to the authenticated account.

## Build and test

```sh
./mvnw test
./mvnw package
```

Tests use Quarkus Dev Services/Testcontainers and require Docker. Production
configuration reads database, bootstrap authentication, picture directory, and
upload-size values from the environment; see `.env.example` and
`src/main/resources/application.properties`.

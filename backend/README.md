# PhotoCloud backend

Quarkus 3.40 / Java 21 REST API backed by PostgreSQL. Docker Compose persists
PostgreSQL data and uploaded files in named Docker volumes (`postgres_data` and
`photo_data`).

## Configure and run locally

Copy `.env.example` to `.env`, set local database and bootstrap credentials,
then run from `backend/`:

```sh
docker compose up -d --build
docker compose logs -f app
```

On first startup, Compose initializes PostgreSQL from `POSTGRES_DB`,
`POSTGRES_USER`, and `POSTGRES_PASSWORD`. Flyway applies the schema migrations
from `src/main/resources/db/migration/`; Hibernate validates the resulting
schema. Development mode instead uses an ephemeral PostgreSQL Dev Service and
recreates its schema on startup.

The backend Dockerfile builds the Quarkus application inside a Maven build
stage, so a fresh checkout only needs Docker Compose.

Do not commit `.env`. The Compose web app is available at
`http://127.0.0.1:8081`; its `/api` requests are proxied to the backend.
Direct development API access defaults to `http://localhost:8080/api`
(Android emulator: `http://10.0.2.2:8080/api`).

### Environment variables

| Variable | Purpose |
| --- | --- |
| `APP_PORT` | Host port for the API (the container always listens on 8080). |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Database credentials used by the app. |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Credentials Postgres is created with. Must match the `DB_*` values. |
| `AUTH_BOOTSTRAP_USERNAME`, `AUTH_BOOTSTRAP_EMAIL`, `AUTH_BOOTSTRAP_PASSWORD` | Account created at startup. Leave empty to skip. |
| `AUTH_TOKEN_LIFETIME` | Token lifetime as an ISO-8601 duration (default `PT8H`). |
| `PHOTO_STORAGE_DIRECTORY` | Photo directory in the container (default `/var/lib/photocloud/photos`, backed by the `photo_data` volume). |
| `PHOTO_MAX_BODY_SIZE` | Maximum request size (default `100M`; raise it for videos). |

Generate passwords with `openssl rand -hex 24`.

**Bootstrap user:** it is created at startup when neither the configured
username nor email already exists. Existing accounts are left unchanged and
logged; credentials are never reset by bootstrap.

## Hosting on a server (Tailscale)

This makes the backend reachable from real phones without opening any port to
the internet. Only devices on your tailnet can reach it.

1. **Compose ports.** Bind the app to localhost and do not publish Postgres
   (the app reaches it over the Docker network):

   ```yaml
   app:
     restart: unless-stopped
     ports:
       - "127.0.0.1:${APP_PORT}:8080"
   db:
     restart: unless-stopped
     # no ports: block
   ```

2. **Start the stack:**

   ```sh
   docker compose up -d --build
   docker compose logs -f app   # wait for "started"
   ```

3. **Install Tailscale** on the server and run `sudo tailscale up`. In the
   Tailscale admin console under DNS, enable **MagicDNS** and **HTTPS
   Certificates**.

4. **Publish inside the tailnet:**

   ```sh
   sudo tailscale serve --bg http://127.0.0.1:8081
   tailscale serve status           # prints https://<server>.<tailnet>.ts.net
   ```

   Do not use `tailscale funnel`; it makes the server public.

5. **Phones:** install Tailscale, sign in to the same account, and enter
   `https://<server>.<tailnet>.ts.net` as the server URL. Plain `http://` is
   rejected by iOS and blocked by default on Android, so use the HTTPS address.

Check it: open `https://<server>.<tailnet>.ts.net/api` on a phone.

Server installation, Tailscale HTTPS, reboot checks, and the automatic update
timer are documented in [`../docs/server-hosting.md`](../docs/server-hosting.md).

### Operations

- Count uploaded photos:
  `docker exec -it postgres-db psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT count(*) FROM photo;"`
- Count stored files:
  `docker exec quarkus-app sh -c 'find /var/lib/photocloud/photos -type f | wc -l'`
- `docker compose down` keeps the data; `docker compose down -v` deletes the
  database and all photos. Back up both volumes regularly.
- Use a long password for the bootstrap account before storing real photos.
  There is no password-reset endpoint yet.

Photo files are stored under `PHOTO_STORAGE_DIRECTORY` (Compose default
`/var/lib/photocloud/photos`) and persist in the `picture-data` volume.
`PHOTO_MAX_BODY_SIZE` controls Quarkus's maximum request size (default `100M`),
`PHOTO_MAX_SIZE` the per-file limit (default `25M`), and `PHOTO_PAGE_SIZE_MAX`
the list page-size cap (default `100`).

## Endpoints

- `GET /api` — health response.
- `GET /api/albums` — list albums.
- `POST /api/auth/login` — authenticate with email and password; returns a
  bearer token.
- `POST /api/auth/register` — register a account.
- `POST /api/pictures/missing` — authenticated batch check. JSON body:
  `{ "hashes": ["<sha256>"] }`; response contains `missingHashes`.
- `POST /api/pictures` — authenticated multipart upload. Only `file` is
  required; `filename`, `capturedAt`, `sha256`, `size`, `mimeType`, and
  `albumId` are optional. JPEG, PNG, and WebP are accepted based on magic
  bytes; the server verifies SHA-256, generates the disk filename, and returns
  `201` with a `Location` header.
- `GET /api/pictures?page=1&size=20` — authenticated metadata list for the
  current account. Responses carry `X-Total-Count`, `X-Page`, and
  `X-Page-Size` headers.
- `GET /api/pictures/{pictureId}` — authenticated image stream for the owner or
  an account with read access to an album containing the picture.

All picture routes require `Authorization: Bearer <token>`. An optional album ID
must belong to the authenticated account. The full route list is available at
`/q/openapi`.

## Build and test

```sh
./mvnw test
./mvnw package
```

Tests use Quarkus Dev Services/Testcontainers and require Docker. Production
configuration reads database, bootstrap authentication, picture directory, and
upload-size values from the environment; see `.env.example` and
`src/main/resources/application.properties`.

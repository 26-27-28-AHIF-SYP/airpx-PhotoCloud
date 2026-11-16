# PhotoCloud backend

Quarkus 3.40 / Java 21 REST API backed by PostgreSQL. Docker Compose persists
PostgreSQL data and uploaded files in named Docker volumes (`postgres_data` and
`photo_data`).

## Configure and run locally

Copy `.env.example` to `.env`, set local database and bootstrap credentials,
then run from `backend/`:

```sh
./mvnw package -DskipTests
docker compose up -d --build
docker compose logs -f app
```

Re-run the Maven package command after backend source changes before rebuilding
the image. The Dockerfile copies the already packaged `target/quarkus-app/`;
`./mvnw test` alone does not refresh that runnable image input.

Do not commit `.env`. The default development API base URL is
`http://localhost:8080/api` (Android emulator: `http://10.0.2.2:8080/api`).

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

**Bootstrap user:** it is created at startup only. If a user already exists
with the same username but a different email than `AUTH_BOOTSTRAP_EMAIL`, the
insert fails with a duplicate-key error and the app does not start. After the
first start, keep the three values identical to the existing account or empty
them.

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

2. **Build the jar.** The server does not need Java; build in Docker:

   ```sh
   cd backend
   docker run --rm -v "$PWD":/build -v ~/.m2:/root/.m2 -w /build \
     maven:3-eclipse-temurin-21 mvn package -DskipTests
   sudo chown -R $USER:$USER target
   ```

3. **Start the stack:**

   ```sh
   docker compose up -d --build
   docker compose logs -f app   # wait for "started"
   ```

4. **Install Tailscale** on the server and run `sudo tailscale up`. In the
   Tailscale admin console under DNS, enable **MagicDNS** and **HTTPS
   Certificates**.

5. **Publish inside the tailnet:**

   ```sh
   sudo tailscale serve --bg 8080   # use your APP_PORT
   tailscale serve status           # prints https://<server>.<tailnet>.ts.net
   ```

   Do not use `tailscale funnel`; it makes the server public.

6. **Phones:** install Tailscale, sign in to the same account, and enter
   `https://<server>.<tailnet>.ts.net` as the server URL. Plain `http://` is
   rejected by iOS and blocked by default on Android, so use the HTTPS address.

Check it: open `https://<server>.<tailnet>.ts.net/api` on a phone.

### Updating the server

```sh
git pull
cd backend
docker run --rm -v "$PWD":/build -v ~/.m2:/root/.m2 -w /build \
  maven:3-eclipse-temurin-21 mvn package -DskipTests
docker compose up -d --build
```

### Operations

- Count uploaded photos:
  `docker exec -it postgres-db psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT count(*) FROM photo;"`
- Count stored files:
  `docker exec quarkus-app sh -c 'find /var/lib/photocloud/photos -type f | wc -l'`
- `docker compose down` keeps the data; `docker compose down -v` deletes the
  database and all photos. Back up both volumes regularly.
- Use a long password for the bootstrap account before storing real photos.
  There is no password-reset endpoint yet.

## Endpoints

- `GET /api` — health response.
- `GET /api/albums` — list albums.
- `POST /api/auth/login` — authenticate with email and password; returns a
  bearer token.
- `POST /api/auth/register` — register a user.
- `POST /api/photos/missing` — authenticated batch check. JSON body:
  `{ "hashes": ["<sha256>"] }`; response contains `missingHashes`.
- `POST /api/photos` — authenticated multipart upload. Fields: `file`,
  `filename`, `capturedAt`, `sha256`, `size`, `mimeType`, optional `albumId`.
  The server accepts image MIME types, verifies size and SHA-256, generates the
  disk filename, and associates the photo with the authenticated user.
- `GET /api/photos` — authenticated metadata list for the current user.

All photo routes require `Authorization: Bearer <token>`. An optional album ID
must belong to the authenticated user. The full route list is available at
`/q/openapi`.

## Build and test

```sh
./mvnw test
./mvnw package
```

Tests use Quarkus Dev Services/Testcontainers and require Docker. Production
configuration reads database, bootstrap authentication, photo directory, and
upload-size values from the environment; see `.env.example` and
`src/main/resources/application.properties`.
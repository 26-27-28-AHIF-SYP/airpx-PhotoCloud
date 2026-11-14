# Backend Contribution Guide

This file applies only to work inside `backend/`. It describes the current
backend implementation and the rules for extending it. Repository-level
guidance in `../AGENTS.md` still applies where it does not conflict with this
file.

## Scope

Backend work includes:

- Quarkus REST endpoints and API contracts
- Authentication, users, authorization, and session handling
- Persistence, JPA entities, repositories, and database configuration
- Backend services and DTOs
- Backend tests, integration tests, and deployment configuration

Do not assume that planned picture, album, sharing, AI, or encryption features
already exist. The project proposal describes the product direction; the Java
sources and current API documentation are the source of truth for implemented
behavior.

Changes to Android, iOS, or web clients are outside this guide's scope. When a
backend endpoint changes, update directly related API documentation and client
contracts only when that is part of the task.

## Current backend stack

- Java 21
- Quarkus 3.40.1.2
- Maven Wrapper (`./mvnw`)
- RESTEasy Reactive through `quarkus-rest` and `quarkus-rest-jackson`
- Hibernate ORM with Jakarta Persistence
- PostgreSQL through `quarkus-jdbc-postgresql`
- Hibernate Validator for Bean Validation
- SmallRye OpenAPI
- JUnit, RestAssured, and Quarkus test support
- Testcontainers-backed PostgreSQL Dev Services during tests

The backend is a Maven project rooted at this directory. Run backend commands
from `backend/`, unless a command explicitly uses another path.

## Directory structure

```text
backend/
├── pom.xml
├── mvnw
├── src/
│   ├── main/
│   │   ├── java/at/airpx/
│   │   │   ├── api/                 # HTTP resources
│   │   │   ├── model/               # Request, response, and DTO records
│   │   │   └── workloads/           # Domain services, repositories, entities
│   │   ├── resources/
│   │   │   ├── application.properties
│   │   │   └── import.sql
│   │   └── docker/                  # Quarkus-generated container Dockerfiles
│   └── test/
│       ├── java/at/airpx/           # Unit, REST, and integration tests
│       └── resources/
│           └── application.properties
├── Dockerfile
├── docker-compose.yml
└── .env.example
```

Keep HTTP concerns in `api/`, transport models in `model/`, and business and
persistence logic in `workloads/`. Do not put database queries in REST
resources or expose JPA entities directly when a response DTO is appropriate.

## Running and building

Run the complete backend test suite:

```bash
./mvnw test
```

Run tests quietly:

```bash
./mvnw test -q
```

Run the application in development mode:

```bash
./mvnw quarkus:dev
```

Build the Quarkus application:

```bash
./mvnw package
```

Build without tests when a deployment image must be assembled after tests have
already passed:

```bash
./mvnw package -DskipTests
```

The root `Dockerfile` copies `target/quarkus-app/`, so the Maven package step
must run before building the Docker image.

Start the Docker Compose deployment from `backend/`:

```bash
docker compose --env-file .env up --build
```

Do not commit `.env`. Use `.env.example` as a template and replace
`CHANGE_ME` values locally.

## Configuration and Quarkus profiles

Configuration is in `src/main/resources/application.properties`.

Supported profiles currently include:

- `%dev`: PostgreSQL Dev Services and `drop-and-create` schema handling
- `%test`: PostgreSQL test configuration; Dev Services supplies the database
- `%prod`: PostgreSQL connection settings from environment variables and
  schema validation

Production database variables are:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASSWORD
```

Authentication bootstrap variables are:

```text
AUTH_BOOTSTRAP_USERNAME
AUTH_BOOTSTRAP_EMAIL
AUTH_BOOTSTRAP_PASSWORD
AUTH_TOKEN_LIFETIME
```

Photo storage variables are:

```text
PHOTO_STORAGE_DIRECTORY
PHOTO_MAX_BODY_SIZE
```

`PHOTO_STORAGE_DIRECTORY` defaults to `./data/photos` in the application and
is set to `/var/lib/photocloud/photos` by the Compose example. Compose mounts
that container directory on the `picture-data` Docker volume. `PHOTO_MAX_BODY_SIZE`
sets `quarkus.http.limits.max-body-size` and defaults to `100M`.

The bootstrap values are optional. If all required bootstrap values are
present, the application creates the account only when that normalized email does
not already exist. It does not update an existing account's password.

Never put real credentials in tracked files. The test configuration contains a
deliberate test-only account and must not be used as a production credential
source.

Be careful with schema-management settings:

- `drop-and-create` is suitable for local development and disposable tests,
  not production.
- Production uses `validate`; entity changes require a planned database
  migration before deployment.
- Docker Compose uses the production profile: Flyway applies migrations before
  Hibernate validates the schema. Do not switch Compose to Hibernate `update`.

## Current HTTP endpoints

The currently implemented endpoints are:

```text
GET  /api
GET  /api/albums
POST /api/auth/login
POST /api/auth/register
POST /api/pictures/missing
POST /api/pictures            (multipart upload)
GET  /api/pictures
```

All `/api/pictures` routes require `Authorization: Bearer <token>`. The missing
hash request is JSON `{ "hashes": ["<sha256>"] }` and returns
`{ "missingHashes": [...] }`. Upload multipart fields are `file`, `filename`,
`capturedAt`, `sha256`, `size`, and `mimeType`; `albumId` is optional and must
identify an album owned by the authenticated account. Uploads accept `image/*`,
are SHA-256 checked on the server, and are stored under generated filenames.
The picture list returns only the authenticated account's metadata.

The login request is validated with Bean Validation and uses:

```json
{
  "email": "account@example.com",
  "password": "secret"
}
```

The registration request uses:

```json
{
  "username": "account",
  "email": "account@example.com",
  "password": "secret"
}
```

Current authentication behavior:

- Email addresses are trimmed and normalized to lowercase for lookup.
- Passwords are hashed with PBKDF2-HMAC-SHA256 and a random salt.
- Login returns an opaque bearer token and an expiration timestamp.
- Tokens are currently kept in application memory; they are not yet durable
  across restarts or shared across multiple backend instances.
- Registration returns `201 Created`.
- A duplicate username or email returns `409 Conflict`.
- Invalid request data is rejected through Bean Validation with `400 Bad
  Request`.
- Invalid login credentials return `401 Unauthorized`.

When adding protected endpoints, do not merely accept an `Authorization`
header. Validate the bearer token through a shared authentication mechanism and
derive the authenticated account from it. Do not duplicate token parsing or
authentication logic in individual resources.

### Authentication rules for protected endpoints

Every endpoint that accesses account-specific or private data must be protected
by the central authentication mechanism. A resource must not implement its own
token parsing, token format checks, expiration checks, or account lookup.

Use the standard header format:

```text
Authorization: Bearer <access-token>
```

Authentication handling must:

- reject missing, malformed, unknown, and expired tokens with `401 Unauthorized`;
- extract the authenticated account identity from the validated token context;
- enforce ownership or authorization rules in the service layer before reading
  or changing account-specific data;
- distinguish authentication failures (`401`) from authenticated users lacking
  permission (`403`);
- never accept bearer tokens in query parameters, request bodies, filenames, or
  cookies unless a later API decision explicitly defines that behavior;
- never log access tokens, passwords, password hashes, or complete
  `Authorization` headers;
- avoid revealing whether a account account exists through login error messages;
- keep token lifetime and token-storage behavior configurable and documented.

When introducing a new authentication implementation, first check whether the
existing token service can be extended. If the in-memory token store is replaced
with a database or shared session store, preserve token expiration, revocation,
and multi-instance behavior and add tests for those cases.

For each new protected endpoint, add tests for at least:

1. no `Authorization` header;
2. malformed or invalid token;
3. expired token, when expiration can be controlled in the test;
4. valid token accessing the authenticated account's own data;
5. valid token attempting to access another account's data.

## REST resource conventions

Use constructor injection for dependencies:

```java
public ExampleResource(ExampleService exampleService) {
    this.exampleService = exampleService;
}
```

Use explicit paths and media types. Request records should be validated at the
resource boundary:

```java
public Response create(@Valid @NotNull CreateRequest request) {
    // delegate to the service
}
```

Use records for small request and response DTOs. Keep validation annotations
on request record components. Do not expose password fields in response
records.

Resources should translate service outcomes into HTTP responses. Services
should contain business rules and repositories should contain persistence
operations. Avoid broad exception catches and avoid silently converting
unexpected failures into successful responses.

## Persistence conventions

- Use JPA entities only for persisted state.
- Keep unique constraints in the entity and enforce duplicate handling in the
  service as well.
- Normalize email values consistently during registration and login.
- Hash passwords before persistence; never persist or log plaintext passwords.
- Use `@Transactional` around writes and service operations that require a
  consistent persistence context.
- Add database migrations before relying on `%prod` schema validation for an
  entity change.
- Do not use `drop-and-create` as a production migration strategy.

The current account table is named `app_user` because `account` can conflict with
database-reserved identifiers. Preserve this explicit table name unless a
migration is planned.

## Testing conventions

Backend tests use `@QuarkusTest` and RestAssured for HTTP behavior. Tests
automatically start a PostgreSQL container through Quarkus Dev Services when
Docker is available.

For endpoint changes, cover at least:

- successful response and response body
- validation failures
- authentication or authorization failures where applicable
- duplicate or conflict behavior
- the relevant follow-up flow, such as registering and then logging in

Tests must be independent:

- Do not rely on another test method running first.
- Do not assume JUnit test order.
- Use unique usernames and email addresses where persisted data can remain
  visible during a test run.
- Use the test-only configuration in
  `src/test/resources/application.properties`; never rely on a developer's
  local `.env`.

Run the smallest relevant test during development, then run the full suite
before reporting completion:

```bash
./mvnw test
```

Also check patch formatting:

```bash
git diff --check
```

## API documentation

The OpenAPI contract is stored outside this directory at
`../docs/api/openapi.yaml`. When changing a backend route, request, response,
status code, or authentication behavior, update the corresponding OpenAPI
entry if the documentation is in scope.

Keep the documented contract honest. The picture endpoints above are implemented
in `api/PictureResource.java`; check its paths and tests when changing them.

## Security rules

- Never commit credentials, tokens, private keys, or real personal data.
- Never log passwords, password hashes, bearer tokens, or full authentication
  requests.
- Use constant-time hash comparisons where applicable; keep using the existing
  password hashing helper rather than comparing plaintext passwords.
- Return generic login failure messages to avoid revealing whether an email
  exists.
- Treat account-supplied filenames, metadata, and media content as untrusted.
- Do not add a new authentication implementation for one endpoint when a
  shared service or filter can be used.
- Remember that the current in-memory token store is not suitable for a
  multi-instance production deployment without a shared token/session strategy.

## Change checklist

Before editing:

1. Check `git status --short`.
2. Read the affected resource, service, repository, entity, tests, and
   configuration.
3. Check `../docs/api/openapi.yaml` for the existing API contract.
4. Preserve unrelated worktree changes.

Before finishing:

1. Add focused tests for new behavior.
2. Run `./mvnw test`.
3. Run `git diff --check`.
4. Verify that no `.env`, build output, IDE files, or generated artifacts are
   staged.
5. Review `git diff` and `git diff --cached` before committing.
6. Document any intentionally unimplemented or deployment-dependent behavior.

Keep changes surgical. Do not refactor unrelated modules while implementing a
backend feature.

## Commit suggestions after larger tasks

After completing a larger backend task, provide a commit recommendation before
finishing the work. This applies to feature implementations, substantial bug
fixes, API changes, authentication changes, database changes, and other work
that affects more than a few lines. Do not produce a commit recommendation for
minor edits such as a typo correction or a change limited to a few lines unless
the account asks for one.

The recommendation must:

- state whether the changes should be committed as one commit or split into
  multiple logical commits;
- explain the proposed grouping briefly when more than one commit is
  appropriate;
- provide a clear commit message for every proposed commit;
- mention relevant staged or unstaged files when that affects the recommendation;
- avoid including unrelated worktree changes in the proposed commit.

Use concise, imperative Conventional Commit-style messages where appropriate,
for example:

```text
feat(auth): add account registration
fix(auth): normalize registration email before persistence
test(auth): cover duplicate registration conflicts
```

Do not create a commit automatically unless the account explicitly requests it.

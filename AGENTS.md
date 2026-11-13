# AirPx PhotoCloud repository guide

## Repository layout and current stacks

- `backend/`: Java 21, Quarkus 3.40, Hibernate ORM, PostgreSQL, Docker Compose, REST/Jackson, and SmallRye OpenAPI.
- `androidApp/`: Kotlin Android application with Jetpack Compose/Material 3. The app uses MediaStore, SQLiteOpenHelper for cached hashes, DataStore/Tink for settings and the bearer token, and WorkManager for sync.
- `iosApp/airpix/`: SwiftUI iOS app packaged with SwiftPM and built with xtool. It uses PhotoKit, CryptoKit, Keychain, URLSession, and BGTaskScheduler.
- `iosApp/PhotoCloudSync/`: platform-independent SwiftPM sync use case and unit tests.
- `webapp/`: React/Vite web app.
- `database/`: database documentation; there is no standalone database source/build module yet.
- `docs/`: project, user-story, and API documentation.

## Photo sync contract

Both mobile clients use the backend API. The flow is: authenticate with
`POST /api/auth/login`, compute local SHA-256 hashes, check missing hashes with
`POST /api/photos/missing`, upload missing files as multipart `POST /api/photos`,
then refresh the user's list with `GET /api/photos`. Photos and stored files are
owned by the authenticated user. Do not document planned sharing, AI, or
end-to-end encryption as implemented unless the code confirms it.

## Build and test commands

Run commands from the named module directory:

```sh
cd backend && ./mvnw test
cd androidApp && ./gradlew assembleDebug test
cd iosApp/PhotoCloudSync && swift test
cd iosApp/airpix && xtool dev build
cd webapp && npm ci && npm run build
```

`database/` currently has no source or test command. Linux has no iOS Simulator;
xtool needs a compatible Swift toolchain and iOS SDK. Device installation needs
a physical iPhone and signing configuration. The web app has no test script.

## Development networking and permissions

The Android emulator reaches the host backend at
`http://10.0.2.2:8080/api`. This address is Android-emulator-specific. An iPhone
must use the host's LAN IP or a LAN hostname. Development HTTP exceptions must
stay limited to explicitly named development hosts in platform configuration.
Android photo sync requires media permission; Android 14+ can grant partial
access to selected media. iOS photo access is controlled by PhotoKit
authorization.

## Working and commit conventions

- Keep changes focused and preserve unrelated worktree edits.
- Run the relevant module checks and `git diff --check` before finishing.
- Use small, logical Conventional Commits with imperative English subjects.
- Every commit must end with these trailers, separated from its body by one
  blank line:


- Never commit `.env`, credentials, tokens, build output, or generated files.
  Use `.env.example` for documented variable changes.

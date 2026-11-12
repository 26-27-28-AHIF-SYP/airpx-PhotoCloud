# Android app guide

## Stack and layout

- Kotlin Android app, package `com.airpx.photocloud`; Jetpack Compose and
  Material 3; min SDK 29, compile/target SDK 36, Java toolchain 17.
- `app/src/main/kotlin/com/airpx/photocloud/domain/`: sync model, repository
  contract, and use cases.
- `.../data/`: MediaStore scanner, SQLiteOpenHelper hash cache, HTTP API and
  repository, DataStore/Tink settings, and WorkManager worker/scheduler.
- `.../ui/`: Compose screens. Screen state is currently managed in the app's
  Compose/activity state; there is no ViewModel/DI framework requirement.

## Photo sync behavior

MediaStore scan results are hashed with SHA-256. The SQLite cache is keyed by
MediaStore ID and `DATE_MODIFIED`; unchanged items reuse their saved hash. Sync
checks the backend's missing-hash endpoint, uploads missing images as multipart,
and reads the user's photo list. The Auto-Backup screen offers unique periodic
WorkManager work and a unique one-time **Jetzt synchronisieren** request.
WorkManager requires a connected network, or an unmetered network when **Nur
WLAN** is enabled, and retries failures with exponential backoff.

## Permissions and settings

- `READ_MEDIA_IMAGES` is requested on Android 13+; `READ_EXTERNAL_STORAGE` is
  used on API 29–32.
- Android 14+ also declares `READ_MEDIA_VISUAL_USER_SELECTED`; selected-photo
  access limits scanning to the assets visible to the app.
- The app manifest declares `INTERNET` and `ACCESS_NETWORK_STATE`.
- Server URL and preferences use DataStore. The bearer token is encrypted with
  Tink and Android Keystore-backed key material.
- Development HTTP is scoped in `app/src/main/res/xml/network_security_config.xml`.

## Build and tests

From this directory:

```sh
./gradlew assembleDebug test
```

The Android emulator reaches a host backend at
`http://10.0.2.2:8080/api`; this address is not the iPhone or physical-device
host address. Use a reachable LAN host for physical devices.

Keep changes focused. Do not document unimplemented resumable uploads,
notification progress, server-side album management, or OEM-specific battery
guidance as existing behavior. Never commit `.env`, credentials, build output,
or generated files.

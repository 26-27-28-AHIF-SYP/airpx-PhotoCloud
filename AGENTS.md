# AirPx PhotoCloud repository guide

## Project folders and stacks

- `backend/`: Quarkus 3.40, Java 21, Hibernate ORM, PostgreSQL, REST/Jackson, and SmallRye OpenAPI. Run Maven commands from this folder.
- `androidApp/`: Kotlin 2.2.10, Jetpack Compose/Material 3, Android Gradle Plugin 9.5.1, Gradle 9.6, min SDK 29 and target SDK 36. WorkManager schedules sync; MediaStore reads images; SQLiteOpenHelper stores the hash cache; DataStore and Tink protect settings and the bearer token.
- `iosApp/airpix/`: Swift Package Manager app package built with xtool, SwiftUI, PhotoKit, CryptoKit, Keychain, URLSession, and BGTaskScheduler. `Package.swift`, `xtool.yml`, and `Info.plist` are in this folder.
- `docs/`: product and API documentation.

## Build and test

```sh
cd backend && ./mvnw test
cd androidApp && ./gradlew assembleDebug test
cd iosApp/airpix && xtool build
```

The platform-independent iOS sync unit tests run with `cd iosApp/PhotoCloudSync && swift test`. Linux has no iOS Simulator. xtool needs a compatible Swift toolchain and configured iOS SDK; device installs require a physical iPhone and signing setup.

## Development networking and iOS notes

The Android emulator reaches the host backend at `http://10.0.2.2:8080/api`. This address is specific to the Android emulator; an iPhone needs a host address reachable on its network. Keep development HTTP exceptions scoped to named development hosts in platform configuration.

The iOS app is SwiftPM/xtool-based, not an Xcode project. Make app changes under `iosApp/airpix/Sources/airpix`; the platform-independent sync domain and tests are in `iosApp/PhotoCloudSync`. `Info.plist` is supplied through `iosApp/airpix/xtool.yml`. Its HTTP exceptions are limited to `10.0.2.2`, `localhost`, `127.0.0.1`, and `photocloud.local`. Photo access follows the user's PhotoKit authorization, and iOS schedules background processing opportunistically; force-quitting can defer work until the app is reopened. App Store distribution and final archive workflows require macOS/Xcode.

Do not commit credentials or edit a local `.env`. Use `.env.example` for documented variable changes and preserve unrelated worktree edits.

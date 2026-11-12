# iOS app guide

## Project and stack

- `airpix/` is the SwiftPM app package, built with xtool; it is not an Xcode
  project. App sources are under `airpix/Sources/airpix/`.
- `PhotoCloudSync/` is the platform-independent SwiftPM sync package and its
  unit tests.
- The app uses SwiftUI, PhotoKit, CryptoKit, Keychain, URLSession, and
  BGTaskScheduler. `Info.plist` is supplied through `airpix/xtool.yml`.

## Sync contract

The client uses `GET /api`, `POST /api/auth/login`, `POST /api/photos/missing`,
multipart `POST /api/photos`, and `GET /api/photos`. It caches hashes by PhotoKit
asset local identifier and modification date. Tokens belong in Keychain. Keep
development HTTP exceptions limited to the explicit hosts listed in
`airpix/Info.plist`; do not enable arbitrary cleartext traffic globally.

PhotoKit access must be requested with user-facing context and must respect
limited-library authorization. Auto-Backup is opt-in. BGProcessingTask timing
is controlled by iOS and must not be described as a guaranteed schedule.

## Build and test

```sh
cd airpix && xtool dev build
cd PhotoCloudSync && swift test
```

Run these from `iosApp/`. xtool needs a compatible Swift toolchain and iOS SDK;
device installation needs a physical iPhone and signing/device setup. Linux has
no iOS Simulator. PhotoKit, Keychain, network, and background behavior require
device runtime verification.

Keep changes focused, preserve unrelated edits, and do not add dependencies
without a requirement. Never commit credentials, `.env`, or build output.

# AirPx PhotoCloud iOS

The native iOS client is a SwiftPM app package at `airpix/`, built on Linux
with [xtool](https://github.com/xtool-org/xtool). The shared Swift-only sync
logic and its tests are in `PhotoCloudSync/`.

## Setup and build

On Linux, install a Swift toolchain supported by the current xtool release,
install xtool, and install/configure the iOS SDK using xtool's setup
instructions. From the repository root:

```sh
cd iosApp/airpix
xtool dev build
```

Installing and running requires a physical iPhone, device trust/developer-mode
setup, and signing configuration supported by xtool. There is no Xcode project
and Linux has no iOS Simulator. Distribution/archive workflows may require a
macOS/Xcode environment.

Run the platform-independent use-case tests on Linux with:

```sh
cd iosApp/PhotoCloudSync
swift test
```

The app package itself uses iOS-only frameworks and cannot be tested as a
regular Linux Swift package. The app was built with `xtool dev build` in a
clean copy of the source tree; it has not been installed or exercised on an
iPhone in this environment. PhotoKit authorization, Keychain behavior,
background execution, and live server upload therefore remain device-runtime
checks.

## Connect to the backend

Start the backend using [`../backend/README.md`](../backend/README.md). On an
iPhone, enter the backend host's LAN IP address or a LAN hostname, for example
`http://192.168.1.20:8080/api`. Do not use `localhost` (that points to the
iPhone) or Android's `10.0.2.2` emulator address. Use HTTPS outside development.

`airpix/Info.plist` includes `NSLocalNetworkUsageDescription`. Its
`NSAppTransportSecurity.NSExceptionDomains` exceptions permit HTTP only for
`10.0.2.2`, `localhost`, `127.0.0.1`, and `photocloud.local`; there is no global
`NSAllowsArbitraryLoads` exception. For an iPhone on a LAN, configure a
reachable development hostname from that list (normally `photocloud.local`) or
use HTTPS. The app also needs `NSPhotoLibraryUsageDescription` for PhotoKit.

## Implemented functionality

- German **Serververbindung** screen with server URL, **Server prüfen**, and
  email/password login. The bearer token is stored in Keychain.
- PhotoKit read authorization, including the system's limited-library access.
- Photo hashing with CryptoKit and local hash records keyed by asset
  `localIdentifier` and modification date.
- Hash-missing check, multipart upload of missing photos, and refresh of the
  server photo list using the backend `/api` endpoints.
- Manual **Jetzt synchronisieren**, opt-in **Auto-Backup**, and uploaded,
  pending, and failed counts. Background URLSession uploads and a
  network-constrained BGProcessingTask are configured.

The structure follows `data/` (server, PhotoKit, storage), `domain/` (the
`PhotoCloudSync` SwiftPM use case), and `ui/` (SwiftUI screens). iOS decides
when background processing runs; it is not a precise timer. Force-quitting can
defer background work until the app is opened again. Uploads are not resumable.

## Bootstrap account

For the local development server, sign in with the email and password set in
the backend `.env` as `AUTH_BOOTSTRAP_EMAIL` and `AUTH_BOOTSTRAP_PASSWORD`.
These are local credentials; do not commit `.env` or share its contents.

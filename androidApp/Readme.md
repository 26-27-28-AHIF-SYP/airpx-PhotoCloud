# AirPx PhotoCloud Android

This is the Kotlin/Jetpack Compose Android client in `androidApp/`. It supports
server login, browsing local media, and photo backup using the photo sync API.

## Setup and run

Requirements: JDK 17 and Android SDK 36. Open `androidApp/` in Android Studio,
or build from a shell:

```sh
cd androidApp
./gradlew assembleDebug
```

Start the backend as described in [`backend/README.md`](../backend/README.md).
For the Android emulator, set the server URL to
`http://10.0.2.2:8080/api`; `10.0.2.2` forwards to the development host. Do not
use `localhost` inside the emulator. Sign in with the bootstrap email and
password configured as `AUTH_BOOTSTRAP_EMAIL` and `AUTH_BOOTSTRAP_PASSWORD` in
the local backend `.env` file. Change the example password before using it.

On a physical Android device, use a host IP or hostname reachable from that
device's network. Cleartext HTTP is allowed only for the named development
hosts in `app/src/main/res/xml/network_security_config.xml`; use HTTPS for a
remote server.

## Photo access and synchronization

The app reads image media through MediaStore. It requests `READ_MEDIA_IMAGES`
on Android 13+ and `READ_EXTERNAL_STORAGE` on older supported releases. On
Android 14+, the user can grant full or selected-photo access through
`READ_MEDIA_VISUAL_USER_SELECTED`; partial access limits the scan to visible
photos. The app also offers the system photo picker for selecting media.

In **Auto-Backup** settings:

- **Auto-Backup** schedules unique periodic WorkManager work (every 12 hours).
- **Nur WLAN** constrains sync to an unmetered network when enabled.
- **Jetzt synchronisieren** enqueues unique one-time work, subject to network
  connectivity and photo permission. The button is enabled when a server and
  bearer-token login are saved, Auto-Backup is enabled, media access is granted
  (full or partial), and no one-time sync is already running or waiting for
  network. The screen lists these states and gives each unmet requirement. A
  zero count before the first run is shown as “Noch keine Synchronisierung
  gestartet.” A zero pending count does not block a new sync.

After login, **Serververbindung** shows the saved email and an **Abmelden**
action. Login, network, expired-session, and upload failures are shown on the
connection or sync screen.

The sync scans photos, reuses cached SHA-256 hashes when the MediaStore ID and
`DATE_MODIFIED` are unchanged, asks the server which hashes are missing, uploads
only missing files, and refreshes the server photo list. The screen displays
uploaded, pending, and failed counts from WorkManager progress/output.

## Architecture

- `domain/`: sync models, repository contract, and `SynchronizePhotosUseCase`.
- `data/`: MediaStore scanner and SQLiteOpenHelper hash cache, API client,
  repository, DataStore/Tink settings, and WorkManager worker/scheduler.
- `ui/`: Compose screens, including server connection and Auto-Backup.

The access token is encrypted with Tink using Android Keystore-backed key
material. Networking uses the existing `HttpURLConnection` stack; no additional
network library is required.

## Build and tests

```sh
cd androidApp
./gradlew assembleDebug test
```

The unit test for the sync use case is under
`app/src/test/java/com/airpx/photocloud/`.

## Known limitations

- WorkManager is subject to Android scheduling and device battery policies; a
  periodic run is not an exact timer.
- Uploads are individual multipart requests and are not resumable. A retry may
  restart an interrupted file upload.
- Server login state is stored securely, but the current connection screen does
  not yet provide a complete account-management flow.
- Partial media permission backs up only media the user has allowed the app to
  see.

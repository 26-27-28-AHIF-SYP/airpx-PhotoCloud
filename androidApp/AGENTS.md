# AGENTS.md — AirPx Photo Cloud Android Development Guidelines

This document provides instructions, architectural rules, and technical conventions for AI agents and developers working on the AirPx Photo Cloud Android application.

---

## 1. Project Overview

- **Project Name:** AirPx Photo Cloud (Android Client)
- **Package / Namespace:** `com.airpx.photocloud`
- **Application ID:** `com.airpx.photocloud`
- **Purpose:** A private, secure, self-hosted and cloud-connected photo gallery and automatic synchronization client for Android.
- **Key Capabilities:**
  - Automatic background image backup through WorkManager.
  - SHA-256 content deduplication before upload.
  - Scoped storage compliance with partial photo access (`READ_MEDIA_VISUAL_USER_SELECTED` on Android 14+).
  - OEM battery kill resilience and transparent sync telemetry.
  - Material 3 Compose UI with in-activity screen state and a timeline gallery.

---

## 2. Technical Stack & Target Platforms

| Component | Standard / Version | Notes |
|---|---|---|
| **Language** | Kotlin 2.2.10 | Coroutines and immutable UI state |
| **UI Toolkit** | Jetpack Compose + Material 3 | Single Activity, declarative UI, edge-to-edge |
| **Navigation** | Screen state in `MainActivity` | Navigation 3 is not configured |
| **Min SDK** | **29** (Android 10) | Avoids legacy external storage code paths |
| **Target SDK** | **36** (Android 16) | Google Play requirement |
| **Compile SDK** | **36** (Android 16) | See `app/build.gradle.kts` |
| **Java Toolchain** | JDK 17 | FooJay resolver convention |
| **Build System** | Gradle Kotlin DSL (`build.gradle.kts`) | Version catalog in `gradle/libs.versions.toml` |
| **Background Work**| AndroidX WorkManager | Battery & unmetered network constraints |

---

## 3. Architecture & Layer Boundaries

AirPx follows Clean Architecture principles separated into three distinct layers:

```
androidApp/app/src/main/kotlin/com/airpx/photocloud/
├── domain/                  # Pure Kotlin (No Android SDK dependencies)
│   ├── model/               # PhotoItem, Album, SyncStatus, UserAccount
│   ├── repository/          # PhotoRepository, SyncRepository, AuthRepository
│   └── usecase/             # GetPhotosUseCase, StartSyncUseCase, etc.
│
├── data/                    # Android implementations and data sources
│   ├── mediastore/          # MediaStore scanner, ContentObserver, hash calculator
│   ├── local/               # SQLiteOpenHelper photo hash cache, DataStore settings
│   ├── remote/              # HttpURLConnection API client; multipart retries restart files
│   ├── sync/                # WorkManager SyncWorker, SyncManager
│   └── repository/          # PhotoRepositoryImpl, SyncRepositoryImpl
│
└── ui/                      # Jetpack Compose UI
    ├── navigation/          # Navigation keys, NavDisplay setup
    ├── screens/
    │   ├── photos/          # Photo timeline grid & detail view
    │   ├── albums/          # Album list & device buckets
    │   ├── sync/            # Sync dashboard & OEM battery guidance
    │   └── settings/        # Server configuration & account settings
    ├── components/          # Reusable cards, badges, permission banners
    └── theme/               # Material 3 colors, typography, shapes
```

### Layer Separation Rules
1. **Domain Layer:** Must NEVER import `android.*` packages. This ensures future portability to Kotlin Multiplatform (KMP). Use pure Kotlin data types (strings, primitives, `kotlin.time.Instant`).
2. **Data Layer:** Handles `MediaStore`, the SQLite hash cache, WorkManager, HttpURLConnection uploads, and cryptographic hashing. Converts Android-specific structures (`Uri`, `Cursor`) into domain models.
3. **UI Layer:** Strictly unidirectional data flow (MVI/MVVM). ViewModels expose `StateFlow<UiState>` and consume UI events. Composable functions must be stateless or hoist state.

---

## 4. Storage, Permissions & Security

### 4.1 Permissions
- `READ_MEDIA_IMAGES` (API 33+)
- `READ_MEDIA_VISUAL_USER_SELECTED` (API 34+) — Required for Android 14+ partial access support. The app must function cleanly when the user only grants access to selected photos.
- `ACCESS_MEDIA_LOCATION` (API 29+) — Used to read EXIF GPS metadata via `MediaStore.setRequireOriginal()`.
- `POST_NOTIFICATIONS` (API 33+) — For background upload progress notifications.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_DATA_SYNC` — For long initial backups.
- `INTERNET` & `ACCESS_NETWORK_STATE` — Network synchronization constraints.

### 4.2 Security Rules
- **Token and Secret Storage:** Always use Jetpack DataStore with Google Tink (`androidx.datastore:datastore-tink`) backed by Android Keystore.
- **Deprecated APIs:** DO NOT use `EncryptedSharedPreferences` (deprecated by Google and OWASP MASTG).
- **Sensitive Logs:** Never log authentication tokens, raw passwords, or unhashed photo file paths in production logs.

---

## 5. Background Synchronization & Deduplication

1. **Two-Tier Media Detection:**
   - WorkManager periodically scans MediaStore; a hash cache keyed by MediaStore ID and `DATE_MODIFIED` avoids re-hashing unchanged files.
2. **Deduplication:**
   - Compute SHA-256 content hash of local media before upload.
   - Query local `synced_media` table and server API before sending file bytes.
3. **Uploads:**
   - Multipart uploads send one image at a time. WorkManager retries failed work; the next hash check skips content already accepted by the server.
4. **OEM Battery Restrictions:**
   - Display clear sync status (last sync time, failed count).
   - Detect aggressive battery killers (Xiaomi, Samsung, OnePlus) and offer in-app guidance linking to vendor battery optimization settings.

---

## 6. Build, Test & Tooling Guidelines for Agents

- **Run Build:**
  ```bash
  ./gradlew assembleDebug
  ```
- **Run Unit Tests:**
  ```bash
  ./gradlew assembleDebug test
  ```
- **Lint / Code Analysis:**
  ```bash
  ./gradlew lintDebug
  ```
- **File Edits:**
  - Always use built-in IDE modification tools (`replace_file_content`, `multi_replace_file_content`, `write_file`).
  - NEVER use shell commands (`sed`, `awk`, `cat >`, echo) to modify source files.
- **Dependencies:**
  - Define dependencies centrally in `gradle/libs.versions.toml`.
  - Reference them via typed bundles/aliases in `app/build.gradle.kts`.

## Repository and emulator notes

The Android project directory is `androidApp/`; the iOS SwiftPM/xtool project is `iosApp/airpix/`. From this directory run `./gradlew assembleDebug test`. On the Android emulator, use `http://10.0.2.2:8080/api` to reach the host backend. `10.0.2.2` is Android-emulator-specific; iOS devices need a host URL reachable on their LAN.

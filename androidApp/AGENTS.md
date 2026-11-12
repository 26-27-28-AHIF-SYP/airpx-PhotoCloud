# AGENTS.md — AirPx Photo Cloud Android Development Guidelines

This document provides instructions, architectural rules, and technical conventions for AI agents and developers working on the AirPx Photo Cloud Android application.

---

## 1. Project Overview

- **Project Name:** AirPx Photo Cloud (Android Client)
- **Package / Namespace:** `com.airpx.photocloud`
- **Application ID:** `com.airpx.photocloud`
- **Purpose:** A private, secure, self-hosted and cloud-connected photo gallery and automatic synchronization client for Android.
- **Key Capabilities:**
  - Automatic background photo & video backup with resume support (`tus` protocol).
  - SHA-256 content deduplication before upload.
  - Scoped storage compliance with partial photo access (`READ_MEDIA_VISUAL_USER_SELECTED` on Android 14+).
  - OEM battery kill resilience and transparent sync telemetry.
  - Material 3 Compose UI with Navigation 3, timeline gallery, and album management.

---

## 2. Technical Stack & Target Platforms

| Component | Standard / Version | Notes |
|---|---|---|
| **Language** | Kotlin 2.3+ | Modern idiomatic Kotlin (coroutines, Flow, immutability) |
| **UI Toolkit** | Jetpack Compose + Material 3 | Single Activity, declarative UI, edge-to-edge |
| **Navigation** | Navigation 3 (`androidx.navigation3`) | Type-safe backstack & serializable keys |
| **Min SDK** | **29** (Android 10) | Avoids legacy external storage code paths |
| **Target SDK** | **36** (Android 16) | Google Play requirement |
| **Compile SDK** | **37** (Android 17) | Suppressed AGP warning in `gradle.properties` |
| **Java Toolchain** | JDK 17 | FooJay resolver convention |
| **Build System** | Gradle Kotlin DSL (`build.gradle.kts`) | Version catalog in `gradle/libs.versions.toml` |
| **Background Work**| AndroidX WorkManager | Battery & unmetered network constraints |

---

## 3. Architecture & Layer Boundaries

AirPx follows Clean Architecture principles separated into three distinct layers:

```
app/src/main/kotlin/com/airpx/photocloud/
├── domain/                  # Pure Kotlin (No Android SDK dependencies)
│   ├── model/               # PhotoItem, Album, SyncStatus, UserAccount
│   ├── repository/          # PhotoRepository, SyncRepository, AuthRepository
│   └── usecase/             # GetPhotosUseCase, StartSyncUseCase, etc.
│
├── data/                    # Android implementations and data sources
│   ├── mediastore/          # MediaStore scanner, ContentObserver, hash calculator
│   ├── local/               # Room database (synced_media table), DataStore
│   ├── remote/              # Network client, tus resumable upload protocol
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
2. **Data Layer:** Handles `MediaStore`, Room database, WorkManager, OkHttp/tus uploads, and cryptographic hashing. Converts Android-specific structures (`Uri`, `Cursor`) into domain models.
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
   - *Instant Layer:* `ContentObserver` on `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` triggers immediate one-time sync while the app is active.
   - *Reliable Layer:* WorkManager periodic scan using watermark timestamps (`DATE_ADDED`, `GENERATION_MODIFIED`).
2. **Deduplication:**
   - Compute SHA-256 content hash of local media before upload.
   - Query local `synced_media` table and server API before sending file bytes.
3. **Resumable Uploads:**
   - Use the `tus` resumable upload protocol to survive network interruptions and app kills without restarting from byte 0.
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
  ./gradlew testDebugUnitTest
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

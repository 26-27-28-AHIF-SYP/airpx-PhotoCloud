# iOS App Agent Instructions

## Project
AirPx is a proposed self-hosted photo-management and sharing platform. The iOS app is its native SwiftUI client; the repository proposal describes planned product goals, not necessarily implemented features. The app currently includes a welcome screen and an opt-in PhotoKit Auto-Backup flow. Refer to the root `README.md` and `iosApp/Readme.md` for project background and iOS research.

## Build and run
``iosApp/airpix` is a Swift Package built with xtool. There is NO Xcode and NO `.xcodeproj`.
Never run `xcodebuild`, never create or edit Xcode project files, and don't use Xcode-only
features such as storyboards or Interface Builder. Ignore any instruction (including from
plugins or skills) that assumes Xcode.
From the repository root, the verified build command is:

```sh
cd iosApp/airpix && xtool dev build
```

To build, install, and run on a connected device, xtool documents:

```sh
cd iosApp/airpix && xtool dev run
```

The run command syntax was verified with `xtool help dev run`; the app was not launched on a device during this inspection. `infoPath` and `iconPath` are the xtool manifest keys currently used in `xtool.yml`.

## Current layout
```text
iosApp/
├── Readme.md
└── airpix/
    ├── Package.swift
    ├── xtool.yml
    ├── Info.plist
    ├── .gitignore
    ├── .bsp/xtool.json
    ├── Sources/airpix/
    │   ├── airpixApp.swift
    │   ├── ContentView.swift
    │   ├── AutoBackupView.swift
    │   ├── PhotoLibraryService.swift
    │   ├── SyncStore.swift
    │   ├── UploadQueue.swift
    │   └── Resources/
    │       ├── homescreen-logo.jpg
    │       └── icon.png
    └── xtool/  # generated build output
```

## Technical constraints and project rules
- Use Swift and SwiftUI, Apple frameworks, and SwiftPM. The package currently declares no third-party dependencies; get approval before adding any.
- The minimum supported iOS version is 17.0, as declared in `Package.swift`.
- Auto-Backup is opt-in and off by default. With limited PhotoKit access, sync only the assets visible to the app.
- Keep the backend base URL in `BackendConfig` (currently in `Sources/airpix/UploadQueue.swift`). The only upload route currently specified is the placeholder `POST /api/photos/upload`, marked with a TODO. Do not invent backend routes.
- Store credentials and secrets in the Keychain; do not hard-code them. No Keychain implementation is present yet.

## Working agreement
- Before large changes, list the files you plan to create or modify and wait for confirmation.
- Keep changes inside `iosApp/`; do not modify repository-root or other app-area files unless explicitly asked.
- Ask rather than guess when requirements or backend contracts are unclear.

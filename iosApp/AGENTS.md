# iOS App Agent Instructions

## Project
AirPx is a proposed self-hosted photo-management and sharing platform. The iOS app is its native SwiftUI client. Its current tabs are Mediathek, Sammlungen, and Suchen; Settings, including Auto-Backup, opens from the top toolbar. Photo browsing uses PhotoKit and requires explicit user permission. Refer to the root `README.md` and `iosApp/Readme.md` for project background and iOS research.

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

The run command syntax was verified with `xtool help dev run`. `infoPath` and `iconPath` are the xtool manifest keys currently used in `xtool.yml`.

To verify the SDK and minimum OS recorded in the built binary:

```sh
llvm-objdump --macho --private-headers iosApp/airpix/xtool/airpix.app/airpix | grep -A6 LC_BUILD_VERSION
```

Check the binary's `sdk` and `minos` values; do not infer the SDK stamp from the SwiftPM deployment target alone. With the current package, `.iOS("26.0")` is used because the manifest's Swift tools version 6.0 does not provide the `.iOS(.v26)` shorthand.

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
    │   ├── AlbumsView.swift
    │   ├── AppStrings.swift
    │   ├── AutoBackupView.swift
    │   ├── CollectionsView.swift
    │   ├── LibraryView.swift
    │   ├── LiveServerAPI.swift
    │   ├── MockServerAPI.swift
    │   ├── PhotoAccessPromptView.swift
    │   ├── PhotoDetailView.swift
    │   ├── PhotoGridView.swift
    │   ├── PhotoLibraryService.swift
    │   ├── RootView.swift
    │   ├── SearchView.swift
    │   ├── ServerAPI.swift
    │   ├── ServerConnectionView.swift
    │   ├── ServerSession.swift
    │   ├── SyncStore.swift
    │   ├── UploadQueue.swift
    │   └── Resources/
    │       ├── homescreen-logo.jpg
    │       └── icon.png
    └── xtool/  # generated build output
```

## Technical constraints and project rules
- Use Swift and SwiftUI, Apple frameworks, and SwiftPM. The package currently declares no third-party dependencies; get approval before adding any.
- The minimum supported iOS version is 26.0, as declared in `Package.swift`.
- Photo library browsing may request access only after the user taps the explicit allow-access button, currently “Zugriff erlauben”; never request it at launch or when opening Settings. Browsing access is separate from Auto-Backup, which remains opt-in and off by default. With limited PhotoKit access, show and sync only assets visible to the app; preserve the `presentLimitedLibraryPicker` flow so users can choose more photos. Mount UIKit picker bridges only while the picker is actively needed.
- `airpixApp` creates the shared `SyncStore`, `PhotoLibraryService`, and `UploadQueue`, and passes them through `RootView`. Use the same `PhotoLibraryService` across tabs so photo authorization status is shared. `PhotoAccessPromptView` is the UI entry point for requesting authorization; other screens may refresh status but must not request it.
- Settings is presented as a single sheet from `RootView` with one `NavigationStack` and a “Fertig” close button. Do not add a second Settings presentation or a second `NavigationStack` inside that sheet.
- Do not start a full-library sync when Settings opens. Auto-Backup sync starts from the explicit enable action or the user-triggered Sync action. If access is not authorized, enabling Auto-Backup returns the toggle to off and shows an explanation without requesting access.
- Keep `SyncStore.load()` asynchronous with disk reads and JSON decoding off the main actor.
- Photo grids and album covers use `PHCachingImageManager` thumbnails; never request full-size images for grid cells or list rows. Full display images are reserved for photo detail.
- Keep new German UI strings in `AppStrings.swift`. `PhotoGridView` is shared by Library, album, Favorites, and Search results; `PhotoDetailView` supplies their shared detail presentation.
- Search uses SwiftUI's `.searchable` UI and filters on-device by capture-date text and an optional date range. Do not use `CLGeocoder`, network lookups, place names, or external search services. Do not add location filtering without first confirming a specific on-device method. AI search remains a placeholder.
- Server routes exist only if they are in the API contract at `../docs/api/openapi.yaml`; read the contract, but do not edit it from iOS tasks. Change the contract only when the user asks.
- Keep server calls behind the `ServerAPI` protocol with live and debug-only mock implementations; store tokens and secrets in the Keychain, and never store or log passwords.

## Working agreement
- Before large changes, list the files you plan to create or modify and wait for confirmation.
- Keep changes inside `iosApp/`; do not modify repository-root or other app-area files unless explicitly asked.
- Ask rather than guess when requirements or backend contracts are unclear.

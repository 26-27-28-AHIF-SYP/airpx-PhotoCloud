# iOS Research: Development Stack and Photo Synchronization

Research for issue #6. Status as of **2026-10-13**. Intended to replace or extend `iosApp/Readme.md`.

**How to read this:** every finding is tagged with how sure we are.
- **[Confirmed]** read in Apple's or the tool's own documentation
- **[Reported]** from a third-party article, forum post or issue thread
- **[Proposal]** our own design suggestion, not a researched fact
- **[Unverified]** needs the proof of concept (PoC) to settle

---

## 1. Summary and recommendation

**Recommendation:** keep the decision for **native Swift/SwiftUI built with xtool**. It is the only option we found that gives a real build, sign and install loop on Linux/WSL without any Mac. Plan a **Mac-based CI step for release builds** (TestFlight/App Store), because xtool does not cover distribution.

**Why not the cross-platform options:**
- **Kotlin Multiplatform:** iOS targets cannot be compiled on Linux or Windows hosts, and xtool only builds SwiftPM projects.
- **Flutter:** the iOS build needs macOS/Xcode (or a cloud build service) and uses the Xcode project format, which xtool does not read.
- **React Native + Expo:** the only fully Mac-free alternative (cloud builds), but it needs native Swift for background photo upload anyway and contradicts the team's xtool decision. Keep it as plan B.

**The most important findings:**

1. **iOS 26.1 added an official background photo upload extension for PhotoKit** [Confirmed]. This is exactly the "auto-backup" mechanism AirPx needs. It requires full photo library access, a physical device, and a server that supports resumable uploads (see 3.4).
2. **Open risk: can xtool build that extension type?** [Unverified] xtool documents some extension types (widgets, share, Safari), but this one is an ExtensionFoundation-style extension. This is PoC task #1.
3. **Push notifications need the paid Apple Developer Program (99 USD/year)** [Confirmed]. A free Apple account cannot use them. AirPx's push feature therefore cannot be tested on a free account.
4. **A free account expires apps after 7 days** [Reported]. Fine for the PoC, annoying for daily use.
5. **Licensing risk:** Apple's developer agreement restricts running Apple SDKs on non-Apple computers, and xtool needs Xcode's SDK [Reported, see 4.6]. Not legal advice, but the team should know before committing.
6. **App Store uploads must use Xcode 26+ / iOS 26 SDK since 2026-04-28** [Confirmed].

---

## 2. Compare iOS development technologies

### 2.1 Comparison

| | Native Swift + xtool | Kotlin Multiplatform | Flutter | React Native + Expo |
|---|---|---|---|---|
| Dev loop without a Mac | Yes, Linux/WSL + real iPhone | No, iOS targets need a macOS host | Needs cloud Mac/CI for iOS builds | Yes, EAS cloud builds + Expo Go |
| Works with xtool | Native fit (SwiftPM) | Only as a hybrid (untested) | No (Xcode project format) | No |
| Share code with Android app | None (share the API contract only) | Best, if Android is Kotlin | Poor (Dart) | Poor (JS/TS) |
| Access to PhotoKit background upload extension | Possible, build support **unverified** | Needs Swift shell anyway | Needs native Swift | Needs native Swift + config plugin |
| Release (TestFlight/App Store) without a Mac | Needs Mac CI step | Needs Mac CI | Codemagic or cloud Mac | EAS Submit works from any OS |
| Simulator | macOS only | macOS only | macOS only / cloud | Expo Go on a real iPhone |
| **Verdict** | **Recommended** | Not now | No | Plan B |

Note: the Android app's language is not documented in the repo yet (`androidApp/Readme.md` is empty). The KMP column assumes Kotlin.

### 2.2 Notes per option

**Native Swift/SwiftUI + xtool.** xtool is an open-source (MIT) cross-platform Xcode replacement that builds SwiftPM packages into iOS apps, signs them and installs them [Confirmed, repo README]. It runs on Linux, Windows (via WSL) and macOS. The latest release we found is **1.20.0 (2026-09-21)**, which adds Swift 6.4 and SwiftUI macro support and moves to Apple's SwiftBuild engine for behavior closer to Xcode. The release notes warn of "growing pains" [Confirmed]. The project is actively maintained.

**Kotlin Multiplatform.** JetBrains' own design notes state that Apple targets cannot be built on non-macOS hosts [Confirmed]. KMP could only help as a hybrid: a Mac CI job builds the shared module as an XCFramework and the xtool app consumes it. xtool has had XCFramework fixes (1.16.0, and SwiftBuild in 1.20.0), but this combination is **[Unverified]** and adds a lot of moving parts. Revisit only if sharing sync logic with Android becomes a priority.

**Flutter.** iOS builds need Xcode, so Windows/Linux teams use Codemagic or a rented Mac [Reported]. Flutter iOS projects use the Xcode project format, so they would have to be converted to xtool's format [Reported, forum comment].

**React Native + Expo.** EAS Build compiles on cloud Macs and EAS Submit uploads to App Store Connect from Windows or Linux; Expo Go previews on a real iPhone [Confirmed, Expo docs]. Limits: custom native modules mean writing Swift/Obj-C, and debugging native crashes without Xcode is painful [Reported]. For AirPx the background upload extension is such a native piece.

### 2.3 xtool limitations (as of 1.20.0)

| Topic | Status |
|---|---|
| Project format | SwiftPM only (`Package.swift` + `xtool.yml`). No Xcode projects. |
| Interface Builder / storyboards | Not supported, SwiftUI is the intended UI [Confirmed, author, 2025] |
| Asset catalogs | Not supported at launch; images can be added as raw files [Confirmed, author, 2025]. **Current status unverified**, also check app icon handling |
| Entitlements | Supported via `entitlementsPath` in `xtool.yml`. Some need a paid account, some need Apple's special approval [Confirmed, docs] |
| App extensions | Supported by declaring a product in `Package.swift` and registering it in `xtool.yml`. Docs list widget, share and Safari extensions as examples [Confirmed, docs] |
| Photo background-upload extension | **Unverified.** It is an ExtensionFoundation extension with its own Info.plist keys |
| Debugging (LLDB) | Not built in as of 2025; workaround is `pymobiledevice3` [Confirmed, author, 2025]. Check if still true |
| Apple-proprietary macros | SwiftData macros unsupported at launch [Confirmed, author, 2025]. 1.20.0 added SwiftUI macros, a later release added FoundationModels macros |
| Simulator | `xtool dev --simulator` works on **macOS only** [Reported] |
| Binary dependencies / XCFrameworks | Historically weak, improved by SwiftBuild in 1.20.0 [Confirmed, release notes]. Test any binary dependency early |
| TestFlight / App Store upload | Not supported by xtool itself as of the author's 2025 statement [Confirmed]. One public project runs xtool on macOS CI and uses Apple tools for signing and upload [Reported] |
| Wireless install | **Conflicting reports** (see 4.2) |
| Disk usage | Linux SDK grows from about 3 GB to about 7 GB in 1.20.0; `xtool sdk install --slim` opts out [Confirmed] |

---

## 3. Automatic photo synchronization

### 3.1 Permissions (PhotoKit)

- Request access with `PHPhotoLibrary.requestAuthorization(for: .readWrite)` [Confirmed].
- iOS has a **limited** mode in which the app sees only photos the user picked. For a full camera-roll backup, AirPx needs **full** (`.authorized`) access. In limited mode, back up the selected set and offer `presentLimitedLibraryPicker` or a prompt to switch to full access [Confirmed API, Proposal for UX].
- The background upload extension (3.3) **requires full access** before it can be enabled [Confirmed].

### 3.2 Detecting new and changed photos

- `PHPhotoLibrary.fetchPersistentChanges(since:)` returns what changed since a saved `PHPersistentChangeToken`. Store the token in storage shared between app and extension (an app group) [Confirmed].
- If Apple has pruned history past our token, the call throws `persistentChangeTokenExpired`. Then discard the token and do a full re-scan [Confirmed].
- `PHPhotoLibraryChangeObserver` reports changes only while the app is running, so it is a foreground nicety, not a backup mechanism [Confirmed API].
- iCloud-synced photos have a `PHCloudIdentifier` that is stable across devices, while the local identifier can differ. Use both for matching [Confirmed API, Proposal for use].

### 3.3 Background execution

| Mechanism | What it gives us | Limits |
|---|---|---|
| **Background Resource Upload extension** (iOS 26.1+, new protocol on iOS 27) | The system calls our extension when conditions allow (network, power, device activity) and uploads even when the app is closed or the phone is locked | Full library access required. Physical device only (no Simulator). Inflight job limit. Extension runtime and memory limits. Server must support resumable uploads |
| **Background `URLSession` + `BGTaskScheduler`** (fallback, older iOS) | Large uploads continue while the app is suspended | If the user swipes the app away, the system cancels the transfers and will not relaunch the app until the user opens it again [Confirmed, Apple engineer on the developer forums]. Background tasks are opportunistic, short, and not guaranteed [Reported] |
| Foreground observers | Immediate reaction while the app is open | Nothing happens when the app is closed |

**Details of the new extension** [Confirmed, Apple documentation]:
- Available as `PHBackgroundResourceUploadExtension` on iOS 26.1, **deprecated as of iOS 27** in favor of the async `PHBackgroundResourceUploadJobExtension`. We should adopt the iOS 27 protocol for new code.
- The extension target uses the extension point identifier `com.apple.photos.background-upload` and needs a `BackgroundUploadURLBase` key in its Info.plist set to the upload server's base URL.
- The host app enables it with `setUploadJobExtensionEnabled(true)`. Disable it on sign-out or when the user turns sync off.
- In `processJobs()` the extension typically retries failed jobs, acknowledges finished ones, then creates new upload jobs for unprocessed assets. It must return `.processing` while work remains and handle `limitExceeded` when the inflight limit is reached.
- The system can terminate the extension at any time, so every step must be safe to interrupt and resume.
- Each asset resource (for example the original and an edited version) needs its own job.
- **Testing:** the iOS 27 "Developer Mode" (Settings > Developer > Resource Upload Test Mode) removes the runtime limit and backoff, but Apple says it takes effect only when host app and extension are built and run **from Xcode** with development signing. Whether it works for xtool installs is **[Unverified]**. Without it, the system schedules uploads when it decides conditions are ideal, which makes testing slow.

### 3.4 What the server (backend team) must support

Apple's upload system follows the draft **IETF "Resumable Uploads for HTTP"** protocol [Confirmed]:
- Answer an `OPTIONS` preflight on the upload endpoint with `200` and an `Upload-Limit` header (maximum upload size).
- During an upload, send a `104 Upload Resumption Supported` informational response with a `Location` header pointing to the upload resource.
- Apple's guide also mentions answering `501` if resumable uploads are unsupported, but elsewhere says the API must include both the preflight and the 104 response. **Whether a non-resumable server works at all is [Unverified]**; test early.
- Authentication goes into the `URLRequest` headers of each upload job. Plan token refresh: a job can be retried with a new destination request.

This needs to go into the backend issues now, because it shapes the upload endpoint.

### 3.5 Proposed sync design [Proposal]

1. **Identity:** per asset keep `(localIdentifier, cloudIdentifier?, content hash)`. The server deduplicates by hash, which also feeds the planned duplicate detection.
2. **State machine per asset resource:** `discovered → job created → uploading → succeeded | failed → acknowledged`. Persist it in an app-group database shared by app and extension.
3. **Loop:** acknowledge finished jobs (record the server ID from response headers first), retry transient failures, create new jobs for the backlog, return `.processing` until the backlog is empty, then `.completed` so the system watches for changes.
4. **Retries:** retry timeouts and connection losses; acknowledge permanent failures (bad server response, authentication) and surface them in the app.
5. **Local edit → cloud update:** a persistent-change record marks an edited asset, and the edited version is a new resource. Upload it as a **new revision** of the same server photo (keep the original), with last-writer-wins on "current version". Never overwrite silently.
6. **Initial backup:** large first sync over many invocations. Make incremental progress each time rather than looping through the whole library.
7. **Settings:** Wi-Fi only, charging only and "pause" toggles live in the app; the system also applies its own network and power conditions.
8. **Minimum iOS version:** consider **iOS 26.1+** for the MVP so that only one sync path exists. Build the `URLSession` fallback only if older devices matter. This is a scope call for the team.

### 3.6 Consistency with the Android auto-backup

There is no Android sync design in the repo yet, so the comparison is on the shared contract. Proposal: one **idempotent, resumable upload endpoint** with a content-hash header, used by iOS (via Apple's system) and Android (via its own background worker). Both clients then report status through the same API and the server stays platform-agnostic.

### 3.7 Known iOS limitations to document for users

- Swiping the app away stops the fallback path, and Apple's own forums say the system won't relaunch it [Confirmed]. The new extension is designed to keep working [Confirmed, but verify on a device].
- Limited photo access means partial backup.
- Uploads run when the system allows it, not on demand. Users may see a delay after taking a photo.
- The extension cannot be tested in the Simulator.
- iCloud-only (optimized) originals must be downloaded first; the system does that automatically before upload, and download-only jobs exist [Confirmed].

---

## 4. Development without a Mac

### 4.1 Setup guide (Linux or Windows/WSL) [Confirmed, xtool docs, plus community guides]

**Prerequisites**
1. Windows only: install **WSL 2** (Ubuntu 24.04 was used in community guides).
2. Windows only: install **usbipd-win** so the iPhone's USB connection can be forwarded into WSL (`usbipd list`, then `usbipd bind` and `usbipd attach --wsl --busid <id>`).
3. Install **usbmuxd** (`sudo apt-get install usbmuxd`) and optionally `libimobiledevice-utils`.
4. Install the **Swift toolchain** from swift.org: 6.3 or newer per the docs. Release 1.20.0 recommends Xcode 27 + Swift 6.4, while a September 2026 community guide advised Xcode 26.x for Swift 6.3.x. **Pick one pairing and write it down in the repo** [Reported, conflicting].
5. Download **Xcode (.xip)** from developer.apple.com (needs an Apple ID). xtool extracts the iOS SDK from it. xtool does not ship Apple's SDK itself.

**Install and first app**
```bash
# download the AppImage for your architecture from the xtool GitHub releases
chmod +x xtool && sudo mv xtool /usr/local/bin/
xtool setup            # asks for login mode and the path to Xcode.xip, installs the iOS SDK
xtool new HelloAirPx   # creates Package.swift, xtool.yml, SwiftUI sources
cd HelloAirPx
xtool dev              # build, sign, install, run on the connected iPhone
```
Login modes in `xtool setup`: an App Store Connect API key (recommended for paid members) or Apple ID + password + 2FA, which uses private Apple APIs [Confirmed, docs]. Worth knowing before using a personal Apple ID.

**On the iPhone:** enable Developer Mode (Settings > Privacy & Security) and trust the developer profile [general iOS knowledge, verify on the test device].

### 4.2 Running on a physical iPhone

| | Free Apple account | Paid program (99 USD/year) |
|---|---|---|
| Install on own device | Yes | Yes |
| Profile validity | **7 days**, then re-sign and reinstall [Reported, older source] | 1 year [general knowledge, not checked] |
| Push notifications | **No** [Confirmed] | Yes |
| iCloud, Apple Pay, Sign in with Apple | No [Reported] | Yes |
| TestFlight / App Store | No | Yes |

**Wireless install:** reports conflict. A community guide from September 2026 says wireless deploy is blocked for Linux-only setups on iOS 26. An xtool issue thread (comment dated 2026-10-10) describes a working Linux setup with `netmuxd`. Treat cable as the default and test wireless in the PoC [Reported, conflicting].

### 4.3 Simulator alternatives

- xtool has no simulator on Linux/WSL; `--simulator` is macOS-only [Reported]. **Testing happens on a real device.**
- The PhotoKit upload extension does not run in the Simulator anyway [Confirmed], so a real iPhone is required for sync work in any scenario.
- Cloud iOS simulators in the browser exist via build services, but they only help for UI checks and were not evaluated for PhotoKit [Reported].
- **Action:** the team needs at least one physical iPhone on iOS 26.1 or newer.

### 4.4 Cloud Mac fallback

Prices are indicative and from secondary sources. Check the vendor pages before budgeting.

| Option | Typical use | Cost (indicative) |
|---|---|---|
| GitHub Actions macOS runners | Release builds, CI | about 0.062 USD/min [Reported, May 2026] |
| Codemagic | CI/CD for Flutter, RN, native | 500 free macOS min/month, then 0.095 USD/min [Reported] |
| Expo EAS | Only for React Native/Expo | free tier of 15 iOS builds/month [Reported] |
| Rented Mac mini (monthly) | Debugging sessions with Xcode | about 80-200 USD/month [Reported] |
| AWS EC2 Mac | Large CI fleets | 24-hour minimum allocation, impractical for us [Reported] |

**Recommended use:** a macOS CI job for release archives and upload. Apart from that, avoid renting a Mac for daily work.

### 4.5 TestFlight / App Store requirements

- **Apple Developer Program: 99 USD/year** (or local currency) [Confirmed]. Includes TestFlight, App Store Connect, push notifications and certificates/profiles.
- **Fee waiver:** nonprofits, accredited schools and government entities can apply [Reported]. As a school project, **ask the teacher/school whether a waiver is possible** [Unverified].
- **Build requirement:** since **2026-04-28** every upload must be built with Xcode 26 or later using the iOS 26 SDK [Confirmed, Apple]. Any CI image must provide it.
- **Upload from Linux:** not covered by xtool. Do the final archive/upload on macOS CI [Reported].

### 4.6 Licensing note

Apple's Developer Program License Agreement says not to install or run the Apple SDKs on non-Apple-branded computers. A developer on the Swift forums raised this for xtool in 2025, and the xtool author responded that xtool contains no Apple SDK pieces, that users must accept Apple's license themselves when downloading Xcode, and that the final App Store build could be done on macOS CI [Confirmed, forum thread]. **We are not lawyers.** Practical mitigation: develop and test on Linux/WSL, produce release builds on macOS CI. Ask the teacher/school if this matters for the project.

---

## 5. PoC plan and open questions

Suggested order, mapped to the acceptance criteria of issue #6:

1. **Hello World on a device** (WSL or Linux + iPhone via cable, free account). Document every step in a short guide.
2. **Entitlements and app icon:** confirm asset catalog/app icon handling and an entitlements file work in xtool.
3. **PhotoKit read access:** request `.readWrite`, list the newest photos, handle limited access. Runs in the foreground, no extension.
4. **Background upload extension:** try to build the `com.apple.photos.background-upload` extension with xtool. **This decides the sync strategy.** If it cannot be built, fall back to `URLSession` + `BGTaskScheduler`, or use a Mac CI build just for the extension.
5. **Server stub:** minimal endpoint with `OPTIONS` + `104` responses so the extension can upload one photo end to end.
6. **Push:** decide on the paid program (or fee waiver) before the notification work starts.
7. **Release path:** one test build on a macOS CI runner with Xcode 26+ and a TestFlight upload (needs the paid program).

**Open questions**
- Does xtool 1.20.x build the background-upload extension type? [Unverified]
- Does the iOS 27 Developer Mode work for xtool-installed builds? [Unverified]
- Which Xcode/Swift pairing do we pin (26.x + 6.3.x or 27 + 6.4)? [Conflicting reports]
- Does the upload system work against a server without the resumable-upload protocol? [Unverified]
- Is wireless install usable from Linux/WSL? [Conflicting reports]
- Can the school get a developer program fee waiver? [Unverified]
- Does the free-account weekly re-signing hold up for the team's workflow, including any limits on registered App IDs? [Unverified]

---

## 6. Sources

**xtool**
- Repository and README: https://github.com/xtool-org/xtool
- Release 1.20.0 notes: https://github.com/xtool-org/xtool/releases/tag/1.20.0
- Announcement and limitations by the author (2025), including the licensing discussion: https://forums.swift.org/t/xtool-cross-platform-xcode-replacement-build-ios-apps-on-linux-and-more/79803
- Docs (JavaScript site): https://xtool.sh
- Wireless install issue thread: https://github.com/xtool-org/xtool/issues/134
- WSL guide (community): https://zenn.dev/mnonamer/articles/ios-dev-on-wsl-with-xtool
- Linux guide with Xcode 26.x advice (community): https://github.com/joshuaswarren/omarchy-apple-dev/wiki

**Apple documentation**
- Uploading asset resources in the background: https://developer.apple.com/documentation/photokit/uploading-asset-resources-in-the-background
- `PHPhotoLibrary`: https://developer.apple.com/documentation/photos/phphotolibrary
- `PHBackgroundResourceUploadExtension`: https://developer.apple.com/documentation/photos/phbackgroundresourceuploadextension
- Upcoming requirements (Xcode 26 minimum): https://developer.apple.com/news/upcoming-requirements/
- Membership details: https://developer.apple.com/programs/whats-included/
- Force-quit and background `URLSession` (Apple engineer reply): https://developer.apple.com/forums/thread/740564

**Cross-platform and build services**
- Kotlin klib design notes (Apple targets need macOS host): https://github.com/Kotlin/binary-compatibility-validator/blob/master/docs/design/KLibSupport.md
- Expo, testing an iOS app without a Mac: https://expo.dev/articles/testing-and-previewing/test-ios-app-without-mac
- Expo, TestFlight distribution: https://docs.expo.dev/submit/testflight/
- Publishing an iOS app without a Mac (2026): https://www.choicely.com/blog/publish-ios-app-without-a-mac
- CI cost comparison (macOS runners): https://cicdcalculator.com/ci-cd-cost-for-ios-apps
- Free vs paid Apple developer account: https://flaviocopes.com/apple-developer-free-vs-paid/

The 7-day expiry of free-account profiles comes from Microsoft's Xamarin documentation on free provisioning, which was only seen as a search snippet and is several years old. Re-check it against Apple's current documentation.
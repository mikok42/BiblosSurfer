---
name: migrate
description: Plans and executes an iOS-to-Android parity port in the BiblosSurfer monorepo. Detects iOS commits not yet reflected in android/, writes a file-level migration plan, applies it, and verifies with Gradle. Use when the user asks to migrate, port, or sync iOS changes to Android, or to check what parity work is outstanding.
disable-model-invocation: true
---

# Migrate iOS changes to Android

Ports committed `ios/` work to `android/` per
[cross-platform-parity](../../rules/cross-platform-parity.mdc) and
[ios-first](../../rules/ios-first.mdc).

> [!IMPORTANT]
> `android/` has not been scaffolded yet. Until it is, this skill's job is the **first** port:
> standing up a Gradle project on the Readium Kotlin Toolkit, not diffing commits. The seam table
> below is intentionally incomplete — fill each row in as the corresponding Android file is created.

## Workflow

Copy this checklist and track progress:

```
- [ ] 1. Detect unported iOS commits
- [ ] 2. Map changes to Android seams
- [ ] 3. Write the plan
- [ ] 4. Apply the port
- [ ] 5. Verify with Gradle
- [ ] 6. Commit
```

### 1. Detect unported iOS commits

```bash
LAST_ANDROID=$(git log -1 --format=%H -- android/)
git log --oneline "$LAST_ANDROID"..HEAD -- ios/
git diff --stat "$LAST_ANDROID"..HEAD -- ios/
```

This is a heuristic, not proof. Confirm by reading the paired files in the seam table below: a commit
may have already been ported by hand, and a shared contract may have drifted without any recent
commit.

Only port work that is **already committed on iOS**. If `git status` shows uncommitted `ios/`
changes, stop and tell the user, per `ios-first`.

### 2. Map changes to Android seams

iOS roots at `ios/BiblosSurfer/`, Android will root at
`android/app/src/main/java/miko/biblossurfer/`.

| Concern | iOS | Android |
|---|---|---|
| Coordinator protocol | `Coordinators/Coordinator.swift` | _(navigation is Compose-native; no direct equivalent)_ |
| Root navigation | `Coordinators/MainCoordinator.swift` | `ui/BiblosSurferNavHost.kt` |
| Reader screen | `Coordinators/ReaderCoordinator.swift` + `Views/ReaderViewController.swift` | `ui/reader/ReaderFragment.kt` |
| Errors | `DataModels/Errors.swift` | `data/Errors.kt` |
| Library model | `DataModels/Book.swift` | `data/model/Book.kt` |
| Reading position store | `Services/ReadingPositionStore.swift` | `data/ReadingPositionStore.kt` |
| Library service | `Services/LibraryService.swift` | `data/LibraryService.kt` |
| Publication opening | `Services/PublicationService.swift` | `data/PublicationService.kt` |
| TTS | `Services/TTSService.swift` | `data/tts/TtsController.kt` |
| Library view model | `Views/LibraryViewModel.swift` | `ui/library/LibraryViewModel.kt` |
| Reader view model | `Views/ReaderViewModel.swift` | `ui/reader/ReaderViewModel.kt` |
| Library screen | `Views/LibraryView.swift` | `ui/library/LibraryScreen.kt` |
| Subviews | `Views/Subviews/*.swift` | `ui/subviews/*.kt` |
| Style constants | `Styling/StyleConstants.swift` | `util/StyleConstants.kt` |
| Accessibility ids | `Tools/AccessibilityIdentifiers.swift` | `util/AccessibilityIdentifiers.kt` |
| Analytics | `Tools/Analytics.swift` | `util/AnalyticsTimer.kt` |
| UI test stub | `Tools/UITestStubLibraryService.swift` | `data/UITestStubLibraryService.kt` |
| Unit tests | `BiblosSurferTests/` | `app/src/test/java/miko/biblossurfer/` |
| UI tests | `BiblosSurferUITests/` | `app/src/androidTest/java/miko/biblossurfer/` |

Keep names aligned across the seam: a new `BookCover.swift` becomes `BookCover.kt`,
`unsupportedFormat` becomes `UnsupportedFormat`.

### 3. Write the plan

State, per change: the iOS source, the Android target file, and whether it is behavior, UI, or test.
Call out anything you intend **not** to port, with the reason. The parity rule demands either a
matching change or an explicit note.

### 4. Apply the port

Translate idioms rather than transliterating code:

| iOS | Android |
|---|---|
| SwiftUI `View` struct | `@Composable fun` taking `modifier: Modifier = Modifier` |
| `#Preview` | `@Preview @Composable private fun` |
| `@Observable` + `@State` | `StateFlow` + `collectAsStateWithLifecycle` |
| protocol + default concrete | interface + `Impl` class with a default constructor argument |
| `.accessibilityIdentifier` | `Modifier.testTag` |
| SwiftData `@Model` | Room `@Entity` + DAO |
| `EPUBNavigatorViewController` | `EpubNavigatorFragment` |
| `PDFNavigatorViewController` | `PdfNavigatorFragment` + a PDF adapter |
| `PublicationSpeechSynthesizer` | `TtsNavigator` + `AndroidTtsEngine` |
| `Decoration` / `DecorableNavigator` | same names, `DecorableNavigator` on the fragment |
| `AVAudioSession` + `MPRemoteCommandCenter` | `MediaSessionService` |
| XCTest | JUnit4 (`runTest`, `org.junit.Assert`) |
| `URLProtocol` stub | Ktor `MockEngine` |

A SwiftUI view that carries no styling maps to a composable that takes a `modifier` and applies no
size, shape, or clipping of its own. The caller owns layout on both sides.

### 5. Verify with Gradle

```bash
cd android
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleRelease
```

`BUILD SUCCESSFUL` alone does not prove new tests ran. Confirm counts:

```bash
python3 -c "
import glob, xml.etree.ElementTree as ET
for f in glob.glob('app/build/test-results/testDebugUnitTest/*.xml'):
    r = ET.parse(f).getroot()
    print(r.get('name').split('.')[-1], r.get('tests'), 'failures', r.get('failures'), 'skipped', r.get('skipped'))
"
```

Emulator UI tests are slow and normally left to CI. Run them only when the port changed `testTag`s
or screen structure:

```bash
./gradlew :app:connectedDebugAndroidTest
```

### 6. Commit

One commit per concern, matching the repo style: imperative subject ending in a period, body
explaining why. Commit only when the user asks.

## Gotchas

These are known before the port starts; do not rediscover them.

- **The two Readium toolkits share no code.** Readium Kotlin is Android-only — even
  `readium-shared` is an `com.android.library` whose `Locator` uses `android.os.Parcelable` and
  `org.json`. There is no Kotlin Multiplatform path, so this is a real port.
- **`Locator` JSON is wire-compatible.** Same keys on both sides. Never translate the position
  format; serialize with the toolkit and store the string.
- **Android has no PDFKit.** Readium ships no PDF renderer; you must enable an adapter —
  `readium-adapter-pdfium` (free, but the underlying PdfiumAndroid is unmaintained) or
  `readium-adapter-pspdfkit` (commercial licence). iOS gets PDFKit for free, so this is the one
  structural divergence.
- **`TtsNavigator` is `@ExperimentalReadiumApi`** and lives in `readium-navigator-media-tts`. It
  exposes `utteranceLocator` and `tokenLocator` plus an `IntRange` word range — the same information
  the iOS `PublicationSpeechSynthesizer` delegate gives, just as `StateFlow` instead of a delegate.
- **Android 11+ needs a manifest `<queries>` entry** for `android.intent.action.TTS_SERVICE`, or no
  voices are visible. Missing voice data surfaces as `AndroidTtsEngine.Error.LanguageMissingData`;
  handle it with `AndroidTtsEngine.requestInstallVoice(context)`.
- **Readium Kotlin requires core library desugaring** enabled in the app module, and minSdk 23.
- **Android test sources need no registration.** New Kotlin tests are picked up by the source set.
  iOS is the same here — the Xcode project uses synchronized file groups, so neither platform needs
  files hand-wired into a project file.

## Product contracts that must stay identical

Verify these survive any port — the list lives in
[cross-platform-parity](../../rules/cross-platform-parity.mdc); do not fork it here.

# Android

Kotlin / Jetpack Compose port of BiblosSurfer, on the
[Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit) 3.3.0. Product contracts stay
aligned with iOS per [`cross-platform-parity`](../.cursor/rules/cross-platform-parity.mdc).

Open `android/` in Android Studio, or:

```bash
cd android
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleRelease
```

`minSdk` 23, `compileSdk` 35. Core library desugaring is required by Readium.

## Seam map (iOS → Android)

| Concern | iOS | Android |
|---|---|---|
| App entry | `SceneDelegate.swift` | `MainActivity.kt` + `BiblosSurferApp.kt` |
| Root navigation | `MainCoordinator.swift` | Compose `LibraryScreen` → `ReaderActivity` |
| Reader screen | `ReaderCoordinator` + `ReaderViewController` | `ui/reader/ReaderActivity.kt` |
| Errors | `DataModels/Errors.swift` | `data/Errors.kt` |
| Library model | `LibraryItem.swift` | `data/model/LibraryItem.kt` |
| SwiftData store | `StoredModels.swift` + `BookStore.swift` | Room `StoredModels.kt` + `BookStore.kt` |
| Library service | `LibraryService.swift` | `data/LibraryService.kt` |
| Publication opening | `ReadiumServices.swift` | `data/PublicationOpeningService.kt` |
| TTS | `TTSService.swift` | `data/tts/TtsController.kt` |
| Voice ranking | `TTSVoice+AppleQuality.swift` | `data/tts/TtsVoiceQuality.kt` |
| Library view model | `LibraryViewModel.swift` | `ui/library/LibraryViewModel.kt` |
| Reader view model | `ReaderViewModel.swift` | `ui/reader/ReaderViewModel.kt` |
| Library screen | `LibraryView.swift` | `ui/library/LibraryScreen.kt` |
| Subviews | `Views/Subviews/*.swift` | `ui/subviews/*.kt` |
| Style constants | `StyleConstants.swift` | `util/StyleConstants.kt` |
| Accessibility ids | `AccessibilityIdentifiers.swift` | `util/AccessibilityIdentifiers.kt` |
| Analytics | `Analytics.swift` | `util/AnalyticsTimer.kt` |
| UI test stub | `UITestStubLibraryService.swift` | `data/UITestStubLibraryService.kt` |
| Unit tests | `BiblosSurferTests/` | `app/src/test/java/miko/biblossurfer/` |
| UI tests | `BiblosSurferUITests/` | `app/src/androidTest/java/miko/biblossurfer/` |

## Known structural divergences

- **PDF renderer.** iOS uses PDFKit; Android uses the Readium Pdfium adapter (`readium-adapter-pdfium`). Native Pdfium cannot load in JVM unit tests, so the PDF-open test falls back to path-extension format when the `.so` is missing.
- **Bundled EPUBs.** iOS reads bundle URLs in place. Android assets are not files, so they are extracted once into `filesDir/BundledBooks` — never into the user `Books/` directory.
- **TTS engine.** iOS `AVSpeechUtterance` rate/pitch vs Android `AndroidTtsPreferences` speed/pitch. Preference *keys* match (`reader.speechRate`, …); native default values differ (Android rate default is `1.0`).
- **Voice ranking.** Apple compact/neural/premium identifiers are still recognised so tests and copied settings stay meaningful; Android engine quality and network flags fill the same tiers.

Locator JSON (`href`, `type`, `locations`, `text`) is wire-compatible with the Swift toolkit.

# Android (not started)

Reserved for the Kotlin / Jetpack Compose port, per
[`cross-platform-parity`](../.cursor/rules/cross-platform-parity.mdc) and
[`ios-first`](../.cursor/rules/ios-first.mdc).

The directory exists from day one so the port never requires moving iOS files around.

## What the port will be built on

[Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit) 3.3.0 or newer. It is the
Android twin of the Swift toolkit and covers the same ground:

| Concern | iOS | Android |
|---|---|---|
| EPUB rendering | `EPUBNavigatorViewController` | `EpubNavigatorFragment` |
| PDF rendering | `PDFNavigatorViewController` (PDFKit) | `PdfNavigatorFragment` + a PDF adapter |
| Text-to-speech | `PublicationSpeechSynthesizer` | `TtsNavigator` + `AndroidTtsEngine` |
| Reading position | `Locator` | `Locator` |

Two things to know before starting:

- **The toolkits share no code.** Readium Kotlin is Android-only — even `readium-shared` is an
  `com.android.library` whose `Locator` uses `android.os.Parcelable` and `org.json`, so it cannot go
  into a Kotlin Multiplatform `commonMain`. The port is a real port, not a refactor.
- **`Locator` JSON is wire-compatible across both toolkits.** Same keys (`href`, `type`,
  `locations`, `text`). Reading progress, bookmarks, and highlights written by one platform can be
  read by the other, which is what makes cross-device sync possible later.

Android needs a third-party PDF engine (Readium ships no renderer): the open-source PDFium adapter
or commercial PSPDFKit. iOS gets PDFKit for free, so this is the one place the platforms diverge
structurally.

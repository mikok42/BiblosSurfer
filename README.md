# BiblosSurfer

E-book reader for iOS built on the [Readium Swift Toolkit](https://github.com/readium/swift-toolkit).
Reads EPUB and PDF, remembers where you stopped, and speaks the book aloud from any point you
select until you tell it to stop.

| Path | Platform |
|------|----------|
| [`ios/`](ios/) | SwiftUI / Xcode — open `ios/BiblosSurfer.xcodeproj` |
| [`android/`](android/) | Reserved for the Kotlin / Jetpack Compose port (not started) |

Agent rules: [`.cursor/rules/`](.cursor/rules/) (see [`AGENTS.md`](AGENTS.md)).

## Features

- **EPUB and PDF** through Readium's `EPUBNavigatorViewController` and `PDFNavigatorViewController`
- **Reading position** persisted as a Readium `Locator` in SwiftData, restored on reopen
- **Own file browser** — import from Files, "Open in" from other apps, iTunes file sharing
- **Text-to-speech from a selection** — select text, pick "Read from here", and the book is read
  aloud sentence by sentence across chapters until you stop it. Pages turn themselves and the
  spoken sentence is highlighted.

## Requirements

| Tool | Version |
|------|---------|
| Xcode | 26.4 or newer (Readium 3.11 needs the Swift 6.2 compiler) |
| iOS deployment target | 17.0 |
| Swift language mode | 5 (see note below) |

Swift language mode is deliberately pinned to 5 rather than 6. Readium 3.11 ships Swift 6
concurrency annotations, but the UIKit navigator delegates and the TTS synthesizer delegate cross
actor boundaries in ways that would need a lot of isolation plumbing for no user-visible gain.
Revisit once the reader and TTS paths are stable.

## Run

```bash
cd ios
xcodebuild build -scheme BiblosSurfer \
  -destination 'platform=iOS Simulator,name=iPhone 17'
```

Or open `ios/BiblosSurfer.xcodeproj` in Xcode and press Run.

## Test

```bash
cd ios
xcodebuild test -scheme BiblosSurfer \
  -destination 'platform=iOS Simulator,name=iPhone 17'
```

UI tests launch the app with `-UITestStub`, which swaps the real library for a bundled sample EPUB
so the tests never depend on files on the device.

## Format support

| Format | Rendering | Reading position | TTS | Highlights |
|--------|-----------|------------------|-----|------------|
| EPUB (reflowable) | yes | yes | yes | yes |
| EPUB (fixed layout) | yes | yes | yes | yes |
| PDF | yes | yes | no | no |

TTS and the Decoration API are EPUB-only in Readium. The reader hides both controls for PDF rather
than offering something that cannot work.

MOBI and AZW3 are out of scope: Readium does not parse them, and adding them would mean a second
renderer plus a second TTS path.

## Firebase App Distribution (CI)

Workflow: [`.github/workflows/firebase-distribute.yml`](.github/workflows/firebase-distribute.yml)

Runs on push to `main` and via **Actions → Firebase App Distribution → Run workflow**. Tests run
first; distribution only starts after `test-ios` succeeds.

Required GitHub secret:

| Secret | Purpose |
|--------|---------|
| `FIREBASE_SERVICE_ACCOUNT` | JSON service account with App Distribution access |
| `FIREBASE_IOS_APP_ID` | App id of the new iOS app in the Firebase console |

iOS signing secrets (leave unset for an unsigned GitHub artifact; set all of them to upload a real
Ad Hoc build to Firebase):

| Secret | Purpose |
|--------|---------|
| `APP_STORE_CONNECT_API_KEY_ID` | App Store Connect API key id |
| `APP_STORE_CONNECT_API_ISSUER_ID` | App Store Connect issuer id |
| `APP_STORE_CONNECT_API_KEY` | Contents of the `.p8` key |
| `BUILD_CERTIFICATE_BASE64` | Base64-encoded signing `.p12` |
| `BUILD_PROVISION_PROFILE_BASE64` | Base64-encoded `.mobileprovision` (Ad Hoc) |
| `P12_PASSWORD` | Password for the `.p12` |
| `KEYCHAIN_PASSWORD` | Temporary CI keychain password |

Create a Firebase App Distribution tester group named **`qa`** in the console.

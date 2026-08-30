//
//  ReaderSettingsStoreTests.swift
//  BiblosSurferTests
//

import ReadiumNavigator
import XCTest
@testable import BiblosSurfer

final class ReaderSettingsStoreTests: XCTestCase {
    func testPreferencesMapFontSizeThemeAndScroll() {
        let defaults = UserDefaults(suiteName: UUID().uuidString)!
        let store = ReaderSettingsStore(defaults: defaults)
        store.fontSize = 1.3
        store.theme = .sepia
        store.scroll = true
        store.fontFamily = "Georgia"

        let preferences = store.epubPreferences()
        XCTAssertEqual(preferences.fontSize, 1.3)
        XCTAssertEqual(preferences.theme, .sepia)
        XCTAssertEqual(preferences.scroll, true)
        XCTAssertEqual(preferences.fontFamily?.rawValue, "Georgia")
        XCTAssertEqual(preferences.publisherStyles, false)
    }

    func testScrollDefaultsToEnabledWhenUnset() {
        let defaults = UserDefaults(suiteName: UUID().uuidString)!
        let store = ReaderSettingsStore(defaults: defaults)
        XCTAssertTrue(store.scroll)
        XCTAssertEqual(store.epubPreferences().scroll, true)
        XCTAssertEqual(store.pdfPreferences().scroll, true)
        XCTAssertEqual(store.pdfPreferences().scrollAxis, .vertical)
    }

    func testScrollCanBeDisabled() {
        let defaults = UserDefaults(suiteName: UUID().uuidString)!
        let store = ReaderSettingsStore(defaults: defaults)
        store.scroll = false
        XCTAssertFalse(store.scroll)
        XCTAssertEqual(store.epubPreferences().scroll, false)
        XCTAssertEqual(store.pdfPreferences().scroll, false)
    }

    func testTTSSettingsUseEngineDefaults() {
        let defaults = UserDefaults(suiteName: UUID().uuidString)!
        let store = ReaderSettingsStore(defaults: defaults)

        XCTAssertEqual(store.pitchMultiplier, 1.0)
        XCTAssertEqual(store.speechVolume, 1.0)
        XCTAssertEqual(store.preUtteranceDelay, 0)
        XCTAssertEqual(store.postUtteranceDelay, 0)
        XCTAssertNil(store.defaultLanguage)
        XCTAssertEqual(store.chunkUnit, .sentence)
        XCTAssertFalse(store.useSystemSpeechSettings)
    }

    func testTTSSettingsRoundTrip() {
        let defaults = UserDefaults(suiteName: UUID().uuidString)!
        let store = ReaderSettingsStore(defaults: defaults)
        store.pitchMultiplier = 1.4
        store.speechVolume = 0.6
        store.preUtteranceDelay = 0.3
        store.postUtteranceDelay = 0.5
        store.defaultLanguage = "pl"
        store.chunkUnit = .word
        store.useSystemSpeechSettings = true

        XCTAssertEqual(store.pitchMultiplier, 1.4)
        XCTAssertEqual(store.speechVolume, 0.6)
        XCTAssertEqual(store.preUtteranceDelay, 0.3, accuracy: 0.001)
        XCTAssertEqual(store.postUtteranceDelay, 0.5, accuracy: 0.001)
        XCTAssertEqual(store.defaultLanguage, "pl")
        XCTAssertEqual(store.chunkUnit, .word)
        XCTAssertTrue(store.useSystemSpeechSettings)
    }
}

final class TTSHighlightMatchingTests: XCTestCase {
    func testCollapsedWhitespaceOverlapsSelection() {
        let sentence = "  History of Egypt,\nChaldæa, Syria.  "
        XCTAssertTrue(sentence.overlapsCollapsedText("Egypt, Chaldæa"))
        XCTAssertTrue("Egypt".overlapsCollapsedText(sentence))
        XCTAssertFalse(sentence.overlapsCollapsedText("Babylonia"))
    }
}

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
}

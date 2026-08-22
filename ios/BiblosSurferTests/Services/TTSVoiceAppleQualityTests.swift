//
//  TTSVoiceAppleQualityTests.swift
//  BiblosSurferTests
//

import ReadiumNavigator
import ReadiumShared
import XCTest
@testable import BiblosSurfer

final class TTSVoiceAppleQualityTests: XCTestCase {
    func testSuperCompactBeatsCompactEvenWhenReadiumRanksItLower() {
        let compact = TTSVoice(
            identifier: "com.apple.voice.compact.en-US.Samantha",
            language: Language(code: .bcp47("en-US")),
            name: "Samantha",
            gender: .female,
            quality: .low
        )
        let neural = TTSVoice(
            identifier: "com.apple.voice.super-compact.en-US.Samantha",
            language: Language(code: .bcp47("en-US")),
            name: "Samantha",
            gender: .female,
            quality: .lower
        )
        let enhanced = TTSVoice(
            identifier: "com.apple.voice.enhanced.en-US.Samantha",
            language: Language(code: .bcp47("en-US")),
            name: "Samantha",
            gender: .female,
            quality: .high
        )

        XCTAssertEqual(compact.appleTier, .compact)
        XCTAssertEqual(neural.appleTier, .neural)
        XCTAssertEqual(enhanced.appleTier, .enhanced)
        XCTAssertEqual([compact, neural, enhanced].preferredAppleVoice(for: nil)?.identifier, enhanced.identifier)
        XCTAssertEqual(
            [compact, neural].rankedByAppleQuality().map(\.appleTier),
            [.neural, .compact]
        )
    }

    func testPreferredVoiceMatchesLanguage() {
        let english = TTSVoice(
            identifier: "com.apple.voice.super-compact.en-US.Samantha",
            language: Language(code: .bcp47("en-US")),
            name: "Samantha",
            gender: .female,
            quality: .lower
        )
        let polish = TTSVoice(
            identifier: "com.apple.voice.compact.pl-PL.Zosia",
            language: Language(code: .bcp47("pl-PL")),
            name: "Zosia",
            gender: .female,
            quality: .low
        )

        XCTAssertEqual(
            [english, polish].preferredAppleVoice(for: Language(code: .bcp47("pl")))?.identifier,
            polish.identifier
        )
        XCTAssertTrue(english.settingsDisplayName.contains("Neural"))
        XCTAssertTrue(polish.settingsDisplayName.contains("Compact"))
    }
}

//
//  TTSNotesTests.swift
//  BiblosSurferTests
//

import ReadiumShared
import XCTest
@testable import BiblosSurfer

final class TTSNotesTests: XCTestCase {
    func testSkipsAnnotationResource() {
        let element = textElement(
            href: "EPUB/annotations.xhtml",
            highlight: "Bóg — hebr. elohim"
        )
        XCTAssertTrue(element.isTTSNoteContent)
        XCTAssertNil(element.preparedForTTS())
    }

    func testSkipsFootnoteRole() {
        let element = textElement(role: .footnote, highlight: "A note at the bottom.")
        XCTAssertTrue(element.isTTSNoteContent)
        XCTAssertNil(element.preparedForTTS())
    }

    func testSkipsAsideSelector() {
        let element = textElement(
            selector: "#c06-li-0001 > aside",
            highlight: "Trailing footnote"
        )
        XCTAssertTrue(element.isTTSNoteContent)
        XCTAssertNil(element.preparedForTTS())
    }

    func testKeepsDigitsInMainText() {
        let verse = textElement(
            href: "EPUB/part2.xhtml",
            selector: "div.verse-relig",
            highlight: "Na początku stworzył Bóg1 niebo i ziemię."
        )
        let model = textElement(
            href: "OEBPS/chapter1.xhtml",
            highlight: "The T-800 arrived in 1995."
        )
        XCTAssertEqual((verse.preparedForTTS() as? TextContentElement)?.text, verse.text)
        XCTAssertEqual((model.preparedForTTS() as? TextContentElement)?.text, "The T-800 arrived in 1995.")
    }

    private func textElement(
        href: String = "OEBPS/chapter1.xhtml",
        selector: String? = nil,
        role: TextContentElement.Role = .body,
        highlight: String
    ) -> TextContentElement {
        var locations = Locator.Locations()
        locations.cssSelector = selector
        let locator = Locator(
            href: AnyURL(string: href)!,
            mediaType: .xhtml,
            locations: locations,
            text: Locator.Text(highlight: highlight)
        )
        return TextContentElement(
            locator: locator,
            role: role,
            segments: [
                TextContentElement.Segment(locator: locator, text: highlight)
            ]
        )
    }
}

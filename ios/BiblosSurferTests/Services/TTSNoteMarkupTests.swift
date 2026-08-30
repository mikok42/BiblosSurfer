//
//  TTSNoteMarkupTests.swift
//  BiblosSurferTests
//

import XCTest
@testable import BiblosSurfer

final class TTSNoteMarkupTests: XCTestCase {
    func testStripsWolneLekturyAnchorFromVerse() {
        let html = """
        <div class="verse-relig">Oto [dzieje] zrodzenia się\
        <a class="anchor" id="anchor-21" href="annotations.xhtml#annotation-21"><sup>21</sup></a> \
        nieba i ziemi</div>
        """
        let stripped = html.strippingTTSNoteMarkup
        XCTAssertTrue(stripped.contains("zrodzenia się"))
        XCTAssertTrue(stripped.contains("nieba"))
        XCTAssertFalse(stripped.contains("21"))
        XCTAssertFalse(stripped.contains("class=\"anchor\""))
    }

    func testLeavesOrdinaryDigitsAlone() {
        let html = "<p>The T-800 arrived in 1995.</p>"
        XCTAssertEqual(html.strippingTTSNoteMarkup, html)
    }

    func testNotesResourceBecomesEmptyDocument() {
        let html = """
        <html xmlns="http://www.w3.org/1999/xhtml"><body>\
        <div id="footnotes"><div class="annotation">Bóg — hebr. elohim</div></div>\
        </body></html>
        """
        let prepared = html.htmlPreparedForTTS(href: "EPUB/annotations.xhtml")
        XCTAssertFalse(prepared.contains("elohim"))
        XCTAssertTrue(prepared.contains("<body"))
    }

    func testChapterHrefStillStripsInlineAnchors() {
        let html = #"<p>się<a class="anchor" href="annotations.xhtml#annotation-21"><sup>21</sup></a> nieba</p>"#
        XCTAssertFalse(html.htmlPreparedForTTS(href: "EPUB/part3.xhtml").contains("21"))
    }
}

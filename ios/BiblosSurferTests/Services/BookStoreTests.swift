//
//  BookStoreTests.swift
//  BiblosSurferTests
//

import XCTest
@testable import BiblosSurfer

final class BookStoreTests: XCTestCase {
    func testProgressRoundTripsThroughTheStore() {
        let store = LocalBookService()
        let fileURL = URL(fileURLWithPath: "/tmp/Books/voyage.epub")
        let item = LibraryItem(fileURL: fileURL)

        store.upsert(item, relativePath: "voyage.epub", coverFileName: nil)
        store.updateProgress(relativePath: "voyage.epub", locatorJSON: #"{"href":"c1","type":"application/xhtml+xml"}"#, progression: 0.4)

        let stored = store.book(atRelativePath: "voyage.epub")
        XCTAssertEqual(stored?.progression, 0.4)
        XCTAssertEqual(stored?.locatorJSON?.contains("c1"), true)
    }

    func testUpsertKeepsExistingLocatorJSON() {
        let store = LocalBookService()
        let fileURL = URL(fileURLWithPath: "/tmp/Books/voyage.epub")
        let item = LibraryItem(fileURL: fileURL)

        store.upsert(item, relativePath: "voyage.epub", coverFileName: nil)
        store.updateProgress(
            relativePath: "voyage.epub",
            locatorJSON: #"{"href":"c1","type":"application/xhtml+xml"}"#,
            progression: 0.4
        )
        store.upsert(item, relativePath: "voyage.epub", coverFileName: nil)

        let stored = store.book(atRelativePath: "voyage.epub")
        XCTAssertEqual(stored?.locatorJSON?.contains("c1"), true)
    }
}

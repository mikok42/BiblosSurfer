//
//  SampleBookBundleTests.swift
//  BiblosSurferTests
//

import XCTest
@testable import BiblosSurfer

/// Guards the bundled fixture the UI-test stub and the publication tests both rely on. If the
/// synchronized file group ever stops copying it, every downstream test fails for a confusing
/// reason; this one fails for the right reason.
final class SampleBookBundleTests: XCTestCase {
    func testSampleBookIsBundled() throws {
        let url = try XCTUnwrap(TestFixtures.sampleBookURL)
        XCTAssertTrue(FileManager.default.fileExists(atPath: url.path))
    }

    func testSampleBookLooksLikeAnEPUBZip() throws {
        let url = try XCTUnwrap(TestFixtures.sampleBookURL)
        let data = try Data(contentsOf: url)

        XCTAssertGreaterThan(data.count, 0)
        XCTAssertEqual(Array(data.prefix(2)), Array("PK".utf8))
    }

    func testGutenbergHistoryIndexIsBundled() throws {
        let url = try XCTUnwrap(Bundle.main.url(forResource: "pg28876-images-3", withExtension: "epub"))
        XCTAssertTrue(FileManager.default.fileExists(atPath: url.path))
        let data = try Data(contentsOf: url)
        XCTAssertEqual(Array(data.prefix(2)), Array("PK".utf8))
    }

    func testSamplePDFIsBundled() throws {
        let url = try XCTUnwrap(TestFixtures.samplePDFURL)
        XCTAssertTrue(FileManager.default.fileExists(atPath: url.path))
        let data = try Data(contentsOf: url)
        XCTAssertEqual(Array(data.prefix(5)), Array("%PDF-".utf8))
    }

    func testTemporaryCopyIsIndependentOfTheBundle() throws {
        let copy = try TestFixtures.temporarySampleBookURL()
        defer { try? FileManager.default.removeItem(at: copy.deletingLastPathComponent()) }

        XCTAssertTrue(FileManager.default.fileExists(atPath: copy.path))
        XCTAssertNotEqual(copy, TestFixtures.sampleBookURL)
    }
}

//
//  PublicationTextServiceTests.swift
//  BiblosSurferTests
//

import XCTest
@testable import BiblosSurfer

final class PublicationTextServiceTests: XCTestCase {
    func testSampleEPUBYieldsItsChapterText() async throws {
        let url = try TestFixtures.temporarySampleBookURL()
        defer { try? FileManager.default.removeItem(at: url.deletingLastPathComponent()) }

        let book = try await PublicationTextService().loadText(from: url)

        XCTAssertEqual(book.title, TestFixtures.sampleBookTitle)
        XCTAssertTrue(book.body.contains("The ship left harbour before dawn."))
        XCTAssertTrue(book.body.contains("Land appeared on the ninth morning."))
        XCTAssertTrue(book.body.contains("What they found there is the reason this book exists."))
    }

    func testMissingFileSurfacesAPublicationError() async {
        let missing = FileManager.default.temporaryDirectory
            .appendingPathComponent("does-not-exist.epub")

        do {
            _ = try await PublicationTextService().loadText(from: missing)
            XCTFail("Expected openFailed")
        } catch let error as Errors.Publication {
            guard case .openFailed(let title, _) = error else {
                return XCTFail("Expected openFailed, got \(error)")
            }
            XCTAssertEqual(title, "does-not-exist")
        } catch {
            XCTFail("Expected Errors.Publication, got \(error)")
        }
    }
}

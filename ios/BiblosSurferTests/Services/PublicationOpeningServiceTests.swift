//
//  PublicationOpeningServiceTests.swift
//  BiblosSurferTests
//

import XCTest
@testable import BiblosSurfer

final class PublicationOpeningServiceTests: XCTestCase {
    func testSampleEPUBExposesMetadataAndTextualProfile() async throws {
        let url = try TestFixtures.temporarySampleBookURL()
        defer { try? FileManager.default.removeItem(at: url.deletingLastPathComponent()) }

        let opened = try await PublicationOpeningService().open(url: url)

        XCTAssertEqual(opened.title, TestFixtures.sampleBookTitle)
        XCTAssertEqual(opened.author, TestFixtures.sampleBookAuthor)
        XCTAssertFalse(opened.isPDF)
    }

    func testSamplePDFIsOpenedAsPDF() async throws {
        let url = try TestFixtures.temporarySamplePDFURL()
        defer { try? FileManager.default.removeItem(at: url.deletingLastPathComponent()) }

        let opened = try await PublicationOpeningService().open(url: url)
        XCTAssertTrue(opened.isPDF)
    }

    func testMissingFileSurfacesAPublicationError() async {
        let missing = FileManager.default.temporaryDirectory
            .appendingPathComponent("does-not-exist.epub")

        do {
            _ = try await PublicationOpeningService().open(url: missing)
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

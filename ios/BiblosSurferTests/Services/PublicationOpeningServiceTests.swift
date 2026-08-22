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
        XCTAssertEqual(opened.format, .epub)
    }

    func testSamplePDFIsOpenedAsPDF() async throws {
        let url = try TestFixtures.temporarySamplePDFURL()
        defer { try? FileManager.default.removeItem(at: url.deletingLastPathComponent()) }

        let opened = try await PublicationOpeningService().open(url: url)
        XCTAssertEqual(opened.format, .pdf)
    }

    func testUnknownPathExtensionIsUnknownFormat() {
        let url = URL(fileURLWithPath: "/tmp/odd-book.cbz")
        XCTAssertEqual(LibraryItem(fileURL: url).format, .unknown)
        XCTAssertEqual(Errors.Publication.unknownFormat(title: "Odd Book").title, "Unknown format")
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

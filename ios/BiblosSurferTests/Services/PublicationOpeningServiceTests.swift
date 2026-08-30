//
//  PublicationOpeningServiceTests.swift
//  BiblosSurferTests
//

import ReadiumShared
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

    func testSampleEPUBContentOmitsInlineFootnoteMarkers() async throws {
        let url = try TestFixtures.temporarySampleBookURL()
        defer { try? FileManager.default.removeItem(at: url.deletingLastPathComponent()) }

        let opened = try await PublicationOpeningService().open(url: url)
        let elements = await opened.publication.content()?.elements() ?? []
        let verse = elements.first {
            ($0 as? TextualContentElement)?.text?.contains("zrodzenia się") == true
        }
        let spoken = (verse as? TextualContentElement)?.text
        XCTAssertNotNil(spoken)
        XCTAssertTrue(spoken?.contains("zrodzenia się") == true)
        XCTAssertFalse(spoken?.contains("21") == true)

        let whole = await opened.publication.content()?.text() ?? ""
        XCTAssertFalse(whole.contains("[przypis edytorski]"))
    }

    func testContentFromRemovedAnchorSelectorStillYieldsChapterText() async throws {
        let url = try TestFixtures.temporarySampleBookURL()
        defer { try? FileManager.default.removeItem(at: url.deletingLastPathComponent()) }

        let opened = try await PublicationOpeningService().open(url: url)
        let link = try XCTUnwrap(
            opened.publication.readingOrder.first(where: { $0.href.contains("part3") })
        )
        var locations = Locator.Locations()
        locations.cssSelector = "#anchor-21"
        let locator = Locator(
            href: link.url(),
            mediaType: link.mediaType ?? .xhtml,
            locations: locations,
            text: Locator.Text(highlight: "zrodzenia się21 nieba")
        )
        let elements = await opened.publication.content(from: locator)?.elements() ?? []
        XCTAssertFalse(elements.isEmpty)
        XCTAssertTrue(
            elements.contains { ($0 as? TextualContentElement)?.text?.contains("zrodzenia się") == true }
        )
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

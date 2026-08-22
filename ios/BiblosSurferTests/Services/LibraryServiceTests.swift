//
//  LibraryServiceTests.swift
//  BiblosSurferTests
//

import XCTest
@testable import BiblosSurfer

final class LibraryServiceTests: XCTestCase {
    private var directory: URL!
    private var covers: URL!
    private var service: LibraryService!

    override func setUpWithError() throws {
        directory = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        covers = directory.appendingPathComponent("Covers", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        service = LibraryService(
            booksDirectory: directory,
            coversDirectory: covers,
            bundledBookURLs: [TestFixtures.sampleBookURL].compactMap { $0 }
        )
    }

    override func tearDownWithError() throws {
        try? FileManager.default.removeItem(at: directory)
    }

    func testBundledBookAppearsAlongsideUserBooks() async throws {
        let leftover = directory.appendingPathComponent("The Sample Voyage.epub")
        XCTAssertTrue(FileManager.default.createFile(atPath: leftover.path, contents: Data("old-mock".utf8)))

        let items = try await service.loadItems()
        let titles = Set(items.map(\.title))

        XCTAssertTrue(FileManager.default.fileExists(atPath: leftover.path))
        XCTAssertTrue(titles.contains(TestFixtures.sampleBookTitle))
        XCTAssertTrue(titles.contains("The Sample Voyage"))
        XCTAssertEqual(
            items.first { $0.title == TestFixtures.sampleBookTitle }?.fileURL,
            TestFixtures.sampleBookURL
        )
    }

    func testBundledBookIsListedFromTheBundleWithoutCopying() async throws {
        let items = try await service.loadItems()

        XCTAssertEqual(items.count, 1)
        XCTAssertEqual(items.first?.title, TestFixtures.sampleBookTitle)
        XCTAssertEqual(items.first?.format, .epub)
        XCTAssertEqual(items.first?.isEPUB, true)
        XCTAssertEqual(items.first?.fileURL, TestFixtures.sampleBookURL)
        let copiedBooks = try FileManager.default.contentsOfDirectory(at: directory, includingPropertiesForKeys: nil)
            .filter { ["epub", "pdf"].contains($0.pathExtension.lowercased()) }
        XCTAssertEqual(copiedBooks.count, 0)
    }

    func testNonBookFilesAreNotListed() async throws {
        _ = try await service.loadItems()
        let notes = directory.appendingPathComponent("notes.txt")
        XCTAssertTrue(FileManager.default.createFile(atPath: notes.path, contents: Data("hello".utf8)))

        let items = try await service.loadItems()

        XCTAssertEqual(items.map(\.title), [TestFixtures.sampleBookTitle])
    }

    func testImportingUnsupportedFormatThrows() async {
        let notes = directory.appendingPathComponent("notes.txt")
        XCTAssertTrue(FileManager.default.createFile(atPath: notes.path, contents: Data()))

        do {
            _ = try await service.importBook(from: notes)
            XCTFail("Expected unsupportedFormat")
        } catch let error as Errors.Library {
            guard case .unsupportedFormat(let fileExtension) = error else {
                return XCTFail("Expected unsupportedFormat, got \(error)")
            }
            XCTAssertEqual(fileExtension, "txt")
        } catch {
            XCTFail("Expected Errors.Library, got \(error)")
        }
    }

    func testImportingEPUBCopiesIntoTheLibrary() async throws {
        _ = try await service.loadItems()
        let source = try TestFixtures.temporarySampleBookURL()
        defer { try? FileManager.default.removeItem(at: source.deletingLastPathComponent()) }

        let imported = try await service.importBook(from: source)

        XCTAssertTrue(imported.isEPUB)
        XCTAssertTrue(imported.fileURL.path.hasPrefix(directory.path))
        XCTAssertTrue(FileManager.default.fileExists(atPath: imported.fileURL.path))

        let items = try await service.loadItems()
        XCTAssertEqual(items.count, 1)
        XCTAssertTrue(items.contains { $0.fileURL.path.hasPrefix(directory.path) })
    }
}

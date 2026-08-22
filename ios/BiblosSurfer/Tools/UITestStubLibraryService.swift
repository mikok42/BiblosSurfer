//
//  UITestStubLibraryService.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import Foundation

/// Deterministic library for previews and UI tests. Points at the bundled sample EPUB so the
/// screens never depend on files in Documents.
final class UITestStubLibraryService: LibraryServiceProtocol {
    static let sampleTitle = "The Sample Voyage"

    var sampleBookURL: URL? {
        Bundle.main.url(forResource: "SampleBook", withExtension: "epub")
    }

    func loadItems() async throws -> [LibraryItem] {
        guard let sampleBookURL else {
            throw Errors.Library.fileMissing(fileName: "SampleBook.epub")
        }
        return [
            LibraryItem(
                id: UUID(uuidString: "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE") ?? UUID(),
                fileName: "The Sample Voyage.epub",
                title: Self.sampleTitle,
                author: "Ferdek Test",
                fileURL: sampleBookURL,
                coverURL: nil,
                locatorJSON: nil,
                progression: 0,
                isPDF: false,
                folderName: nil
            )
        ]
    }

    func importBook(from sourceURL: URL) async throws -> LibraryItem {
        throw Errors.Library.unsupportedFormat(fileExtension: sourceURL.pathExtension)
    }
}

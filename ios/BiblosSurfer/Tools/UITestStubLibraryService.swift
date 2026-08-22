//
//  UITestStubLibraryService.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import Foundation

/// Deterministic library for previews and UI tests. Points at the bundled Genesis EPUB so the
/// screens never depend on files in Documents.
final class UITestStubLibraryService: LibraryServiceProtocol {
    static let sampleTitle = "Genesis. Księga Rodzaju. Bereszit"
    static let sampleAuthor = "Izaak Cylkow"
    static let resourceName = "genesis-ksiega-rodzaju-bereszit"
    static let fileExtension = "epub"

    var sampleBookURL: URL? {
        Bundle.main.url(forResource: Self.resourceName, withExtension: Self.fileExtension)
    }

    func loadItems() async throws -> [LibraryItem] {
        guard let sampleBookURL else {
            throw Errors.Library.fileMissing(fileName: "\(Self.resourceName).\(Self.fileExtension)")
        }
        return [
            LibraryItem(
                id: UUID(uuidString: "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE") ?? UUID(),
                fileName: "\(Self.resourceName).\(Self.fileExtension)",
                title: Self.sampleTitle,
                author: Self.sampleAuthor,
                fileURL: sampleBookURL,
                coverURL: nil,
                locatorJSON: nil,
                progression: 0,
                format: .epub,
                folderName: nil
            )
        ]
    }

    func importBook(from sourceURL: URL) async throws -> LibraryItem {
        throw Errors.Library.unsupportedFormat(fileExtension: sourceURL.pathExtension)
    }
}

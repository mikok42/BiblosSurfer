//
//  BookStore.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import Foundation
import SwiftData

protocol BookStoreProtocol: AnyObject {
    func book(atRelativePath relativePath: String) -> LibraryItem?
    func upsert(_ item: LibraryItem, relativePath: String, coverFileName: String?)
    func updateProgress(relativePath: String, locatorJSON: String, progression: Double)
    func addBookmark(relativePath: String, locatorJSON: String, title: String?)
}

final class LocalBookService: BookStoreProtocol {
    private var items: [String: LibraryItem] = [:]
    private var bookmarks: [String: [(String, String?)]] = [:]

    func book(atRelativePath relativePath: String) -> LibraryItem? {
        items[relativePath]
    }

    func upsert(_ item: LibraryItem, relativePath: String, coverFileName: String?) {
        let existing = items[relativePath]
        items[relativePath] = LibraryItem(
            id: item.id,
            fileName: item.fileName,
            title: item.title,
            author: item.author,
            fileURL: item.fileURL,
            coverURL: item.coverURL ?? existing?.coverURL,
            locatorJSON: item.locatorJSON ?? existing?.locatorJSON,
            progression: item.progression,
            format: item.format,
            folderName: item.folderName
        )
    }

    func updateProgress(relativePath: String, locatorJSON: String, progression: Double) {
        guard var item = items[relativePath] else { return }
        item = LibraryItem(
            id: item.id,
            fileName: item.fileName,
            title: item.title,
            author: item.author,
            fileURL: item.fileURL,
            coverURL: item.coverURL,
            locatorJSON: locatorJSON,
            progression: progression,
            format: item.format,
            folderName: item.folderName
        )
        items[relativePath] = item
    }

    func addBookmark(relativePath: String, locatorJSON: String, title: String?) {
        bookmarks[relativePath, default: []].append((locatorJSON, title))
    }
}

final class SwiftDataBookStore: BookStoreProtocol {
    private let container: ModelContainer
    private let context: ModelContext
    private let booksDirectory: URL
    private let coversDirectory: URL

    init(fileManager: FileManager = .default) {
        let schema = Schema([StoredBook.self, StoredFolder.self, StoredBookmark.self])
        if let persistent = try? ModelContainer(for: schema) {
            self.container = persistent
        } else {
            let inMemory = ModelConfiguration(isStoredInMemoryOnly: true)
            self.container = try! ModelContainer(for: schema, configurations: [inMemory])
        }
        self.context = ModelContext(container)
        self.booksDirectory = fileManager.defaultBooksDirectory
        self.coversDirectory = fileManager.defaultCoversDirectory
    }

    func book(atRelativePath relativePath: String) -> LibraryItem? {
        storedBook(relativePath: relativePath).map { $0.asLibraryItem(booksDirectory: booksDirectory, coversDirectory: coversDirectory) }
    }

    func upsert(_ item: LibraryItem, relativePath: String, coverFileName: String?) {
        let stored = storedBook(relativePath: relativePath) ?? StoredBook(
            id: item.id,
            fileName: item.fileName,
            title: item.title,
            relativePath: relativePath,
            format: item.format
        )
        stored.id = item.id
        stored.fileName = item.fileName
        stored.title = item.title
        stored.author = item.author
        stored.relativePath = relativePath
        stored.coverFileName = coverFileName ?? stored.coverFileName
        stored.locatorJSON = item.locatorJSON ?? stored.locatorJSON
        stored.progression = item.progression
        stored.format = item.format
        stored.folderName = item.folderName
        context.insert(stored)
        try? context.save()
    }

    func updateProgress(relativePath: String, locatorJSON: String, progression: Double) {
        guard let stored = storedBook(relativePath: relativePath) else { return }
        stored.locatorJSON = locatorJSON
        stored.progression = progression
        try? context.save()
    }

    func addBookmark(relativePath: String, locatorJSON: String, title: String?) {
        guard let stored = storedBook(relativePath: relativePath) else { return }
        let bookmark = StoredBookmark(locatorJSON: locatorJSON, title: title, book: stored)
        context.insert(bookmark)
        try? context.save()
    }

    private func storedBook(relativePath: String) -> StoredBook? {
        let predicate = #Predicate<StoredBook> { $0.relativePath == relativePath }
        var descriptor = FetchDescriptor<StoredBook>(predicate: predicate)
        descriptor.fetchLimit = 1
        return try? context.fetch(descriptor).first
    }
}

private extension StoredBook {
    func asLibraryItem(booksDirectory: URL, coversDirectory: URL) -> LibraryItem {
        LibraryItem(
            id: id,
            fileName: fileName,
            title: title,
            author: author,
            fileURL: booksDirectory.appendingPathComponent(relativePath),
            coverURL: coverFileName.map { coversDirectory.appendingPathComponent($0) },
            locatorJSON: locatorJSON,
            progression: progression,
            format: format,
            folderName: folderName
        )
    }
}

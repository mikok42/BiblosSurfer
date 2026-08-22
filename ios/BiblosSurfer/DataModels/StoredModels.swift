//
//  StoredModels.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import Foundation
import SwiftData

@Model
final class StoredBook {
    var id: UUID
    var fileName: String
    var title: String
    var author: String?
    var relativePath: String
    var coverFileName: String?
    var locatorJSON: String?
    var progression: Double
    var isPDF: Bool
    var folderName: String?
    var importedAt: Date
    @Relationship(deleteRule: .cascade, inverse: \StoredBookmark.book)
    var bookmarks: [StoredBookmark]

    init(
        id: UUID = UUID(),
        fileName: String,
        title: String,
        author: String? = nil,
        relativePath: String,
        coverFileName: String? = nil,
        locatorJSON: String? = nil,
        progression: Double = 0,
        isPDF: Bool,
        folderName: String? = nil,
        importedAt: Date = Date(),
        bookmarks: [StoredBookmark] = []
    ) {
        self.id = id
        self.fileName = fileName
        self.title = title
        self.author = author
        self.relativePath = relativePath
        self.coverFileName = coverFileName
        self.locatorJSON = locatorJSON
        self.progression = progression
        self.isPDF = isPDF
        self.folderName = folderName
        self.importedAt = importedAt
        self.bookmarks = bookmarks
    }
}

@Model
final class StoredFolder {
    var name: String

    init(name: String) {
        self.name = name
    }
}

@Model
final class StoredBookmark {
    var locatorJSON: String
    var createdAt: Date
    var title: String?
    var book: StoredBook?

    init(locatorJSON: String, createdAt: Date = Date(), title: String? = nil, book: StoredBook? = nil) {
        self.locatorJSON = locatorJSON
        self.createdAt = createdAt
        self.title = title
        self.book = book
    }
}

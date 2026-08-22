//
//  LibraryService.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import Foundation
import UIKit

protocol LibraryServiceProtocol {
    func loadItems() async throws -> [LibraryItem]
    func importBook(from sourceURL: URL) async throws -> LibraryItem
}

final class LibraryService: LibraryServiceProtocol {
    private let fileManager: FileManager
    private let booksDirectory: URL
    private let coversDirectory: URL
    private let bundledBookURL: URL?
    private let opener: PublicationOpeningServiceProtocol
    private let bookStore: BookStoreProtocol

    init(
        fileManager: FileManager = .default,
        booksDirectory: URL? = nil,
        coversDirectory: URL? = nil,
        bundledBookURL: URL? = Bundle.main.url(forResource: "genesis-ksiega-rodzaju-bereszit", withExtension: "epub"),
        opener: PublicationOpeningServiceProtocol = PublicationOpeningService(),
        bookStore: BookStoreProtocol = LocalBookService()
    ) {
        self.fileManager = fileManager
        self.booksDirectory = booksDirectory ?? fileManager.defaultBooksDirectory
        self.coversDirectory = coversDirectory ?? fileManager.defaultCoversDirectory
        self.bundledBookURL = bundledBookURL
        self.opener = opener
        self.bookStore = bookStore
    }

    func loadItems() async throws -> [LibraryItem] {
        try ensureDirectories()
        let urls = try libraryFileURLs()
        for url in urls where bookStore.book(atRelativePath: url.lastPathComponent) == nil {
            _ = try? await catalog(fileURL: url)
        }
        return urls
            .map { url in
                if let stored = bookStore.book(atRelativePath: url.lastPathComponent) {
                    return stored.replacingFileURL(url)
                }
                return LibraryItem(fileURL: url)
            }
            .sorted { $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending }
    }

    func importBook(from sourceURL: URL) async throws -> LibraryItem {
        let accessing = sourceURL.startAccessingSecurityScopedResource()
        defer {
            if accessing {
                sourceURL.stopAccessingSecurityScopedResource()
            }
        }

        let fileExtension = sourceURL.pathExtension.lowercased()
        guard fileExtension == "epub" || fileExtension == "pdf" else {
            throw Errors.Library.unsupportedFormat(fileExtension: fileExtension)
        }

        try ensureDirectories()
        let destination = uniqueDestination(for: sourceURL.lastPathComponent)
        do {
            try fileManager.copyItem(at: sourceURL, to: destination)
        } catch {
            throw Errors.Library.copyFailed(
                fileName: sourceURL.lastPathComponent,
                underlying: error.localizedDescription
            )
        }
        return try await catalog(fileURL: destination)
    }

    private func libraryFileURLs() throws -> [URL] {
        var urls = try listedBookURLs()
        if let bundledBookURL,
           !urls.contains(where: { $0.lastPathComponent == bundledBookURL.lastPathComponent }) {
            urls.append(bundledBookURL)
        }
        return urls
    }

    private func catalog(fileURL: URL) async throws -> LibraryItem {
        let opened: OpenedPublication
        do {
            opened = try await opener.open(url: fileURL)
        } catch let error as DescriptiveError {
            throw error
        } catch {
            throw Errors.Publication.openFailed(
                title: fileURL.deletingPathExtension().lastPathComponent,
                underlying: error.localizedDescription
            )
        }

        var coverFileName: String?
        if let cover = opened.cover, let data = cover.jpegData(compressionQuality: 0.8) {
            let name = fileURL.deletingPathExtension().lastPathComponent + ".jpg"
            let coverURL = coversDirectory.appendingPathComponent(name)
            try? data.write(to: coverURL, options: .atomic)
            coverFileName = name
        }

        let relativePath = fileURL.lastPathComponent
        let existing = bookStore.book(atRelativePath: relativePath)
        let item = LibraryItem(
            id: existing?.id ?? UUID(),
            fileName: fileURL.lastPathComponent,
            title: opened.title,
            author: opened.author,
            fileURL: fileURL,
            coverURL: coverFileName.map { coversDirectory.appendingPathComponent($0) },
            locatorJSON: existing?.locatorJSON,
            progression: existing?.progression ?? 0,
            format: opened.format,
            folderName: existing?.folderName
        )
        bookStore.upsert(item, relativePath: relativePath, coverFileName: coverFileName)
        return item
    }

    private func ensureDirectories() throws {
        do {
            try fileManager.createDirectory(at: booksDirectory, withIntermediateDirectories: true)
            try fileManager.createDirectory(at: coversDirectory, withIntermediateDirectories: true)
        } catch {
            throw Errors.Library.copyFailed(
                fileName: booksDirectory.lastPathComponent,
                underlying: error.localizedDescription
            )
        }
    }

    private func listedBookURLs() throws -> [URL] {
        let contents: [URL]
        do {
            contents = try fileManager.contentsOfDirectory(
                at: booksDirectory,
                includingPropertiesForKeys: nil,
                options: [.skipsHiddenFiles]
            )
        } catch {
            throw Errors.Library.copyFailed(
                fileName: booksDirectory.lastPathComponent,
                underlying: error.localizedDescription
            )
        }
        return contents.filter {
            let ext = $0.pathExtension.lowercased()
            return ext == "epub" || ext == "pdf"
        }
    }

    private func uniqueDestination(for fileName: String) -> URL {
        var destination = booksDirectory.appendingPathComponent(fileName)
        let baseName = destination.deletingPathExtension().lastPathComponent
        let fileExtension = destination.pathExtension
        var suffix = 1
        while fileManager.fileExists(atPath: destination.path) {
            destination = booksDirectory.appendingPathComponent("\(baseName) \(suffix).\(fileExtension)")
            suffix += 1
        }
        return destination
    }
}

private extension LibraryItem {
    func replacingFileURL(_ fileURL: URL) -> LibraryItem {
        LibraryItem(
            id: id,
            fileName: fileName,
            title: title,
            author: author,
            fileURL: fileURL,
            coverURL: coverURL,
            locatorJSON: locatorJSON,
            progression: progression,
            format: format,
            folderName: folderName
        )
    }
}

extension FileManager {
    var defaultBooksDirectory: URL {
        urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("Books", isDirectory: true)
    }

    var defaultCoversDirectory: URL {
        urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("Covers", isDirectory: true)
    }
}

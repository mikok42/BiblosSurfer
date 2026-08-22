//
//  ServiceProvider.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 22/08/2026.
//

import Foundation

protocol ServiceProviderProtocol: AnyObject {
    var bookService: BookStoreProtocol { get }
    var libraryService: LibraryServiceProtocol { get }
    var publicationOpener: PublicationOpeningServiceProtocol { get }
    var settings: ReaderSettingsStore { get }
}

final class ServiceProvider: ServiceProviderProtocol {
    let bookService: BookStoreProtocol
    let libraryService: LibraryServiceProtocol
    let publicationOpener: PublicationOpeningServiceProtocol
    let settings: ReaderSettingsStore

    init() {
        publicationOpener = PublicationOpeningService()
        bookService = SwiftDataBookStore()
        settings = ReaderSettingsStore()
        libraryService = LibraryService(
            opener: publicationOpener,
            bookStore: bookService
        )
    }
}

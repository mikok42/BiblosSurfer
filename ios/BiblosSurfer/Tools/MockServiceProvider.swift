//
//  MockServiceProvider.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 22/08/2026.
//

import Foundation

final class MockServiceProvider: ServiceProviderProtocol {
    let bookService: BookStoreProtocol
    let libraryService: LibraryServiceProtocol
    let publicationOpener: PublicationOpeningServiceProtocol
    let settings: ReaderSettingsStore

    init() {
        publicationOpener = PublicationOpeningService()
        bookService = LocalBookService()
        settings = ReaderSettingsStore(defaults: UserDefaults(suiteName: "miko.BiblosSurfer.mock") ?? .standard)
        libraryService = UITestStubLibraryService()
    }
}

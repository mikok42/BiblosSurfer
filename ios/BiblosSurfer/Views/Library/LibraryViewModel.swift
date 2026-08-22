//
//  LibraryViewModel.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import Foundation
import Observation

protocol LibraryRouting: AnyObject {
    func openBook(_ item: LibraryItem)
}

struct LibraryViewState: Observable {
    var items: [LibraryItem] = []
    var isLoading = true
    var error: DescriptiveError?
}

@Observable
final class LibraryViewModel: ErrorDismissing {
    private let libraryService: LibraryServiceProtocol
    weak var router: LibraryRouting?

    var viewProperties: LibraryViewState

    init(libraryService: LibraryServiceProtocol = LibraryService()) {
        self.libraryService = libraryService
        self.viewProperties = .init()
    }

    @MainActor
    func reload() async {
        viewProperties = .init()
        await load()
    }

    @MainActor
    func load() async {
        viewProperties.isLoading = true
        defer { viewProperties.isLoading = false }
        do {
            viewProperties.items = try await libraryService.loadItems()
            viewProperties.error = nil
        } catch let error as DescriptiveError {
            viewProperties.error = error
        } catch {
            viewProperties.error = Errors.Library.copyFailed(
                fileName: "library",
                underlying: error.localizedDescription
            )
        }
    }

    func open(_ item: LibraryItem) {
        router?.openBook(item)
    }

    @MainActor
    func dismissError() {
        Task { await reload() }
    }

    @MainActor
    func importBook(from url: URL) async {
        do {
            _ = try await libraryService.importBook(from: url)
            await load()
        } catch let error as DescriptiveError {
            viewProperties.error = error
        } catch {
            viewProperties.error = Errors.Library.copyFailed(
                fileName: url.lastPathComponent,
                underlying: error.localizedDescription
            )
        }
    }

    func presentImportFailure(_ error: Error) {
        viewProperties.error = Errors.Library.copyFailed(
            fileName: "import",
            underlying: error.localizedDescription
        )
    }
}

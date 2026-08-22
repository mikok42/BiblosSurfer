//
//  BookViewModel.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import Foundation
import Observation

struct BookViewState: Observable {
    var title: String = ""
    var body: String = ""
    var isLoading = true
    var error: DescriptiveError?
}

@Observable
final class BookViewModel {
    private let fileURL: URL
    private let textService: PublicationTextServiceProtocol

    var viewProperties: BookViewState

    init(
        fileURL: URL,
        textService: PublicationTextServiceProtocol = PublicationTextService()
    ) {
        self.fileURL = fileURL
        self.textService = textService
        self.viewProperties = .init()
    }

    @MainActor
    func load() async {
        viewProperties.isLoading = true
        defer { viewProperties.isLoading = false }
        do {
            let book = try await textService.loadText(from: fileURL)
            viewProperties.title = book.title
            viewProperties.body = book.body
            viewProperties.error = nil
        } catch let error as DescriptiveError {
            viewProperties.error = error
        } catch {
            viewProperties.error = Errors.Publication.openFailed(
                title: fileURL.deletingPathExtension().lastPathComponent,
                underlying: error.localizedDescription
            )
        }
    }
}

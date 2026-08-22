//
//  ReaderCoordinator.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import SwiftUI
import UIKit

final class ReaderCoordinator: Coordinator {
    var childCoordinators: [Coordinator] = []
    var navigationController: UINavigationController

    private let item: LibraryItem
    private let opener: PublicationOpeningServiceProtocol
    private let bookStore: BookStoreProtocol
    private let settings: ReaderSettingsStore
    private let timer = AnalyticsTimer(reportName: AnalyticsEvent.openingPublication)

    init(
        item: LibraryItem,
        navigationController: UINavigationController,
        opener: PublicationOpeningServiceProtocol,
        bookStore: BookStoreProtocol,
        settings: ReaderSettingsStore
    ) {
        self.item = item
        self.navigationController = navigationController
        self.opener = opener
        self.bookStore = bookStore
        self.settings = settings
    }

    func start() {
        timer.startTimer()
        Task { @MainActor in
            do {
                let opened = try await opener.open(url: item.fileURL)
                timer.endTimer()
                timer.reportToAnalytics()
                var resolved = item
                resolved = LibraryItem(
                    id: item.id,
                    fileName: item.fileName,
                    title: opened.title,
                    author: opened.author,
                    fileURL: item.fileURL,
                    coverURL: item.coverURL,
                    locatorJSON: item.locatorJSON,
                    progression: item.progression,
                    format: opened.format,
                    folderName: item.folderName
                )
                let reader = try ReaderViewController(
                    publication: opened.publication,
                    item: resolved,
                    bookStore: bookStore,
                    settings: settings
                )
                navigationController.pushViewController(reader, animated: true)
            } catch let error as DescriptiveError {
                timer.endTimer()
                timer.reportToAnalytics()
                present(error: error)
            } catch {
                timer.endTimer()
                timer.reportToAnalytics()
                present(error: Errors.Publication.openFailed(
                    title: item.title,
                    underlying: error.localizedDescription
                ))
            }
        }
    }

    private func present(error: DescriptiveError) {
        let viewModel = ErrorViewModel(error: error)
        viewModel.router = self
        let view = ErrorView(error: error, handler: viewModel)
        navigationController.pushViewController(UIHostingController(rootView: view), animated: true)
    }
}

extension ReaderCoordinator: ErrorRouting {
    func dismissError() {
        navigationController.popViewController(animated: true)
    }
}

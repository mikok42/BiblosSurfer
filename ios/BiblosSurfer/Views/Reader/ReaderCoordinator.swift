//
//  ReaderCoordinator.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import ReadiumNavigator
import ReadiumShared
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
                let session = try makeSession(publication: opened.publication, item: resolved)
                let reader = ReaderViewController(
                    publication: opened.publication,
                    item: resolved,
                    bookStore: bookStore,
                    settings: settings,
                    session: session
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

    func makeSession(publication: Publication, item: LibraryItem) throws -> ReaderSession {
        let initialLocation = item.locatorJSON.flatMap { try? Locator(jsonString: $0) }
        switch item.format {
        case .pdf:
            let pdf = try PDFNavigatorViewController(
                publication: publication,
                initialLocation: initialLocation,
                config: PDFNavigatorViewController.Configuration(
                    preferences: settings.pdfPreferences()
                )
            )
            return ReaderSession(
                navigatorController: pdf,
                visualNavigator: pdf,
                epubNavigator: nil,
                pdfNavigator: pdf,
                preferences: pdf
            )
        case .epub:
            var config = EPUBNavigatorViewController.Configuration(
                preferences: settings.epubPreferences()
            )
            config.editingActions.append(
                EditingAction(title: "Czytaj od tego miejsca", action: #selector(ReaderViewController.readFromSelection))
            )
            let epub = try EPUBNavigatorViewController(
                publication: publication,
                initialLocation: initialLocation,
                config: config
            )
            return ReaderSession(
                navigatorController: epub,
                visualNavigator: epub,
                epubNavigator: epub,
                pdfNavigator: nil,
                preferences: epub
            )
        case .unknown:
            throw Errors.Publication.unknownFormat(title: item.title)
        }
    }

    private func present(error: DescriptiveError) {
        let viewModel = ErrorViewModel(error: error)
        viewModel.router = self
        let view = ErrorView(error: error, handler: viewModel)
        navigationController.pushViewController(UIHostingController(rootView: view), animated: true)
    }
}

extension ReaderCoordinator: ErrorDismissing {
    func dismissError() {
        navigationController.popViewController(animated: true)
    }
}

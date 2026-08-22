//
//  MainCoordinator.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import SwiftUI
import UIKit

class MainCoordinator: Coordinator {
    var childCoordinators: [Coordinator] = []
    var navigationController: UINavigationController
    private let serviceProvider: ServiceProviderProtocol

    init(
        navigationController: UINavigationController,
        serviceProvider: ServiceProviderProtocol
    ) {
        self.navigationController = navigationController
        self.serviceProvider = serviceProvider
    }

    func start() {
        let viewModel = LibraryViewModel(libraryService: serviceProvider.libraryService)
        let view = LibraryView(viewModel: viewModel) { [weak self] item in
            self?.showBook(item)
        }
        navigationController.pushViewController(
            UIHostingController(rootView: view),
            animated: false
        )
    }

    func showBook(_ item: LibraryItem) {
        let reader = ReaderCoordinator(
            item: item,
            navigationController: navigationController,
            opener: serviceProvider.publicationOpener,
            bookStore: serviceProvider.bookService,
            settings: serviceProvider.settings
        )
        childCoordinators.append(reader)
        reader.start()
    }

    func importIncomingFile(from url: URL) {
        Task { @MainActor in
            _ = try? await serviceProvider.libraryService.importBook(from: url)
        }
    }
}

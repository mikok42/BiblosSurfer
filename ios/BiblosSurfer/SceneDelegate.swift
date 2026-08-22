//
//  SceneDelegate.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import UIKit

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?
    private var coordinator: MainCoordinator?
    private var serviceProvider: ServiceProviderProtocol?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = (scene as? UIWindowScene) else { return }

        let serviceProvider: ServiceProviderProtocol = ProcessInfo.processInfo.isUITestStubLaunch
            ? MockServiceProvider()
            : ServiceProvider()
        self.serviceProvider = serviceProvider

        let navigationController = UINavigationController()
        let coordinator = MainCoordinator(
            navigationController: navigationController,
            serviceProvider: serviceProvider
        )
        self.coordinator = coordinator
        coordinator.start()

        let window = UIWindow(windowScene: windowScene)
        window.rootViewController = navigationController
        self.window = window
        window.makeKeyAndVisible()

        connectionOptions.urlContexts.forEach { context in
            coordinator.importIncomingFile(from: context.url)
        }
    }

    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        URLContexts.forEach { context in
            coordinator?.importIncomingFile(from: context.url)
        }
    }
}

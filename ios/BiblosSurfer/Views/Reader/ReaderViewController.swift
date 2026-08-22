//
//  ReaderViewController.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import ReadiumNavigator
import ReadiumShared
import SwiftUI
import UIKit

final class ReaderViewController: UIViewController {
    private let publication: Publication
    private let item: LibraryItem
    private let bookStore: BookStoreProtocol
    private let openerRelativePath: String
    private let viewModel: ReaderViewModel
    private let navigatorController: UIViewController
    private let visualNavigator: VisualNavigator
    private let epubNavigator: EPUBNavigatorViewController?
    private let readerView: ReaderView
    private var ttsService: TTSService?
    private let locationDebouncer = Debouncer()
    private let wordDebouncer = Debouncer()
    private var ttsPanelHost: UIHostingController<TTSPanelView>?
    private var isMoving = false

    init(
        publication: Publication,
        item: LibraryItem,
        bookStore: BookStoreProtocol,
        settings: ReaderSettingsStore
    ) throws {
        let initialLocation = item.locatorJSON.flatMap { try? Locator(jsonString: $0) }
        let pdfNavigator: PDFNavigatorViewController?
        let epubNavigator: EPUBNavigatorViewController?
        let navigatorController: UIViewController
        let visualNavigator: VisualNavigator

        switch item.format {
        case .pdf:
            let pdf = try PDFNavigatorViewController(
                publication: publication,
                initialLocation: initialLocation
            )
            pdfNavigator = pdf
            epubNavigator = nil
            navigatorController = pdf
            visualNavigator = pdf
        case .epub:
            var config = EPUBNavigatorViewController.Configuration(
                preferences: settings.epubPreferences()
            )
            config.editingActions.append(
                EditingAction(title: "Czytaj od tu", action: #selector(readFromSelection))
            )
            let epub = try EPUBNavigatorViewController(
                publication: publication,
                initialLocation: initialLocation,
                config: config
            )
            pdfNavigator = nil
            epubNavigator = epub
            navigatorController = epub
            visualNavigator = epub
        case .unknown:
            throw Errors.Publication.unknownFormat(title: item.title)
        }

        self.publication = publication
        self.item = item
        self.bookStore = bookStore
        self.openerRelativePath = item.fileURL.lastPathComponent
        self.viewModel = ReaderViewModel(
            title: item.title,
            format: item.format,
            canSpeak: item.format == .epub && PublicationSpeechSynthesizer.canSpeak(publication: publication),
            settings: settings
        )
        self.navigatorController = navigatorController
        self.visualNavigator = visualNavigator
        self.epubNavigator = epubNavigator
        self.readerView = ReaderView(navigatorView: navigatorController.view)
        super.init(nibName: nil, bundle: nil)
        pdfNavigator?.delegate = self
        epubNavigator?.delegate = self

        if viewModel.viewProperties.canSpeak {
            let tts = TTSService(publication: publication, settings: settings, bookTitle: item.title)
            tts?.delegate = self
            self.ttsService = tts
        }
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func loadView() {
        view = readerView
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        title = viewModel.viewProperties.title
        navigationItem.rightBarButtonItem = UIBarButtonItem(
            image: UIImage(systemName: "textformat.size"),
            style: .plain,
            target: self,
            action: #selector(openSettings)
        )
        navigationItem.rightBarButtonItem?.accessibilityIdentifier = AccessibilityIdentifiers.Reader.settings

        addChild(navigatorController)
        navigatorController.didMove(toParent: self)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        ttsService?.stop()
        locationDebouncer.cancel()
        wordDebouncer.cancel()
    }

    @objc func readFromSelection() {
        guard viewModel.viewProperties.canSpeak else { return }
        let locator = epubNavigator?.currentSelection?.locator
        installTTSPanelIfNeeded()
        ttsService?.start(from: locator)
        epubNavigator?.clearSelection()
    }

    @objc private func openSettings() {
        let settingsView = ReaderSettingsView(
            viewModel: viewModel,
            voices: ttsService?.availableVoices ?? [],
            onChange: { [weak self] in
                self?.applyReaderSettings()
            },
            onClose: { [weak self] in
                self?.dismiss(animated: true)
            }
        )
        let host = UIHostingController(rootView: settingsView)
        host.modalPresentationStyle = .formSheet
        present(host, animated: true)
    }

    private func applyReaderSettings() {
        if let epubNavigator {
            epubNavigator.submitPreferences(viewModel.settings.epubPreferences())
        }
    }

    private func installTTSPanelIfNeeded() {
        guard ttsPanelHost == nil else { return }
        let host = UIHostingController(
            rootView: TTSPanelView(
                viewModel: viewModel,
                onPlayPause: { [weak self] in self?.ttsService?.pauseOrResume() },
                onStop: { [weak self] in
                    self?.ttsService?.stop()
                    self?.removeTTSPanel()
                },
                onNext: { [weak self] in self?.ttsService?.next() },
                onPrevious: { [weak self] in self?.ttsService?.previous() }
            )
        )
        host.view.backgroundColor = .clear
        addChild(host)
        readerView.setTTSPanel(host.view)
        host.didMove(toParent: self)
        ttsPanelHost = host
    }

    private func removeTTSPanel() {
        ttsPanelHost?.willMove(toParent: nil)
        readerView.setTTSPanel(nil)
        ttsPanelHost?.removeFromParent()
        ttsPanelHost = nil
        epubNavigator?.apply(decorations: [], in: "tts")
    }

    private func persist(locator: Locator) {
        locationDebouncer.schedule(after: 1) { [weak self] in
            guard let self else { return }
            let json = (try? locator.jsonString()) ?? ""
            self.bookStore.updateProgress(
                relativePath: self.openerRelativePath,
                locatorJSON: json,
                progression: locator.locations.totalProgression ?? 0
            )
        }
    }

    private func followSpokenRange(_ locator: Locator) {
        guard !isMoving else { return }
        wordDebouncer.schedule(after: 1) { [weak self] in
            guard let self else { return }
            self.isMoving = true
            Task { @MainActor in
                _ = await self.visualNavigator.go(to: locator)
                self.isMoving = false
            }
        }
    }

    private func highlightUtterance(_ locator: Locator?) {
        guard let epubNavigator else { return }
        var decorations: [Decoration] = []
        if let locator {
            decorations.append(
                Decoration(id: "tts-utterance", locator: locator, style: .highlight(tint: .systemOrange))
            )
        }
        epubNavigator.apply(decorations: decorations, in: "tts")
    }
}

extension ReaderViewController: EPUBNavigatorDelegate, PDFNavigatorDelegate {
    func navigator(_ navigator: Navigator, locationDidChange locator: Locator) {
        persist(locator: locator)
    }

    func navigator(_ navigator: Navigator, presentError error: NavigatorError) {
        viewModel.presentError(
            Errors.Publication.openFailed(title: item.title, underlying: error.localizedDescription)
        )
    }
}

extension ReaderViewController: TTSServiceDelegate {
    func ttsService(_ service: TTSService, didChange state: PublicationSpeechSynthesizer.State) {
        viewModel.apply(ttsState: state)
        switch state {
        case .stopped:
            highlightUtterance(nil)
            removeTTSPanel()
        case .paused(let utterance):
            highlightUtterance(utterance.locator)
            installTTSPanelIfNeeded()
        case .playing(let utterance, let range):
            highlightUtterance(utterance.locator)
            installTTSPanelIfNeeded()
            if let range {
                followSpokenRange(range)
            }
        }
        ttsPanelHost?.rootView = TTSPanelView(
            viewModel: viewModel,
            onPlayPause: { [weak self] in self?.ttsService?.pauseOrResume() },
            onStop: { [weak self] in
                self?.ttsService?.stop()
                self?.removeTTSPanel()
            },
            onNext: { [weak self] in self?.ttsService?.next() },
            onPrevious: { [weak self] in self?.ttsService?.previous() }
        )
    }

    func ttsService(_ service: TTSService, didFail error: DescriptiveError) {
        viewModel.presentError(error)
        let errorView = ErrorView(error: error) { [weak self] in
            self?.dismiss(animated: true)
        }
        present(UIHostingController(rootView: errorView), animated: true)
    }
}

//
//  ReaderViewModel.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import Foundation
import Observation
import ReadiumNavigator
import ReadiumShared
import UIKit

protocol ReaderActions: AnyObject {
    func playPauseTTS()
    func stopTTS()
    func nextUtterance()
    func previousUtterance()
    func applyReaderSettings()
    func closeSettings()
    func dismissPresentedError()
}

struct ReaderViewState: Observable {
    var title: String = ""
    var isPlaying = false
    var canSpeak = false
    var format: PublicationFormat = .unknown
    var error: DescriptiveError?
}

@Observable
final class ReaderViewModel: ErrorDismissing {
    var viewProperties: ReaderViewState
    let settings: ReaderSettingsStore
    weak var actions: ReaderActions?
    var availableVoices: [TTSVoice] = []

    init(title: String, format: PublicationFormat, canSpeak: Bool, settings: ReaderSettingsStore = ReaderSettingsStore()) {
        self.settings = settings
        self.viewProperties = ReaderViewState(
            title: title,
            canSpeak: canSpeak && format == .epub,
            format: format
        )
    }

    func apply(ttsState: PublicationSpeechSynthesizer.State) {
        switch ttsState {
        case .stopped:
            viewProperties.isPlaying = false
        case .paused:
            viewProperties.isPlaying = false
        case .playing:
            viewProperties.isPlaying = true
        }
    }

    func presentError(_ error: DescriptiveError) {
        viewProperties.error = error
    }

    func playPause() {
        actions?.playPauseTTS()
    }

    func stopReading() {
        actions?.stopTTS()
    }

    func nextUtterance() {
        actions?.nextUtterance()
    }

    func previousUtterance() {
        actions?.previousUtterance()
    }

    func settingsDidChange() {
        actions?.applyReaderSettings()
    }

    func closeSettings() {
        actions?.closeSettings()
    }

    func dismissError() {
        viewProperties.error = nil
        actions?.dismissPresentedError()
    }
}

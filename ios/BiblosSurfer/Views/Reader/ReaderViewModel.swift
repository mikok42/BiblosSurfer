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
    var settingsEpoch = 0

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

    var ttsLanguages: [Language] {
        _ = settingsEpoch
        var seen = Set<String>()
        var languages: [Language] = []
        for voice in availableVoices {
            let language = voice.language.removingRegion()
            if seen.insert(language.code.bcp47).inserted {
                languages.append(language)
            }
        }
        return languages.sorted { $0.localizedDescription() < $1.localizedDescription() }
    }

    var ttsVoices: [TTSVoice] {
        _ = settingsEpoch
        guard let code = settings.defaultLanguage else {
            return availableVoices.rankedByAppleQuality()
        }
        return availableVoices
            .filterByLanguage(Language(code: .bcp47(code)))
            .rankedByAppleQuality()
    }

    func selectTTSLanguage(_ code: String?) {
        settings.defaultLanguage = code
        if let code, let voiceId = settings.voiceIdentifier,
           let voice = availableVoices.first(where: { $0.identifier == voiceId }),
           voice.language.removingRegion().code.bcp47 != Language(code: .bcp47(code)).removingRegion().code.bcp47 {
            settings.voiceIdentifier = nil
        }
        settingsDidChange()
    }

    func settingsDidChange() {
        settingsEpoch += 1
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

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

struct ReaderViewState: Observable {
    var title: String = ""
    var isPlaying = false
    var canSpeak = false
    var format: PublicationFormat = .unknown
    var error: DescriptiveError?
}

@Observable
final class ReaderViewModel {
    var viewProperties: ReaderViewState
    let settings: ReaderSettingsStore

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
}

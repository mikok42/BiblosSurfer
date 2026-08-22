//
//  AccessibilityIdentifiers.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import Foundation

enum AccessibilityIdentifiers {
    enum Library {
        static let title = "library.title"
        static let importButton = "library.importButton"
        static let grid = "library.grid"
        static let emptyState = "library.emptyState"
        static let loading = "library.loading"

        static func cell(_ bookTitle: String) -> String {
            "library.cell.\(bookTitle)"
        }
    }

    enum Reader {
        static let container = "reader.container"
        static let title = "reader.title"
        static let close = "reader.close"
        static let settings = "reader.settings"
        static let progress = "reader.progress"
        static let ttsPlay = "reader.ttsPlay"
        static let ttsPause = "reader.ttsPause"
        static let ttsStop = "reader.ttsStop"
        static let ttsNext = "reader.ttsNext"
        static let ttsPrevious = "reader.ttsPrevious"
        static let ttsPanel = "reader.ttsPanel"
    }

    enum Settings {
        static let fontSize = "settings.fontSize"
        static let fontFamily = "settings.fontFamily"
        static let theme = "settings.theme"
        static let scrollMode = "settings.scrollMode"
        static let tts = "settings.tts"
        static let voice = "settings.voice"
        static let speechRate = "settings.speechRate"
        static let pitch = "settings.pitch"
        static let volume = "settings.volume"
        static let preUtteranceDelay = "settings.preUtteranceDelay"
        static let postUtteranceDelay = "settings.postUtteranceDelay"
        static let language = "settings.language"
        static let chunkUnit = "settings.chunkUnit"
        static let systemSpeech = "settings.systemSpeech"
        static let done = "settings.done"
    }

    enum Error {
        static let title = "error.title"
        static let description = "error.description"
        static let dismiss = "error.dismiss"
    }
}

//
//  ReaderSettingsStore.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import AVFoundation
import Foundation
import ReadiumNavigator
import ReadiumShared

enum TTSChunkUnit: String, CaseIterable, Identifiable {
    case word
    case sentence
    case paragraph

    var id: String { rawValue }

    var textUnit: TextUnit {
        switch self {
        case .word: .word
        case .sentence: .sentence
        case .paragraph: .paragraph
        }
    }

    var title: String {
        switch self {
        case .word: "Word"
        case .sentence: "Sentence"
        case .paragraph: "Paragraph"
        }
    }
}

final class ReaderSettingsStore {
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    var fontSize: Double {
        get { defaults.object(forKey: "reader.fontSize") as? Double ?? 1.0 }
        set { defaults.set(newValue, forKey: "reader.fontSize") }
    }

    var fontFamily: String {
        get { defaults.string(forKey: "reader.fontFamily") ?? "Original" }
        set { defaults.set(newValue, forKey: "reader.fontFamily") }
    }

    var theme: Theme {
        get { Theme(rawValue: defaults.string(forKey: "reader.theme") ?? "") ?? .light }
        set { defaults.set(newValue.rawValue, forKey: "reader.theme") }
    }

    var scroll: Bool {
        get { defaults.bool(forKey: "reader.scroll") }
        set { defaults.set(newValue, forKey: "reader.scroll") }
    }

    var voiceIdentifier: String? {
        get { defaults.string(forKey: "reader.voiceIdentifier") }
        set { defaults.set(newValue, forKey: "reader.voiceIdentifier") }
    }

    var speechRate: Float {
        get {
            let stored = defaults.object(forKey: "reader.speechRate") as? Float
            return stored ?? AVSpeechUtteranceDefaultSpeechRate
        }
        set { defaults.set(newValue, forKey: "reader.speechRate") }
    }

    var pitchMultiplier: Float {
        get {
            let stored = defaults.object(forKey: "reader.pitchMultiplier") as? Float
            return stored ?? 1.0
        }
        set { defaults.set(newValue, forKey: "reader.pitchMultiplier") }
    }

    var speechVolume: Float {
        get {
            let stored = defaults.object(forKey: "reader.speechVolume") as? Float
            return stored ?? 1.0
        }
        set { defaults.set(newValue, forKey: "reader.speechVolume") }
    }

    var preUtteranceDelay: TimeInterval {
        get { defaults.object(forKey: "reader.preUtteranceDelay") as? TimeInterval ?? 0 }
        set { defaults.set(newValue, forKey: "reader.preUtteranceDelay") }
    }

    var postUtteranceDelay: TimeInterval {
        get { defaults.object(forKey: "reader.postUtteranceDelay") as? TimeInterval ?? 0 }
        set { defaults.set(newValue, forKey: "reader.postUtteranceDelay") }
    }

    var defaultLanguage: String? {
        get { defaults.string(forKey: "reader.defaultLanguage") }
        set { defaults.set(newValue, forKey: "reader.defaultLanguage") }
    }

    var chunkUnit: TTSChunkUnit {
        get {
            TTSChunkUnit(rawValue: defaults.string(forKey: "reader.chunkUnit") ?? "") ?? .sentence
        }
        set { defaults.set(newValue.rawValue, forKey: "reader.chunkUnit") }
    }

    var useSystemSpeechSettings: Bool {
        get { defaults.bool(forKey: "reader.useSystemSpeechSettings") }
        set { defaults.set(newValue, forKey: "reader.useSystemSpeechSettings") }
    }

    func epubPreferences() -> EPUBPreferences {
        var preferences = EPUBPreferences()
        preferences.fontSize = fontSize
        preferences.scroll = scroll
        preferences.theme = theme
        if fontFamily != "Original" {
            preferences.fontFamily = FontFamily(rawValue: fontFamily)
            preferences.publisherStyles = false
        }
        return preferences
    }
}
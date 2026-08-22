//
//  ReaderSettingsStore.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import AVFoundation
import Foundation
import ReadiumNavigator

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
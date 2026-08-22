//
//  TTSVoice+AppleQuality.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 22/08/2026.
//

import Foundation
import ReadiumNavigator
import ReadiumShared

enum AppleTTSVoiceTier: Int, Comparable {
    case compact = 0
    case standard = 1
    case neural = 2
    case enhanced = 3
    case premium = 4

    static func < (lhs: Self, rhs: Self) -> Bool {
        lhs.rawValue < rhs.rawValue
    }

    var title: String {
        switch self {
        case .compact: "Compact"
        case .standard: "Standard"
        case .neural: "Neural"
        case .enhanced: "Enhanced"
        case .premium: "Premium"
        }
    }
}

extension TTSVoice {
    var appleTier: AppleTTSVoiceTier {
        let id = identifier.lowercased()
        if id.contains("super-compact") {
            return .neural
        }
        if quality == .higher || id.contains("premium") {
            return .premium
        }
        if quality == .high || id.contains("enhanced") {
            return .enhanced
        }
        if id.contains("compact") {
            return .compact
        }
        return .standard
    }

    var settingsDisplayName: String {
        "\(name) · \(appleTier.title)"
    }
}

extension [TTSVoice] {
    func rankedByAppleQuality() -> [TTSVoice] {
        sorted { lhs, rhs in
            if lhs.appleTier != rhs.appleTier {
                return lhs.appleTier > rhs.appleTier
            }
            return lhs.name.localizedCaseInsensitiveCompare(rhs.name) == .orderedAscending
        }
    }

    func preferredAppleVoice(for language: Language?) -> TTSVoice? {
        let candidates: [TTSVoice]
        if let language {
            let matching = filterByLanguage(language)
            candidates = matching.isEmpty ? self : matching
        } else {
            candidates = self
        }
        return candidates.rankedByAppleQuality().first
    }
}

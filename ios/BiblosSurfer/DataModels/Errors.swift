//
//  Errors.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import Foundation

protocol DescriptiveError: Error, CustomStringConvertible {
    var description: String { get }
    var title: String { get }
}

struct Errors {}

extension Errors {
    enum Library: DescriptiveError, Equatable {
        case unsupportedFormat(fileExtension: String)
        case copyFailed(fileName: String, underlying: String)
        case fileMissing(fileName: String)

        var description: String {
            switch self {
            case .unsupportedFormat(let fileExtension):
                return "[Library] BiblosSurfer cannot open .\(fileExtension) files. Try EPUB or PDF."
            case .copyFailed(let fileName, let underlying):
                return "[Library] Could not add \(fileName) to your library: \(underlying)"
            case .fileMissing(let fileName):
                return "[Library] \(fileName) is no longer on this device."
            }
        }

        var title: String {
            switch self {
            case .unsupportedFormat:
                return "Unsupported format"
            case .copyFailed:
                return "Could not add the book"
            case .fileMissing:
                return "Book file missing"
            }
        }
    }
}

extension Errors {
    enum Publication: DescriptiveError, Equatable {
        case openFailed(title: String, underlying: String)
        case unsupportedForReading(title: String)
        case noNavigator(title: String)

        var description: String {
            switch self {
            case .openFailed(let title, let underlying):
                return "[Publication] Could not open \(title): \(underlying)"
            case .unsupportedForReading(let title):
                return "[Publication] \(title) parsed, but its format cannot be displayed."
            case .noNavigator(let title):
                return "[Publication] No reader is available for the format of \(title)."
            }
        }

        var title: String {
            switch self {
            case .openFailed:
                return "Could not open the book"
            case .unsupportedForReading, .noNavigator:
                return "Cannot display this book"
            }
        }
    }
}

extension Errors {
    enum TTS: DescriptiveError, Equatable {
        case noSpeakableContent
        case noVoiceForLanguage(language: String)
        case engineFailed(underlying: String)

        var description: String {
            switch self {
            case .noSpeakableContent:
                return "[TTS] This book has no text that can be read aloud."
            case .noVoiceForLanguage(let language):
                return "[TTS] No installed voice speaks \(language). Add one in Settings, Accessibility, Spoken Content."
            case .engineFailed(let underlying):
                return "[TTS] Speech synthesis failed: \(underlying)"
            }
        }

        var title: String {
            switch self {
            case .noSpeakableContent:
                return "Nothing to read aloud"
            case .noVoiceForLanguage:
                return "Missing voice"
            case .engineFailed:
                return "Read aloud stopped"
            }
        }
    }
}

//
//  ContentElement+TTSNotes.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 30/08/2026.
//

import Foundation
import ReadiumShared

private let ttsNoteTokens: Set<String> = [
    "aside",
    "annotation", "annotations",
    "footnote", "footnotes",
    "endnote", "endnotes",
    "note", "notes", "noteref",
    "doc-footnote", "doc-endnote", "doc-endnotes",
    "przypis", "przypisy"
]

extension ContentElement {
    /// Drops footnote / annotation bodies. Leaves ordinary digits in the main text alone.
    func preparedForTTS() -> ContentElement? {
        isTTSNoteContent ? nil : self
    }

    var isTTSNoteContent: Bool {
        if let text = self as? TextContentElement, text.role == .footnote {
            return true
        }
        return locator.isTTSNoteLocation
    }
}

extension Locator {
    var isTTSNoteLocation: Bool {
        if href.isTTSNoteResource { return true }
        if let selector = locations.cssSelector, selector.looksLikeTTSNoteSelector {
            return true
        }
        return false
    }
}

extension AnyURL {
    var isTTSNoteResource: Bool {
        let last = URL(string: string)?.lastPathComponent ?? string.split(separator: "/").last.map(String.init) ?? string
        let stem = last.split(separator: ".").first.map(String.init) ?? last
        return ttsNoteTokens.contains(stem.lowercased())
    }
}

extension String {
    var looksLikeTTSNoteSelector: Bool {
        let tokens = lowercased().split { !$0.isLetter && $0 != "-" }.map(String.init)
        return tokens.contains { ttsNoteTokens.contains($0) }
    }
}

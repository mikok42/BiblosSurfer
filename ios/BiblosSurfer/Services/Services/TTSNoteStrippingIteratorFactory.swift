//
//  TTSNoteStrippingIteratorFactory.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 30/08/2026.
//

import Foundation
import ReadiumShared

private let emptyTTSNoteDocument = """
<html xmlns="http://www.w3.org/1999/xhtml"><body/></html>
"""

private let ttsNoteResourceStems: Set<String> = [
    "annotation", "annotations",
    "footnote", "footnotes",
    "endnote", "endnotes",
    "note", "notes",
    "przypis", "przypisy"
]

final class TTSNoteStrippingIteratorFactory: ResourceContentIteratorFactory {
    private let html = HTMLResourceContentIterator.Factory()

    func make(
        publication: Publication,
        readingOrderIndex: Int,
        resource: Resource,
        locator: Locator
    ) -> ContentIterator? {
        guard locator.mediaType.isHTML else { return nil }
        let href = locator.href.string
        let cleaned = resource.mapAsString { $0.htmlPreparedForTTS(href: href) }
        return html.make(
            publication: publication,
            readingOrderIndex: readingOrderIndex,
            resource: cleaned,
            locator: locator
        )
    }
}

extension String {
    func htmlPreparedForTTS(href: String) -> String {
        href.isTTSNoteResourcePath ? emptyTTSNoteDocument : strippingTTSNoteMarkup
    }

    var isTTSNoteResourcePath: Bool {
        let last = URL(string: self)?.lastPathComponent
            ?? split(separator: "/").last.map(String.init)
            ?? self
        let stem = last.split(separator: ".").first.map(String.init) ?? last
        return ttsNoteResourceStems.contains(stem.lowercased())
    }

    /// Drops Wolne Lektury / EPUB3 note markup. Leaves ordinary digits (T-800) alone.
    var strippingTTSNoteMarkup: String {
        removingHTMLElements(named: "a", whereAttributesMatch: #"class\s*=\s*["'][^"']*\banchor\b[^"']*["']"#)
            .removingHTMLElements(named: "a", whereAttributesMatch: #"(?:epub:type|type)\s*=\s*["'][^"']*\bnoteref\b[^"']*["']"#)
            .removingHTMLElements(named: "aside", whereAttributesMatch: #"(?:epub:type|type)\s*=\s*["'][^"']*\b(?:footnote|endnote)s?\b[^"']*["']"#)
            .removingHTMLElements(named: "div", whereAttributesMatch: #"id\s*=\s*["']footnotes["']"#)
            .removingHTMLElements(named: "div", whereAttributesMatch: #"class\s*=\s*["'][^"']*\bannotation\b[^"']*["']"#)
    }

    func removingHTMLElements(named tag: String, whereAttributesMatch pattern: String) -> String {
        let escapedTag = NSRegularExpression.escapedPattern(for: tag)
        let regexPattern = "<\(escapedTag)\\b[^>]*\(pattern)[^>]*>.*?</\(escapedTag)\\s*>"
        guard let regex = try? NSRegularExpression(
            pattern: regexPattern,
            options: [.caseInsensitive, .dotMatchesLineSeparators]
        ) else {
            return self
        }
        let range = NSRange(startIndex..., in: self)
        return regex.stringByReplacingMatches(in: self, range: range, withTemplate: "")
    }
}

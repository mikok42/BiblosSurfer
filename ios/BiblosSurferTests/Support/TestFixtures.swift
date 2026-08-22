//
//  TestFixtures.swift
//  BiblosSurferTests
//

import Foundation
import ReadiumShared
@testable import BiblosSurfer

enum TestFixtures {
    static let sampleBookTitle = UITestStubLibraryService.sampleTitle
    static let sampleBookAuthor = UITestStubLibraryService.sampleAuthor

    /// The EPUB bundled with the app target (Wolne Lektury Genesis / Cylkow).
    /// Unit tests run inside the app host, so `Bundle.main` resolves it.
    static var sampleBookURL: URL? {
        Bundle.main.url(
            forResource: UITestStubLibraryService.resourceName,
            withExtension: UITestStubLibraryService.fileExtension
        )
    }

    /// Copies the bundled sample into a fresh temporary directory so a test can mutate or delete it
    /// without touching the bundle.
    static func temporarySampleBookURL() throws -> URL {
        guard let source = sampleBookURL else {
            throw CocoaError(.fileNoSuchFile)
        }
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let destination = directory.appendingPathComponent(
            "\(UITestStubLibraryService.resourceName).\(UITestStubLibraryService.fileExtension)"
        )
        try FileManager.default.copyItem(at: source, to: destination)
        return destination
    }

    static var samplePDFURL: URL? {
        Bundle.main.url(forResource: "SampleBook", withExtension: "pdf")
    }

    static func temporarySamplePDFURL() throws -> URL {
        guard let source = samplePDFURL else {
            throw CocoaError(.fileNoSuchFile)
        }
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let destination = directory.appendingPathComponent("SampleBook.pdf")
        try FileManager.default.copyItem(at: source, to: destination)
        return destination
    }

    static func locator(
        href: String = "OEBPS/chapter1.xhtml",
        mediaType: MediaType = .xhtml,
        title: String? = "Chapter One",
        progression: Double? = 0.25,
        totalProgression: Double? = 0.1,
        position: Int? = 3,
        highlight: String? = "The ship left harbour before dawn."
    ) -> Locator {
        Locator(
            href: AnyURL(string: href)!,
            mediaType: mediaType,
            title: title,
            locations: Locator.Locations(
                progression: progression,
                totalProgression: totalProgression,
                position: position
            ),
            text: Locator.Text(highlight: highlight)
        )
    }
}

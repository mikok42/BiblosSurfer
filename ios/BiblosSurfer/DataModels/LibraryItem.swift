//
//  LibraryItem.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import Foundation

enum PublicationFormat: String, Hashable, Codable {
    case epub
    case pdf
    case unknown

    init(pathExtension: String) {
        switch pathExtension.lowercased() {
        case "epub":
            self = .epub
        case "pdf":
            self = .pdf
        default:
            self = .unknown
        }
    }
}

struct LibraryItem: Identifiable, Hashable {
    var id: UUID
    let fileName: String
    let title: String
    let author: String?
    let fileURL: URL
    let coverURL: URL?
    let locatorJSON: String?
    let progression: Double
    let format: PublicationFormat
    let folderName: String?

    var isEPUB: Bool { format == .epub }
    var isPDF: Bool { format == .pdf }
}

extension LibraryItem {
    init(fileURL: URL, id: UUID = UUID()) {
        let fileName = fileURL.lastPathComponent
        self.init(
            id: id,
            fileName: fileName,
            title: fileURL.deletingPathExtension().lastPathComponent,
            author: nil,
            fileURL: fileURL,
            coverURL: nil,
            locatorJSON: nil,
            progression: 0,
            format: PublicationFormat(pathExtension: fileURL.pathExtension),
            folderName: nil
        )
    }
}

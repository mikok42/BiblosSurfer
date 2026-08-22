//
//  LibraryItem.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import Foundation

struct LibraryItem: Identifiable, Hashable {
    var id: UUID
    let fileName: String
    let title: String
    let author: String?
    let fileURL: URL
    let coverURL: URL?
    let locatorJSON: String?
    let progression: Double
    let isPDF: Bool
    let folderName: String?

    var isEPUB: Bool { !isPDF }
}

extension LibraryItem {
    init(fileURL: URL, id: UUID = UUID()) {
        let fileName = fileURL.lastPathComponent
        let isPDF = fileURL.pathExtension.lowercased() == "pdf"
        self.init(
            id: id,
            fileName: fileName,
            title: fileURL.deletingPathExtension().lastPathComponent,
            author: nil,
            fileURL: fileURL,
            coverURL: nil,
            locatorJSON: nil,
            progression: 0,
            isPDF: isPDF,
            folderName: nil
        )
    }
}

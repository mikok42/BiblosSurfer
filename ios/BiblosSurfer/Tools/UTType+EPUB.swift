//
//  UTType+EPUB.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import UniformTypeIdentifiers

extension UTType {
    static var epubPublication: UTType {
        UTType(filenameExtension: "epub") ?? UTType(importedAs: "org.idpf.epub-container")
    }
}

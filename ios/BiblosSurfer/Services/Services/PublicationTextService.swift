//
//  PublicationTextService.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import Foundation
import ReadiumShared
import ReadiumStreamer

struct BookText: Equatable {
    let title: String
    let body: String
}

protocol PublicationTextServiceProtocol {
    func loadText(from fileURL: URL) async throws -> BookText
}

final class PublicationTextService: PublicationTextServiceProtocol {
    private let assetRetriever: AssetRetriever
    private let publicationOpener: PublicationOpener

    init(
        assetRetriever: AssetRetriever? = nil,
        publicationOpener: PublicationOpener? = nil
    ) {
        let httpClient = DefaultHTTPClient()
        let retriever = assetRetriever ?? AssetRetriever(httpClient: httpClient)
        self.assetRetriever = retriever
        self.publicationOpener = publicationOpener ?? PublicationOpener(
            parser: DefaultPublicationParser(
                httpClient: httpClient,
                assetRetriever: retriever,
                pdfFactory: DefaultPDFDocumentFactory()
            )
        )
    }

    func loadText(from fileURL: URL) async throws -> BookText {
        let displayTitle = fileURL.deletingPathExtension().lastPathComponent
        guard let readiumURL = FileURL(url: fileURL) else {
            throw Errors.Publication.openFailed(
                title: displayTitle,
                underlying: "Invalid file URL"
            )
        }

        switch await assetRetriever.retrieve(url: readiumURL) {
        case .success(let asset):
            switch await publicationOpener.open(asset: asset, allowUserInteraction: false) {
            case .success(let publication):
                let title = publication.metadata.title ?? displayTitle
                guard let body = await publication.content()?.text(), !body.isEmpty else {
                    throw Errors.Publication.openFailed(
                        title: title,
                        underlying: "No text content"
                    )
                }
                return BookText(title: title, body: body)
            case .failure(let error):
                throw Errors.Publication.openFailed(
                    title: displayTitle,
                    underlying: String(describing: error)
                )
            }
        case .failure(let error):
            throw Errors.Publication.openFailed(
                title: displayTitle,
                underlying: String(describing: error)
            )
        }
    }
}

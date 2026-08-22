//
//  ReadiumServices.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import Foundation
import ReadiumShared
import ReadiumStreamer
import UIKit

struct OpenedPublication {
    let publication: Publication
    let title: String
    let author: String?
    let isPDF: Bool
    let cover: UIImage?
}

protocol PublicationOpeningServiceProtocol {
    func open(url: URL) async throws -> OpenedPublication
}

final class PublicationOpeningService: PublicationOpeningServiceProtocol {
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

    func open(url: URL) async throws -> OpenedPublication {
        let displayTitle = url.deletingPathExtension().lastPathComponent
        guard let readiumURL = FileURL(url: url) else {
            throw Errors.Publication.openFailed(title: displayTitle, underlying: "Invalid file URL")
        }

        switch await assetRetriever.retrieve(url: readiumURL) {
        case .success(let asset):
            switch await publicationOpener.open(asset: asset, allowUserInteraction: false) {
            case .success(let publication):
                let title = publication.metadata.title ?? displayTitle
                let author = publication.metadata.authors.first?.name
                let isPDF = publication.conforms(to: .pdf)
                let supportsReading = isPDF || publication.conforms(to: .epub) || publication.readingOrder.contains(where: {
                    $0.mediaType?.isHTML == true
                })
                guard supportsReading else {
                    throw Errors.Publication.unsupportedForReading(title: title)
                }
                let cover: UIImage?
                switch await publication.coverFitting(maxSize: CGSize(width: 400, height: 600)) {
                case .success(let image):
                    cover = image
                case .failure:
                    cover = nil
                }
                return OpenedPublication(
                    publication: publication,
                    title: title,
                    author: author,
                    isPDF: isPDF,
                    cover: cover
                )
            case .failure(let error):
                throw Errors.Publication.openFailed(title: displayTitle, underlying: String(describing: error))
            }
        case .failure(let error):
            throw Errors.Publication.openFailed(title: displayTitle, underlying: String(describing: error))
        }
    }
}

//
//  Photo.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import SwiftUI
import UIKit

/// Renders an image from either a local file or a remote URL, without imposing any size, shape, or
/// clipping — the caller owns layout. Book covers extracted from a publication are local files, so
/// that path is the common one; the remote path exists for future OPDS catalogues.
struct Photo: View {
    let url: URL?
    var isLoading: Bool = false
    var placeholderSystemImage: String = "book.closed"

    init(url: URL?, isLoading: Bool = false, placeholderSystemImage: String = "book.closed") {
        self.url = url
        self.isLoading = isLoading
        self.placeholderSystemImage = placeholderSystemImage
    }

    var body: some View {
        Color.clear
            .overlay {
                imageContent
            }
            .clipped()
    }

    @ViewBuilder
    private var imageContent: some View {
        if isLoading, url == nil {
            ProgressView()
                .accessibilityIdentifier(AccessibilityIdentifiers.Library.loading)
        } else if let image = localImage {
            Image(uiImage: image)
                .renderingMode(.original)
                .resizable()
                .scaledToFill()
        } else if let url, !url.isFileURL {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill()
                case .failure:
                    placeholder
                case .empty:
                    Color.clear
                @unknown default:
                    placeholder
                }
            }
        } else {
            placeholder
        }
    }

    private var localImage: UIImage? {
        guard let url, url.isFileURL else { return nil }
        return UIImage(contentsOfFile: url.path)
    }

    private var placeholder: some View {
        Image(systemName: placeholderSystemImage)
            .resizable()
            .scaledToFit()
            .padding(StyleConstants.contentMargin)
            .foregroundStyle(.tertiary)
    }
}

#Preview("Loading") {
    Photo(url: nil, isLoading: true)
        .frame(width: 110, height: 165)
}

#Preview("Missing cover") {
    Photo(url: nil)
        .frame(width: 110, height: 165)
}

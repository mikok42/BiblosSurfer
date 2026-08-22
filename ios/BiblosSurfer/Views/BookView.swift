//
//  BookView.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import SwiftUI

struct BookView: View {
    @State private var viewModel: BookViewModel
    var onClose: () -> Void

    init(viewModel: BookViewModel, onClose: @escaping () -> Void) {
        _viewModel = State(initialValue: viewModel)
        self.onClose = onClose
    }

    var body: some View {
        Group {
            if let error = viewModel.viewProperties.error {
                ErrorView(error: error, onDismiss: onClose)
            } else if viewModel.viewProperties.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    Text(viewModel.viewProperties.body)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(StyleConstants.contentMargin)
                        .accessibilityIdentifier(AccessibilityIdentifiers.Reader.container)
                }
            }
        }
        .navigationTitle(viewModel.viewProperties.title)
        .navigationBarTitleDisplayMode(.inline)
        .accessibilityIdentifier(AccessibilityIdentifiers.Reader.title)
        .task {
            await viewModel.load()
        }
    }
}

#Preview("Loaded") {
    NavigationStack {
        BookView(
            viewModel: BookViewModel(
                fileURL: URL(fileURLWithPath: "/tmp/sample.epub"),
                textService: PreviewBookTextService()
            ),
            onClose: {}
        )
    }
}

private struct PreviewBookTextService: PublicationTextServiceProtocol {
    func loadText(from fileURL: URL) async throws -> BookText {
        BookText(
            title: "The Sample Voyage",
            body: "The ship left harbour before dawn.\nNobody on board could name the sea they were crossing."
        )
    }
}

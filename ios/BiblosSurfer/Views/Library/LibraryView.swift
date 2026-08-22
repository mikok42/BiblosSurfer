//
//  LibraryView.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import SwiftUI
import UniformTypeIdentifiers

struct LibraryView: View {
    @State private var viewModel: LibraryViewModel
    var onSelect: (LibraryItem) -> Void
    @State private var isImporterPresented = false

    init(viewModel: LibraryViewModel, onSelect: @escaping (LibraryItem) -> Void) {
        _viewModel = State(initialValue: viewModel)
        self.onSelect = onSelect
    }

    var body: some View {
        if let error = viewModel.viewProperties.error {
            ErrorView(error: error) {
                Task { await viewModel.reload() }
            }
        } else {
            libraryContent
                .navigationTitle("Library")
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            isImporterPresented = true
                        } label: {
                            Image(systemName: "plus")
                        }
                        .accessibilityIdentifier(AccessibilityIdentifiers.Library.importButton)
                    }
                }
                .fileImporter(
                    isPresented: $isImporterPresented,
                    allowedContentTypes: [.epubPublication, .pdf],
                    allowsMultipleSelection: false
                ) { result in
                    switch result {
                    case .success(let urls):
                        guard let url = urls.first else { return }
                        Task { await viewModel.importBook(from: url) }
                    case .failure(let error):
                        viewModel.presentImportFailure(error)
                    }
                }
                .accessibilityIdentifier(AccessibilityIdentifiers.Library.title)
                .task {
                    guard viewModel.viewProperties.items.isEmpty else { return }
                    await viewModel.load()
                }
        }
    }

    @ViewBuilder
    private var libraryContent: some View {
        if viewModel.viewProperties.isLoading, viewModel.viewProperties.items.isEmpty {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .accessibilityIdentifier(AccessibilityIdentifiers.Library.loading)
        } else if viewModel.viewProperties.items.isEmpty {
            Text("No books yet")
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .accessibilityIdentifier(AccessibilityIdentifiers.Library.emptyState)
        } else {
            ScrollView {
                LazyVGrid(
                    columns: [
                        GridItem(
                            .adaptive(minimum: StyleConstants.coverGridMinWidth),
                            spacing: StyleConstants.coverGridSpacing
                        )
                    ],
                    spacing: StyleConstants.coverGridSpacing
                ) {
                    ForEach(viewModel.viewProperties.items) { item in
                        Button {
                            if let item = viewModel.itemToOpen(item) {
                                onSelect(item)
                            }
                        } label: {
                            VStack(alignment: .leading, spacing: StyleConstants.tightSpacing) {
                                ZStack(alignment: .bottom) {
                                    Photo(url: item.coverURL)
                                        .aspectRatio(StyleConstants.coverAspectRatio, contentMode: .fit)
                                        .background(Color.secondary.opacity(0.12))
                                        .clipShape(RoundedRectangle(cornerRadius: StyleConstants.cornerRadius, style: .continuous))
                                    GeometryReader { proxy in
                                        Rectangle()
                                            .fill(Color.accentColor)
                                            .frame(width: proxy.size.width * item.progression, height: StyleConstants.progressBarHeight)
                                            .frame(maxHeight: .infinity, alignment: .bottom)
                                    }
                                    .clipShape(RoundedRectangle(cornerRadius: StyleConstants.cornerRadius, style: .continuous))
                                }
                                Text(item.title)
                                    .font(.caption)
                                    .foregroundStyle(.primary)
                                    .lineLimit(2)
                                    .multilineTextAlignment(.leading)
                            }
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier(AccessibilityIdentifiers.Library.cell(item.title))
                    }
                }
                .padding(StyleConstants.contentMargin)
            }
            .accessibilityIdentifier(AccessibilityIdentifiers.Library.grid)
        }
    }
}

#Preview("Stub library") {
    NavigationStack {
        LibraryView(viewModel: LibraryViewModel(libraryService: MockServiceProvider().libraryService), onSelect: { _ in })
    }
}

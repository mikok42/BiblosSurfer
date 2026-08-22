//
//  ErrorView.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import SwiftUI

struct ErrorView: View {
    private let error: DescriptiveError
    private let onDismiss: (() -> Void)?

    init(error: DescriptiveError, onDismiss: (() -> Void)? = nil) {
        self.error = error
        self.onDismiss = onDismiss
    }

    var body: some View {
        VStack(spacing: StyleConstants.stackSpacing) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
            Text(error.title)
                .font(.headline)
                .accessibilityIdentifier(AccessibilityIdentifiers.Error.title)
            Text(error.description)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .accessibilityIdentifier(AccessibilityIdentifiers.Error.description)
            Button("Dismiss") {
                onDismiss?()
            }
            .buttonStyle(.borderedProminent)
            .accessibilityIdentifier(AccessibilityIdentifiers.Error.dismiss)
        }
        .padding(StyleConstants.contentMargin)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#Preview("Unsupported format") {
    ErrorView(error: Errors.Library.unsupportedFormat(fileExtension: "mobi"))
}

#Preview("Nothing to read aloud") {
    ErrorView(error: Errors.TTS.noSpeakableContent)
}

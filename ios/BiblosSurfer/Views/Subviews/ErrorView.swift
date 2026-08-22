//
//  ErrorView.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import Observation
import SwiftUI

protocol ErrorDismissing: AnyObject {
    func dismissError()
}

protocol ErrorRouting: AnyObject {
    func dismissError()
}

@Observable
final class ErrorViewModel: ErrorDismissing {
    let error: DescriptiveError
    weak var router: ErrorRouting?

    init(error: DescriptiveError) {
        self.error = error
    }

    func dismissError() {
        router?.dismissError()
    }
}

struct ErrorView: View {
    private let error: DescriptiveError
    private let handler: ErrorDismissing?

    init(error: DescriptiveError, handler: ErrorDismissing? = nil) {
        self.error = error
        self.handler = handler
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
                handler?.dismissError()
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

//
//  TTSPanelView.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import SwiftUI

struct TTSPanelView: View {
    @Bindable var viewModel: ReaderViewModel
    var onPlayPause: () -> Void
    var onStop: () -> Void
    var onNext: () -> Void
    var onPrevious: () -> Void

    var body: some View {
        HStack(spacing: StyleConstants.stackSpacing) {
            Button(action: onPrevious) {
                Image(systemName: "backward.fill")
            }
            .accessibilityIdentifier(AccessibilityIdentifiers.Reader.ttsPrevious)

            Button(action: onPlayPause) {
                Image(systemName: viewModel.viewProperties.isPlaying ? "pause.fill" : "play.fill")
            }
            .accessibilityIdentifier(
                viewModel.viewProperties.isPlaying
                    ? AccessibilityIdentifiers.Reader.ttsPause
                    : AccessibilityIdentifiers.Reader.ttsPlay
            )

            Button(action: onStop) {
                Image(systemName: "stop.fill")
            }
            .accessibilityIdentifier(AccessibilityIdentifiers.Reader.ttsStop)

            Button(action: onNext) {
                Image(systemName: "forward.fill")
            }
            .accessibilityIdentifier(AccessibilityIdentifiers.Reader.ttsNext)
        }
        .font(.title2)
        .padding(StyleConstants.ttsPanelPadding)
        .frame(maxWidth: .infinity)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: StyleConstants.ttsPanelCornerRadius, style: .continuous))
        .padding(StyleConstants.contentMargin)
        .accessibilityIdentifier(AccessibilityIdentifiers.Reader.ttsPanel)
    }
}

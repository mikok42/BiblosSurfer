//
//  StyleConstants.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import CoreGraphics

/// Layout metrics only. Reading typography is not here on purpose — font family, size, and theme
/// belong to the user and are driven through the Readium Preferences API in `ReaderSettings`.
struct StyleConstants {
    static let contentMargin: CGFloat = 16
    static let stackSpacing: CGFloat = 12
    static let tightSpacing: CGFloat = 4

    static let cornerRadius: CGFloat = 8
    static let coverAspectRatio: CGFloat = 2.0 / 3.0
    static let coverGridMinWidth: CGFloat = 110
    static let coverGridSpacing: CGFloat = 16

    static let progressBarHeight: CGFloat = 3
    static let ttsPanelCornerRadius: CGFloat = 20
    static let ttsPanelPadding: CGFloat = 12
}

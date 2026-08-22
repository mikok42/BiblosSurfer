//
//  ReaderView.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import UIKit

final class ReaderView: UIView {
    private weak var ttsPanelView: UIView?

    init(navigatorView: UIView) {
        super.init(frame: .zero)
        backgroundColor = .systemBackground
        accessibilityIdentifier = AccessibilityIdentifiers.Reader.container
        navigatorView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(navigatorView)
        NSLayoutConstraint.activate([
            navigatorView.topAnchor.constraint(equalTo: topAnchor),
            navigatorView.leadingAnchor.constraint(equalTo: leadingAnchor),
            navigatorView.trailingAnchor.constraint(equalTo: trailingAnchor),
            navigatorView.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func setTTSPanel(_ panelView: UIView?) {
        ttsPanelView?.removeFromSuperview()
        ttsPanelView = panelView
        guard let panelView else { return }
        panelView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(panelView)
        NSLayoutConstraint.activate([
            panelView.leadingAnchor.constraint(equalTo: leadingAnchor),
            panelView.trailingAnchor.constraint(equalTo: trailingAnchor),
            panelView.bottomAnchor.constraint(equalTo: safeAreaLayoutGuide.bottomAnchor)
        ])
    }
}

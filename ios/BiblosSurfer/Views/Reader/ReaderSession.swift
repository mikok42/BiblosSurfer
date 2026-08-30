//
//  ReaderSession.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 30/08/2026.
//

import ReadiumNavigator
import UIKit

protocol ReaderPreferencesApplying: AnyObject {
    func submitReaderPreferences(_ settings: ReaderSettingsStore)
}

extension EPUBNavigatorViewController: ReaderPreferencesApplying {
    func submitReaderPreferences(_ settings: ReaderSettingsStore) {
        submitPreferences(settings.epubPreferences())
    }
}

extension PDFNavigatorViewController: ReaderPreferencesApplying {
    func submitReaderPreferences(_ settings: ReaderSettingsStore) {
        submitPreferences(settings.pdfPreferences())
    }
}

struct ReaderSession {
    let navigatorController: UIViewController
    let visualNavigator: VisualNavigator
    let epubNavigator: EPUBNavigatorViewController?
    let pdfNavigator: PDFNavigatorViewController?
    let preferences: ReaderPreferencesApplying

    func bindDelegate(_ delegate: EPUBNavigatorDelegate & PDFNavigatorDelegate) {
        epubNavigator?.delegate = delegate
        pdfNavigator?.delegate = delegate
    }
}

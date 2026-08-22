//
//  Analytics.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import Foundation
import OSLog

enum AnalyticsEvent {
    static let importBook = "import_book"
    static let openingPublication = "opening_publication"
    static let ttsStartLatency = "tts_start_latency"
}

/// Measures a span and reports its duration. `onReport` is injected so tests observe the
/// measurement without a analytics backend, and so swapping in Firebase later touches one line.
class AnalyticsTimer {
    internal let reportName: String
    private let onReport: (String, TimeInterval) -> Void

    private var startTime: TimeInterval = 0
    private var endTime: TimeInterval = 0
    private var duration: TimeInterval { endTime - startTime }

    init(
        reportName: String,
        onReport: @escaping (String, TimeInterval) -> Void = { name, duration in
            Logger(subsystem: "miko.BiblosSurfer", category: "analytics")
                .info("\(name, privacy: .public) duration_ms=\(duration, privacy: .public)")
        }
    ) {
        self.reportName = reportName
        self.onReport = onReport
    }

    func startTimer() {
        startTime = (Double(DispatchTime.now().uptimeNanoseconds) / 1000000.0)
    }

    func endTimer() {
        endTime = (Double(DispatchTime.now().uptimeNanoseconds) / 1000000.0)
    }

    func reportToAnalytics() {
        onReport(reportName, duration)
    }
}

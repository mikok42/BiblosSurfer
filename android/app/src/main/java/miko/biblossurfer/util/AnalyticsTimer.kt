package miko.biblossurfer.util

import android.os.SystemClock
import android.util.Log

object AnalyticsEvent {
    const val importBook = "import_book"
    const val openingPublication = "opening_publication"
    const val ttsStartLatency = "tts_start_latency"
}

/**
 * Measures a span and reports its duration. [onReport] is injected so tests observe the
 * measurement without an analytics backend, and so swapping in Firebase later touches one line.
 */
class AnalyticsTimer(
    val reportName: String,
    private val onReport: (String, Double) -> Unit = { name, duration ->
        Log.i("analytics", "$name duration_ms=$duration")
    },
) {
    private var startTime: Double = 0.0
    private var endTime: Double = 0.0
    val duration: Double get() = endTime - startTime

    fun startTimer() {
        startTime = SystemClock.elapsedRealtime().toDouble()
    }

    fun endTimer() {
        endTime = SystemClock.elapsedRealtime().toDouble()
    }

    fun reportToAnalytics() {
        onReport(reportName, duration)
    }
}

package miko.biblossurfer.data.tts

import miko.biblossurfer.data.overlapsCollapsedText

/**
 * Skips utterances that sit before a "read from here" highlight. Readium's
 * selection locator is the current *page*, so TTS would otherwise start at
 * the top of the resource.
 */
class TtsStartAnchor {
    var highlight: String? = null
    private var skippedUnmatched = false

    fun resetSkip() {
        skippedUnmatched = false
    }

    fun shouldSpeak(utteranceText: String): Boolean {
        val target = highlight ?: return true
        if (utteranceText.overlapsCollapsedText(target)) {
            highlight = null
            return true
        }
        // Page progression starts one block early. Skip that verse once;
        // if the selection still does not match, speak anyway so the panel
        // does not vanish.
        if (!skippedUnmatched) {
            skippedUnmatched = true
            return false
        }
        highlight = null
        return true
    }
}

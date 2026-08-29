package miko.biblossurfer.util

object AccessibilityIdentifiers {
    object Library {
        const val title = "library.title"
        const val importButton = "library.importButton"
        const val grid = "library.grid"
        const val emptyState = "library.emptyState"
        const val loading = "library.loading"

        fun cell(bookTitle: String): String = "library.cell.$bookTitle"
    }

    object Reader {
        const val container = "reader.container"
        const val title = "reader.title"
        const val close = "reader.close"
        const val settings = "reader.settings"
        const val progress = "reader.progress"
        const val ttsPlay = "reader.ttsPlay"
        const val ttsPause = "reader.ttsPause"
        const val ttsStop = "reader.ttsStop"
        const val ttsNext = "reader.ttsNext"
        const val ttsPrevious = "reader.ttsPrevious"
        const val ttsPanel = "reader.ttsPanel"
    }

    object Settings {
        const val fontSize = "settings.fontSize"
        const val fontFamily = "settings.fontFamily"
        const val theme = "settings.theme"
        const val scrollMode = "settings.scrollMode"
        const val tts = "settings.tts"
        const val voice = "settings.voice"
        const val speechRate = "settings.speechRate"
        const val pitch = "settings.pitch"
        const val volume = "settings.volume"
        const val preUtteranceDelay = "settings.preUtteranceDelay"
        const val postUtteranceDelay = "settings.postUtteranceDelay"
        const val language = "settings.language"
        const val chunkUnit = "settings.chunkUnit"
        const val systemSpeech = "settings.systemSpeech"
        const val done = "settings.done"
    }

    object Error {
        const val title = "error.title"
        const val description = "error.description"
        const val dismiss = "error.dismiss"
    }
}

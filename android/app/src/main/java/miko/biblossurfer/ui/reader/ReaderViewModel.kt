package miko.biblossurfer.ui.reader

import miko.biblossurfer.data.DescriptiveError
import miko.biblossurfer.data.ReaderSettingsStore
import miko.biblossurfer.data.model.PublicationFormat
import miko.biblossurfer.data.tts.TtsPlaybackState
import miko.biblossurfer.data.tts.TtsVoiceInfo
import miko.biblossurfer.data.tts.filterByLanguage
import miko.biblossurfer.data.tts.rankedByAppleQuality
import miko.biblossurfer.ui.library.ErrorDismissing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface ReaderActions {
    fun playPauseTTS()
    fun stopTTS()
    fun nextUtterance()
    fun previousUtterance()
    fun applyReaderSettings()
    fun closeSettings()
    fun dismissPresentedError()
}

data class ReaderViewState(
    val title: String = "",
    val isPlaying: Boolean = false,
    val canSpeak: Boolean = false,
    val format: PublicationFormat = PublicationFormat.UNKNOWN,
    val error: DescriptiveError? = null,
    val showSettings: Boolean = false,
    val showTtsPanel: Boolean = false,
)

class ReaderViewModel(
    title: String,
    format: PublicationFormat,
    canSpeak: Boolean,
    val settings: ReaderSettingsStore,
) : ErrorDismissing {
    var actions: ReaderActions? = null
    var availableVoices: List<TtsVoiceInfo> = emptyList()
    var settingsEpoch: Int = 0
        private set

    private val _viewProperties = MutableStateFlow(
        ReaderViewState(
            title = title,
            canSpeak = canSpeak && format == PublicationFormat.EPUB,
            format = format,
        )
    )
    val viewProperties: StateFlow<ReaderViewState> = _viewProperties.asStateFlow()

    fun apply(ttsState: TtsPlaybackState) {
        _viewProperties.update { state ->
            when (ttsState) {
                TtsPlaybackState.STOPPED -> state.copy(isPlaying = false, showTtsPanel = false)
                TtsPlaybackState.PAUSED -> state.copy(isPlaying = false, showTtsPanel = true)
                TtsPlaybackState.PLAYING -> state.copy(isPlaying = true, showTtsPanel = true)
            }
        }
    }

    fun presentError(error: DescriptiveError) {
        _viewProperties.update { it.copy(error = error) }
    }

    fun playPause() {
        actions?.playPauseTTS()
    }

    fun stopReading() {
        actions?.stopTTS()
    }

    fun nextUtterance() {
        actions?.nextUtterance()
    }

    fun previousUtterance() {
        actions?.previousUtterance()
    }

    fun ttsLanguages(): List<String> {
        val seen = linkedSetOf<String>()
        for (voice in availableVoices) {
            seen.add(voice.languageWithoutRegion)
        }
        return seen.sorted()
    }

    fun ttsVoices(): List<TtsVoiceInfo> {
        val code = settings.defaultLanguage ?: return availableVoices.rankedByAppleQuality()
        return availableVoices.filterByLanguage(code).rankedByAppleQuality()
    }

    fun selectTTSLanguage(code: String?) {
        settings.defaultLanguage = code
        if (code != null) {
            val voiceId = settings.voiceIdentifier
            val voice = availableVoices.firstOrNull { it.identifier == voiceId }
            if (voice != null && voice.languageWithoutRegion != code.lowercase().substringBefore("-")) {
                settings.voiceIdentifier = null
            }
        }
        settingsDidChange()
    }

    fun settingsDidChange() {
        settingsEpoch += 1
        actions?.applyReaderSettings()
    }

    fun openSettings() {
        _viewProperties.update { it.copy(showSettings = true) }
    }

    fun closeSettings() {
        _viewProperties.update { it.copy(showSettings = false) }
        actions?.closeSettings()
    }

    override fun dismissError() {
        _viewProperties.update { it.copy(error = null) }
        actions?.dismissPresentedError()
    }
}

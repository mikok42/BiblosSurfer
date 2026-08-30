package miko.biblossurfer.data.tts

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import miko.biblossurfer.data.DescriptiveError
import miko.biblossurfer.data.Errors
import miko.biblossurfer.data.ReaderSettingsStore
import org.readium.navigator.media.tts.AndroidTtsNavigator
import org.readium.navigator.media.tts.AndroidTtsNavigatorFactory
import org.readium.navigator.media.tts.TtsNavigator
import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.navigator.media.tts.android.AndroidTtsPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.tokenizer.DefaultTextContentTokenizer

interface TtsServiceDelegate {
    fun ttsServiceDidChange(isPlaying: Boolean, utteranceLocator: Locator?, tokenLocator: Locator?)
    fun ttsServiceDidFail(error: DescriptiveError)
}

enum class TtsPlaybackState {
    STOPPED,
    PAUSED,
    PLAYING,
}

@OptIn(ExperimentalReadiumApi::class)
class TtsController(
    private val application: Application,
    publication: Publication,
    private val settings: ReaderSettingsStore,
    private val bookTitle: String,
    private val scope: CoroutineScope,
) : TtsNavigator.Listener {
    private val factory: AndroidTtsNavigatorFactory? = AndroidTtsNavigatorFactory(
        application,
        publication,
        tokenizerFactory = { language ->
            DefaultTextContentTokenizer(unit = settings.chunkUnit.textUnit, language = language)
        },
    )

    private var navigator: AndroidTtsNavigator? = null
    private var playbackJob: Job? = null
    private var locationJob: Job? = null
    var delegate: TtsServiceDelegate? = null

    val availableVoices: List<TtsVoiceInfo>
        get() = navigator?.voices.orEmpty().map { it.toVoiceInfo() }

    val canSpeak: Boolean get() = factory != null

    var state: TtsPlaybackState = TtsPlaybackState.STOPPED
        private set

    suspend fun start(from: Locator?) {
        val factory = factory ?: run {
            delegate?.ttsServiceDidFail(Errors.Tts.NoSpeakableContent)
            return
        }
        closeNavigator()
        val created = factory.createNavigator(
            listener = this,
            initialLocator = from,
            initialPreferences = ttsPreferences(),
        ).getOrElse { error ->
            delegate?.ttsServiceDidFail(Errors.Tts.EngineFailed(error.toString()))
            return
        }
        navigator = created
        observe(created)
        created.play()
        state = TtsPlaybackState.PLAYING
        delegate?.ttsServiceDidChange(true, from, null)
    }

    fun applySettings() {
        navigator?.submitPreferences(ttsPreferences())
    }

    fun stop() {
        navigator?.pause()
        closeNavigator()
        state = TtsPlaybackState.STOPPED
        delegate?.ttsServiceDidChange(false, null, null)
    }

    fun pause() {
        navigator?.pause()
        state = TtsPlaybackState.PAUSED
        delegate?.ttsServiceDidChange(false, null, null)
    }

    fun resume() {
        navigator?.play()
        state = TtsPlaybackState.PLAYING
        delegate?.ttsServiceDidChange(true, null, null)
    }

    fun pauseOrResume() {
        when (state) {
            TtsPlaybackState.PLAYING -> pause()
            TtsPlaybackState.PAUSED -> resume()
            TtsPlaybackState.STOPPED -> Unit
        }
    }

    fun next() {
        navigator?.skipToNextUtterance()
    }

    fun previous() {
        navigator?.skipToPreviousUtterance()
    }

    override fun onStopRequested() {
        stop()
    }

    fun close() {
        closeNavigator()
    }

    private fun observe(nav: AndroidTtsNavigator) {
        playbackJob?.cancel()
        locationJob?.cancel()
        playbackJob = nav.playback
            .onEach { playback ->
                when (val playbackState = playback.state) {
                    is TtsNavigator.State.Ended -> stop()
                    is TtsNavigator.State.Failure -> {
                        val engine = (playbackState.error as? TtsNavigator.Error.EngineError<*>)?.cause
                        if (engine is AndroidTtsEngine.Error.LanguageMissingData) {
                            AndroidTtsEngine.requestInstallVoice(application)
                        }
                        delegate?.ttsServiceDidFail(Errors.Tts.EngineFailed(playbackState.error.toString()))
                    }
                    else -> Unit
                }
            }
            .launchIn(scope)
        locationJob = nav.location
            .onEach { location ->
                val playing = state == TtsPlaybackState.PLAYING
                delegate?.ttsServiceDidChange(playing, location.utteranceLocator, location.tokenLocator)
            }
            .launchIn(scope)
    }

    private fun closeNavigator() {
        playbackJob?.cancel()
        locationJob?.cancel()
        navigator?.close()
        navigator = null
    }

    private fun ttsPreferences(): AndroidTtsPreferences {
        val language = settings.defaultLanguage?.let { Language(it) }
        val voices = buildMap {
            val voiceId = settings.voiceIdentifier
            if (voiceId != null && language != null) {
                put(language.removeRegion(), AndroidTtsEngine.Voice.Id(voiceId))
            }
        }
        return if (settings.useSystemSpeechSettings) {
            AndroidTtsPreferences(language = language, voices = voices)
        } else {
            AndroidTtsPreferences(
                language = language,
                voices = voices,
                pitch = settings.pitchMultiplier.toDouble(),
                speed = settings.speechRate.toDouble(),
            )
        }
    }
}

@OptIn(ExperimentalReadiumApi::class)
fun AndroidTtsEngine.Voice.toVoiceInfo(): TtsVoiceInfo =
    TtsVoiceInfo(
        identifier = id.value,
        name = id.value,
        languageTag = language.code,
        quality = when (quality) {
            AndroidTtsEngine.Voice.Quality.Highest -> 500
            AndroidTtsEngine.Voice.Quality.High -> 400
            AndroidTtsEngine.Voice.Quality.Normal -> 300
            AndroidTtsEngine.Voice.Quality.Low -> 100
            AndroidTtsEngine.Voice.Quality.Lowest -> 50
        },
        requiresNetwork = requiresNetwork,
    )

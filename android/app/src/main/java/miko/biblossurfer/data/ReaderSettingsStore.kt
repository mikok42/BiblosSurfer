package miko.biblossurfer.data

import android.content.SharedPreferences
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.util.tokenizer.TextUnit

enum class TTSChunkUnit {
    WORD,
    SENTENCE,
    PARAGRAPH;

    val textUnit: TextUnit
        get() = when (this) {
            WORD -> TextUnit.Word
            SENTENCE -> TextUnit.Sentence
            PARAGRAPH -> TextUnit.Paragraph
        }

    val title: String
        get() = when (this) {
            WORD -> "Word"
            SENTENCE -> "Sentence"
            PARAGRAPH -> "Paragraph"
        }

    val storageValue: String
        get() = when (this) {
            WORD -> "word"
            SENTENCE -> "sentence"
            PARAGRAPH -> "paragraph"
        }

    companion object {
        val allCases: List<TTSChunkUnit> = entries

        fun fromStorage(value: String?): TTSChunkUnit =
            entries.firstOrNull { it.storageValue == value } ?: SENTENCE
    }
}

class ReaderSettingsStore(
    private val defaults: SharedPreferences,
) {
    var fontSize: Double
        get() = defaults.getFloat(KEY_FONT_SIZE, 1.0f).toDouble()
        set(value) { defaults.edit().putFloat(KEY_FONT_SIZE, value.toFloat()).apply() }

    var fontFamily: String
        get() = defaults.getString(KEY_FONT_FAMILY, "Original") ?: "Original"
        set(value) { defaults.edit().putString(KEY_FONT_FAMILY, value).apply() }

    var theme: Theme
        get() = Theme.entries.firstOrNull {
            it.name.equals(defaults.getString(KEY_THEME, "") ?: "", ignoreCase = true)
        } ?: Theme.LIGHT
        set(value) { defaults.edit().putString(KEY_THEME, value.name.lowercase()).apply() }

    var scroll: Boolean
        get() = defaults.getBoolean(KEY_SCROLL, false)
        set(value) { defaults.edit().putBoolean(KEY_SCROLL, value).apply() }

    var voiceIdentifier: String?
        get() = defaults.getString(KEY_VOICE, null)
        set(value) { defaults.edit().putString(KEY_VOICE, value).apply() }

    var speechRate: Float
        get() = if (defaults.contains(KEY_SPEECH_RATE)) defaults.getFloat(KEY_SPEECH_RATE, 1.0f) else 1.0f
        set(value) { defaults.edit().putFloat(KEY_SPEECH_RATE, value).apply() }

    var pitchMultiplier: Float
        get() = if (defaults.contains(KEY_PITCH)) defaults.getFloat(KEY_PITCH, 1.0f) else 1.0f
        set(value) { defaults.edit().putFloat(KEY_PITCH, value).apply() }

    var speechVolume: Float
        get() = if (defaults.contains(KEY_VOLUME)) defaults.getFloat(KEY_VOLUME, 1.0f) else 1.0f
        set(value) { defaults.edit().putFloat(KEY_VOLUME, value).apply() }

    var preUtteranceDelay: Double
        get() = if (defaults.contains(KEY_PRE_DELAY)) defaults.getFloat(KEY_PRE_DELAY, 0f).toDouble() else 0.0
        set(value) { defaults.edit().putFloat(KEY_PRE_DELAY, value.toFloat()).apply() }

    var postUtteranceDelay: Double
        get() = if (defaults.contains(KEY_POST_DELAY)) defaults.getFloat(KEY_POST_DELAY, 0f).toDouble() else 0.0
        set(value) { defaults.edit().putFloat(KEY_POST_DELAY, value.toFloat()).apply() }

    var defaultLanguage: String?
        get() = defaults.getString(KEY_LANGUAGE, null)
        set(value) { defaults.edit().putString(KEY_LANGUAGE, value).apply() }

    var chunkUnit: TTSChunkUnit
        get() = TTSChunkUnit.fromStorage(defaults.getString(KEY_CHUNK, null))
        set(value) { defaults.edit().putString(KEY_CHUNK, value.storageValue).apply() }

    var useSystemSpeechSettings: Boolean
        get() = defaults.getBoolean(KEY_SYSTEM_SPEECH, false)
        set(value) { defaults.edit().putBoolean(KEY_SYSTEM_SPEECH, value).apply() }

    fun epubPreferences(): EpubPreferences {
        var preferences = EpubPreferences(
            fontSize = fontSize,
            scroll = scroll,
            theme = theme,
        )
        if (fontFamily != "Original") {
            preferences = preferences.copy(
                fontFamily = FontFamily(fontFamily),
                publisherStyles = false,
            )
        }
        return preferences
    }

    companion object {
        const val KEY_FONT_SIZE = "reader.fontSize"
        const val KEY_FONT_FAMILY = "reader.fontFamily"
        const val KEY_THEME = "reader.theme"
        const val KEY_SCROLL = "reader.scroll"
        const val KEY_VOICE = "reader.voiceIdentifier"
        const val KEY_SPEECH_RATE = "reader.speechRate"
        const val KEY_PITCH = "reader.pitchMultiplier"
        const val KEY_VOLUME = "reader.speechVolume"
        const val KEY_PRE_DELAY = "reader.preUtteranceDelay"
        const val KEY_POST_DELAY = "reader.postUtteranceDelay"
        const val KEY_LANGUAGE = "reader.defaultLanguage"
        const val KEY_CHUNK = "reader.chunkUnit"
        const val KEY_SYSTEM_SPEECH = "reader.useSystemSpeechSettings"
    }
}

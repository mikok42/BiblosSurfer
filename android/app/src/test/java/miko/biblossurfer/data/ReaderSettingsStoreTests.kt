package miko.biblossurfer.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.preferences.Theme
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ReaderSettingsStoreTests {
    @Test
    fun preferencesMapFontSizeThemeAndScroll() {
        val defaults = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(UUID.randomUUID().toString(), Context.MODE_PRIVATE)
        val store = ReaderSettingsStore(defaults)
        store.fontSize = 1.3
        store.theme = Theme.SEPIA
        store.scroll = true
        store.fontFamily = "Georgia"

        val preferences = store.epubPreferences()
        assertEquals(1.3, preferences.fontSize)
        assertEquals(Theme.SEPIA, preferences.theme)
        assertEquals(true, preferences.scroll)
        assertEquals("Georgia", preferences.fontFamily?.name)
        assertEquals(false, preferences.publisherStyles)
    }

    @Test
    fun scrollDefaultsToEnabledWhenUnset() {
        val defaults = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(UUID.randomUUID().toString(), Context.MODE_PRIVATE)
        val store = ReaderSettingsStore(defaults)
        assertTrue(store.scroll)
        assertEquals(true, store.epubPreferences().scroll)
        assertEquals(
            org.readium.r2.navigator.preferences.Axis.VERTICAL,
            store.pdfPreferences().scrollAxis,
        )
    }

    @Test
    fun scrollCanBeDisabled() {
        val defaults = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(UUID.randomUUID().toString(), Context.MODE_PRIVATE)
        val store = ReaderSettingsStore(defaults)
        store.scroll = false
        assertFalse(store.scroll)
        assertEquals(false, store.epubPreferences().scroll)
    }

    @Test
    fun ttsSettingsUseEngineDefaults() {
        val defaults = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(UUID.randomUUID().toString(), Context.MODE_PRIVATE)
        val store = ReaderSettingsStore(defaults)
        assertEquals(1.0f, store.pitchMultiplier)
        assertEquals(1.0f, store.speechVolume)
        assertEquals(0.0, store.preUtteranceDelay, 0.0)
        assertEquals(0.0, store.postUtteranceDelay, 0.0)
        assertNull(store.defaultLanguage)
        assertEquals(TTSChunkUnit.SENTENCE, store.chunkUnit)
        assertFalse(store.useSystemSpeechSettings)
    }

    @Test
    fun ttsSettingsRoundTrip() {
        val defaults = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(UUID.randomUUID().toString(), Context.MODE_PRIVATE)
        val store = ReaderSettingsStore(defaults)
        store.pitchMultiplier = 1.4f
        store.speechVolume = 0.6f
        store.preUtteranceDelay = 0.3
        store.postUtteranceDelay = 0.5
        store.defaultLanguage = "pl"
        store.chunkUnit = TTSChunkUnit.WORD
        store.useSystemSpeechSettings = true

        assertEquals(1.4f, store.pitchMultiplier)
        assertEquals(0.6f, store.speechVolume, 0.0f)
        assertEquals(0.3, store.preUtteranceDelay, 0.001)
        assertEquals(0.5, store.postUtteranceDelay, 0.001)
        assertEquals("pl", store.defaultLanguage)
        assertEquals(TTSChunkUnit.WORD, store.chunkUnit)
        assertTrue(store.useSystemSpeechSettings)
    }
}

class TtsHighlightMatchingTests {
    @Test
    fun collapsedWhitespaceOverlapsSelection() {
        val sentence = "  History of Egypt,\nChaldæa, Syria.  "
        assertTrue(sentence.overlapsCollapsedText("Egypt, Chaldæa"))
        assertTrue("Egypt".overlapsCollapsedText(sentence))
        assertFalse(sentence.overlapsCollapsedText("Babylonia"))
    }
}

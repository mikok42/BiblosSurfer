package miko.biblossurfer.data.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsVoiceQualityTests {
    @Test
    fun superCompactBeatsCompactEvenWhenReadiumRanksItLower() {
        val compact = TtsVoiceInfo(
            identifier = "com.apple.voice.compact.en-US.Samantha",
            name = "Samantha",
            languageTag = "en-US",
            quality = 100,
        )
        val neural = TtsVoiceInfo(
            identifier = "com.apple.voice.super-compact.en-US.Samantha",
            name = "Samantha",
            languageTag = "en-US",
            quality = 50,
        )
        val enhanced = TtsVoiceInfo(
            identifier = "com.apple.voice.enhanced.en-US.Samantha",
            name = "Samantha",
            languageTag = "en-US",
            quality = 400,
        )

        assertEquals(AndroidTtsVoiceTier.COMPACT, compact.appleTier)
        assertEquals(AndroidTtsVoiceTier.NEURAL, neural.appleTier)
        assertEquals(AndroidTtsVoiceTier.ENHANCED, enhanced.appleTier)
        assertEquals(
            enhanced.identifier,
            listOf(compact, neural, enhanced).preferredAppleVoice(null)?.identifier,
        )
        assertEquals(
            listOf(AndroidTtsVoiceTier.NEURAL, AndroidTtsVoiceTier.COMPACT),
            listOf(compact, neural).rankedByAppleQuality().map { it.appleTier },
        )
    }

    @Test
    fun preferredVoiceMatchesLanguage() {
        val english = TtsVoiceInfo(
            identifier = "com.apple.voice.super-compact.en-US.Samantha",
            name = "Samantha",
            languageTag = "en-US",
            quality = 50,
        )
        val polish = TtsVoiceInfo(
            identifier = "com.apple.voice.compact.pl-PL.Zosia",
            name = "Zosia",
            languageTag = "pl-PL",
            quality = 100,
        )
        assertEquals(
            polish.identifier,
            listOf(english, polish).preferredAppleVoice("pl")?.identifier,
        )
        assertTrue(english.settingsDisplayName.contains("Neural"))
        assertTrue(polish.settingsDisplayName.contains("Compact"))
    }
}

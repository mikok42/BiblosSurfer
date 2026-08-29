package miko.biblossurfer.data.tts

/**
 * Android analogue of iOS `TTSVoice+AppleQuality`. Android voices do not use Apple compact /
 * neural / premium identifiers, so ranking uses engine quality plus network vs local.
 */
enum class AndroidTtsVoiceTier(val rank: Int) {
    COMPACT(0),
    STANDARD(1),
    NEURAL(2),
    ENHANCED(3),
    PREMIUM(4);

    val title: String
        get() = when (this) {
            COMPACT -> "Compact"
            STANDARD -> "Standard"
            NEURAL -> "Neural"
            ENHANCED -> "Enhanced"
            PREMIUM -> "Premium"
        }
}

data class TtsVoiceInfo(
    val identifier: String,
    val name: String,
    val languageTag: String,
    val quality: Int,
    val requiresNetwork: Boolean = false,
) {
    val appleTier: AndroidTtsVoiceTier
        get() {
            val id = identifier.lowercase()
            if (id.contains("super-compact")) return AndroidTtsVoiceTier.NEURAL
            if (quality >= 500 || id.contains("premium")) return AndroidTtsVoiceTier.PREMIUM
            if (quality >= 400 || id.contains("enhanced") || requiresNetwork) return AndroidTtsVoiceTier.ENHANCED
            if (id.contains("compact") || quality <= 100) return AndroidTtsVoiceTier.COMPACT
            return AndroidTtsVoiceTier.STANDARD
        }

    val settingsDisplayName: String
        get() = "$name · ${appleTier.title}"

    val languageWithoutRegion: String
        get() = languageTag.substringBefore("-").substringBefore("_").lowercase()
}

fun List<TtsVoiceInfo>.rankedByAppleQuality(): List<TtsVoiceInfo> =
    sortedWith(
        compareByDescending<TtsVoiceInfo> { it.appleTier.rank }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    )

fun List<TtsVoiceInfo>.filterByLanguage(languageTag: String): List<TtsVoiceInfo> {
    val needle = languageTag.substringBefore("-").substringBefore("_").lowercase()
    return filter { it.languageWithoutRegion == needle }
}

fun List<TtsVoiceInfo>.preferredAppleVoice(languageTag: String?): TtsVoiceInfo? {
    val candidates = if (languageTag != null) {
        val matching = filterByLanguage(languageTag)
        matching.ifEmpty { this }
    } else {
        this
    }
    return candidates.rankedByAppleQuality().firstOrNull()
}

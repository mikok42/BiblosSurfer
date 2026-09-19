package miko.biblossurfer.data

import org.readium.r2.shared.publication.Locator

/**
 * Collapses whitespace the way iOS `String.collapsedWhitespace` does so a "read from here"
 * selection can match a spoken chunk even when the locator spans a page.
 */
fun String.collapsedWhitespace(): String =
    split(Regex("""\s+""")).filter { it.isNotEmpty() }.joinToString(" ")

fun String.overlapsCollapsedText(other: String): Boolean {
    val haystack = collapsedWhitespace()
    val needle = other.collapsedWhitespace()
    if (haystack.isEmpty() || needle.isEmpty()) return false
    if (haystack.contains(needle, ignoreCase = true) || needle.contains(haystack, ignoreCase = true)) {
        return true
    }
    val haystackPlain = haystack.strippingTTSDigits()
    val needlePlain = needle.strippingTTSDigits()
    if (haystackPlain.isEmpty() || needlePlain.isEmpty()) return false
    return haystackPlain.contains(needlePlain, ignoreCase = true) ||
        needlePlain.contains(haystackPlain, ignoreCase = true)
}

fun String.strippingTTSDigits(): String =
    filter { !it.isDigit() }.collapsedWhitespace()

/**
 * Visual selection still contains flattened `<sup>21</sup>` (`się21`).
 * Speech does not; this is only for matching the start anchor.
 */
fun String.strippingInlineTTSNoteMarkers(): String =
    replace(Regex("""(?<=\p{L})\d{1,3}(?=\s|\p{Punct}|$)"""), "")
        .replace(Regex("""\[\d{1,3}]"""), "")
        .replace(Regex("""[¹²³⁴⁵⁶⁷⁸⁹⁰]+"""), "")

fun Locator.removingTTSNoteSelector(): Locator {
    val others = locations.otherLocations.toMutableMap()
    others.remove("cssSelector")
    return copy(locations = locations.copy(otherLocations = others))
}

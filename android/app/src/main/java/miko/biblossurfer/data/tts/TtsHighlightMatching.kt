package miko.biblossurfer.data

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
    return haystack.contains(needle, ignoreCase = true) || needle.contains(haystack, ignoreCase = true)
}

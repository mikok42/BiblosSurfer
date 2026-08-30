package miko.biblossurfer.data.tts

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.html.cssSelector
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.util.Url

private val ttsNoteTokens: Set<String> = setOf(
    "aside",
    "annotation", "annotations",
    "footnote", "footnotes",
    "endnote", "endnotes",
    "note", "notes", "noteref",
    "doc-footnote", "doc-endnote", "doc-endnotes",
    "przypis", "przypisy",
)

/**
 * Drops footnote / annotation bodies. Leaves ordinary digits in the main text alone.
 */
@OptIn(ExperimentalReadiumApi::class)
fun Content.Element.preparedForTTS(): Content.Element? =
    if (isTTSNoteContent) null else this

@OptIn(ExperimentalReadiumApi::class)
val Content.Element.isTTSNoteContent: Boolean
    get() {
        val text = this as? Content.TextElement
        if (text?.role == Content.TextElement.Role.Footnote) return true
        return locator.isTTSNoteLocation
    }

val Locator.isTTSNoteLocation: Boolean
    get() {
        if (href.isTTSNoteResource) return true
        val selector = locations.cssSelector
        return selector != null && selector.looksLikeTTSNoteSelector
    }

val Url.isTTSNoteResource: Boolean
    get() {
        val string = toString()
        val last = string.substringAfterLast('/')
        val stem = last.substringBefore('.')
        return stem.lowercase() in ttsNoteTokens
    }

val String.looksLikeTTSNoteSelector: Boolean
    get() {
        val tokens = lowercase().split(Regex("[^\\p{L}-]+")).filter { it.isNotEmpty() }
        return tokens.any { it in ttsNoteTokens }
    }

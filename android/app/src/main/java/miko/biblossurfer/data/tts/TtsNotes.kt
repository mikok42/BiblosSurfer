package miko.biblossurfer.data.tts

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.html.cssSelector
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.ContentService
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
        val last = filename ?: toString().substringAfterLast('/')
        val stem = last.substringBefore('.')
        return stem.lowercase() in ttsNoteTokens
    }

val String.looksLikeTTSNoteSelector: Boolean
    get() {
        val tokens = lowercase().split(Regex("[^\\p{L}-]+")).filter { it.isNotEmpty() }
        return tokens.any { it in ttsNoteTokens }
    }

/**
 * Kotlin TTS only accepts a tokenizer over raw strings, unlike iOS which wraps the content
 * tokenizer. Filtering here drops note elements before utterances are produced.
 */
@OptIn(ExperimentalReadiumApi::class)
class SkippingNotesContentService(
    private val inner: ContentService,
) : ContentService {
    override fun content(start: Locator?): Content {
        val original = inner.content(start)
        return object : Content {
            override fun iterator(): Content.Iterator =
                SkippingNotesIterator(original.iterator())
        }
    }

    override fun close() {
        inner.close()
    }
}

@OptIn(ExperimentalReadiumApi::class)
class SkippingNotesIterator(
    private val inner: Content.Iterator,
) : Content.Iterator {
    private var preparedNext: Content.Element? = null
    private var preparedPrevious: Content.Element? = null

    override suspend fun hasNext(): Boolean {
        preparedNext = null
        while (inner.hasNext()) {
            val element = inner.next()
            if (element.preparedForTTS() != null) {
                preparedNext = element
                return true
            }
        }
        return false
    }

    override fun next(): Content.Element =
        preparedNext ?: error("hasNext() must be called first")

    override suspend fun hasPrevious(): Boolean {
        preparedPrevious = null
        while (inner.hasPrevious()) {
            val element = inner.previous()
            if (element.preparedForTTS() != null) {
                preparedPrevious = element
                return true
            }
        }
        return false
    }

    override fun previous(): Content.Element =
        preparedPrevious ?: error("hasPrevious() must be called first")
}

package miko.biblossurfer.data.tts

import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.PublicationServicesHolder
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.iterators.HtmlResourceContentIterator
import org.readium.r2.shared.publication.services.content.iterators.ResourceContentIteratorFactory
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.map
import java.nio.charset.StandardCharsets

private const val emptyTTSNoteDocument =
    """<html xmlns="http://www.w3.org/1999/xhtml"><body/></html>"""

private val ttsNoteResourceStems: Set<String> = setOf(
    "annotation", "annotations",
    "footnote", "footnotes",
    "endnote", "endnotes",
    "note", "notes",
    "przypis", "przypisy",
)

@OptIn(ExperimentalReadiumApi::class)
class TTSNoteStrippingIteratorFactory : ResourceContentIteratorFactory {
    private val html = HtmlResourceContentIterator.Factory()

    override suspend fun create(
        manifest: Manifest,
        servicesHolder: PublicationServicesHolder,
        readingOrderIndex: Int,
        resource: Resource,
        mediaType: MediaType,
        locator: Locator,
    ): Content.Iterator? {
        val link = manifest.readingOrder.getOrNull(readingOrderIndex) ?: return null
        val isHtml = mediaType.isHtml || locator.mediaType.isHtml
        if (!isHtml) return null
        val href = link.url().toString()
        val cleaned = resource.map { bytes ->
            val htmlText = String(bytes, StandardCharsets.UTF_8)
            Try.success(htmlText.htmlPreparedForTTS(href).toByteArray(StandardCharsets.UTF_8))
        }
        return html.create(
            manifest,
            servicesHolder,
            readingOrderIndex,
            cleaned,
            mediaType,
            locator,
        )
    }
}

fun String.htmlPreparedForTTS(href: String): String =
    if (href.isTTSNoteResourcePath) emptyTTSNoteDocument else strippingTTSNoteMarkup

val String.isTTSNoteResourcePath: Boolean
    get() {
        val last = substringAfterLast('/')
        val stem = last.substringBefore('.')
        return stem.lowercase() in ttsNoteResourceStems
    }

/** Drops Wolne Lektury / EPUB3 note markup. Leaves ordinary digits (T-800) alone. */
val String.strippingTTSNoteMarkup: String
    get() = removingHTMLElements("a", """class\s*=\s*["'][^"']*\banchor\b[^"']*["']""")
        .removingHTMLElements("a", """(?:epub:type|type)\s*=\s*["'][^"']*\bnoteref\b[^"']*["']""")
        .removingHTMLElements("aside", """(?:epub:type|type)\s*=\s*["'][^"']*\b(?:footnote|endnote)s?\b[^"']*["']""")
        .removingHTMLElements("div", """id\s*=\s*["']footnotes["']""")
        .removingHTMLElements("div", """class\s*=\s*["'][^"']*\bannotation\b[^"']*["']""")

fun String.removingHTMLElements(tag: String, attributesPattern: String): String {
    val escapedTag = Regex.escape(tag)
    val regex = Regex(
        "<$escapedTag\\b[^>]*$attributesPattern[^>]*>.*?</$escapedTag\\s*>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    return replace(regex, "")
}

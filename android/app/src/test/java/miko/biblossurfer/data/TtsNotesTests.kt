package miko.biblossurfer.data.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType

@OptIn(ExperimentalReadiumApi::class)
class TtsNotesTests {
    @Test
    fun skipsAnnotationResource() {
        val element = textElement(
            href = "EPUB/annotations.xhtml",
            highlight = "Bóg — hebr. elohim",
        )
        assertTrue(element.isTTSNoteContent)
        assertNull(element.preparedForTTS())
    }

    @Test
    fun skipsFootnoteRole() {
        val element = textElement(
            role = Content.TextElement.Role.Footnote,
            highlight = "A note at the bottom.",
        )
        assertTrue(element.isTTSNoteContent)
        assertNull(element.preparedForTTS())
    }

    @Test
    fun skipsAsideSelector() {
        val element = textElement(
            selector = "#c06-li-0001 > aside",
            highlight = "Trailing footnote",
        )
        assertTrue(element.isTTSNoteContent)
        assertNull(element.preparedForTTS())
    }

    @Test
    fun keepsDigitsInMainText() {
        val verse = textElement(
            href = "EPUB/part2.xhtml",
            selector = "div.verse-relig",
            highlight = "Na początku stworzył Bóg1 niebo i ziemię.",
        )
        val model = textElement(
            href = "OEBPS/chapter1.xhtml",
            highlight = "The T-800 arrived in 1995.",
        )
        assertEquals(
            verse.text,
            (verse.preparedForTTS() as? Content.TextElement)?.text,
        )
        assertEquals(
            "The T-800 arrived in 1995.",
            (model.preparedForTTS() as? Content.TextElement)?.text,
        )
    }

    private fun textElement(
        href: String = "OEBPS/chapter1.xhtml",
        selector: String? = null,
        role: Content.TextElement.Role = Content.TextElement.Role.Body,
        highlight: String,
    ): Content.TextElement {
        val locator = Locator(
            href = Url(href)!!,
            mediaType = MediaType.XHTML,
            locations = Locator.Locations(
                otherLocations = if (selector != null) mapOf("cssSelector" to selector) else emptyMap(),
            ),
            text = Locator.Text(highlight = highlight),
        )
        return Content.TextElement(
            locator = locator,
            role = role,
            segments = listOf(
                Content.TextElement.Segment(
                    locator = locator,
                    text = highlight,
                    attributes = emptyList(),
                ),
            ),
        )
    }
}

package miko.biblossurfer.data.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TTSNoteMarkupTests {
    @Test
    fun stripsWolneLekturyAnchorFromVerse() {
        val html = """
            <div class="verse-relig">Oto [dzieje] zrodzenia się<a class="anchor" id="anchor-21" href="annotations.xhtml#annotation-21"><sup>21</sup></a> nieba i ziemi</div>
        """.trimIndent()
        val stripped = html.strippingTTSNoteMarkup
        assertTrue(stripped.contains("zrodzenia się"))
        assertTrue(stripped.contains("nieba"))
        assertFalse(stripped.contains("21"))
        assertFalse(stripped.contains("class=\"anchor\""))
    }

    @Test
    fun leavesOrdinaryDigitsAlone() {
        val html = "<p>The T-800 arrived in 1995.</p>"
        assertEquals(html, html.strippingTTSNoteMarkup)
    }

    @Test
    fun notesResourceBecomesEmptyDocument() {
        val html = """
            <html xmlns="http://www.w3.org/1999/xhtml"><body><div id="footnotes"><div class="annotation">Bóg — hebr. elohim</div></div></body></html>
        """.trimIndent()
        val prepared = html.htmlPreparedForTTS("EPUB/annotations.xhtml")
        assertFalse(prepared.contains("elohim"))
        assertTrue(prepared.contains("<body"))
    }

    @Test
    fun chapterHrefStillStripsInlineAnchors() {
        val html = """<p>się<a class="anchor" href="annotations.xhtml#annotation-21"><sup>21</sup></a> nieba</p>"""
        assertFalse(html.htmlPreparedForTTS("EPUB/part3.xhtml").contains("21"))
    }
}

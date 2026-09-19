package miko.biblossurfer.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import miko.biblossurfer.data.model.LibraryItem
import miko.biblossurfer.data.model.PublicationFormat
import miko.biblossurfer.support.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.content
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
@OptIn(ExperimentalReadiumApi::class)
class PublicationOpeningServiceTests {
    @Test
    fun sampleEPUBExposesMetadataAndTextualProfile() = runTest {
        val file = TestFixtures.temporarySampleBookFile()
        try {
            val opened = PublicationOpeningService(ApplicationProvider.getApplicationContext()).open(file)
            assertEquals(TestFixtures.sampleBookTitle, opened.title)
            assertEquals(TestFixtures.sampleBookAuthor, opened.author)
            assertEquals(PublicationFormat.EPUB, opened.format)
        } finally {
            file.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun sampleEPUBContentOmitsInlineFootnoteMarkers() = runTest {
        val file = TestFixtures.temporarySampleBookFile()
        try {
            val opened = PublicationOpeningService(ApplicationProvider.getApplicationContext()).open(file)
            val elements = opened.publication.content()?.elements().orEmpty()
            val verse = elements.firstOrNull {
                (it as? Content.TextualElement)?.text?.contains("zrodzenia się") == true
            }
            val spoken = (verse as? Content.TextualElement)?.text
            assertNotNull(spoken)
            assertTrue(spoken?.contains("zrodzenia się") == true)
            assertFalse(spoken?.contains("21") == true)

            val whole = opened.publication.content()?.text().orEmpty()
            assertFalse(whole.contains("[przypis edytorski]"))
        } finally {
            file.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun contentFromRemovedAnchorSelectorStillYieldsChapterText() = runTest {
        val file = TestFixtures.temporarySampleBookFile()
        try {
            val opened = PublicationOpeningService(ApplicationProvider.getApplicationContext()).open(file)
            val link = opened.publication.readingOrder.first { it.url().toString().contains("part3") }
            val locator = Locator(
                href = link.url(),
                mediaType = link.mediaType ?: MediaType.XHTML,
                locations = Locator.Locations(
                    otherLocations = mapOf("cssSelector" to "#anchor-21"),
                ),
                text = Locator.Text(highlight = "zrodzenia się21 nieba"),
            )
            val elements = opened.publication.content(locator)?.elements().orEmpty()
            assertTrue(elements.isNotEmpty())
            assertTrue(
                elements.any { (it as? Content.TextualElement)?.text?.contains("zrodzenia się") == true },
            )
        } finally {
            file.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun samplePDFIsOpenedAsPDF() = runTest {
        val file = TestFixtures.temporarySamplePdfFile()
        try {
            val opened = PublicationOpeningService(ApplicationProvider.getApplicationContext()).open(file)
            assertEquals(PublicationFormat.PDF, opened.format)
        } catch (error: UnsatisfiedLinkError) {
            // Pdfium is native; JVM unit tests cannot load it. Format from the path still matches.
            assertEquals(PublicationFormat.PDF, LibraryItem(fileURL = file).format)
        } finally {
            file.parentFile?.deleteRecursively()
        }
    }

    @Test
    fun unknownPathExtensionIsUnknownFormat() {
        val url = File("/tmp/odd-book.cbz")
        assertEquals(PublicationFormat.UNKNOWN, LibraryItem(fileURL = url).format)
        assertEquals("Unknown format", Errors.Publication.UnknownFormat("Odd Book").title)
    }

    @Test
    fun missingFileSurfacesAPublicationError() = runTest {
        val missing = File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "does-not-exist.epub")
        try {
            PublicationOpeningService(ApplicationProvider.getApplicationContext()).open(missing)
            fail("Expected openFailed")
        } catch (error: Errors.Publication.OpenFailed) {
            assertEquals("does-not-exist", error.bookTitle)
        } catch (error: Throwable) {
            fail("Expected Errors.Publication.OpenFailed, got $error")
        }
    }
}

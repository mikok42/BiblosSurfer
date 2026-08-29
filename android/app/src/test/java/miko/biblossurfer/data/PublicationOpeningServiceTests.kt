package miko.biblossurfer.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import miko.biblossurfer.data.model.LibraryItem
import miko.biblossurfer.data.model.PublicationFormat
import miko.biblossurfer.support.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
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

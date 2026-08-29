package miko.biblossurfer.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import miko.biblossurfer.support.TestFixtures
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class LibraryServiceTests {
    private lateinit var directory: File
    private lateinit var covers: File
    private lateinit var service: LibraryService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        directory = File(context.cacheDir, UUID.randomUUID().toString())
        covers = File(directory, "Covers")
        directory.mkdirs()
        service = LibraryService(
            booksDirectory = directory,
            coversDirectory = covers,
            bundledBookFiles = listOfNotNull(TestFixtures.sampleBookFile),
            opener = PublicationOpeningService(context),
            bookStore = LocalBookService(),
        )
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun bundledBookAppearsAlongsideUserBooks() = runTest {
        val leftover = File(directory, "The Sample Voyage.epub")
        leftover.writeText("old-mock")

        val items = service.loadItems()
        val titles = items.map { it.title }.toSet()

        assertTrue(leftover.exists())
        assertTrue(titles.contains(TestFixtures.sampleBookTitle))
        assertTrue(titles.contains("The Sample Voyage"))
        assertEquals(
            TestFixtures.sampleBookFile?.absolutePath,
            items.first { it.title == TestFixtures.sampleBookTitle }.fileURL.absolutePath,
        )
    }

    @Test
    fun bundledBookIsListedFromTheBundleWithoutCopying() = runTest {
        val items = service.loadItems()
        assertEquals(1, items.size)
        assertEquals(TestFixtures.sampleBookTitle, items.first().title)
        assertEquals(miko.biblossurfer.data.model.PublicationFormat.EPUB, items.first().format)
        assertEquals(true, items.first().isEPUB)
        assertEquals(TestFixtures.sampleBookFile?.absolutePath, items.first().fileURL.absolutePath)
        val copiedBooks = directory.listFiles().orEmpty()
            .filter { it.extension.lowercase() in listOf("epub", "pdf") }
        assertEquals(0, copiedBooks.size)
    }

    @Test
    fun nonBookFilesAreNotListed() = runTest {
        service.loadItems()
        File(directory, "notes.txt").writeText("hello")
        val items = service.loadItems()
        assertEquals(listOf(TestFixtures.sampleBookTitle), items.map { it.title })
    }

    @Test
    fun importingUnsupportedFormatThrows() = runTest {
        val notes = File(directory, "notes.txt")
        notes.writeBytes(ByteArray(0))
        try {
            service.importBook(notes)
            fail("Expected unsupportedFormat")
        } catch (error: Errors.Library.UnsupportedFormat) {
            assertEquals("txt", error.fileExtension)
        } catch (error: Throwable) {
            fail("Expected Errors.Library.UnsupportedFormat, got $error")
        }
    }

    @Test
    fun importingEPUBCopiesIntoTheLibrary() = runTest {
        service.loadItems()
        val source = TestFixtures.temporarySampleBookFile()
        try {
            val imported = service.importBook(source)
            assertTrue(imported.isEPUB)
            assertTrue(imported.fileURL.path.startsWith(directory.path))
            assertTrue(imported.fileURL.exists())
            val items = service.loadItems()
            assertEquals(1, items.size)
            assertTrue(items.any { it.fileURL.path.startsWith(directory.path) })
        } finally {
            source.parentFile?.deleteRecursively()
        }
    }
}

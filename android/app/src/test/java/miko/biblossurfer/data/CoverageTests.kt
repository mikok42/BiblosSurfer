package miko.biblossurfer.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import miko.biblossurfer.data.model.LibraryItem
import miko.biblossurfer.data.model.PublicationFormat
import miko.biblossurfer.ui.library.LibraryViewModel
import miko.biblossurfer.ui.reader.ReaderViewModel
import miko.biblossurfer.util.AccessibilityIdentifiers
import miko.biblossurfer.util.AnalyticsEvent
import miko.biblossurfer.util.AnalyticsTimer
import miko.biblossurfer.util.Debouncer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.UUID
import android.content.Context

class ErrorsTests {
    @Test
    fun libraryUnsupportedFormatCopyMatchesIos() {
        val error = Errors.Library.UnsupportedFormat("mobi")
        assertEquals("Unsupported format", error.title)
        assertEquals(
            "[Library] BiblosSurfer cannot open .mobi files. Try EPUB or PDF.",
            error.description,
        )
    }

    @Test
    fun publicationUnknownFormatCopyMatchesIos() {
        val error = Errors.Publication.UnknownFormat("Odd Book")
        assertEquals("Unknown format", error.title)
        assertEquals("[Publication] Odd Book has an unknown format.", error.description)
    }

    @Test
    fun ttsMissingVoiceCopyMatchesIos() {
        val error = Errors.Tts.NoVoiceForLanguage("pl")
        assertEquals("Missing voice", error.title)
        assertTrue(error.description.contains("pl"))
    }
}

class AccessibilityIdentifiersTests {
    @Test
    fun dottedScreenElementNamesMatchIos() {
        assertEquals("library.title", AccessibilityIdentifiers.Library.title)
        assertEquals("library.importButton", AccessibilityIdentifiers.Library.importButton)
        assertEquals("library.cell.Genesis", AccessibilityIdentifiers.Library.cell("Genesis"))
        assertEquals("reader.container", AccessibilityIdentifiers.Reader.container)
        assertEquals("reader.ttsPlay", AccessibilityIdentifiers.Reader.ttsPlay)
        assertEquals("settings.fontSize", AccessibilityIdentifiers.Settings.fontSize)
        assertEquals("error.dismiss", AccessibilityIdentifiers.Error.dismiss)
    }
}

class AnalyticsTimerTests {
    @Test
    fun reportsDurationUnderTheEventName() {
        var reportedName: String? = null
        var reportedDuration: Double? = null
        val timer = AnalyticsTimer(AnalyticsEvent.openingPublication) { name, duration ->
            reportedName = name
            reportedDuration = duration
        }
        timer.startTimer()
        Thread.sleep(5)
        timer.endTimer()
        timer.reportToAnalytics()
        assertEquals("opening_publication", reportedName)
        assertTrue((reportedDuration ?: 0.0) >= 0.0)
    }
}

class LibraryItemTests {
    @Test
    fun pathExtensionMapsToPublicationFormat() {
        assertEquals(PublicationFormat.EPUB, LibraryItem(File("/tmp/a.epub")).format)
        assertEquals(PublicationFormat.PDF, LibraryItem(File("/tmp/a.pdf")).format)
        assertEquals(PublicationFormat.UNKNOWN, LibraryItem(File("/tmp/a.mobi")).format)
        assertTrue(LibraryItem(File("/tmp/a.epub")).isEPUB)
        assertTrue(LibraryItem(File("/tmp/a.pdf")).isPDF)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DebouncerTests {
    @Test
    fun laterScheduleCancelsTheEarlierAction() = runTest {
        val debouncer = Debouncer(this)
        var value = 0
        debouncer.schedule(1.0) { value = 1 }
        debouncer.schedule(1.0) { value = 2 }
        advanceTimeBy(1500)
        assertEquals(2, value)
    }
}

class LibraryViewModelTests {
    @Test
    fun loadSurfacesLibraryErrors() = kotlinx.coroutines.test.runTest {
        val failing = object : LibraryServiceProtocol {
            override suspend fun loadItems(): List<LibraryItem> {
                throw Errors.Library.FileMissing("gone.epub")
            }
            override suspend fun importBook(from: File): LibraryItem {
                error("unused")
            }
        }
        val vm = LibraryViewModel(failing)
        vm.load()
        assertTrue(vm.viewProperties.value.error is Errors.Library.FileMissing)
        assertFalse(vm.viewProperties.value.isLoading)
    }

    @Test
    fun loadPublishesSortedItems() = kotlinx.coroutines.test.runTest {
        val items = listOf(
            LibraryItem(File("/tmp/zeta.epub")),
            LibraryItem(File("/tmp/alpha.epub")),
        )
        val service = object : LibraryServiceProtocol {
            override suspend fun loadItems(): List<LibraryItem> = items.sortedBy { it.title }
            override suspend fun importBook(from: File): LibraryItem = items.first()
        }
        val vm = LibraryViewModel(service)
        vm.load()
        assertEquals(listOf("alpha", "zeta"), vm.viewProperties.value.items.map { it.title })
        assertNull(vm.viewProperties.value.error)
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ReaderViewModelTests {
    @Test
    fun canSpeakIsHiddenForPdf() {
        val defaults = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(UUID.randomUUID().toString(), Context.MODE_PRIVATE)
        val vm = ReaderViewModel(
            title = "Paper",
            format = PublicationFormat.PDF,
            canSpeak = true,
            settings = ReaderSettingsStore(defaults),
        )
        assertFalse(vm.viewProperties.value.canSpeak)
    }

    @Test
    fun canSpeakIsOfferedForEpub() {
        val defaults = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(UUID.randomUUID().toString(), Context.MODE_PRIVATE)
        val vm = ReaderViewModel(
            title = "Genesis",
            format = PublicationFormat.EPUB,
            canSpeak = true,
            settings = ReaderSettingsStore(defaults),
        )
        assertTrue(vm.viewProperties.value.canSpeak)
    }

    @Test
    fun selectingLanguageClearsMismatchedVoice() {
        val defaults = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(UUID.randomUUID().toString(), Context.MODE_PRIVATE)
        val vm = ReaderViewModel(
            title = "Genesis",
            format = PublicationFormat.EPUB,
            canSpeak = true,
            settings = ReaderSettingsStore(defaults),
        )
        vm.availableVoices = listOf(
            miko.biblossurfer.data.tts.TtsVoiceInfo("en-voice", "Samantha", "en-US", 300),
            miko.biblossurfer.data.tts.TtsVoiceInfo("pl-voice", "Zosia", "pl-PL", 300),
        )
        vm.settings.voiceIdentifier = "en-voice"
        vm.selectTTSLanguage("pl")
        assertNull(vm.settings.voiceIdentifier)
        assertEquals("pl", vm.settings.defaultLanguage)
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class RoomBookStoreTests {
    @Test
    fun progressRoundTripsThroughRoom() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = BookDatabase.create(context, inMemory = true)
        val store = RoomBookStore(
            dao = db.bookDao(),
            booksDirectory = File(context.cacheDir, "Books"),
            coversDirectory = File(context.cacheDir, "Covers"),
        )
        val item = LibraryItem(File("/tmp/Books/voyage.epub"))
        store.upsert(item, "voyage.epub", null)
        store.updateProgress("voyage.epub", """{"href":"c1","type":"application/xhtml+xml"}""", 0.4)
        val stored = store.book("voyage.epub")
        assertEquals(0.4, stored?.progression)
        assertTrue(stored?.locatorJSON?.contains("c1") == true)
        db.close()
    }
}

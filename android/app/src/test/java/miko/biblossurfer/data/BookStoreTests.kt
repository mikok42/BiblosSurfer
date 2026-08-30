package miko.biblossurfer.data

import miko.biblossurfer.data.model.LibraryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BookStoreTests {
    @Test
    fun progressRoundTripsThroughTheStore() {
        val store = LocalBookService()
        val fileURL = File("/tmp/Books/voyage.epub")
        val item = LibraryItem(fileURL = fileURL)

        store.upsert(item, "voyage.epub", null)
        store.updateProgress(
            relativePath = "voyage.epub",
            locatorJSON = """{"href":"c1","type":"application/xhtml+xml"}""",
            progression = 0.4,
        )

        val stored = store.book("voyage.epub")
        assertEquals(0.4, stored?.progression)
        assertTrue(stored?.locatorJSON?.contains("c1") == true)
    }

    @Test
    fun upsertKeepsExistingLocatorJSON() {
        val store = LocalBookService()
        val fileURL = File("/tmp/Books/voyage.epub")
        val item = LibraryItem(fileURL = fileURL)

        store.upsert(item, "voyage.epub", null)
        store.updateProgress(
            relativePath = "voyage.epub",
            locatorJSON = """{"href":"c1","type":"application/xhtml+xml"}""",
            progression = 0.4,
        )
        store.upsert(item, "voyage.epub", null)

        val stored = store.book("voyage.epub")
        assertTrue(stored?.locatorJSON?.contains("c1") == true)
    }
}

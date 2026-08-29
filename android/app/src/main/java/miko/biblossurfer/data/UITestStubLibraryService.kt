package miko.biblossurfer.data

import android.content.Context
import miko.biblossurfer.data.model.LibraryItem
import miko.biblossurfer.data.model.PublicationFormat
import java.util.UUID

/**
 * Deterministic library for previews and UI tests. Points at the bundled Genesis EPUB so the
 * screens never depend on files in app storage.
 */
class UITestStubLibraryService(private val context: Context) : LibraryServiceProtocol {
    companion object {
        const val sampleTitle = "Genesis. Księga Rodzaju. Bereszit"
        const val sampleAuthor = "Izaak Cylkow"
        const val resourceName = "genesis-ksiega-rodzaju-bereszit"
        const val fileExtension = "epub"
    }

    val sampleBookFile = context.bundledEpubFiles()
        .firstOrNull { it.name == "$resourceName.$fileExtension" }

    override suspend fun loadItems(): List<LibraryItem> {
        val file = sampleBookFile
            ?: throw Errors.Library.FileMissing("$resourceName.$fileExtension")
        return listOf(
            LibraryItem(
                id = UUID.fromString("AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE"),
                fileName = "$resourceName.$fileExtension",
                title = sampleTitle,
                author = sampleAuthor,
                fileURL = file,
                coverURL = null,
                locatorJSON = null,
                progression = 0.0,
                format = PublicationFormat.EPUB,
                folderName = null,
            )
        )
    }

    override suspend fun importBook(from: java.io.File): LibraryItem {
        throw Errors.Library.UnsupportedFormat(from.extension)
    }
}

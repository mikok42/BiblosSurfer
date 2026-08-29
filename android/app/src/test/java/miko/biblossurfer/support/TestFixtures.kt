package miko.biblossurfer.support

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import miko.biblossurfer.data.UITestStubLibraryService
import miko.biblossurfer.data.bundledEpubFiles
import miko.biblossurfer.data.bundledPdfFile
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import java.io.File
import java.util.UUID

object TestFixtures {
    val sampleBookTitle: String = UITestStubLibraryService.sampleTitle
    val sampleBookAuthor: String = UITestStubLibraryService.sampleAuthor

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    val sampleBookFile: File?
        get() = context.bundledEpubFiles().firstOrNull {
            it.name == "${UITestStubLibraryService.resourceName}.${UITestStubLibraryService.fileExtension}"
        }

    fun temporarySampleBookFile(): File {
        val source = sampleBookFile ?: error("sample EPUB missing")
        val directory = File(context.cacheDir, UUID.randomUUID().toString())
        directory.mkdirs()
        val destination = File(
            directory,
            "${UITestStubLibraryService.resourceName}.${UITestStubLibraryService.fileExtension}",
        )
        source.copyTo(destination)
        return destination
    }

    val samplePdfFile: File?
        get() = context.bundledPdfFile()

    fun temporarySamplePdfFile(): File {
        val source = samplePdfFile ?: error("sample PDF missing")
        val directory = File(context.cacheDir, UUID.randomUUID().toString())
        directory.mkdirs()
        val destination = File(directory, "SampleBook.pdf")
        source.copyTo(destination)
        return destination
    }

    val gutenbergHistoryFile: File?
        get() = context.bundledEpubFiles().firstOrNull { it.name.startsWith("pg28876") }

    fun locator(
        href: String = "OEBPS/chapter1.xhtml",
        mediaType: MediaType = MediaType.XHTML,
        title: String? = "Chapter One",
        progression: Double? = 0.25,
        totalProgression: Double? = 0.1,
        position: Int? = 3,
        highlight: String? = "The ship left harbour before dawn.",
    ): Locator = Locator(
        href = Url(href)!!,
        mediaType = mediaType,
        title = title,
        locations = Locator.Locations(
            progression = progression,
            totalProgression = totalProgression,
            position = position,
        ),
        text = Locator.Text(highlight = highlight),
    )
}

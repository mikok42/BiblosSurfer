package miko.biblossurfer.data

import android.content.Context
import android.graphics.Bitmap
import miko.biblossurfer.data.model.LibraryItem
import miko.biblossurfer.data.model.PublicationFormat
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

interface LibraryServiceProtocol {
    suspend fun loadItems(): List<LibraryItem>
    suspend fun importBook(from: File): LibraryItem
}

class LibraryService(
    private val booksDirectory: File,
    private val coversDirectory: File,
    private val bundledBookFiles: List<File>,
    private val opener: PublicationOpeningServiceProtocol,
    private val bookStore: BookStoreProtocol,
) : LibraryServiceProtocol {

    constructor(
        context: Context,
        opener: PublicationOpeningServiceProtocol = PublicationOpeningService(context),
        bookStore: BookStoreProtocol = LocalBookService(),
        bundledBookFiles: List<File> = context.bundledEpubFiles(),
    ) : this(
        booksDirectory = context.defaultBooksDirectory(),
        coversDirectory = context.defaultCoversDirectory(),
        bundledBookFiles = bundledBookFiles,
        opener = opener,
        bookStore = bookStore,
    )

    override suspend fun loadItems(): List<LibraryItem> {
        ensureDirectories()
        val urls = libraryFileURLs()
        for (url in urls) {
            if (bookStore.book(url.name) == null) {
                runCatching { catalog(url) }
            }
        }
        return urls
            .map { url ->
                bookStore.book(url.name)?.replacingFileURL(url) ?: LibraryItem(fileURL = url)
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    }

    override suspend fun importBook(from: File): LibraryItem {
        val fileExtension = from.extension.lowercase()
        if (fileExtension != "epub" && fileExtension != "pdf") {
            throw Errors.Library.UnsupportedFormat(fileExtension)
        }
        ensureDirectories()
        val destination = uniqueDestination(from.name)
        try {
            from.copyTo(destination, overwrite = false)
        } catch (error: Exception) {
            throw Errors.Library.CopyFailed(from.name, error.localizedMessage ?: error.toString())
        }
        return catalog(destination)
    }

    private fun libraryFileURLs(): List<File> {
        val urls = listedBookURLs().toMutableList()
        val existingNames = urls.map { it.name }.toSet()
        for (bundled in bundledBookFiles) {
            if (bundled.name !in existingNames) {
                urls.add(bundled)
            }
        }
        return urls
    }

    private suspend fun catalog(fileURL: File): LibraryItem {
        val opened = try {
            opener.open(fileURL)
        } catch (error: Exception) {
            if (error is DescriptiveError) throw error
            throw Errors.Publication.OpenFailed(
                fileURL.nameWithoutExtension,
                error.localizedMessage ?: error.toString(),
            )
        }

        var coverFileName: String? = null
        opened.cover?.let { cover ->
            val name = fileURL.nameWithoutExtension + ".jpg"
            val coverURL = File(coversDirectory, name)
            runCatching {
                FileOutputStream(coverURL).use { out ->
                    cover.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                coverFileName = name
            }
        }

        val relativePath = fileURL.name
        val existing = bookStore.book(relativePath)
        val item = LibraryItem(
            id = existing?.id ?: UUID.randomUUID(),
            fileName = fileURL.name,
            title = opened.title,
            author = opened.author,
            fileURL = fileURL,
            coverURL = coverFileName?.let { File(coversDirectory, it) },
            locatorJSON = existing?.locatorJSON,
            progression = existing?.progression ?: 0.0,
            format = opened.format,
            folderName = existing?.folderName,
        )
        bookStore.upsert(item, relativePath, coverFileName)
        return item
    }

    private fun ensureDirectories() {
        try {
            booksDirectory.mkdirs()
            coversDirectory.mkdirs()
        } catch (error: Exception) {
            throw Errors.Library.CopyFailed(
                booksDirectory.name,
                error.localizedMessage ?: error.toString(),
            )
        }
    }

    private fun listedBookURLs(): List<File> {
        val contents = try {
            booksDirectory.listFiles() ?: emptyArray()
        } catch (error: Exception) {
            throw Errors.Library.CopyFailed(
                booksDirectory.name,
                error.localizedMessage ?: error.toString(),
            )
        }
        return contents.filter { file ->
            val ext = file.extension.lowercase()
            !file.isHidden && (ext == "epub" || ext == "pdf")
        }
    }

    private fun uniqueDestination(fileName: String): File {
        var destination = File(booksDirectory, fileName)
        val baseName = destination.nameWithoutExtension
        val fileExtension = destination.extension
        var suffix = 1
        while (destination.exists()) {
            destination = File(booksDirectory, "$baseName $suffix.$fileExtension")
            suffix += 1
        }
        return destination
    }
}

fun Context.defaultBooksDirectory(): File = File(filesDir, "Books")

fun Context.defaultCoversDirectory(): File = File(filesDir, "Covers")

/**
 * Bundled EPUBs live in `assets/books/`. Android assets are not real files, so they are extracted
 * once into `filesDir/BundledBooks` — not into `Books/`, matching iOS (bundle URLs stay outside
 * the user library directory).
 */
fun Context.bundledEpubFiles(): List<File> {
    val destDir = File(filesDir, "BundledBooks")
    destDir.mkdirs()
    val names = assets.list("books").orEmpty().filter { it.endsWith(".epub", ignoreCase = true) }
    return names.map { name ->
        val dest = File(destDir, name)
        if (!dest.exists()) {
            assets.open("books/$name").use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
        dest
    }
}

fun Context.bundledPdfFile(): File? {
    val destDir = File(filesDir, "BundledBooks")
    destDir.mkdirs()
    val name = assets.list("books").orEmpty().firstOrNull { it.endsWith(".pdf", ignoreCase = true) }
        ?: return null
    val dest = File(destDir, name)
    if (!dest.exists()) {
        assets.open("books/$name").use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
    }
    return dest
}

package miko.biblossurfer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import miko.biblossurfer.data.model.LibraryItem
import miko.biblossurfer.data.model.PublicationFormat
import java.io.File
import java.util.UUID

interface BookStoreProtocol {
    fun book(atRelativePath: String): LibraryItem?
    fun upsert(item: LibraryItem, relativePath: String, coverFileName: String?)
    fun updateProgress(relativePath: String, locatorJSON: String, progression: Double)
    fun addBookmark(relativePath: String, locatorJSON: String, title: String?)
}

class LocalBookService : BookStoreProtocol {
    private val items = mutableMapOf<String, LibraryItem>()
    private val bookmarks = mutableMapOf<String, MutableList<Pair<String, String?>>>()

    override fun book(atRelativePath: String): LibraryItem? = items[atRelativePath]

    override fun upsert(item: LibraryItem, relativePath: String, coverFileName: String?) {
        items[relativePath] = item
    }

    override fun updateProgress(relativePath: String, locatorJSON: String, progression: Double) {
        val item = items[relativePath] ?: return
        items[relativePath] = item.copy(locatorJSON = locatorJSON, progression = progression)
    }

    override fun addBookmark(relativePath: String, locatorJSON: String, title: String?) {
        bookmarks.getOrPut(relativePath) { mutableListOf() }.add(locatorJSON to title)
    }
}

@Database(
    entities = [StoredBook::class, StoredFolder::class, StoredBookmark::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(PublicationFormatConverters::class)
abstract class BookDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        fun create(context: Context, inMemory: Boolean = false): BookDatabase {
            val builder = if (inMemory) {
                Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            } else {
                Room.databaseBuilder(context, BookDatabase::class.java, "biblos-surfer.db")
            }
            return builder
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

class RoomBookStore(
    private val dao: BookDao,
    private val booksDirectory: File,
    private val coversDirectory: File,
) : BookStoreProtocol {

    constructor(context: Context) : this(
        dao = BookDatabase.create(context).bookDao(),
        booksDirectory = context.defaultBooksDirectory(),
        coversDirectory = context.defaultCoversDirectory(),
    )

    override fun book(atRelativePath: String): LibraryItem? =
        dao.bookByRelativePath(atRelativePath)?.asLibraryItem(booksDirectory, coversDirectory)

    override fun upsert(item: LibraryItem, relativePath: String, coverFileName: String?) {
        val existing = dao.bookByRelativePath(relativePath)
        val stored = StoredBook(
            id = item.id.toString(),
            fileName = item.fileName,
            title = item.title,
            author = item.author,
            relativePath = relativePath,
            coverFileName = coverFileName ?: existing?.coverFileName,
            locatorJSON = item.locatorJSON ?: existing?.locatorJSON,
            progression = item.progression,
            format = item.format,
            folderName = item.folderName,
            importedAt = existing?.importedAt ?: System.currentTimeMillis(),
        )
        dao.upsert(stored)
    }

    override fun updateProgress(relativePath: String, locatorJSON: String, progression: Double) {
        val stored = dao.bookByRelativePath(relativePath) ?: return
        dao.upsert(stored.copy(locatorJSON = locatorJSON, progression = progression))
    }

    override fun addBookmark(relativePath: String, locatorJSON: String, title: String?) {
        val stored = dao.bookByRelativePath(relativePath) ?: return
        dao.insertBookmark(
            StoredBookmark(
                locatorJSON = locatorJSON,
                title = title,
                bookId = stored.id,
            )
        )
    }
}

fun StoredBook.asLibraryItem(booksDirectory: File, coversDirectory: File): LibraryItem =
    LibraryItem(
        id = runCatching { UUID.fromString(id) }.getOrDefault(UUID.randomUUID()),
        fileName = fileName,
        title = title,
        author = author,
        fileURL = File(booksDirectory, relativePath),
        coverURL = coverFileName?.let { File(coversDirectory, it) },
        locatorJSON = locatorJSON,
        progression = progression,
        format = format,
        folderName = folderName,
    )

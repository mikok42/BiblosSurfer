package miko.biblossurfer.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import miko.biblossurfer.data.model.PublicationFormat
import java.util.Date
import java.util.UUID

@Entity(tableName = "stored_books", indices = [Index(value = ["relativePath"], unique = true)])
data class StoredBook(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val title: String,
    val author: String? = null,
    val relativePath: String,
    val coverFileName: String? = null,
    val locatorJSON: String? = null,
    val progression: Double = 0.0,
    val format: PublicationFormat = PublicationFormat.UNKNOWN,
    val folderName: String? = null,
    val importedAt: Long = Date().time,
)

@Entity(tableName = "stored_folders")
data class StoredFolder(
    @PrimaryKey val name: String,
)

@Entity(
    tableName = "stored_bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = StoredBook::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("bookId")]
)
data class StoredBookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val locatorJSON: String,
    val createdAt: Long = Date().time,
    val title: String? = null,
    val bookId: String? = null,
)

class PublicationFormatConverters {
    @TypeConverter
    fun fromFormat(value: PublicationFormat): String = value.name.lowercase()

    @TypeConverter
    fun toFormat(value: String): PublicationFormat =
        PublicationFormat.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: PublicationFormat.UNKNOWN
}

@Dao
interface BookDao {
    @Query("SELECT * FROM stored_books WHERE relativePath = :relativePath LIMIT 1")
    fun bookByRelativePath(relativePath: String): StoredBook?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(book: StoredBook)

    @Update
    fun update(book: StoredBook)

    @Insert
    fun insertBookmark(bookmark: StoredBookmark)
}

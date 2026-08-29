package miko.biblossurfer.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File
import java.util.UUID

enum class PublicationFormat {
    EPUB,
    PDF,
    UNKNOWN;

    companion object {
        fun fromPathExtension(pathExtension: String): PublicationFormat =
            when (pathExtension.lowercase()) {
                "epub" -> EPUB
                "pdf" -> PDF
                else -> UNKNOWN
            }
    }
}

@Parcelize
data class LibraryItem(
    val id: UUID,
    val fileName: String,
    val title: String,
    val author: String?,
    val fileURL: File,
    val coverURL: File?,
    val locatorJSON: String?,
    val progression: Double,
    val format: PublicationFormat,
    val folderName: String?,
) : Parcelable {
    val isEPUB: Boolean get() = format == PublicationFormat.EPUB
    val isPDF: Boolean get() = format == PublicationFormat.PDF

    constructor(fileURL: File, id: UUID = UUID.randomUUID()) : this(
        id = id,
        fileName = fileURL.name,
        title = fileURL.nameWithoutExtension,
        author = null,
        fileURL = fileURL,
        coverURL = null,
        locatorJSON = null,
        progression = 0.0,
        format = PublicationFormat.fromPathExtension(fileURL.extension),
        folderName = null,
    )

    fun replacingFileURL(fileURL: File): LibraryItem = copy(fileURL = fileURL)
}

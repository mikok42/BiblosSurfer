package miko.biblossurfer.data

import android.content.Context
import android.graphics.Bitmap
import miko.biblossurfer.data.model.PublicationFormat
import miko.biblossurfer.data.tts.SkippingNotesContentService
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content.ContentService
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File

data class OpenedPublication(
    val publication: Publication,
    val title: String,
    val author: String?,
    val format: PublicationFormat,
    val cover: Bitmap?,
)

interface PublicationOpeningServiceProtocol {
    suspend fun open(url: File): OpenedPublication
}

class PublicationOpeningService(
    context: Context,
    assetRetriever: AssetRetriever? = null,
    publicationOpener: PublicationOpener? = null,
) : PublicationOpeningServiceProtocol {

    private val assetRetriever: AssetRetriever
    private val publicationOpener: PublicationOpener

    init {
        val httpClient = DefaultHttpClient()
        val retriever = assetRetriever ?: AssetRetriever(
            contentResolver = context.contentResolver,
            httpClient = httpClient,
        )
        this.assetRetriever = retriever
        this.publicationOpener = publicationOpener ?: PublicationOpener(
            publicationParser = DefaultPublicationParser(
                context = context,
                httpClient = httpClient,
                assetRetriever = retriever,
                pdfFactory = PdfiumDocumentFactory(context),
            )
        )
    }

    override suspend fun open(url: File): OpenedPublication {
        val displayTitle = url.nameWithoutExtension
        val asset = assetRetriever.retrieve(url).getOrElse { error ->
            throw Errors.Publication.OpenFailed(displayTitle, error.toString())
        }
        val publication = publicationOpener.open(
            asset = asset,
            allowUserInteraction = false,
            onCreatePublication = { skipTtsNotes() },
        ).getOrElse { error ->
            throw Errors.Publication.OpenFailed(displayTitle, error.toString())
        }
        val title = publication.metadata.title ?: displayTitle
        val author = publication.metadata.authors.firstOrNull()?.name
            ?: publication.metadata.translators.firstOrNull()?.name
            ?: publication.metadata.contributors.firstOrNull()?.name
        val format = publication.publicationFormat
        if (format == PublicationFormat.UNKNOWN) {
            throw Errors.Publication.UnknownFormat(title)
        }
        val cover: Bitmap? = publication.cover()
        return OpenedPublication(
            publication = publication,
            title = title,
            author = author,
            format = format,
            cover = cover,
        )
    }
}

val Publication.publicationFormat: PublicationFormat
    get() {
        if (conformsTo(Publication.Profile.PDF)) {
            return PublicationFormat.PDF
        }
        val isHtml = readingOrder.any { link ->
            val type = link.mediaType?.toString().orEmpty()
            type.contains("html", ignoreCase = true)
        }
        if (conformsTo(Publication.Profile.EPUB) || isHtml) {
            return PublicationFormat.EPUB
        }
        return PublicationFormat.UNKNOWN
    }

@OptIn(ExperimentalReadiumApi::class)
fun Publication.Builder.skipTtsNotes() {
    servicesBuilder.decorate(ContentService::class) { oldFactory ->
        { context ->
            val original = oldFactory?.invoke(context) as? ContentService
            original?.let(::SkippingNotesContentService)
        }
    }
}


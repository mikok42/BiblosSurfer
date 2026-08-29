package miko.biblossurfer.data

import android.content.Context

interface ServiceProviderProtocol {
    val bookService: BookStoreProtocol
    val libraryService: LibraryServiceProtocol
    val publicationOpener: PublicationOpeningServiceProtocol
    val settings: ReaderSettingsStore
}

class ServiceProvider(context: Context) : ServiceProviderProtocol {
    override val publicationOpener: PublicationOpeningServiceProtocol =
        PublicationOpeningService(context)
    override val bookService: BookStoreProtocol = RoomBookStore(context)
    override val settings: ReaderSettingsStore =
        ReaderSettingsStore(context.getSharedPreferences("miko.biblossurfer", Context.MODE_PRIVATE))
    override val libraryService: LibraryServiceProtocol = LibraryService(
        context = context,
        opener = publicationOpener,
        bookStore = bookService,
    )
}

class MockServiceProvider(context: Context) : ServiceProviderProtocol {
    override val publicationOpener: PublicationOpeningServiceProtocol =
        PublicationOpeningService(context)
    override val bookService: BookStoreProtocol = LocalBookService()
    override val settings: ReaderSettingsStore =
        ReaderSettingsStore(
            context.getSharedPreferences("miko.biblossurfer.mock", Context.MODE_PRIVATE)
        )
    override val libraryService: LibraryServiceProtocol = UITestStubLibraryService(context)
}

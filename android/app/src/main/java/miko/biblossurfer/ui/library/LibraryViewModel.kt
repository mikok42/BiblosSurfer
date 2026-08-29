package miko.biblossurfer.ui.library

import miko.biblossurfer.data.DescriptiveError
import miko.biblossurfer.data.Errors
import miko.biblossurfer.data.LibraryServiceProtocol
import miko.biblossurfer.data.model.LibraryItem
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface LibraryRouting {
    fun openBook(item: LibraryItem)
}

interface ErrorDismissing {
    fun dismissError()
}

data class LibraryViewState(
    val items: List<LibraryItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: DescriptiveError? = null,
)

class LibraryViewModel(
    private val libraryService: LibraryServiceProtocol,
) : ErrorDismissing {
    var router: LibraryRouting? = null

    private val _viewProperties = MutableStateFlow(LibraryViewState())
    val viewProperties: StateFlow<LibraryViewState> = _viewProperties.asStateFlow()

    suspend fun reload() {
        _viewProperties.value = LibraryViewState()
        load()
    }

    suspend fun load() {
        _viewProperties.update { it.copy(isLoading = true) }
        try {
            val items = libraryService.loadItems()
            _viewProperties.update { it.copy(items = items, error = null, isLoading = false) }
        } catch (error: Exception) {
            if (error is DescriptiveError) {
                _viewProperties.update { it.copy(error = error, isLoading = false) }
                return
            }
            _viewProperties.update {
                it.copy(
                    error = Errors.Library.CopyFailed("library", error.localizedMessage ?: error.toString()),
                    isLoading = false,
                )
            }
        }
    }

    fun open(item: LibraryItem) {
        router?.openBook(item)
    }

    override fun dismissError() {
        // Caller launches reload on the Main dispatcher, matching iOS `Task { await reload() }`.
    }

    suspend fun importBook(from: File) {
        try {
            libraryService.importBook(from)
            load()
        } catch (error: Exception) {
            if (error is DescriptiveError) {
                _viewProperties.update { it.copy(error = error) }
                return
            }
            _viewProperties.update {
                it.copy(
                    error = Errors.Library.CopyFailed(from.name, error.localizedMessage ?: error.toString())
                )
            }
        }
    }

    fun presentImportFailure(error: Throwable) {
        _viewProperties.update {
            it.copy(
                error = Errors.Library.CopyFailed(
                    "import",
                    error.localizedMessage ?: error.toString(),
                )
            )
        }
    }
}

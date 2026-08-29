package miko.biblossurfer.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import miko.biblossurfer.data.LocalBookService
import miko.biblossurfer.data.MockServiceProvider
import miko.biblossurfer.data.model.LibraryItem
import miko.biblossurfer.ui.subviews.ErrorView
import miko.biblossurfer.ui.subviews.Photo
import miko.biblossurfer.util.AccessibilityIdentifiers
import miko.biblossurfer.util.StyleConstants
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.viewProperties.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (state.items.isEmpty()) {
            viewModel.load()
        }
    }

    val importer = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        kotlinx.coroutines.MainScope().launchImport(viewModel, context, uri)
    }

    if (state.error != null) {
        ErrorView(
            error = state.error!!,
            handler = object : ErrorDismissing {
                override fun dismissError() {
                    kotlinx.coroutines.MainScope().launch { viewModel.reload() }
                }
            },
            modifier = modifier,
        )
        return
    }

    Scaffold(
        modifier = modifier.testTag(AccessibilityIdentifiers.Library.title),
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                actions = {
                    IconButton(
                        onClick = {
                            importer.launch(arrayOf("application/epub+zip", "application/pdf"))
                        },
                        modifier = Modifier.testTag(AccessibilityIdentifiers.Library.importButton),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Import")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading && state.items.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag(AccessibilityIdentifiers.Library.loading),
                    )
                }
                state.items.isEmpty() -> {
                    Text(
                        text = "No books yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag(AccessibilityIdentifiers.Library.emptyState),
                    )
                }
                else -> {
                    LibraryGrid(
                        items = state.items,
                        onOpen = viewModel::open,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryGrid(
    items: List<LibraryItem>,
    onOpen: (LibraryItem) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = StyleConstants.coverGridMinWidth.dp),
        contentPadding = PaddingValues(StyleConstants.contentMargin.dp),
        horizontalArrangement = Arrangement.spacedBy(StyleConstants.coverGridSpacing.dp),
        verticalArrangement = Arrangement.spacedBy(StyleConstants.coverGridSpacing.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag(AccessibilityIdentifiers.Library.grid),
    ) {
        items(items, key = { it.id }) { item ->
            Column(
                modifier = Modifier
                    .clickable { onOpen(item) }
                    .testTag(AccessibilityIdentifiers.Library.cell(item.title)),
                verticalArrangement = Arrangement.spacedBy(StyleConstants.tightSpacing.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(StyleConstants.coverAspectRatio)
                        .clip(RoundedCornerShape(StyleConstants.cornerRadius.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                ) {
                    Photo(
                        url = item.coverURL,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(item.progression.toFloat().coerceIn(0f, 1f))
                            .height(StyleConstants.progressBarHeight.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchImport(
    viewModel: LibraryViewModel,
    context: android.content.Context,
    uri: Uri,
) {
    launch {
        try {
            val copied = withContext(Dispatchers.IO) {
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "import.epub"
                val dest = File(context.cacheDir, name)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Could not open $uri")
                dest
            }
            viewModel.importBook(copied)
        } catch (error: Exception) {
            viewModel.presentImportFailure(error)
        }
    }
}

@Preview(name = "Stub library")
@Composable
private fun LibraryScreenPreview() {
    val context = LocalContext.current
    LibraryScreen(viewModel = LibraryViewModel(MockServiceProvider(context).libraryService))
}

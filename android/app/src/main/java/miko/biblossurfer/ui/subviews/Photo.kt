package miko.biblossurfer.ui.subviews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import android.graphics.BitmapFactory
import miko.biblossurfer.util.AccessibilityIdentifiers
import miko.biblossurfer.util.StyleConstants
import java.io.File
import java.net.URL

/**
 * Renders an image from either a local file or a remote URL, without imposing any size, shape, or
 * clipping — the caller owns layout. Book covers extracted from a publication are local files, so
 * that path is the common one; the remote path exists for future OPDS catalogues.
 */
@Composable
fun Photo(
    url: File?,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    remoteUrl: String? = null,
) {
    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isLoading && url == null && remoteUrl == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.testTag(AccessibilityIdentifiers.Library.loading),
                )
            }
            url != null && url.isFile && url.exists() -> {
                val bitmap = remember(url.path) { BitmapFactory.decodeFile(url.path) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Placeholder()
                }
            }
            remoteUrl != null -> {
                // Remote covers are a future OPDS path; until then show the placeholder.
                Placeholder()
            }
            else -> Placeholder()
        }
    }
}

@Composable
private fun Placeholder() {
    Icon(
        imageVector = Icons.Filled.MenuBook,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.outline,
        modifier = Modifier
            .fillMaxSize()
            .padding(StyleConstants.contentMargin.dp),
    )
}

@Preview(name = "Loading")
@Composable
private fun PhotoLoadingPreview() {
    Photo(url = null, isLoading = true, modifier = Modifier.padding(8.dp))
}

@Preview(name = "Missing cover")
@Composable
private fun PhotoMissingPreview() {
    Photo(url = null, modifier = Modifier.padding(8.dp))
}

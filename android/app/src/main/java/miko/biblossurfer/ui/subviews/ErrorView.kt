package miko.biblossurfer.ui.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import miko.biblossurfer.data.DescriptiveError
import miko.biblossurfer.data.Errors
import miko.biblossurfer.ui.library.ErrorDismissing
import miko.biblossurfer.util.AccessibilityIdentifiers
import miko.biblossurfer.util.StyleConstants

@Composable
fun ErrorView(
    error: DescriptiveError,
    handler: ErrorDismissing? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(StyleConstants.contentMargin.dp),
        verticalArrangement = Arrangement.spacedBy(StyleConstants.stackSpacing.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = error.title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag(AccessibilityIdentifiers.Error.title),
        )
        Text(
            text = error.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(AccessibilityIdentifiers.Error.description),
        )
        Button(
            onClick = { handler?.dismissError() },
            modifier = Modifier.testTag(AccessibilityIdentifiers.Error.dismiss),
        ) {
            Text("Dismiss")
        }
    }
}

@Preview(name = "Unsupported format")
@Composable
private fun ErrorViewUnsupportedPreview() {
    ErrorView(error = Errors.Library.UnsupportedFormat("mobi"))
}

@Preview(name = "Nothing to read aloud")
@Composable
private fun ErrorViewTtsPreview() {
    ErrorView(error = Errors.Tts.NoSpeakableContent)
}

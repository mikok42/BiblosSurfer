package miko.biblossurfer.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import miko.biblossurfer.util.AccessibilityIdentifiers
import miko.biblossurfer.util.StyleConstants

@Composable
fun TtsPanelView(
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.viewProperties.collectAsStateWithLifecycle()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(StyleConstants.contentMargin.dp)
            .testTag(AccessibilityIdentifiers.Reader.ttsPanel),
        shape = RoundedCornerShape(StyleConstants.ttsPanelCornerRadius.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(StyleConstants.ttsPanelPadding.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = viewModel::previousUtterance,
                modifier = Modifier.testTag(AccessibilityIdentifiers.Reader.ttsPrevious),
            ) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
            }
            IconButton(
                onClick = viewModel::playPause,
                modifier = Modifier.testTag(
                    if (state.isPlaying) AccessibilityIdentifiers.Reader.ttsPause
                    else AccessibilityIdentifiers.Reader.ttsPlay
                ),
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                )
            }
            IconButton(
                onClick = viewModel::stopReading,
                modifier = Modifier.testTag(AccessibilityIdentifiers.Reader.ttsStop),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = "Stop")
            }
            IconButton(
                onClick = viewModel::nextUtterance,
                modifier = Modifier.testTag(AccessibilityIdentifiers.Reader.ttsNext),
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next")
            }
        }
    }
}

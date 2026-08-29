package miko.biblossurfer.ui.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import miko.biblossurfer.util.AccessibilityIdentifiers
import org.readium.r2.navigator.preferences.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsView(
    viewModel: ReaderViewModel,
    onOpenTts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.viewProperties.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                actions = {
                    TextButton(
                        onClick = viewModel::closeSettings,
                        modifier = Modifier.testTag(AccessibilityIdentifiers.Settings.done),
                    ) { Text("Done") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Reading", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp, 8.dp))
            ListItem(
                headlineContent = {
                    Text("Font size ${"%.1f".format(viewModel.settings.fontSize)}×")
                },
                supportingContent = {
                    Slider(
                        value = viewModel.settings.fontSize.toFloat(),
                        onValueChange = {
                            viewModel.settings.fontSize = it.toDouble()
                            viewModel.settingsDidChange()
                        },
                        valueRange = 0.7f..2.0f,
                        modifier = Modifier.testTag(AccessibilityIdentifiers.Settings.fontSize),
                    )
                }
            )
            ListItem(
                headlineContent = { Text("Typeface") },
                supportingContent = {
                    Column(modifier = Modifier.testTag(AccessibilityIdentifiers.Settings.fontFamily)) {
                        listOf("Original", "Georgia", "Palatino", "Helvetica").forEach { family ->
                            FilterChip(
                                selected = viewModel.settings.fontFamily == family,
                                onClick = {
                                    viewModel.settings.fontFamily = family
                                    viewModel.settingsDidChange()
                                },
                                label = { Text(family) },
                            )
                        }
                    }
                }
            )
            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = {
                    Column(modifier = Modifier.testTag(AccessibilityIdentifiers.Settings.theme)) {
                        listOf(Theme.LIGHT to "Light", Theme.DARK to "Dark", Theme.SEPIA to "Sepia").forEach { (theme, label) ->
                            FilterChip(
                                selected = viewModel.settings.theme == theme,
                                onClick = {
                                    viewModel.settings.theme = theme
                                    viewModel.settingsDidChange()
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            )
            ListItem(
                headlineContent = { Text("Scroll") },
                trailingContent = {
                    Switch(
                        checked = viewModel.settings.scroll,
                        onCheckedChange = {
                            viewModel.settings.scroll = it
                            viewModel.settingsDidChange()
                        },
                        modifier = Modifier.testTag(AccessibilityIdentifiers.Settings.scrollMode),
                    )
                }
            )
            if (state.canSpeak) {
                ListItem(
                    headlineContent = { Text("Text to speech") },
                    modifier = Modifier.testTag(AccessibilityIdentifiers.Settings.tts),
                    supportingContent = { Text("Voice, rate, chunks") },
                    trailingContent = {
                        TextButton(onClick = onOpenTts) { Text("Open") }
                    }
                )
            }
        }
    }
}

package miko.biblossurfer.ui.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import miko.biblossurfer.data.TTSChunkUnit
import miko.biblossurfer.util.AccessibilityIdentifiers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TTSSettingsView(
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier,
) {
    val _state by viewModel.viewProperties.collectAsStateWithLifecycle()
    key(viewModel.settingsEpoch) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text("TTS") },
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
                Text("Voice", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp, 8.dp))
                ListItem(
                    headlineContent = { Text("Language") },
                    supportingContent = {
                        val selected = viewModel.settings.defaultLanguage
                        Column {
                            FilterChip(
                                selected = selected == null,
                                onClick = { viewModel.selectTTSLanguage(null) },
                                label = { Text("Publication") },
                                modifier = Modifier.testTag(AccessibilityIdentifiers.Settings.language),
                            )
                            viewModel.ttsLanguages().forEach { code ->
                                FilterChip(
                                    selected = selected == code,
                                    onClick = { viewModel.selectTTSLanguage(code) },
                                    label = { Text(code) },
                                )
                            }
                        }
                    }
                )
                ListItem(
                    headlineContent = { Text("Voice") },
                    supportingContent = {
                        Column {
                            FilterChip(
                                selected = viewModel.settings.voiceIdentifier == null,
                                onClick = {
                                    viewModel.settings.voiceIdentifier = null
                                    viewModel.settingsDidChange()
                                },
                                label = { Text("Default (best available)") },
                                modifier = Modifier.testTag(AccessibilityIdentifiers.Settings.voice),
                            )
                            viewModel.ttsVoices().forEach { voice ->
                                FilterChip(
                                    selected = viewModel.settings.voiceIdentifier == voice.identifier,
                                    onClick = {
                                        viewModel.settings.voiceIdentifier = voice.identifier
                                        viewModel.settingsDidChange()
                                    },
                                    label = { Text(voice.settingsDisplayName) },
                                )
                            }
                        }
                    }
                )

                Text("Speech", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp, 8.dp))
                ListItem(
                    headlineContent = { Text("Use system settings") },
                    trailingContent = {
                        Switch(
                            checked = viewModel.settings.useSystemSpeechSettings,
                            onCheckedChange = {
                                viewModel.settings.useSystemSpeechSettings = it
                                viewModel.settingsDidChange()
                            },
                            modifier = Modifier.testTag(AccessibilityIdentifiers.Settings.systemSpeech),
                        )
                    }
                )
                if (!viewModel.settings.useSystemSpeechSettings) {
                    LabeledSlider(
                        title = "Rate",
                        value = viewModel.settings.speechRate,
                        range = 0.1f..4.0f,
                        identifier = AccessibilityIdentifiers.Settings.speechRate,
                    ) {
                        viewModel.settings.speechRate = it
                        viewModel.settingsDidChange()
                    }
                    LabeledSlider(
                        title = "Pitch",
                        value = viewModel.settings.pitchMultiplier,
                        range = 0.5f..2.0f,
                        identifier = AccessibilityIdentifiers.Settings.pitch,
                    ) {
                        viewModel.settings.pitchMultiplier = it
                        viewModel.settingsDidChange()
                    }
                    LabeledSlider(
                        title = "Volume",
                        value = viewModel.settings.speechVolume,
                        range = 0f..1f,
                        identifier = AccessibilityIdentifiers.Settings.volume,
                    ) {
                        viewModel.settings.speechVolume = it
                        viewModel.settingsDidChange()
                    }
                }

                Text("Timing", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp, 8.dp))
                ListItem(
                    headlineContent = {
                        Text("Pause before ${"%.1f".format(viewModel.settings.preUtteranceDelay)}s")
                    },
                    modifier = Modifier.testTag(AccessibilityIdentifiers.Settings.preUtteranceDelay),
                    supportingContent = {
                        Slider(
                            value = viewModel.settings.preUtteranceDelay.toFloat(),
                            onValueChange = {
                                viewModel.settings.preUtteranceDelay = it.toDouble()
                                viewModel.settingsDidChange()
                            },
                            valueRange = 0f..2f,
                        )
                    }
                )
                ListItem(
                    headlineContent = {
                        Text("Pause after ${"%.1f".format(viewModel.settings.postUtteranceDelay)}s")
                    },
                    modifier = Modifier.testTag(AccessibilityIdentifiers.Settings.postUtteranceDelay),
                    supportingContent = {
                        Slider(
                            value = viewModel.settings.postUtteranceDelay.toFloat(),
                            onValueChange = {
                                viewModel.settings.postUtteranceDelay = it.toDouble()
                                viewModel.settingsDidChange()
                            },
                            valueRange = 0f..2f,
                        )
                    }
                )

                Text("Chunks", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(16.dp, 8.dp))
                Row(modifier = Modifier.padding(horizontal = 16.dp).testTag(AccessibilityIdentifiers.Settings.chunkUnit)) {
                    TTSChunkUnit.allCases.forEach { unit ->
                        FilterChip(
                            selected = viewModel.settings.chunkUnit == unit,
                            onClick = {
                                viewModel.settings.chunkUnit = unit
                                viewModel.settingsDidChange()
                            },
                            label = { Text(unit.title) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun LabeledSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    identifier: String,
    onChange: (Float) -> Unit,
) {
    ListItem(
        headlineContent = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(title, modifier = Modifier.weight(1f))
                Text("%.2f".format(value), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        supportingContent = {
            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = range,
                modifier = Modifier.testTag(identifier),
            )
        }
    )
}

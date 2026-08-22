//
//  TTSSettingsView.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import AVFoundation
import SwiftUI

struct TTSSettingsView: View {
    @Bindable var viewModel: ReaderViewModel

    private var bindings: BindableSettings {
        BindableSettings(viewModel: viewModel)
    }

    var body: some View {
        let _ = viewModel.settingsEpoch
        Form {
            Section("Voice") {
                Picker("Language", selection: bindings.defaultLanguage) {
                    Text("Publication").tag(Optional<String>.none)
                    ForEach(viewModel.ttsLanguages, id: \.code.bcp47) { language in
                        Text(language.localizedDescription()).tag(Optional(language.code.bcp47))
                    }
                }
                .accessibilityIdentifier(AccessibilityIdentifiers.Settings.language)

                Picker("Voice", selection: bindings.voiceIdentifier) {
                    Text("Default (best available)").tag(Optional<String>.none)
                    ForEach(viewModel.ttsVoices, id: \.identifier) { voice in
                        Text(voice.settingsDisplayName).tag(Optional(voice.identifier))
                    }
                }
                .accessibilityIdentifier(AccessibilityIdentifiers.Settings.voice)
            }

            Section("Speech") {
                Toggle("Use system settings", isOn: bindings.useSystemSpeechSettings)
                    .accessibilityIdentifier(AccessibilityIdentifiers.Settings.systemSpeech)

                if !viewModel.settings.useSystemSpeechSettings {
                    labeledSlider(
                        title: "Rate",
                        value: bindings.speechRate,
                        range: AVSpeechUtteranceMinimumSpeechRate ... AVSpeechUtteranceMaximumSpeechRate,
                        identifier: AccessibilityIdentifiers.Settings.speechRate
                    )
                    labeledSlider(
                        title: "Pitch",
                        value: bindings.pitchMultiplier,
                        range: 0.5 ... 2.0,
                        identifier: AccessibilityIdentifiers.Settings.pitch
                    )
                    labeledSlider(
                        title: "Volume",
                        value: bindings.speechVolume,
                        range: 0 ... 1,
                        identifier: AccessibilityIdentifiers.Settings.volume
                    )
                }
            }

            Section("Timing") {
                Stepper(
                    value: bindings.preUtteranceDelay,
                    in: 0 ... 2,
                    step: 0.1
                ) {
                    Text("Pause before \(viewModel.settings.preUtteranceDelay, specifier: "%.1f")s")
                }
                .accessibilityIdentifier(AccessibilityIdentifiers.Settings.preUtteranceDelay)

                Stepper(
                    value: bindings.postUtteranceDelay,
                    in: 0 ... 2,
                    step: 0.1
                ) {
                    Text("Pause after \(viewModel.settings.postUtteranceDelay, specifier: "%.1f")s")
                }
                .accessibilityIdentifier(AccessibilityIdentifiers.Settings.postUtteranceDelay)
            }

            Section("Chunks") {
                Picker("Split by", selection: bindings.chunkUnit) {
                    ForEach(TTSChunkUnit.allCases) { unit in
                        Text(unit.title).tag(unit)
                    }
                }
                .pickerStyle(.segmented)
                .accessibilityIdentifier(AccessibilityIdentifiers.Settings.chunkUnit)
            }
        }
        .navigationTitle("TTS")
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Done", action: viewModel.closeSettings)
                    .accessibilityIdentifier(AccessibilityIdentifiers.Settings.done)
            }
        }
    }

    private func labeledSlider(
        title: String,
        value: Binding<Float>,
        range: ClosedRange<Float>,
        identifier: String
    ) -> some View {
        VStack(alignment: .leading) {
            HStack {
                Text(title)
                Spacer()
                Text(value.wrappedValue, format: .number.precision(.fractionLength(2)))
                    .foregroundStyle(.secondary)
            }
            Slider(value: value, in: range)
                .accessibilityIdentifier(identifier)
        }
    }
}

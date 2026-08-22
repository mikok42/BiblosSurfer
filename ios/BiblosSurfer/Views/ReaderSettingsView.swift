//
//  ReaderSettingsView.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import AVFoundation
import ReadiumNavigator
import SwiftUI

struct ReaderSettingsView: View {
    @Bindable var viewModel: ReaderViewModel
    var voices: [TTSVoice]
    var onChange: () -> Void
    var onClose: () -> Void

    var body: some View {
        NavigationStack {
            Form {
                Section("Reading") {
                    Stepper(
                        value: BindableSettings(store: viewModel.settings, onChange: onChange).fontSize,
                        in: 0.7 ... 2.0,
                        step: 0.1
                    ) {
                        Text("Font size \(viewModel.settings.fontSize, specifier: "%.1f")×")
                    }
                    .accessibilityIdentifier(AccessibilityIdentifiers.Settings.fontSize)

                    Picker("Typeface", selection: BindableSettings(store: viewModel.settings, onChange: onChange).fontFamily) {
                        Text("Original").tag("Original")
                        Text("Georgia").tag("Georgia")
                        Text("Palatino").tag("Palatino")
                        Text("Helvetica").tag("Helvetica")
                    }
                    .accessibilityIdentifier(AccessibilityIdentifiers.Settings.fontFamily)

                    Picker("Theme", selection: BindableSettings(store: viewModel.settings, onChange: onChange).theme) {
                        Text("Light").tag(Theme.light)
                        Text("Dark").tag(Theme.dark)
                        Text("Sepia").tag(Theme.sepia)
                    }
                    .accessibilityIdentifier(AccessibilityIdentifiers.Settings.theme)

                    Toggle("Scroll", isOn: BindableSettings(store: viewModel.settings, onChange: onChange).scroll)
                        .accessibilityIdentifier(AccessibilityIdentifiers.Settings.scrollMode)
                }

                if viewModel.viewProperties.canSpeak {
                    Section("Read aloud") {
                        Picker("Voice", selection: BindableSettings(store: viewModel.settings, onChange: onChange).voiceIdentifier) {
                            Text("Default").tag(Optional<String>.none)
                            ForEach(voices, id: \.identifier) { voice in
                                Text(voice.name).tag(Optional(voice.identifier))
                            }
                        }
                        .accessibilityIdentifier(AccessibilityIdentifiers.Settings.voice)

                        Slider(
                            value: BindableSettings(store: viewModel.settings, onChange: onChange).speechRate,
                            in: AVSpeechUtteranceMinimumSpeechRate ... AVSpeechUtteranceMaximumSpeechRate
                        )
                        .accessibilityIdentifier(AccessibilityIdentifiers.Settings.speechRate)
                    }
                }
            }
            .navigationTitle("Settings")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done", action: onClose)
                        .accessibilityIdentifier(AccessibilityIdentifiers.Settings.done)
                }
            }
        }
    }
}

private struct BindableSettings {
    let store: ReaderSettingsStore
    let onChange: () -> Void

    var fontSize: Binding<Double> {
        Binding(
            get: { store.fontSize },
            set: { store.fontSize = $0; onChange() }
        )
    }

    var fontFamily: Binding<String> {
        Binding(
            get: { store.fontFamily },
            set: { store.fontFamily = $0; onChange() }
        )
    }

    var theme: Binding<Theme> {
        Binding(
            get: { store.theme },
            set: { store.theme = $0; onChange() }
        )
    }

    var scroll: Binding<Bool> {
        Binding(
            get: { store.scroll },
            set: { store.scroll = $0; onChange() }
        )
    }

    var voiceIdentifier: Binding<String?> {
        Binding(
            get: { store.voiceIdentifier },
            set: { store.voiceIdentifier = $0; onChange() }
        )
    }

    var speechRate: Binding<Float> {
        Binding(
            get: { store.speechRate },
            set: { store.speechRate = $0; onChange() }
        )
    }
}

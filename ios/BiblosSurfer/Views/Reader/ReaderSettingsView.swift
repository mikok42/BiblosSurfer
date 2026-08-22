//
//  ReaderSettingsView.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import ReadiumNavigator
import SwiftUI

struct ReaderSettingsView: View {
    @Bindable var viewModel: ReaderViewModel
    @State private var isTTSPresented = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Reading") {
                    Stepper(
                        value: BindableSettings(viewModel: viewModel).fontSize,
                        in: 0.7 ... 2.0,
                        step: 0.1
                    ) {
                        Text("Font size \(viewModel.settings.fontSize, specifier: "%.1f")×")
                    }
                    .accessibilityIdentifier(AccessibilityIdentifiers.Settings.fontSize)

                    Picker("Typeface", selection: BindableSettings(viewModel: viewModel).fontFamily) {
                        Text("Original").tag("Original")
                        Text("Georgia").tag("Georgia")
                        Text("Palatino").tag("Palatino")
                        Text("Helvetica").tag("Helvetica")
                    }
                    .accessibilityIdentifier(AccessibilityIdentifiers.Settings.fontFamily)

                    Picker("Theme", selection: BindableSettings(viewModel: viewModel).theme) {
                        Text("Light").tag(Theme.light)
                        Text("Dark").tag(Theme.dark)
                        Text("Sepia").tag(Theme.sepia)
                    }
                    .accessibilityIdentifier(AccessibilityIdentifiers.Settings.theme)

                    Toggle("Scroll", isOn: BindableSettings(viewModel: viewModel).scroll)
                        .accessibilityIdentifier(AccessibilityIdentifiers.Settings.scrollMode)
                }

                if viewModel.viewProperties.canSpeak {
                    Section {
                        NavigationLink {
                            TTSSettingsView(viewModel: viewModel)
                        } label: {
                            Label("Text to speech", systemImage: "speaker.wave.2")
                        }
                        .accessibilityIdentifier(AccessibilityIdentifiers.Settings.tts)
                    }
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .navigationDestination(isPresented: $isTTSPresented) {
                TTSSettingsView(viewModel: viewModel)
            }
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done", action: viewModel.closeSettings)
                        .accessibilityIdentifier(AccessibilityIdentifiers.Settings.done)
                }
            }
        }
    }
}

struct BindableSettings {
    let viewModel: ReaderViewModel
    var store: ReaderSettingsStore { viewModel.settings }

    var fontSize: Binding<Double> {
        Binding(
            get: { store.fontSize },
            set: { store.fontSize = $0; viewModel.settingsDidChange() }
        )
    }

    var fontFamily: Binding<String> {
        Binding(
            get: { store.fontFamily },
            set: { store.fontFamily = $0; viewModel.settingsDidChange() }
        )
    }

    var theme: Binding<Theme> {
        Binding(
            get: { store.theme },
            set: { store.theme = $0; viewModel.settingsDidChange() }
        )
    }

    var scroll: Binding<Bool> {
        Binding(
            get: { store.scroll },
            set: { store.scroll = $0; viewModel.settingsDidChange() }
        )
    }

    var defaultLanguage: Binding<String?> {
        Binding(
            get: { store.defaultLanguage },
            set: { viewModel.selectTTSLanguage($0) }
        )
    }

    var voiceIdentifier: Binding<String?> {
        Binding(
            get: { store.voiceIdentifier },
            set: { store.voiceIdentifier = $0; viewModel.settingsDidChange() }
        )
    }

    var speechRate: Binding<Float> {
        Binding(
            get: { store.speechRate },
            set: { store.speechRate = $0; viewModel.settingsDidChange() }
        )
    }

    var pitchMultiplier: Binding<Float> {
        Binding(
            get: { store.pitchMultiplier },
            set: { store.pitchMultiplier = $0; viewModel.settingsDidChange() }
        )
    }

    var speechVolume: Binding<Float> {
        Binding(
            get: { store.speechVolume },
            set: { store.speechVolume = $0; viewModel.settingsDidChange() }
        )
    }

    var preUtteranceDelay: Binding<TimeInterval> {
        Binding(
            get: { store.preUtteranceDelay },
            set: { store.preUtteranceDelay = $0; viewModel.settingsDidChange() }
        )
    }

    var postUtteranceDelay: Binding<TimeInterval> {
        Binding(
            get: { store.postUtteranceDelay },
            set: { store.postUtteranceDelay = $0; viewModel.settingsDidChange() }
        )
    }

    var chunkUnit: Binding<TTSChunkUnit> {
        Binding(
            get: { store.chunkUnit },
            set: { store.chunkUnit = $0; viewModel.settingsDidChange() }
        )
    }

    var useSystemSpeechSettings: Binding<Bool> {
        Binding(
            get: { store.useSystemSpeechSettings },
            set: { store.useSystemSpeechSettings = $0; viewModel.settingsDidChange() }
        )
    }
}

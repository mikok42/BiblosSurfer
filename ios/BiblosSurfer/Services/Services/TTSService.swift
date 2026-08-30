//
//  TTSService.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import AVFoundation
import Foundation
import MediaPlayer
import ReadiumNavigator
import ReadiumShared
import UIKit

protocol TTSServiceDelegate: AnyObject {
    func ttsService(_ service: TTSService, didChange state: PublicationSpeechSynthesizer.State)
    func ttsService(_ service: TTSService, didFail error: DescriptiveError)
}

final class TTSService: NSObject {
    private let synthesizer: PublicationSpeechSynthesizer
    private let engine: AVTTSEngine
    private let settings: ReaderSettingsStore
    private let bookTitle: String
    private let startAnchor: TTSStartAnchor
    private let publicationLanguage: Language?
    weak var delegate: TTSServiceDelegate?

    var availableVoices: [TTSVoice] {
        synthesizer.availableVoices
    }

    init?(publication: Publication, settings: ReaderSettingsStore, bookTitle: String) {
        let engine = AVTTSEngine()
        let startAnchor = TTSStartAnchor()
        guard let synthesizer = PublicationSpeechSynthesizer(
            publication: publication,
            config: PublicationSpeechSynthesizer.Configuration(
                defaultLanguage: settings.defaultLanguage.map { Language(code: .bcp47($0)) },
                voiceIdentifier: settings.voiceIdentifier
            ),
            audioSessionConfig: AudioSession.Configuration(
                category: .playback,
                mode: .default,
                routeSharingPolicy: .longFormAudio
            ),
            engineFactory: { engine },
            tokenizerFactory: { defaultLanguage in
                startAnchor.tokenizer(defaultLanguage: defaultLanguage, chunkUnit: settings.chunkUnit.textUnit)
            }
        ) else {
            return nil
        }
        self.engine = engine
        self.synthesizer = synthesizer
        self.settings = settings
        self.bookTitle = bookTitle
        self.startAnchor = startAnchor
        self.publicationLanguage = publication.metadata.language
        super.init()
        engine.delegate = self
        synthesizer.delegate = self
        configureRemoteCommands()
        applySettings()
    }

    deinit {
        MPRemoteCommandCenter.shared().playCommand.removeTarget(self)
        MPRemoteCommandCenter.shared().pauseCommand.removeTarget(self)
        MPRemoteCommandCenter.shared().stopCommand.removeTarget(self)
        MPRemoteCommandCenter.shared().nextTrackCommand.removeTarget(self)
        MPRemoteCommandCenter.shared().previousTrackCommand.removeTarget(self)
    }

    func start(from locator: Locator?) {
        applySettings()
        let highlight = locator?.text.highlight?.collapsedWhitespace
        startAnchor.highlight = (highlight?.isEmpty == false) ? highlight : nil
        synthesizer.start(from: locator)
        updateNowPlaying(isPlaying: true)
    }

    func applySettings() {
        let language = settings.defaultLanguage.map { Language(code: .bcp47($0)) } ?? publicationLanguage
        synthesizer.config.defaultLanguage = language
        synthesizer.config.voiceIdentifier = settings.voiceIdentifier
            ?? availableVoices.preferredAppleVoice(for: language)?.identifier
    }

    func stop() {
        synthesizer.stop()
        updateNowPlaying(isPlaying: false)
    }

    func pause() {
        synthesizer.pause()
        updateNowPlaying(isPlaying: false)
    }

    func resume() {
        synthesizer.resume()
        updateNowPlaying(isPlaying: true)
    }

    func pauseOrResume() {
        switch synthesizer.state {
        case .playing:
            pause()
        case .paused:
            resume()
        case .stopped:
            start(from: nil)
        }
    }

    func next() {
        synthesizer.next()
    }

    func previous() {
        synthesizer.previous()
    }

    private func configureRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()
        center.playCommand.isEnabled = true
        center.pauseCommand.isEnabled = true
        center.stopCommand.isEnabled = true
        center.nextTrackCommand.isEnabled = true
        center.previousTrackCommand.isEnabled = true
        center.playCommand.addTarget { [weak self] _ in
            self?.resume()
            return .success
        }
        center.pauseCommand.addTarget { [weak self] _ in
            self?.pause()
            return .success
        }
        center.stopCommand.addTarget { [weak self] _ in
            self?.stop()
            return .success
        }
        center.nextTrackCommand.addTarget { [weak self] _ in
            self?.next()
            return .success
        }
        center.previousTrackCommand.addTarget { [weak self] _ in
            self?.previous()
            return .success
        }
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [])
        try? AVAudioSession.sharedInstance().setActive(true)
    }

    private func updateNowPlaying(isPlaying: Bool) {
        MPNowPlayingInfoCenter.default().nowPlayingInfo = [
            MPMediaItemPropertyTitle: bookTitle,
            MPNowPlayingInfoPropertyPlaybackRate: isPlaying ? 1.0 : 0.0
        ]
    }
}

extension TTSService: PublicationSpeechSynthesizerDelegate {
    func publicationSpeechSynthesizer(
        _ synthesizer: PublicationSpeechSynthesizer,
        stateDidChange state: PublicationSpeechSynthesizer.State
    ) {
        delegate?.ttsService(self, didChange: state)
        switch state {
        case .playing:
            updateNowPlaying(isPlaying: true)
        case .paused, .stopped:
            updateNowPlaying(isPlaying: false)
        }
    }

    func publicationSpeechSynthesizer(
        _ synthesizer: PublicationSpeechSynthesizer,
        utterance: PublicationSpeechSynthesizer.Utterance,
        didFailWithError error: PublicationSpeechSynthesizer.Error
    ) {
        delegate?.ttsService(self, didFail: Errors.TTS.engineFailed(underlying: String(describing: error)))
    }
}

extension TTSService: AVTTSEngineDelegate {
    func avTTSEngine(_ engine: AVTTSEngine, didCreateUtterance utterance: AVSpeechUtterance) {
        utterance.prefersAssistiveTechnologySettings = settings.useSystemSpeechSettings
        utterance.preUtteranceDelay = settings.preUtteranceDelay
        utterance.postUtteranceDelay = settings.postUtteranceDelay
        guard !settings.useSystemSpeechSettings else { return }
        utterance.rate = settings.speechRate
        utterance.pitchMultiplier = settings.pitchMultiplier
        utterance.volume = settings.speechVolume
    }
}

/// Skips utterances that sit before a "read from here" highlight. Readium's
/// selection locator is the current *page*, so TTS would otherwise start at
/// the top of the resource.
private final class TTSStartAnchor {
    var highlight: String?

    func tokenizer(defaultLanguage: Language?, chunkUnit: TextUnit) -> ContentTokenizer {
        let tokenize = makeTextContentTokenizer(
            defaultLanguage: defaultLanguage,
            contextSnippetLength: 50,
            textTokenizerFactory: { language in
                makeDefaultTextTokenizer(unit: chunkUnit, language: language)
            }
        )
        return { [weak self] element in
            guard let element = element.preparedForTTS() else { return [] }
            let chunks = try tokenize(element)
            guard let highlight = self?.highlight, !highlight.isEmpty else {
                return chunks
            }
            if let index = chunks.firstIndex(where: { $0.matchesTTSHighlight(highlight) }) {
                self?.highlight = nil
                return Array(chunks[index...])
            }
            return []
        }
    }
}

extension ContentElement {
    func matchesTTSHighlight(_ highlight: String) -> Bool {
        spokenText.overlapsCollapsedText(highlight)
    }

    var spokenText: String {
        (self as? TextualContentElement)?.text ?? locator.text.highlight ?? ""
    }
}

extension String {
    var collapsedWhitespace: String {
        split { $0.isWhitespace || $0.isNewline }.joined(separator: " ")
    }

    func overlapsCollapsedText(_ other: String) -> Bool {
        let haystack = collapsedWhitespace
        let needle = other.collapsedWhitespace
        guard !haystack.isEmpty, !needle.isEmpty else { return false }
        return haystack.localizedStandardContains(needle) || needle.localizedStandardContains(haystack)
    }
}

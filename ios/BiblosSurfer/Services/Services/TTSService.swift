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
    weak var delegate: TTSServiceDelegate?

    var availableVoices: [TTSVoice] {
        synthesizer.availableVoices
    }

    init?(publication: Publication, settings: ReaderSettingsStore, bookTitle: String) {
        let engine = AVTTSEngine()
        guard let synthesizer = PublicationSpeechSynthesizer(
            publication: publication,
            config: PublicationSpeechSynthesizer.Configuration(
                voiceIdentifier: settings.voiceIdentifier
            ),
            engineFactory: { engine }
        ) else {
            return nil
        }
        self.engine = engine
        self.synthesizer = synthesizer
        self.settings = settings
        self.bookTitle = bookTitle
        super.init()
        engine.delegate = self
        synthesizer.delegate = self
        configureRemoteCommands()
    }

    deinit {
        MPRemoteCommandCenter.shared().playCommand.removeTarget(self)
        MPRemoteCommandCenter.shared().pauseCommand.removeTarget(self)
        MPRemoteCommandCenter.shared().stopCommand.removeTarget(self)
        MPRemoteCommandCenter.shared().nextTrackCommand.removeTarget(self)
        MPRemoteCommandCenter.shared().previousTrackCommand.removeTarget(self)
    }

    func start(from locator: Locator?) {
        synthesizer.config.voiceIdentifier = settings.voiceIdentifier
        synthesizer.start(from: locator)
        updateNowPlaying(isPlaying: true)
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
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio, options: [])
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
        utterance.rate = settings.speechRate
    }
}

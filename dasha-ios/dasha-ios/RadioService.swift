import Foundation
import AVFoundation
import MediaPlayer
import UIKit
import SwiftUI

// Модель трека — как Song.java на бэкенде
struct Song: Decodable {
    let songName: String?
    let artist: String?
    let duration: Double?
}

// Модель ответа от /api/stream/status
struct StreamStatus: Decodable {
    let streaming: Bool
    let currentSong: Song?
    let positionSeconds: Double?
}

class RadioService: ObservableObject {
    @Published var status: StreamStatus?
    @Published var playing: Bool = false
    @Published var dotVisible: Bool = true

    private var pollTimer: Timer?
    private var dotTimer: Timer?
    private var player: AVPlayer?
    private var prevSongKey: String?

    private lazy var nowPlayingArtwork: MPMediaItemArtwork? = {
        guard let image = UIImage(named: "NowPlayingArtwork") else { return nil }
        let size = CGSize(width: 1024, height: 1024)
        return MPMediaItemArtwork(boundsSize: size) { _ in
            UIGraphicsImageRenderer(size: size).image { ctx in
                UIColor(red: 0.808, green: 0.890, blue: 0.980, alpha: 1).setFill()
                ctx.fill(CGRect(origin: .zero, size: size))
                image.draw(in: CGRect(origin: .zero, size: size))
            }
        }
    }()

    func start() {
        setupRemoteCommands()
        fetch()
        pollTimer = Timer.scheduledTimer(withTimeInterval: 2.0, repeats: true) { [weak self] _ in
            self?.fetch()
        }
        dotTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self, self.status?.streaming == true else { return }
            withAnimation(.easeInOut(duration: 0.8)) {
                self.dotVisible.toggle()
            }
        }
    }

    func stop() {
        pollTimer?.invalidate()
        dotTimer?.invalidate()
        pollTimer = nil
        dotTimer = nil
    }

    func togglePlay() {
        if playing {
            player?.pause()
            player = nil
            playing = false
        } else {
            try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
            try? AVAudioSession.sharedInstance().setActive(true)
            player = makePlayer()
            player?.play()
            playing = true
            updateNowPlaying()
        }
    }

    private func fetch() {
        URLSession.shared.dataTask(with: Config.statusURL) { data, _, error in
            guard let data, error == nil,
                  let decoded = try? JSONDecoder().decode(StreamStatus.self, from: data) else { return }
            DispatchQueue.main.async {
                self.status = decoded
                let newKey = decoded.currentSong.flatMap { s in
                    s.songName.map { "\($0)-\(s.artist ?? "")" }
                }
                if self.playing, let newKey, newKey != self.prevSongKey {
                    self.reconnect()
                } else if self.playing {
                    self.updateNowPlaying()
                }
                self.prevSongKey = newKey
            }
        }.resume()
    }

    private func reconnect() {
        player?.pause()
        player = makePlayer()
        player?.play()
        updateNowPlaying()
    }

    private func makePlayer() -> AVPlayer {
        let url = Config.streamURL.appending(queryItems: [
            URLQueryItem(name: "t", value: "\(Date().timeIntervalSince1970)")
        ])
        return AVPlayer(playerItem: AVPlayerItem(url: url))
    }

    private func updateNowPlaying() {
        var info: [String: Any] = [
            MPMediaItemPropertyTitle:  status?.currentSong?.songName ?? "dasha.",
            MPMediaItemPropertyArtist: status?.currentSong?.artist  ?? "dasha. radio",
            MPNowPlayingInfoPropertyIsLiveStream: true
        ]
        if let artwork = nowPlayingArtwork {
            info[MPMediaItemPropertyArtwork] = artwork
        }
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func setupRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()
        center.playCommand.addTarget  { [weak self] _ in self?.togglePlay(); return .success }
        center.pauseCommand.addTarget { [weak self] _ in self?.togglePlay(); return .success }
        center.stopCommand.addTarget  { [weak self] _ in self?.togglePlay(); return .success }
    }
}

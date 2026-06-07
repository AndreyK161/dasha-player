import Foundation
import AVFoundation
import MediaPlayer
import UIKit

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

// Сервис для опроса бэкенда и воспроизведения стрима
class RadioService: ObservableObject {
    @Published var status: StreamStatus? = nil
    @Published var playing: Bool = false

    private let statusUrl = URL(string: "http://192.168.0.100:8080/api/stream/status")!
    private let streamUrl = URL(string: "http://192.168.0.100:8080/stream")!
    private var timer: Timer?
    private var player: AVPlayer?
    private var prevSongKey: String? = nil

    func start() {
        setupRemoteCommands()
        fetch()
        timer = Timer.scheduledTimer(withTimeInterval: 2.0, repeats: true) { _ in
            self.fetch()
        }
    }

    func stop() {
        timer?.invalidate()
        timer = nil
    }

    func togglePlay() {
        if playing {
            player?.pause()
            player = nil
            playing = false
        } else {
            try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
            try? AVAudioSession.sharedInstance().setActive(true)
            let url = URL(string: "http://192.168.0.100:8000/stream?t=\(Date().timeIntervalSince1970)")!
            let item = AVPlayerItem(url: url)
            player = AVPlayer(playerItem: item)
            player?.play()
            playing = true
            updateNowPlaying()
        }
    }

    private func fetch() {
        URLSession.shared.dataTask(with: statusUrl) { data, _, error in
            guard let data = data, error == nil else { return }
            if let decoded = try? JSONDecoder().decode(StreamStatus.self, from: data) {
                DispatchQueue.main.async {
                    self.status = decoded

                    // Переподключаемся если трек сменился — как reconnectAudio() на вебе
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
            }
        }.resume()
    }

    private func reconnect() {
        player?.pause()
        let url = URL(string: "http://192.168.0.100:8000/stream?t=\(Date().timeIntervalSince1970)")!
        let item = AVPlayerItem(url: url)
        player = AVPlayer(playerItem: item)
        player?.play()
        updateNowPlaying()
    }

    private func updateNowPlaying() {
        var info = [String: Any]()
        info[MPMediaItemPropertyTitle] = status?.currentSong?.songName ?? "dasha."
        info[MPMediaItemPropertyArtist] = status?.currentSong?.artist ?? "dasha. radio"
        info[MPNowPlayingInfoPropertyIsLiveStream] = true
        if let iconImage = UIImage(named: "NowPlayingArtwork") {
            let size = CGSize(width: 1024, height: 1024)
            let artwork = MPMediaItemArtwork(boundsSize: size) { _ in
                let renderer = UIGraphicsImageRenderer(size: size)
                return renderer.image { ctx in
                    UIColor(red: 0.808, green: 0.890, blue: 0.980, alpha: 1).setFill()
                    ctx.fill(CGRect(origin: .zero, size: size))
                    iconImage.draw(in: CGRect(origin: .zero, size: size))
                }
            }
            info[MPMediaItemPropertyArtwork] = artwork
        }
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func setupRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()
        center.playCommand.addTarget { [weak self] _ in
            self?.togglePlay()
            return .success
        }
        center.pauseCommand.addTarget { [weak self] _ in
            self?.togglePlay()
            return .success
        }
        center.stopCommand.addTarget { [weak self] _ in
            self?.togglePlay()
            return .success
        }
    }
}

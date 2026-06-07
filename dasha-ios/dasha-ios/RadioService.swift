import Foundation
import AVFoundation

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

    private let statusUrl = URL(string: "http://localhost:8080/api/stream/status")!
    private let streamUrl = URL(string: "http://localhost:8080/stream")!
    private var timer: Timer?
    private var player: AVPlayer?

    func start() {
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
        print("togglePlay called, playing = \(playing)")
        if playing {
            player?.pause()
            player = nil
            playing = false
        } else {
            try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
            try? AVAudioSession.sharedInstance().setActive(true)
            let url = URL(string: "http://localhost:8000/stream?t=\(Date().timeIntervalSince1970)")!
            let item = AVPlayerItem(url: url)
            player = AVPlayer(playerItem: item)
            player?.play()
            print("Player status: \(player?.status.rawValue ?? -1)")
            NotificationCenter.default.addObserver(forName: .AVPlayerItemFailedToPlayToEndTime, object: item, queue: .main) { n in
                print("Stream error: \(n.userInfo?[AVPlayerItemFailedToPlayToEndTimeErrorKey] ?? "unknown")")
            }
            playing = true
        }
    }

    private func fetch() {
        URLSession.shared.dataTask(with: statusUrl) { data, _, error in
            guard let data = data, error == nil else { return }
            if let decoded = try? JSONDecoder().decode(StreamStatus.self, from: data) {
                DispatchQueue.main.async {
                    self.status = decoded
                }
            }
        }.resume()
    }
}

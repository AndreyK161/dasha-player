import SwiftUI

// Цвета приложения
private extension Color {
    static let appBackground = Color(red: 0.808, green: 0.890, blue: 0.980)
    static let appPrimary    = Color(red: 0.176, green: 0.176, blue: 0.176)
    static let appTrack      = Color(red: 0.75,  green: 0.75,  blue: 0.75)
}

struct ContentView: View {
    @StateObject private var radio = RadioService()
    @GestureState private var isPressed: Bool = false

    var trackText: String {
        guard let song = radio.status?.currentSong else { return "" }
        let name   = song.songName ?? "Неизвестный трек"
        let artist = song.artist   ?? "Неизвестный артист"
        return "\(name) – \(artist)"
    }

    var progress: Double {
        guard let song     = radio.status?.currentSong,
              let duration = song.duration,
              let position = radio.status?.positionSeconds,
              duration > 0 else { return 0 }
        return min(Double(position) / duration, 1.0)
    }

    var isLive: Bool {
        radio.status?.streaming == true
    }

    var body: some View {
        ZStack {
            Color.appBackground.ignoresSafeArea()

            VStack(spacing: 0) {
                // Header
                HStack {
                    HStack(spacing: 7) {
                        Circle()
                            .fill(Color.appPrimary)
                            .frame(width: 6, height: 6)
                            .opacity(isLive ? (radio.dotVisible ? 1.0 : 0.3) : 1.0)
                        Text(isLive ? "в эфире" : "эфир не идёт")
                            .font(.system(size: 13))
                            .tracking(0.4)
                            .foregroundColor(.appPrimary)
                    }
                    .opacity(isLive ? 0.7 : 0.3)
                    Spacer()
                }
                .padding(.horizontal, 32)
                .padding(.vertical, 24)

                Spacer()

                // Кнопка play
                Text("dasha.")
                    .font(.system(size: 72, weight: .regular))
                    .tracking(-1)
                    .foregroundColor(.appPrimary)
                    .scaleEffect(isPressed ? 1.03 : 1.0)
                    .animation(.easeInOut(duration: 0.2), value: isPressed)
                    .gesture(
                        DragGesture(minimumDistance: 0)
                            .updating($isPressed) { _, state, _ in state = true }
                            .onEnded { _ in radio.togglePlay() }
                    )

                VStack(spacing: 24) {
                    // Прогресс-бар
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            RoundedRectangle(cornerRadius: 999)
                                .fill(Color.appTrack)
                                .frame(height: 4)
                            RoundedRectangle(cornerRadius: 999)
                                .fill(Color.appPrimary)
                                .frame(width: geo.size.width * progress, height: 4)
                                .animation(.linear(duration: 1), value: progress)
                        }
                    }
                    .frame(width: 200, height: 4)

                    // Название трека
                    Text(trackText)
                        .font(.system(size: 14))
                        .tracking(0.3)
                        .foregroundColor(.appPrimary)
                        .opacity(0.7)

                    // Ссылки
                    HStack(spacing: 16) {
                        Link("Telegram", destination: URL(string: "https://t.me/pipakik")!)
                        Link("VK", destination: URL(string: "https://vk.com/id658988396")!)
                    }
                    .font(.system(size: 13))
                    .tracking(0.5)
                    .foregroundColor(.appPrimary)
                    .opacity(0.6)
                }
                .padding(.top, 24)

                Spacer()

                // Footer
                Text("All rights reserved. Unauthorized reproduction or distribution of any content is strictly prohibited. dasha. is a registered trademark. ©")
                    .font(.system(size: 11))
                    .tracking(0.5)
                    .multilineTextAlignment(.center)
                    .foregroundColor(.appPrimary)
                    .opacity(0.5)
                    .padding(24)
            }
        }
        .onAppear  { radio.start() }
        .onDisappear { radio.stop() }
    }
}

#Preview {
    ContentView()
}

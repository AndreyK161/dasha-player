import SwiftUI

struct ContentView: View {
    var body: some View {
        ZStack {
            Color(red: 0.808, green: 0.890, blue: 0.980)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // Header
                HStack {
                    Text("в эфире")
                        .font(.system(size: 13))
                        .tracking(0.4)
                        .foregroundColor(Color(red: 0.176, green: 0.176, blue: 0.176))
                        .opacity(0.3)
                    Spacer()
                }
                .padding(.horizontal, 32)
                .padding(.vertical, 24)

                // Center
                Spacer()

                Text("dasha.")
                    .font(.system(size: 72, weight: .regular))
                    .tracking(-1)
                    .foregroundColor(Color(red: 0.176, green: 0.176, blue: 0.176))

                VStack(spacing: 24) {
                    // Progress bar
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            RoundedRectangle(cornerRadius: 999)
                                .fill(Color(red: 0.75, green: 0.75, blue: 0.75))
                                .frame(height: 4)
                            RoundedRectangle(cornerRadius: 999)
                                .fill(Color(red: 0.176, green: 0.176, blue: 0.176))
                                .frame(width: geo.size.width * 0.4, height: 4)
                        }
                    }
                    .frame(width: 200, height: 4)
                    // Track info
                    Text("Unknown Track – Unknown Artist")
                        .font(.system(size: 14))
                        .tracking(0.3)
                        .foregroundColor(Color(red: 0.176, green: 0.176, blue: 0.176))
                        .opacity(0.7)

                    // Links
                    HStack(spacing: 16) {
                        Link("Telegram", destination: URL(string: "https://t.me/pipakik")!)
                            .font(.system(size: 13))
                            .tracking(0.5)
                            .foregroundColor(Color(red: 0.176, green: 0.176, blue: 0.176))
                            .opacity(0.6)
                        Link("VK", destination: URL(string: "https://vk.com/id658988396")!)
                            .font(.system(size: 13))
                            .tracking(0.5)
                            .foregroundColor(Color(red: 0.176, green: 0.176, blue: 0.176))
                            .opacity(0.6)
                    }
                }
                .padding(.top, 24)

                Spacer()

                // Footer
                Text("All rights reserved. Unauthorized reproduction or distribution of any content is strictly prohibited. dasha. is a registered trademark. ©")
                    .font(.system(size: 11))
                    .tracking(0.5)
                    .multilineTextAlignment(.center)
                    .foregroundColor(Color(red: 0.176, green: 0.176, blue: 0.176))
                    .opacity(0.5)
                    .padding(24)
            }
        }
    }
}

#Preview {
    ContentView()
}

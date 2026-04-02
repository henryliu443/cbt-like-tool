import SwiftUI

struct SafetyBannerView: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: "heart.fill")
                    .foregroundStyle(.white)
                    .font(.title3)
                Text("你的安全最重要")
                    .font(.headline)
                    .foregroundStyle(.white)
            }

            Text("如果你正处于危机中，请立即联系专业帮助：")
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.9))

            VStack(alignment: .leading, spacing: 6) {
                hotlineRow(name: "全国心理援助热线", number: "400-161-9995")
                hotlineRow(name: "北京心理危机研究与干预中心", number: "010-82951332")
                hotlineRow(name: "生命热线", number: "400-821-1215")
            }

            Text("你不是一个人，有人在乎你。")
                .font(.caption)
                .foregroundStyle(.white.opacity(0.8))
                .padding(.top, 4)
        }
        .padding(16)
        .background(
            LinearGradient(
                colors: [Color.red.opacity(0.85), Color.orange.opacity(0.75)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    private func hotlineRow(name: String, number: String) -> some View {
        HStack {
            Image(systemName: "phone.fill")
                .font(.caption)
                .foregroundStyle(.white.opacity(0.8))
            Text("\(name)：")
                .font(.caption)
                .foregroundStyle(.white.opacity(0.9))
            Text(number)
                .font(.caption.bold())
                .foregroundStyle(.white)
        }
    }
}

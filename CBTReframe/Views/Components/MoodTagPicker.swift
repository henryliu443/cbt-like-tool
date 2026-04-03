import SwiftUI

struct MoodTag: Identifiable, Hashable {
    let id = UUID()
    let emoji: String
    let label: String
}

struct MoodTagPicker: View {
    @Binding var selectedMood: String

    private let moods: [MoodTag] = [
        MoodTag(emoji: "😔", label: "低落"),
        MoodTag(emoji: "😰", label: "焦虑"),
        MoodTag(emoji: "😤", label: "愤怒"),
        MoodTag(emoji: "😟", label: "担忧"),
        MoodTag(emoji: "😞", label: "失望"),
        MoodTag(emoji: "🫠", label: "疲惫"),
        MoodTag(emoji: "😶", label: "麻木"),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 4) {
                    Text("选择心情")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color("TextPrimary"))
                    Text("必选")
                        .font(.caption2.weight(.medium))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color("AccentColor").opacity(0.12))
                        .foregroundStyle(Color("AccentColor"))
                        .clipShape(Capsule())
                }
                Text("便于结合你当下的情绪，更好地解读和改善自动想法与感受。")
                    .font(.caption)
                    .foregroundStyle(Color("TextSecondary"))
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.horizontal)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(moods) { mood in
                        Button {
                            withAnimation(.spring(response: 0.3)) {
                                selectedMood = mood.label
                            }
                        } label: {
                            HStack(spacing: 4) {
                                Text(mood.emoji)
                                    .font(.callout)
                                Text(mood.label)
                                    .font(.caption)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(
                                selectedMood == mood.label
                                    ? Color("AccentColor").opacity(0.15)
                                    : Color("CardBackground")
                            )
                            .foregroundStyle(
                                selectedMood == mood.label
                                    ? Color("AccentColor")
                                    : Color("TextSecondary")
                            )
                            .clipShape(Capsule())
                            .overlay(
                                Capsule()
                                    .stroke(
                                        selectedMood == mood.label
                                            ? Color("AccentColor").opacity(0.3)
                                            : Color(.separator).opacity(0.3),
                                        lineWidth: 1
                                    )
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal)
            }
        }
    }
}

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
            Text("现在的心情")
                .font(.caption)
                .foregroundStyle(Color("TextSecondary"))
                .padding(.horizontal)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(moods) { mood in
                        Button {
                            withAnimation(.spring(response: 0.3)) {
                                selectedMood = selectedMood == mood.label ? "" : mood.label
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

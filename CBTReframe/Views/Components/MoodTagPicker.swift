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
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 8) {
                    Image(systemName: "heart.text.square.fill")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Color("AccentColor"))
                    Text("选择心情")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(Color("TextSecondary"))
                        .textCase(.uppercase)
                        .tracking(0.4)
                    Text("必选")
                        .font(.caption2.weight(.semibold))
                        .padding(.horizontal, 7)
                        .padding(.vertical, 3)
                        .background(Color("AccentColor").opacity(0.12))
                        .foregroundStyle(Color("AccentColor"))
                        .clipShape(Capsule())
                }
                Text("便于结合你当下的情绪，更好地解读和改善自动想法与感受。")
                    .font(.caption)
                    .foregroundStyle(Color("TextSecondary"))
                    .fixedSize(horizontal: false, vertical: true)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(moods) { mood in
                        Button {
                            withAnimation(.spring(response: 0.3)) {
                                selectedMood = mood.label
                            }
                        } label: {
                            HStack(spacing: 6) {
                                Text(mood.emoji)
                                    .font(.body)
                                Text(mood.label)
                                    .font(.subheadline.weight(.medium))
                            }
                            .padding(.horizontal, 14)
                            .padding(.vertical, 10)
                            .background(
                                selectedMood == mood.label
                                    ? Color("AccentColor").opacity(0.18)
                                    : Color(.secondarySystemGroupedBackground)
                            )
                            .foregroundStyle(
                                selectedMood == mood.label
                                    ? Color("AccentColor")
                                    : Color("TextPrimary")
                            )
                            .clipShape(Capsule())
                            .overlay(
                                Capsule()
                                    .stroke(
                                        selectedMood == mood.label
                                            ? Color("AccentColor").opacity(0.35)
                                            : Color(.separator).opacity(0.35),
                                        lineWidth: 1
                                    )
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.trailing, 2)
            }
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Color("CardBackground"))
                .shadow(color: .black.opacity(0.06), radius: 12, y: 5)
        )
    }
}

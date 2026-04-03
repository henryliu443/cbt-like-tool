import SwiftUI

struct ThoughtInputCard: View {
    @Binding var text: String
    @FocusState.Binding var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 8) {
                Image(systemName: "bubble.left.and.text.bubble.right")
                    .foregroundStyle(Color("AccentColor"))
                    .font(.subheadline.weight(.medium))
                Text("写下你的想法")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Color("TextSecondary"))
                    .textCase(.uppercase)
                    .tracking(0.4)
            }

            ZStack(alignment: .topLeading) {
                if text.isEmpty {
                    Text("例如：我觉得自己做什么都不够好...")
                        .foregroundStyle(Color("TextSecondary").opacity(0.55))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 10)
                }

                TextEditor(text: $text)
                    .focused($isFocused)
                    .font(.body)
                    .frame(minHeight: 128)
                    .scrollContentBackground(.hidden)
                    .background(Color.clear)
            }
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Color("CardBackground"))
                .shadow(color: .black.opacity(0.06), radius: 12, y: 5)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(
                    isFocused
                        ? Color("AccentColor").opacity(0.45)
                        : Color("TextSecondary").opacity(0.08),
                    lineWidth: isFocused ? 2 : 1
                )
        )
    }
}

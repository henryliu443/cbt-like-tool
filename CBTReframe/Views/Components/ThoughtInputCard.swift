import SwiftUI

struct ThoughtInputCard: View {
    @Binding var text: String
    @FocusState.Binding var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "bubble.left.and.text.bubble.right")
                    .foregroundStyle(Color("AccentColor"))
                    .font(.subheadline)
                Text("写下你的想法")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color("TextSecondary"))
            }

            ZStack(alignment: .topLeading) {
                if text.isEmpty {
                    Text("例如：我觉得自己做什么都不够好...")
                        .foregroundStyle(Color("TextSecondary").opacity(0.5))
                        .padding(.horizontal, 5)
                        .padding(.vertical, 8)
                }

                TextEditor(text: $text)
                    .focused($isFocused)
                    .frame(minHeight: 120)
                    .scrollContentBackground(.hidden)
                    .background(Color.clear)
            }
        }
        .padding(16)
        .background(Color("CardBackground"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.04), radius: 8, y: 4)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(
                    isFocused ? Color("AccentColor").opacity(0.4) : Color.clear,
                    lineWidth: 2
                )
        )
        .padding(.horizontal)
    }
}

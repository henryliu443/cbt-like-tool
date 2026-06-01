import SwiftUI

struct StreamingResultView: View {
    let text: String

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Label("流式输出", systemImage: "text.cursor")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color("TextSecondary"))
            Text(text.isEmpty ? "正在生成..." : text)
                .font(.body)
                .foregroundStyle(Color("TextPrimary"))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(16)
        .background(Color("CardBackground"))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

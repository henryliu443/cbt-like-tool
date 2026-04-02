import SwiftUI

struct ResultCardView: View {
    let result: AnalysisResult
    var inputThought: String = ""
    @State private var copiedToast = false

    var body: some View {
        VStack(spacing: 0) {
            resultSection(
                icon: "exclamationmark.triangle",
                iconColor: .orange,
                label: "认知扭曲",
                value: result.distortion
            )

            sectionDivider()

            resultSection(
                icon: "lightbulb",
                iconColor: Color("AccentColor"),
                label: "替代想法",
                value: result.alternative
            )

            sectionDivider()

            resultSection(
                icon: "figure.walk",
                iconColor: .green,
                label: "建议行动",
                value: result.action
            )

            sectionDivider()

            actionBar
        }
        .background(Color("CardBackground"))
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.06), radius: 12, y: 6)
        .overlay(alignment: .top) {
            if copiedToast {
                HStack(spacing: 6) {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(.green)
                    Text("已复制到剪贴板")
                        .font(.caption.weight(.medium))
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(.ultraThinMaterial)
                .clipShape(Capsule())
                .offset(y: -12)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .padding(.horizontal)
    }

    private var actionBar: some View {
        HStack(spacing: 0) {
            actionButton(icon: "doc.on.doc", label: "复制") {
                copyResultToClipboard()
            }

            Rectangle()
                .fill(Color(.separator).opacity(0.2))
                .frame(width: 1, height: 28)

            actionButton(icon: "paperplane.fill", label: "发到 ChatGPT") {
                sendToChatGPT()
            }
        }
        .padding(.vertical, 4)
    }

    private func actionButton(icon: String, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.caption)
                Text(label)
                    .font(.caption.weight(.medium))
            }
            .foregroundStyle(Color("AccentColor"))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
        }
        .buttonStyle(.plain)
    }

    private func buildClipboardText() -> String {
        var text = ""
        if !inputThought.isEmpty {
            text += "我的想法：\(inputThought)\n\n"
        }
        text += """
        认知扭曲：\(result.distortion)
        替代想法：\(result.alternative)
        建议行动：\(result.action)

        请帮我进一步分析这个想法，给出更多角度和建议。
        """
        return text
    }

    private func copyResultToClipboard() {
        UIPasteboard.general.string = buildClipboardText()
        let impact = UIImpactFeedbackGenerator(style: .light)
        impact.impactOccurred()
        withAnimation(.spring(response: 0.3)) { copiedToast = true }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            withAnimation { copiedToast = false }
        }
    }

    private func sendToChatGPT() {
        UIPasteboard.general.string = buildClipboardText()
        let impact = UIImpactFeedbackGenerator(style: .medium)
        impact.impactOccurred()
        withAnimation(.spring(response: 0.3)) { copiedToast = true }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            if let url = URL(string: "chatgpt://") {
                if UIApplication.shared.canOpenURL(url) {
                    UIApplication.shared.open(url)
                } else if let webURL = URL(string: "https://chat.openai.com") {
                    UIApplication.shared.open(webURL)
                }
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                withAnimation { copiedToast = false }
            }
        }
    }

    private func resultSection(icon: String, iconColor: Color, label: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.subheadline)
                    .foregroundStyle(iconColor)
                    .frame(width: 28, height: 28)
                    .background(iconColor.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 8))

                Text(label)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color("TextSecondary"))
                    .textCase(.uppercase)
            }

            Text(value)
                .font(.body)
                .foregroundStyle(Color("TextPrimary"))
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
    }

    private func sectionDivider() -> some View {
        Rectangle()
            .fill(Color(.separator).opacity(0.2))
            .frame(height: 1)
            .padding(.horizontal, 16)
    }
}

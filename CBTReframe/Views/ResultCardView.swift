import SwiftUI

struct ResultCardView: View {
    let result: AnalysisResult

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
        }
        .background(Color("CardBackground"))
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.06), radius: 12, y: 6)
        .padding(.horizontal)
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

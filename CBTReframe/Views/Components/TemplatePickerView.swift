import SwiftUI

struct TemplatePickerView: View {
    @Binding var selectedTemplate: ThinkingTemplate
    var suggestedTemplate: ThinkingTemplate?
    var onTemplateTapped: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 8) {
                Image(systemName: "wand.and.stars")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color("AccentColor"))
                Text("选择方式")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Color("TextSecondary"))
                    .textCase(.uppercase)
                    .tracking(0.4)

                if let suggested = suggestedTemplate, suggested != selectedTemplate {
                    Spacer(minLength: 8)
                    Button {
                        withAnimation(.spring(response: 0.3)) {
                            selectedTemplate = suggested
                            onTemplateTapped?()
                        }
                        let impact = UIImpactFeedbackGenerator(style: .light)
                        impact.impactOccurred()
                    } label: {
                        HStack(spacing: 3) {
                            Image(systemName: "sparkles")
                                .font(.caption2)
                            Text("推荐: \(suggested.shortLabel)")
                                .font(.caption2.weight(.semibold))
                        }
                        .foregroundStyle(Color("AccentColor"))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Color("AccentColor").opacity(0.12))
                        .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                    .transition(.opacity.combined(with: .scale(scale: 0.9)))
                }
            }

            HStack(spacing: 0) {
                ForEach(ThinkingTemplate.allCases) { template in
                    templateButton(template)
                }
            }
            .padding(5)
            .background(Color("TextSecondary").opacity(0.07))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Color("CardBackground"))
                .shadow(color: .black.opacity(0.06), radius: 12, y: 5)
        )
    }

    private func templateButton(_ template: ThinkingTemplate) -> some View {
        let isSelected = selectedTemplate == template
        let isSuggested = suggestedTemplate == template && !isSelected

        return Button {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                selectedTemplate = template
                onTemplateTapped?()
            }
            let impact = UIImpactFeedbackGenerator(style: .light)
            impact.impactOccurred()
        } label: {
            VStack(spacing: 8) {
                ZStack {
                    Circle()
                        .fill(isSelected ? Color("AccentColor") : Color("AccentColor").opacity(isSuggested ? 0.14 : 0.08))
                        .frame(width: 42, height: 42)

                    Image(systemName: template.icon)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(isSelected ? .white : Color("AccentColor"))
                }

                Text(template.shortLabel)
                    .font(.caption2.weight(isSelected ? .bold : .medium))
                    .foregroundStyle(isSelected ? Color("TextPrimary") : Color("TextSecondary"))

                if isSuggested {
                    Text("推荐")
                        .font(.system(size: 8, weight: .bold))
                        .foregroundStyle(Color("AccentColor"))
                        .padding(.horizontal, 5)
                        .padding(.vertical, 1)
                        .background(Color("AccentColor").opacity(0.12))
                        .clipShape(Capsule())
                } else {
                    Color.clear.frame(height: 12)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(isSelected ? Color("CardBackground") : Color.clear)
                    .shadow(color: isSelected ? .black.opacity(0.07) : .clear, radius: 5, y: 2)
            )
        }
        .buttonStyle(.plain)
    }
}

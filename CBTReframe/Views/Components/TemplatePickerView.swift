import SwiftUI

struct TemplatePickerView: View {
    @Binding var selectedTemplate: PromptTemplate
    var suggestedTemplate: PromptTemplate?
    var onTemplateTapped: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                Image(systemName: "wand.and.stars")
                    .font(.caption)
                    .foregroundStyle(Color("AccentColor"))
                Text("选择方式")
                    .font(.caption.weight(.medium))
                    .foregroundStyle(Color("TextSecondary"))

                if let suggested = suggestedTemplate, suggested != selectedTemplate {
                    Spacer()
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
                                .font(.caption2.weight(.medium))
                        }
                        .foregroundStyle(Color("AccentColor"))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color("AccentColor").opacity(0.1))
                        .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                    .transition(.opacity.combined(with: .scale(scale: 0.9)))
                }
            }

            HStack(spacing: 0) {
                ForEach(PromptTemplate.allCases) { template in
                    templateButton(template)
                }
            }
            .background(Color("CardBackground"))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .shadow(color: .black.opacity(0.03), radius: 4, y: 2)
        }
        .padding(.horizontal)
    }

    private func templateButton(_ template: PromptTemplate) -> some View {
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
            VStack(spacing: 6) {
                ZStack {
                    Circle()
                        .fill(isSelected ? Color("AccentColor") : Color("AccentColor").opacity(isSuggested ? 0.12 : 0.06))
                        .frame(width: 40, height: 40)

                    Image(systemName: template.icon)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(isSelected ? .white : Color("AccentColor"))
                }

                Text(template.shortLabel)
                    .font(.caption2.weight(isSelected ? .bold : .medium))
                    .foregroundStyle(isSelected ? Color("AccentColor") : Color("TextSecondary"))

                if isSuggested {
                    Text("推荐")
                        .font(.system(size: 8, weight: .bold))
                        .foregroundStyle(Color("AccentColor"))
                        .padding(.horizontal, 5)
                        .padding(.vertical, 1)
                        .background(Color("AccentColor").opacity(0.1))
                        .clipShape(Capsule())
                } else {
                    Color.clear.frame(height: 12)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(
                isSelected
                    ? Color("AccentColor").opacity(0.08)
                    : Color.clear
            )
        }
        .buttonStyle(.plain)
    }
}

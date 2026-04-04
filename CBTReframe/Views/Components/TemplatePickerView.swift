import SwiftUI

struct TemplatePickerView: View {
    private static let columnWidth: CGFloat = 82
    private static let iconCircle: CGFloat = 48
    private static let ringDiameter: CGFloat = 56
    private static let ringLineWidth: CGFloat = 2.5

    @Environment(\.accessibilityReduceMotion) private var accessibilityReduceMotion

    @Binding var selectedTemplate: ThinkingTemplate
    var suggestedTemplate: ThinkingTemplate?
    var onTemplateTapped: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            headerRow

            TimelineView(.animation(minimumInterval: accessibilityReduceMotion ? 1.0 : 0.06)) { context in
                let gradientAngle = rainbowGradientAngle(at: context.date)

                LiquidGlassPanel(cornerRadius: 22) {
                    HStack(spacing: 0) {
                        Spacer(minLength: 0)
                        HStack(spacing: 4) {
                            ForEach(ThinkingTemplate.allCases) { template in
                                templateColumn(template, gradientAngle: gradientAngle)
                            }
                        }
                        Spacer(minLength: 0)
                    }
                    .padding(.vertical, 18)
                    .padding(.horizontal, 10)
                }
            }
        }
    }

    private var headerRow: some View {
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
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
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
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func rainbowGradientAngle(at date: Date) -> Angle {
        if accessibilityReduceMotion {
            return .degrees(28)
        }
        let t = date.timeIntervalSinceReferenceDate
        let deg = (t * IntelligenceRainbow.rotationSpeed).truncatingRemainder(dividingBy: 360)
        return .degrees(deg)
    }

    private func templateColumn(_ template: ThinkingTemplate, gradientAngle: Angle) -> some View {
        let isSelected = selectedTemplate == template
        let isSuggested = suggestedTemplate == template && !isSelected

        return Button {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.82)) {
                selectedTemplate = template
                onTemplateTapped?()
            }
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
        } label: {
            VStack(spacing: 10) {
                ZStack {
                    RainbowOrbitalRing(
                        diameter: Self.ringDiameter,
                        lineWidth: Self.ringLineWidth,
                        gradientRotation: gradientAngle,
                        isActive: isSelected
                    )

                    Circle()
                        .fill(.ultraThinMaterial)
                        .overlay {
                            Circle()
                                .fill(
                                    isSelected
                                        ? Color("AccentColor").opacity(0.92)
                                        : Color("AccentColor").opacity(isSuggested ? 0.16 : 0.1)
                                )
                        }
                        .frame(width: Self.iconCircle, height: Self.iconCircle)
                        .overlay {
                            Circle()
                                .stroke(Color.white.opacity(isSelected ? 0.25 : 0.12), lineWidth: 1)
                        }

                    Image(systemName: template.icon)
                        .font(.system(size: 19, weight: .semibold))
                        .foregroundStyle(isSelected ? Color.white : Color("AccentColor"))
                }
                .frame(height: Self.ringDiameter)
                .frame(maxWidth: .infinity)

                Text(template.shortLabel)
                    .font(.caption2.weight(isSelected ? .bold : .medium))
                    .foregroundStyle(isSelected ? Color("TextPrimary") : Color("TextSecondary"))
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)

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
            .frame(width: Self.columnWidth)
        }
        .buttonStyle(.plain)
        .scaleEffect(isSelected ? 1.02 : 1)
        .animation(.spring(response: 0.28, dampingFraction: 0.78), value: isSelected)
        .accessibilityLabel(template.shortLabel)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

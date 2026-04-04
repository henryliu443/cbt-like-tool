import SwiftUI

// MARK: - Rainbow spectrum (Apple Intelligence–like, no asset dependency)

enum IntelligenceRainbow {
    /// Pink → orange → yellow → green → blue → violet → loop
    static let spectrum: [Color] = [
        Color(red: 0.98, green: 0.32, blue: 0.55),
        Color(red: 1.0, green: 0.48, blue: 0.22),
        Color(red: 1.0, green: 0.82, blue: 0.18),
        Color(red: 0.28, green: 0.82, blue: 0.48),
        Color(red: 0.22, green: 0.52, blue: 1.0),
        Color(red: 0.52, green: 0.32, blue: 1.0),
        Color(red: 0.98, green: 0.32, blue: 0.55),
    ]

    static func angularGradient(angle: Angle) -> AngularGradient {
        AngularGradient(
            gradient: Gradient(colors: spectrum),
            center: .center,
            angle: angle
        )
    }

    /// Degrees per second when animating.
    static let rotationSpeed: Double = 38
}

// MARK: - Liquid glass (iOS 17: Material + edge light; not system Liquid Glass API)

struct LiquidGlassPanel<Content: View>: View {
    var cornerRadius: CGFloat = 22
    @ViewBuilder var content: () -> Content

    var body: some View {
        content()
            .frame(maxWidth: .infinity)
            .background {
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(.ultraThinMaterial)
                    .shadow(color: .black.opacity(0.07), radius: 18, y: 8)
            }
            .overlay {
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(
                        LinearGradient(
                            colors: [
                                Color.white.opacity(0.5),
                                Color.white.opacity(0.1),
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1
                    )
            }
    }
}

// MARK: - Rainbow ring (selection accent)

struct RainbowOrbitalRing: View {
    var diameter: CGFloat
    var lineWidth: CGFloat
    /// Total rotation of the gradient (degrees).
    var gradientRotation: Angle
    var isActive: Bool

    var body: some View {
        Circle()
            .strokeBorder(
                IntelligenceRainbow.angularGradient(angle: gradientRotation),
                lineWidth: lineWidth
            )
            .frame(width: diameter, height: diameter)
            .opacity(isActive ? 1 : 0)
            .animation(.easeOut(duration: 0.2), value: isActive)
    }
}

// MARK: - Home: soft ambient wash

struct IntelligenceAmbientBackground: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        TimelineView(.animation(minimumInterval: reduceMotion ? 1.0 : 0.12)) { context in
            let t = context.date.timeIntervalSinceReferenceDate
            let angle = reduceMotion ? Angle(degrees: 24) : Angle(degrees: (t * 12).truncatingRemainder(dividingBy: 360))
            let a = colorScheme == .dark ? 0.09 : 0.06
            let b = colorScheme == .dark ? 0.06 : 0.04
            ZStack {
                AngularGradient(
                    gradient: Gradient(colors: IntelligenceRainbow.spectrum.map { $0.opacity(a) }),
                    center: .center,
                    angle: angle
                )
                AngularGradient(
                    gradient: Gradient(colors: IntelligenceRainbow.spectrum.map { $0.opacity(b) }),
                    center: UnitPoint(x: 0.85, y: 0.2),
                    angle: Angle(degrees: -angle.degrees * 0.7)
                )
            }
            .allowsHitTesting(false)
        }
    }
}

// MARK: - CTA / icon accent stroke

struct RainbowEdgeGlow: Shape {
    var cornerRadius: CGFloat

    func path(in rect: CGRect) -> Path {
        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous).path(in: rect)
    }
}

extension View {
    /// Thin animated rainbow stroke on a rounded rect (e.g. primary button).
    func intelligenceRainbowStroke(
        cornerRadius: CGFloat,
        lineWidth: CGFloat = 1.5,
        gradientRotation: Angle
    ) -> some View {
        overlay {
            RainbowEdgeGlow(cornerRadius: cornerRadius)
                .stroke(
                    IntelligenceRainbow.angularGradient(angle: gradientRotation),
                    lineWidth: lineWidth
                )
        }
    }
}

// MARK: - Header / CTA helpers (single TimelineView each)

struct IntelligenceAnimatedGlyph: View {
    var systemName: String
    var pointSize: CGFloat
    var weight: Font.Weight = .light

    @Environment(\.accessibilityReduceMotion) private var accessibilityReduceMotion

    var body: some View {
        TimelineView(.animation(minimumInterval: accessibilityReduceMotion ? 1.0 : 0.1)) { context in
            let deg = accessibilityReduceMotion
                ? 32.0
                : (context.date.timeIntervalSinceReferenceDate * 22).truncatingRemainder(dividingBy: 360)
            Image(systemName: systemName)
                .font(.system(size: pointSize, weight: weight))
                .foregroundStyle(IntelligenceRainbow.angularGradient(angle: .degrees(deg)))
        }
        .accessibilityHidden(true)
    }
}

struct IntelligenceRainbowCardStroke: View {
    var cornerRadius: CGFloat
    var lineWidth: CGFloat = 1.75

    @Environment(\.accessibilityReduceMotion) private var accessibilityReduceMotion

    var body: some View {
        TimelineView(.animation(minimumInterval: accessibilityReduceMotion ? 1.0 : 0.08)) { context in
            let deg = accessibilityReduceMotion
                ? 40.0
                : (context.date.timeIntervalSinceReferenceDate * IntelligenceRainbow.rotationSpeed)
                .truncatingRemainder(dividingBy: 360)
            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                .stroke(
                    IntelligenceRainbow.angularGradient(angle: .degrees(deg)),
                    lineWidth: lineWidth
                )
        }
        .allowsHitTesting(false)
    }
}

import SwiftUI

struct OnboardingView: View {
    @Bindable var settingsViewModel: SettingsViewModel
    @Binding var hasCompletedOnboarding: Bool
    @State private var currentPage = 0

    var body: some View {
        ZStack {
            Color(.systemGroupedBackground)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                TabView(selection: $currentPage) {
                    welcomePage.tag(0)
                    providerPage.tag(1)
                    readyPage.tag(2)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .animation(.easeInOut(duration: 0.3), value: currentPage)

                pageIndicator
                bottomButtons
            }
        }
    }

    private var welcomePage: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "brain.head.profile")
                .font(.system(size: 72))
                .foregroundStyle(
                    LinearGradient(
                        colors: [Color("GradientStart"), Color("GradientEnd")],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

            Text("欢迎使用 CBT 思维重构")
                .font(.title.bold())
                .foregroundStyle(Color("TextPrimary"))

            VStack(spacing: 16) {
                featureRow(icon: "arrow.triangle.2.circlepath", title: "认知重构", desc: "识别思维陷阱，找到更平衡的视角")
                featureRow(icon: "brain", title: "AI 辅助", desc: "借助 AI 获得专业级的认知行为分析")
                featureRow(icon: "lock.shield", title: "隐私安全", desc: "数据仅存储在你的设备上")
            }
            .padding(.horizontal, 32)

            Spacer()
            Spacer()
        }
    }

    private var providerPage: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "cpu")
                .font(.system(size: 56))
                .foregroundStyle(Color("AccentColor"))

            Text("选择 AI 服务商")
                .font(.title2.bold())
                .foregroundStyle(Color("TextPrimary"))

            Text("你可以随时在设置中更改")
                .font(.subheadline)
                .foregroundStyle(Color("TextSecondary"))

            VStack(spacing: 12) {
                ForEach(AIProvider.allCases) { provider in
                    Button {
                        withAnimation(.spring(response: 0.3)) {
                            settingsViewModel.selectedProvider = provider
                        }
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(provider.displayName)
                                    .font(.body.weight(.medium))
                                if !provider.requiresAPIKey {
                                    Text("免费 · 离线可用 · 无需配置")
                                        .font(.caption)
                                        .foregroundStyle(Color("TextSecondary"))
                                }
                            }
                            Spacer()
                            if settingsViewModel.selectedProvider == provider {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundStyle(Color("AccentColor"))
                            } else {
                                Image(systemName: "circle")
                                    .foregroundStyle(Color("TextSecondary").opacity(0.3))
                            }
                        }
                        .padding(14)
                        .background(
                            settingsViewModel.selectedProvider == provider
                                ? Color("AccentColor").opacity(0.08)
                                : Color("CardBackground")
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(
                                    settingsViewModel.selectedProvider == provider
                                        ? Color("AccentColor").opacity(0.3)
                                        : Color(.separator).opacity(0.2),
                                    lineWidth: 1
                                )
                        )
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(Color("TextPrimary"))
                }
            }
            .padding(.horizontal, 24)

            if settingsViewModel.selectedProvider.requiresAPIKey {
                VStack(spacing: 8) {
                    SecureField("粘贴你的 API Key", text: $settingsViewModel.apiKeyInput)
                        .font(.system(.body, design: .monospaced))
                        .textFieldStyle(.roundedBorder)
                        .padding(.horizontal, 24)

                    Button("保存") {
                        settingsViewModel.saveAPIKey()
                    }
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color("AccentColor"))
                }
            }

            Spacer()
            Spacer()
        }
    }

    private var readyPage: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "checkmark.seal.fill")
                .font(.system(size: 72))
                .foregroundStyle(
                    LinearGradient(
                        colors: [Color("GradientStart"), Color("GradientEnd")],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

            Text("一切就绪!")
                .font(.title.bold())
                .foregroundStyle(Color("TextPrimary"))

            Text("当你有负面想法时\n打开这个 App，写下来\n让 AI 帮你找到新的视角")
                .font(.body)
                .foregroundStyle(Color("TextSecondary"))
                .multilineTextAlignment(.center)
                .lineSpacing(4)

            Spacer()
            Spacer()
        }
    }

    private var pageIndicator: some View {
        HStack(spacing: 8) {
            ForEach(0..<3, id: \.self) { index in
                Circle()
                    .fill(currentPage == index ? Color("AccentColor") : Color("TextSecondary").opacity(0.3))
                    .frame(width: currentPage == index ? 10 : 7, height: currentPage == index ? 10 : 7)
                    .animation(.spring(response: 0.3), value: currentPage)
            }
        }
        .padding(.bottom, 16)
    }

    private var bottomButtons: some View {
        HStack {
            if currentPage > 0 {
                Button("上一步") {
                    withAnimation { currentPage -= 1 }
                }
                .foregroundStyle(Color("TextSecondary"))
            }

            Spacer()

            if currentPage < 2 {
                Button {
                    withAnimation { currentPage += 1 }
                } label: {
                    HStack(spacing: 4) {
                        Text("下一步")
                        Image(systemName: "arrow.right")
                    }
                    .font(.headline)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color("AccentColor"))
                    .clipShape(Capsule())
                }
            } else {
                Button {
                    hasCompletedOnboarding = true
                } label: {
                    HStack(spacing: 4) {
                        Text("开始使用")
                        Image(systemName: "arrow.right")
                    }
                    .font(.headline)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(
                        LinearGradient(
                            colors: [Color("GradientStart"), Color("GradientEnd")],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .clipShape(Capsule())
                }
            }
        }
        .padding(.horizontal, 24)
        .padding(.bottom, 32)
    }

    private func featureRow(icon: String, title: String, desc: String) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(Color("AccentColor"))
                .frame(width: 36, height: 36)
                .background(Color("AccentColor").opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 10))

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color("TextPrimary"))
                Text(desc)
                    .font(.caption)
                    .foregroundStyle(Color("TextSecondary"))
            }
            Spacer()
        }
    }
}

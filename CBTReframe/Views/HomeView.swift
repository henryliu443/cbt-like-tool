import SwiftUI
import SwiftData
import UIKit

struct HomeView: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject private var globalSettings: GlobalSettings
    @Bindable var viewModel: ReframeViewModel
    @State private var isButtonPressed = false
    @State private var showExternalAppChoices = false
    @State private var geminiPulse = false
    @FocusState private var isInputFocused: Bool

    var body: some View {
        ZStack {
            homeBackground
                .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    headerSection

                    VStack(spacing: 20) {
                        ThoughtInputCard(text: $viewModel.inputText, isFocused: $isInputFocused)
                        templatePicker
                        MoodTagPicker(selectedMood: $viewModel.selectedMood, isAkathisia: $viewModel.isAkathisia)
                        analyzeButton
                    }

                    externalMoneySaverSection

                    if viewModel.isLoading && viewModel.loadingBannerStyle != .none {
                        analysisLoadingBanner
                            .transition(.asymmetric(
                                insertion: .opacity.combined(with: .scale(scale: 0.95)),
                                removal: .opacity
                            ))
                    }

                    if let retryNotice = viewModel.retryRecoveryNotice {
                        retryRecoveryBanner(retryNotice)
                            .transition(.opacity.combined(with: .move(edge: .top)))
                    }

                    if viewModel.showCrisisBanner {
                        SafetyBannerView()
                            .transition(.asymmetric(
                                insertion: .opacity.combined(with: .scale(scale: 0.95)),
                                removal: .opacity
                            ))
                    }

                    if let errorMessage = viewModel.errorMessage {
                        errorBanner(errorMessage)
                    }

                    if let result = viewModel.result {
                        ResultCardView(
                            result: result,
                            template: globalSettings.thinkingTemplate,
                            inputThought: viewModel.inputText
                        )
                            .transition(.asymmetric(
                                insertion: .opacity
                                    .combined(with: .move(edge: .bottom))
                                    .combined(with: .scale(scale: 0.95)),
                                removal: .opacity
                            ))
                    }

                    Spacer(minLength: 60)
                }
                .padding(.horizontal, 20)
                .padding(.top, 12)
                .padding(.bottom, 8)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .onTapGesture {
            isInputFocused = false
        }
        .onChange(of: viewModel.selectedMood) { _, newValue in
            if !newValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                viewModel.errorMessage = nil
            }
        }
        .confirmationDialog("已复制到剪贴板", isPresented: $showExternalAppChoices, titleVisibility: .visible) {
            Button("打开 DeepSeek") {
                ExternalAIAppLauncher.openDeepSeek()
            }
            Button("打开 ChatGPT") {
                ExternalAIAppLauncher.openChatGPT()
            }
            Button("打开 Gemini") {
                ExternalAIAppLauncher.openGemini()
            }
            Button("打开 Kimi") {
                ExternalAIAppLauncher.openKimi()
            }
            Button("完成", role: .cancel) {}
        } message: {
            Text("在新建对话里粘贴刚才复制的全部内容即可。")
        }
    }

    private var externalMoneySaverSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                Image(systemName: "yensign.circle")
                    .font(.caption)
                    .foregroundStyle(Color("AccentColor"))
                Text("省钱 / 无 API")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color("TextSecondary"))
            }
            Text("生成与站内一致的完整提示词并复制，再在外站免费或按次使用。")
                .font(.caption2)
                .foregroundStyle(Color("TextSecondary").opacity(0.95))
                .fixedSize(horizontal: false, vertical: true)

            Button {
                copyExternalPromptAndShowLinks()
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "doc.on.doc.fill")
                    Text("复制提示词并选择外站")
                        .font(.subheadline.weight(.medium))
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(Color("AccentColor").opacity(0.08))
                .foregroundStyle(Color("AccentColor"))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .stroke(Color("AccentColor").opacity(0.22), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
            .disabled(!canSubmitAnalysis)
            .opacity(canSubmitAnalysis ? 1 : 0.55)
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(Color("CardBackground").opacity(0.92))
                .shadow(color: .black.opacity(0.04), radius: 8, y: 3)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color("AccentColor").opacity(0.08), lineWidth: 1)
        )
    }

    private func copyExternalPromptAndShowLinks() {
        guard let text = viewModel.buildExternalManualPromptText() else { return }
        UIPasteboard.general.string = text
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        showExternalAppChoices = true
    }

    private var canSubmitAnalysis: Bool {
        let hasText = !viewModel.inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        let hasMood = !viewModel.selectedMood.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        return hasText && hasMood && !viewModel.isLoading
    }

    private var homeBackground: some View {
        ZStack {
            Color(.systemGroupedBackground)
            LinearGradient(
                colors: [
                    Color("AccentColor").opacity(0.07),
                    Color.clear,
                    Color(.systemGroupedBackground)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
    }

    private var headerSection: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 8) {
                Text(viewModel.greeting)
                    .font(.title.weight(.bold))
                    .foregroundStyle(Color("TextPrimary"))
                    .minimumScaleFactor(0.85)
                    .lineLimit(2)

                Text(viewModel.todayQuote)
                    .font(.subheadline)
                    .foregroundStyle(Color("TextSecondary"))
                    .lineLimit(3)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Image(systemName: "brain.head.profile")
                .font(.system(size: 40, weight: .light))
                .symbolRenderingMode(.hierarchical)
                .foregroundStyle(
                    LinearGradient(
                        colors: [Color("GradientStart"), Color("GradientEnd")],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .accessibilityHidden(true)
        }
    }

    private var templatePicker: some View {
        TemplatePickerView(
            selectedTemplate: $globalSettings.thinkingTemplate,
            suggestedTemplate: viewModel.suggestedThinkingTemplate
        )
    }

    private var analyzeButton: some View {
        Button {
            isInputFocused = false
            let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
            impactFeedback.impactOccurred()

            Task {
                await viewModel.analyzeThought(modelContext: modelContext)
                if viewModel.result != nil {
                    let notificationFeedback = UINotificationFeedbackGenerator()
                    notificationFeedback.notificationOccurred(.success)
                }
            }
        } label: {
            HStack(spacing: 8) {
                if viewModel.isLoading {
                    ProgressView()
                        .tint(.white)
                } else {
                    Image(systemName: "arrow.triangle.2.circlepath")
                        .font(.headline)
                }
                Text(analyzeButtonTitle)
                    .font(.headline)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(
                LinearGradient(
                    colors: [Color("GradientStart"), Color("GradientEnd")],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .foregroundStyle(.white)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .shadow(color: Color("GradientEnd").opacity(0.38), radius: 16, y: 8)
        }
        .disabled(!canSubmitAnalysis)
        .opacity(canSubmitAnalysis ? 1 : 0.6)
        .scaleEffect(isButtonPressed ? 0.98 : 1)
        .onLongPressGesture(minimumDuration: .infinity, pressing: { pressing in
            withAnimation(.easeInOut(duration: 0.15)) {
                isButtonPressed = pressing
            }
        }, perform: {})
    }

    private var analyzeButtonTitle: String {
        if viewModel.isLoading {
            return viewModel.loadingBannerStyle == .deepReasoningWithTimer ? "深度思考中…" : "正在分析…"
        }
        return globalSettings.thinkingTemplate.shortLabel
    }

    @State private var spinnerRotation: Double = 0

    @ViewBuilder
    private var analysisLoadingBanner: some View {
        switch viewModel.loadingBannerStyle {
        case .none:
            EmptyView()
        case .deepReasoningWithTimer:
            deepReasoningLoadingBanner
        case .geminiPro:
            geminiProLoadingBanner
        }
    }

    private var deepReasoningLoadingBanner: some View {
        HStack(alignment: .center, spacing: 12) {
            ZStack {
                Circle()
                    .stroke(Color("AccentColor").opacity(0.12), lineWidth: 2.5)
                    .frame(width: 40, height: 40)
                Circle()
                    .trim(from: 0, to: 0.65)
                    .stroke(
                        AngularGradient(
                            colors: [Color("AccentColor"), Color("AccentColor").opacity(0.1)],
                            center: .center
                        ),
                        style: StrokeStyle(lineWidth: 2.5, lineCap: .round)
                    )
                    .frame(width: 40, height: 40)
                    .rotationEffect(.degrees(spinnerRotation))

                Text("\(viewModel.analysisElapsedSeconds)")
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .foregroundStyle(Color("AccentColor"))
            }
            .onAppear {
                withAnimation(.linear(duration: 1.2).repeatForever(autoreverses: false)) {
                    spinnerRotation = 360
                }
            }
            .onDisappear {
                spinnerRotation = 0
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(viewModel.currentThinkingPhrase)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color("TextPrimary"))
                    .animation(.easeInOut(duration: 0.3), value: viewModel.currentThinkingPhrase)

                HStack(spacing: 4) {
                    Text("深度思考")
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(Color("AccentColor"))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color("AccentColor").opacity(0.1))
                        .clipShape(Capsule())
                    Text("\(viewModel.analysisElapsedSeconds)秒")
                        .font(.caption2.monospacedDigit())
                        .foregroundStyle(Color("TextSecondary"))
                }
            }

            Spacer()
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color("CardBackground"))
                .shadow(color: .black.opacity(0.05), radius: 8, y: 4)
        )
    }

    private func errorBanner(_ message: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: "exclamationmark.circle")
                .foregroundStyle(.red)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Color("TextPrimary"))
            Spacer()
            Button {
                withAnimation { viewModel.errorMessage = nil }
            } label: {
                Image(systemName: "xmark.circle.fill")
                    .foregroundStyle(Color("TextSecondary"))
            }
        }
        .padding(14)
        .background(Color.red.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .transition(.opacity.combined(with: .move(edge: .top)))
    }

    private func retryRecoveryBanner(_ message: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: "checkmark.shield")
                .foregroundStyle(Color("AccentColor"))
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Color("TextPrimary"))
            Spacer()
        }
        .padding(14)
        .background(Color("AccentColor").opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private var geminiProLoadingBanner: some View {
        HStack(spacing: 10) {
            Image(systemName: "sparkles")
                .foregroundStyle(Color("AccentColor"))
            Text("Gemini Pro 正在组织回复")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color("TextPrimary"))
            HStack(spacing: 4) {
                ForEach(0..<3, id: \.self) { index in
                    Circle()
                        .fill(Color("AccentColor"))
                        .frame(width: 5, height: 5)
                        .scaleEffect(geminiPulse ? 1.0 : 0.55)
                        .opacity(geminiPulse ? 1.0 : 0.45)
                        .animation(
                            .easeInOut(duration: 0.5).repeatForever(autoreverses: true).delay(Double(index) * 0.12),
                            value: geminiPulse
                        )
                }
            }
            Spacer()
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color("CardBackground"))
                .shadow(color: .black.opacity(0.05), radius: 8, y: 4)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(Color("AccentColor").opacity(0.2), lineWidth: 1)
        )
        .onAppear {
            geminiPulse = true
        }
        .onDisappear {
            geminiPulse = false
        }
    }
}

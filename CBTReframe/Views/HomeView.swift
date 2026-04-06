import SwiftUI
import SwiftData
import UIKit

struct HomeView: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject private var globalSettings: GlobalSettings
    @Environment(\.colorScheme) private var colorScheme
    @Query(sort: \HistoryEntry.createdAt, order: .reverse) private var recentHistory: [HistoryEntry]
    @Bindable var viewModel: ReframeViewModel
    @State private var isButtonPressed = false
    @State private var showExternalAppChoices = false
    @State private var geminiPulse = false
    @State private var showSecondaryTools = false
    @State private var flowStep: HomeFlowStep = .writeThought
    @FocusState private var isInputFocused: Bool

    var body: some View {
        ZStack {
            homeBackground
                .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    headerSection
                    todayDashboard

                    if viewModel.result != nil {
                        newAnalysisButton
                    }

                    VStack(spacing: 20) {
                        ThoughtInputCard(text: $viewModel.inputText, isFocused: $isInputFocused)
                        if flowStep.rawValue >= HomeFlowStep.chooseMode.rawValue {
                            templatePicker
                                .transition(.opacity.combined(with: .move(edge: .top)))
                        }
                        if flowStep.rawValue >= HomeFlowStep.chooseMood.rawValue {
                            MoodTagPicker(selectedMood: $viewModel.selectedMood, isAkathisia: $viewModel.isAkathisia)
                                .transition(.opacity.combined(with: .move(edge: .top)))
                        }
                        analyzeButton
                    }

                    if flowStep.rawValue >= HomeFlowStep.chooseMode.rawValue {
                        DisclosureGroup("更多选项", isExpanded: $showSecondaryTools) {
                            VStack(spacing: 12) {
                                externalMoneySaverSection
                            }
                            .padding(.top, 8)
                        }
                        .padding(.horizontal, 2)
                    }

                    if viewModel.inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && viewModel.result == nil {
                        quickStartSection
                    }

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
                            inputThought: viewModel.inputText,
                            moodTag: viewModel.selectedMood,
                            analysisDepthLabel: globalSettings.analysisDepth.displayName,
                            historyEntryID: viewModel.latestHistoryEntryID
                        )
                            .transition(.asymmetric(
                                insertion: .opacity
                                    .combined(with: .move(edge: .bottom))
                                    .combined(with: .scale(scale: 0.95)),
                                removal: .opacity
                            ))
                    }
                    if viewModel.isStreamingResult {
                        StreamingResultView(text: viewModel.streamingText)
                    }

                    if viewModel.result == nil && !recentHistory.isEmpty {
                        recentHistoryPreview
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
        .onChange(of: viewModel.inputText) { _, newValue in
            let hasText = !newValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            if hasText && flowStep == .writeThought {
                withAnimation(.easeInOut(duration: 0.2)) {
                    flowStep = .chooseMode
                }
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

    // MARK: - Today Dashboard

    private var todayDashboard: some View {
        HStack(spacing: 0) {
            dashboardItem(
                icon: "flame.fill",
                value: "\(viewModel.streakService.currentStreak)",
                label: "连续天数",
                color: .orange
            )
            dashboardDivider
            dashboardItem(
                icon: "brain.head.profile",
                value: "\(viewModel.todayAnalysisCount)",
                label: "今日分析",
                color: Color("AccentColor")
            )
            dashboardDivider
            dashboardItem(
                icon: "trophy.fill",
                value: "\(viewModel.streakService.longestStreak)",
                label: "最长连续",
                color: .yellow
            )
        }
        .padding(.vertical, 14)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color("CardBackground"))
                .shadow(color: .black.opacity(0.05), radius: 8, y: 4)
        )
    }

    private func dashboardItem(icon: String, value: String, label: String, color: Color) -> some View {
        VStack(spacing: 6) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(color)
            Text(value)
                .font(.title2.bold().monospacedDigit())
                .foregroundStyle(Color("TextPrimary"))
            Text(label)
                .font(.caption2)
                .foregroundStyle(Color("TextSecondary"))
        }
        .frame(maxWidth: .infinity)
    }

    private var dashboardDivider: some View {
        Rectangle()
            .fill(Color(.separator).opacity(0.2))
            .frame(width: 1, height: 40)
    }

    // MARK: - New Analysis Button

    private var newAnalysisButton: some View {
        Button {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                viewModel.reset()
                flowStep = .writeThought
            }
            HapticManager.tap()
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "plus.circle.fill")
                    .font(.subheadline)
                Text("开始新的分析")
                    .font(.subheadline.weight(.semibold))
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .foregroundStyle(Color("AccentColor"))
            .background(Color("AccentColor").opacity(0.1))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(Color("AccentColor").opacity(0.2), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    // MARK: - Quick Start

    private var quickStartSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: "lightbulb.fill")
                    .font(.subheadline)
                    .foregroundStyle(.orange)
                Text("不知道怎么开始？试试这些：")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color("TextPrimary"))
            }

            ForEach(ReframeViewModel.quickStartPrompts, id: \.text) { prompt in
                Button {
                    viewModel.inputText = prompt.text
                    HapticManager.tap()
                } label: {
                    HStack(spacing: 10) {
                        Text(prompt.emoji)
                            .font(.title3)
                        Text(prompt.text)
                            .font(.subheadline)
                            .foregroundStyle(Color("TextPrimary"))
                            .multilineTextAlignment(.leading)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Color("TextSecondary"))
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(Color(.secondarySystemGroupedBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(Color("CardBackground"))
                .shadow(color: .black.opacity(0.04), radius: 8, y: 3)
        )
    }

    // MARK: - Recent History Preview

    private var recentHistoryPreview: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "clock.arrow.circlepath")
                    .font(.subheadline)
                    .foregroundStyle(Color("AccentColor"))
                Text("最近的分析")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color("TextPrimary"))
                Spacer()
            }

            ForEach(Array(recentHistory.prefix(3)), id: \.id) { entry in
                VStack(alignment: .leading, spacing: 4) {
                    Text(entry.inputThought)
                        .font(.subheadline)
                        .foregroundStyle(Color("TextPrimary"))
                        .lineLimit(1)
                    HStack(spacing: 6) {
                        if !entry.moodTag.isEmpty {
                            Text(MoodTagPicker.emoji(for: entry.moodTag))
                            Text(entry.moodTag)
                                .font(.caption2)
                                .foregroundStyle(Color("TextSecondary"))
                        }
                        Spacer()
                        Text(entry.createdAt, style: .relative)
                            .font(.caption2)
                            .foregroundStyle(Color("TextSecondary"))
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color(.secondarySystemGroupedBackground))
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(Color("CardBackground"))
                .shadow(color: .black.opacity(0.04), radius: 8, y: 3)
        )
    }

    // MARK: - External Prompt

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

    private var canAdvanceFromThought: Bool {
        !viewModel.inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private var homeBackground: some View {
        ZStack {
            Color(.systemGroupedBackground)
            LinearGradient(
                colors: [
                    Color("AccentColor").opacity(colorScheme == .dark ? 0.04 : 0.07),
                    Color.clear,
                    Color(.systemGroupedBackground)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            IntelligenceAmbientBackground()
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

            IntelligenceAnimatedGlyph(systemName: "brain.head.profile", pointSize: 40, weight: .light)
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
                if flowStep == .writeThought {
                    guard canAdvanceFromThought else { return }
                    withAnimation(.easeInOut(duration: 0.2)) {
                        flowStep = .chooseMode
                    }
                    return
                }
                if flowStep == .chooseMode {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        flowStep = .chooseMood
                    }
                    return
                }
                if flowStep == .chooseMood {
                    await viewModel.analyzeThought(modelContext: modelContext)
                    if viewModel.result != nil {
                        UINotificationFeedbackGenerator().notificationOccurred(.success)
                    }
                    return
                }

                await viewModel.analyzeThought(modelContext: modelContext)
                if viewModel.result != nil {
                    UINotificationFeedbackGenerator().notificationOccurred(.success)
                }
            }
        } label: {
            HStack(spacing: 8) {
                if viewModel.isLoading {
                    ProgressView()
                        .tint(.white)
                } else {
                    Image(systemName: flowStep == .chooseMood ? "arrow.triangle.2.circlepath" : "arrow.right")
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
            .overlay {
                IntelligenceRainbowCardStroke(cornerRadius: 18)
                    .opacity(buttonEnabled ? 0.95 : 0.35)
            }
        }
        .disabled(!buttonEnabled)
        .opacity(buttonEnabled ? 1 : 0.6)
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
        switch flowStep {
        case .writeThought:
            return "下一步：选最省力的方式"
        case .chooseMode:
            return "下一步：点当前心情"
        case .chooseMood:
            return "开始分析"
        }
    }

    private var buttonEnabled: Bool {
        if viewModel.isLoading { return false }
        switch flowStep {
        case .writeThought:
            return canAdvanceFromThought
        case .chooseMode:
            return canAdvanceFromThought
        case .chooseMood:
            return canSubmitAnalysis
        }
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

private enum HomeFlowStep: Int {
    case writeThought
    case chooseMode
    case chooseMood
}

private struct StreamingResultView: View {
    let text: String
    @State private var visibleCount: Int = 1
    @State private var isPaused: Bool = false
    @State private var showAll: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("分段生成中")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color("TextSecondary"))

            if text.isEmpty {
                Text("正在生成...")
                    .font(.body)
                    .foregroundStyle(Color("TextSecondary"))
            } else {
                VStack(alignment: .leading, spacing: 10) {
                    ForEach(Array(visibleChunks.enumerated()), id: \.offset) { _, chunk in
                        if let highlighted = highlightedChunk, highlighted == chunk {
                            Text(chunk)
                                .font(.body.weight(.medium))
                                .foregroundStyle(Color("TextPrimary"))
                                .padding(10)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(Color("AccentColor").opacity(0.1))
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                .transition(.opacity.combined(with: .move(edge: .top)))
                        } else {
                            Text(chunk)
                                .font(.body)
                                .foregroundStyle(Color("TextPrimary"))
                                .fixedSize(horizontal: false, vertical: true)
                                .transition(.opacity.combined(with: .move(edge: .top)))
                        }
                    }
                }
            }

            HStack(spacing: 8) {
                Button(isPaused ? "继续" : "暂停") {
                    isPaused.toggle()
                }
                .buttonStyle(.bordered)

                Button(showAll ? "分段查看" : "显示全部") {
                    showAll.toggle()
                    if showAll {
                        visibleCount = chunks.count
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(Color("AccentColor"))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color("CardBackground"))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .task {
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 900_000_000)
                guard !Task.isCancelled else { return }
                guard !isPaused, !showAll else { continue }
                let target = chunks.count
                if target > visibleCount {
                    withAnimation(.easeOut(duration: 0.2)) {
                        visibleCount += 1
                    }
                }
            }
        }
        .onChange(of: text) { _, _ in
            if visibleCount < 1 {
                visibleCount = 1
            }
            if showAll {
                visibleCount = chunks.count
            }
        }
    }

    private var chunks: [String] {
        let lines = text
            .components(separatedBy: "\n")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        if !lines.isEmpty { return lines }
        let sentenceSplit = text
            .split(whereSeparator: { $0 == "。" || $0 == "！" || $0 == "？" })
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        return sentenceSplit
    }

    private var visibleChunks: [String] {
        if showAll { return chunks }
        return Array(chunks.prefix(max(1, visibleCount)))
    }

    private var highlightedChunk: String? {
        chunks.first(where: { $0.contains("替代想法") || $0.contains("下一步行动") || $0.contains("积极视角") })
    }
}


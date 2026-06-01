import Foundation
import SwiftUI
#if !SKIP
import SwiftData
#endif

@MainActor
@Observable
final class ReframeViewModel {
    var inputText: String = ""
    var result: AnalysisResult?
    var isLoading: Bool = false
    var errorMessage: String?
    var showCrisisBanner: Bool = false
    var isButtonPressed: Bool = false
    var retryRecoveryNotice: String?
    var streamingText: String = ""
    var isStreamingResult: Bool = false
    var latestHistoryEntryID: UUID?

    var selectedMood: String = ""
    var isAkathisia: Bool = false

    var analysisElapsedSeconds: Int = 0
    var thinkingPhraseIndex: Int = 0
    private var thinkingTickerTask: Task<Void, Never>?
    private var retryNoticeTask: Task<Void, Never>?

    var globalSettings: GlobalSettings
    private let resolver: AIProviderResolver
    private let streakService: StreakService

    var currentStreak: Int = 0
    var longestStreak: Int = 0
    var todayAnalysisCount: Int = 0

    static let quickStartPrompts: [(emoji: String, text: String)] = [
        ("💭", "我觉得自己什么都做不好"),
        ("😰", "明天开会我一定会搞砸"),
        ("😔", "没有人真正关心我"),
        ("😤", "所有事情都不顺利"),
        ("🫠", "我太累了，什么都不想做"),
    ]

    private let reframeUseCase: ReframeUseCase

    /// 分析进行中时首页加载条样式（互斥；与 `analyzeThought` 里是否启动计时器一致）。
    enum LoadingBannerStyle: Equatable {
        case none
        /// OpenAI o‑系列 / DeepSeek Reasoner 等：计时 + 阶段文案
        case deepReasoningWithTimer
        /// Gemini Pro：偏慢但无链式思考 UI，仅轻量提示
        case geminiPro
    }

    var loadingBannerStyle: LoadingBannerStyle {
        if modelIndicatesDeepReasoning { return .deepReasoningWithTimer }
        if modelIndicatesGeminiPro { return .geminiPro }
        return .none
    }

    private var modelIndicatesDeepReasoning: Bool {
        let id = resolver.selectedModel.id.lowercased()
        return id.contains("reasoner")
            || id.hasPrefix("o1") || id.hasPrefix("o3") || id.hasPrefix("o4")
            || id.contains("reason")
            || id.contains("thinking")
    }

    private var modelIndicatesGeminiPro: Bool {
        resolver.selectedProvider == .gemini
            && resolver.selectedModel.id.lowercased().contains("pro")
    }

    static let thinkingPhrases: [String] = [
        "理解中",
        "梳理中",
        "提炼中",
        "整理回复",
    ]

    var currentThinkingPhrase: String {
        Self.thinkingPhrases[thinkingPhraseIndex % Self.thinkingPhrases.count]
    }

    var suggestedThinkingTemplate: ThinkingTemplate? {
        ThinkingTemplate.suggest(for: inputText)
    }

    var activeTemplate: PromptTemplate {
        globalSettings.thinkingTemplate.promptTemplate
    }

    init(
        globalSettings: GlobalSettings,
        resolver: AIProviderResolver,
        reframeUseCase: ReframeUseCase,
        streakService: StreakService
    ) {
        self.globalSettings = globalSettings
        self.resolver = resolver
        self.reframeUseCase = reframeUseCase
        self.streakService = streakService
        loadStreak()
        todayAnalysisCount = streakService.todayAnalysisCount()
    }

    var greeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 5..<12: return "早上好"
        case 12..<14: return "中午好"
        case 14..<18: return "下午好"
        case 18..<22: return "晚上好"
        default: return "夜深了"
        }
    }

    static let dailyQuotes: [String] = [
        "每一个想法都只是想法，不是事实。",
        "你不需要相信脑海中的每一句话。",
        "今天也在努力理解自己，这已经很了不起了。",
        "改变从觉察开始。",
        "对自己温柔一点，你正在做一件勇敢的事。",
        "情绪像天气，会变的。",
        "你可以感受到痛苦，同时选择前行。",
        "你不需要完美，只需要前进一小步。",
        "承认情绪本身就是一种力量。",
        "慢一点没关系，你已经在路上了。",
    ]

    var todayQuote: String {
        let dayOfYear = Calendar.current.ordinality(of: .day, in: .year, for: Date()) ?? 0
        return Self.dailyQuotes[dayOfYear % Self.dailyQuotes.count]
    }

    private func incrementTodayCount() {
        todayAnalysisCount = streakService.incrementTodayCount()
    }

    private func loadStreak() {
        let streak = streakService.loadStreak()
        currentStreak = streak.current
        longestStreak = streak.longest
    }

    private func markStreakToday() {
        let streak = streakService.markToday()
        currentStreak = streak.current
        longestStreak = streak.longest
    }

    /// 生成与当前设置、风险路由一致的完整提示词，供复制到外站（免 App 内 API 费用）。
    func buildExternalManualPromptText() -> String? {
        let thought = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !thought.isEmpty else { return nil }
        let mood = selectedMood.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !mood.isEmpty else { return nil }
        let strategy = routeStrategy(level: detectRiskLevel(thought))
        return PromptBuilder.buildExternalPasteboardText(
            thought: thought,
            mood: mood,
            mode: globalSettings.analysisDepth.reframeMode,
            style: globalSettings.responseStyle.legacyResponseStyle,
            template: globalSettings.thinkingTemplate.promptTemplate,
            strategy: strategy,
            hasAkathisia: isAkathisia
        )
    }

    #if !SKIP
    @MainActor
    func analyzeThought(modelContext: ModelContext) async {
        await _analyzeThought(context: modelContext)
    }
    #else
    @MainActor
    func analyzeThought() async {
        await _analyzeThought(context: nil)
    }
    #endif

    @MainActor
    private func _analyzeThought(context: Any?) async {
        let thought = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !thought.isEmpty else { return }

        let moodTrimmed = selectedMood.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !moodTrimmed.isEmpty else {
            errorMessage = "先点一个最接近现在状态的心情，再继续。"
            return
        }
        errorMessage = nil

        isLoading = true
        errorMessage = nil
        isStreamingResult = true
        streamingText = ""
        if loadingBannerStyle == LoadingBannerStyle.deepReasoningWithTimer {
            startThinkingProgress()
        }
        defer {
            stopThinkingProgress()
            isLoading = false
        }

        #if !SKIP
        if let ctx = context as? ModelContext {
            let useCaseOutput = await reframeUseCase.analyze(
                thought: thought,
                mood: moodTrimmed,
                isAkathisia: isAkathisia,
                globalSettings: globalSettings,
                modelContext: ctx
            )
            await processOutput(useCaseOutput)
        }
        #else
        let useCaseOutput = await reframeUseCase.analyze(
            thought: thought,
            mood: moodTrimmed,
            isAkathisia: isAkathisia,
            globalSettings: globalSettings,
            modelContext: ModelContext()
        )
        await processOutput(useCaseOutput)
        #endif
    }

    @MainActor
    private func processOutput(_ useCaseOutput: ReframeUseCaseOutput) async {
        showCrisisBanner = useCaseOutput.showCrisisBanner
        if let message = useCaseOutput.errorMessage {
            errorMessage = message
            return
        }
        guard let analysisResult = useCaseOutput.result else {
            errorMessage = "分析失败，请稍后重试"
            return
        }
        await playStreamingText(for: analysisResult)

        withAnimation(.spring(response: 0.5, dampingFraction: 0.8)) {
            self.result = analysisResult
        }
        isStreamingResult = false
        
        if useCaseOutput.recoveredByRetry {
            showRetryRecoveryNotice()
        }
        latestHistoryEntryID = useCaseOutput.historyEntryID

        markStreakToday()
        incrementTodayCount()
        HapticManager.success()
    }

    @MainActor
    func reset() {
        inputText = ""
        selectedMood = ""
        isAkathisia = false
        result = nil
        errorMessage = nil
        showCrisisBanner = false
        retryRecoveryNotice = nil
        retryNoticeTask?.cancel()
        retryNoticeTask = nil
        stopThinkingProgress()
        latestHistoryEntryID = nil
    }

    @MainActor
    private func startThinkingProgress() {
        analysisElapsedSeconds = 0
        thinkingPhraseIndex = 0
        thinkingTickerTask?.cancel()
        thinkingTickerTask = Task { @MainActor in
            var ticks = 0
            while !Task.isCancelled && isLoading {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                guard !Task.isCancelled, isLoading else { break }
                ticks += 1
                analysisElapsedSeconds = ticks
                if ticks % 2 == 0 {
                    thinkingPhraseIndex = (thinkingPhraseIndex + 1) % Self.thinkingPhrases.count
                }
            }
        }
    }

    @MainActor
    private func stopThinkingProgress() {
        thinkingTickerTask?.cancel()
        thinkingTickerTask = nil
        analysisElapsedSeconds = 0
        thinkingPhraseIndex = 0
    }

    @MainActor
    private func showRetryRecoveryNotice() {
        let notice = "网络波动，已自动重试并成功"
        withAnimation(.easeInOut(duration: 0.2)) {
            retryRecoveryNotice = notice
        }
        retryNoticeTask?.cancel()
        retryNoticeTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: 3_500_000_000)
            guard !Task.isCancelled else { return }
            withAnimation(.easeInOut(duration: 0.2)) {
                if retryRecoveryNotice == notice {
                    retryRecoveryNotice = nil
                }
            }
        }
    }

    @MainActor
    private func playStreamingText(for result: AnalysisResult) async {
        let full = [
            "认知扭曲：\(result.distortion)",
            "替代想法：\(result.alternative)",
            "建议行动：\(result.action)",
        ].joined(separator: "\n")
        streamingText = ""
        for ch in full {
            streamingText = streamingText + String(ch)
            try? await Task.sleep(nanoseconds: 8_000_000)
        }
    }
}

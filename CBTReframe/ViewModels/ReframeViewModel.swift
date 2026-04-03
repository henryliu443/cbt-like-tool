import Foundation
import SwiftUI
import SwiftData

@MainActor
@Observable
final class ReframeViewModel {
    var inputText: String = ""
    var result: AnalysisResult?
    var isLoading: Bool = false
    var errorMessage: String?
    var showCrisisBanner: Bool = false
    var isButtonPressed: Bool = false
    var quickTemplate: PromptTemplate?

    /// 分析前必选，供模型结合情绪解读想法。
    var selectedMood: String = ""

    /// 深度思考类模型：请求耗时长，展示阶段性提示与计时（不向用户展示模型原始思考全文）
    var analysisElapsedSeconds: Int = 0
    var thinkingPhraseIndex: Int = 0
    private var thinkingTickerTask: Task<Void, Never>?

    var settings: SettingsViewModel

    /// OpenAI o‑系列 / DeepSeek Reasoner 等
    var isLongThinkingModel: Bool {
        let id = settings.selectedModel.id.lowercased()
        return id.contains("reasoner")
            || id.hasPrefix("o1") || id.hasPrefix("o3") || id.hasPrefix("o4")
            || id.contains("reason")
            || id.contains("thinking")
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

    var suggestedTemplate: PromptTemplate? {
        PromptTemplate.suggest(for: inputText)
    }

    var activeTemplate: PromptTemplate {
        quickTemplate ?? settings.promptTemplate
    }

    init(settings: SettingsViewModel) {
        self.settings = settings
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
    ]

    var todayQuote: String {
        let dayOfYear = Calendar.current.ordinality(of: .day, in: .year, for: Date()) ?? 0
        return Self.dailyQuotes[dayOfYear % Self.dailyQuotes.count]
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
            mode: settings.reframeMode,
            style: settings.responseStyle,
            template: activeTemplate,
            strategy: strategy
        )
    }

    @MainActor
    func analyzeThought(modelContext: ModelContext) async {
        let thought = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !thought.isEmpty else { return }

        let moodTrimmed = selectedMood.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !moodTrimmed.isEmpty else {
            errorMessage = "请先选择心情，便于结合情绪解读你的想法。"
            return
        }
        errorMessage = nil

        let riskLevel = detectRiskLevel(thought)
        let responseStrategy = routeStrategy(level: riskLevel)
        showCrisisBanner = (riskLevel == .high)

        // 高风险：本地关键词已判定，不调用远端 API（避免安全策略无有效输出且产生费用）
        if shouldUseLocalCrisisOnly(thought) {
            isLoading = true
            errorMessage = nil
            defer { isLoading = false }

            let analysisResult = CrisisLocalSupport.analysisResult
            withAnimation(.spring(response: 0.5, dampingFraction: 0.8)) {
                self.result = analysisResult
            }
            let entry = HistoryEntry(
                inputThought: thought,
                result: analysisResult,
                providerName: CrisisLocalSupport.historyProviderName,
                modelName: CrisisLocalSupport.historyModelName,
                moodTag: moodTrimmed
            )
            modelContext.insert(entry)
            try? modelContext.save()
            return
        }

        isLoading = true
        errorMessage = nil
        if isLongThinkingModel {
            startThinkingProgress()
        }
        defer {
            stopThinkingProgress()
            isLoading = false
        }

        do {
            let service = AIServiceFactory.service(for: settings.selectedProvider)
            let template = activeTemplate
            let analysisResult = try await service.reframe(
                thought: thought,
                mood: moodTrimmed,
                model: settings.selectedModel,
                mode: settings.reframeMode,
                style: settings.responseStyle,
                template: template,
                strategy: responseStrategy
            )

            withAnimation(.spring(response: 0.5, dampingFraction: 0.8)) {
                self.result = analysisResult
            }

            let entry = HistoryEntry(
                inputThought: thought,
                result: analysisResult,
                providerName: settings.selectedProvider.displayName,
                modelName: settings.selectedModel.name,
                moodTag: moodTrimmed
            )
            modelContext.insert(entry)
            try? modelContext.save()

        } catch let error as AIServiceError {
            errorMessage = error.errorDescription
        } catch is CancellationError {
            errorMessage = nil
        } catch let error as DecodingError {
            errorMessage = "AI 响应格式异常，请重试"
            print("[CBTReframe] DecodingError: \(error)")
        } catch {
            errorMessage = "发生了未知错误：\(error.localizedDescription)"
            print("[CBTReframe] Error: \(error)")
        }
    }

    @MainActor
    func reset() {
        inputText = ""
        selectedMood = ""
        result = nil
        errorMessage = nil
        showCrisisBanner = false
        stopThinkingProgress()
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
}

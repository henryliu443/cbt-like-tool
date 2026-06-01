import Foundation
#if !SKIP
import SwiftData
#endif

struct ReframeUseCaseOutput {
    let result: AnalysisResult?
    let errorMessage: String?
    let showCrisisBanner: Bool
    let recoveredByRetry: Bool
    let historyEntryID: UUID?
}

@MainActor
final class ReframeUseCase {
    private let pipeline: ReframePipeline
    private let resolver: AIProviderResolver

    init(pipeline: ReframePipeline, resolver: AIProviderResolver) {
        self.pipeline = pipeline
        self.resolver = resolver
    }

    func analyze(
        thought: String,
        mood: String,
        isAkathisia: Bool,
        globalSettings: GlobalSettings,
        modelContext: ModelContext
    ) async -> ReframeUseCaseOutput {
        let riskLevel = detectRiskLevel(thought)
        let responseStrategy = routeStrategy(level: riskLevel)
        let showCrisisBanner = (riskLevel == .high)
        let template = globalSettings.thinkingTemplate

        // 高风险：本地关键词已判定，不调用远端 API（避免安全策略无有效输出且产生费用）
        if shouldUseLocalCrisisOnly(thought) {
            let analysisResult = CrisisLocalSupport.analysisResult.normalized(for: template)
            let entry = HistoryEntry(
                inputThought: thought,
                result: analysisResult,
                providerName: CrisisLocalSupport.historyProviderName,
                modelName: CrisisLocalSupport.historyModelName,
                moodTag: ReframeUseCase.moodTagForHistory(base: mood, isAkathisia: isAkathisia),
                therapyTemplate: template,
                analysisDepth: globalSettings.analysisDepth,
                responseStyle: globalSettings.responseStyle
            )
            #if !SKIP
            modelContext.insert(entry)
            try? modelContext.save()
            #endif
            return ReframeUseCaseOutput(
                result: analysisResult,
                errorMessage: nil,
                showCrisisBanner: showCrisisBanner,
                recoveredByRetry: false,
                historyEntryID: entry.id
            )
        }

        let envelope = AnalysisInputEnvelope(
            thought: thought,
            mood: mood,
            strategy: responseStrategy,
            hasAkathisia: isAkathisia
        )
        let rawResult = await pipeline.run(envelope: envelope, settings: globalSettings)
        if let message = rawResult.errorMessage {
            return ReframeUseCaseOutput(
                result: nil,
                errorMessage: message,
                showCrisisBanner: showCrisisBanner,
                recoveredByRetry: false,
                historyEntryID: nil
            )
        }
        guard let decodedResult = rawResult.result else {
            return ReframeUseCaseOutput(
                result: nil,
                errorMessage: "分析失败，请稍后重试",
                showCrisisBanner: showCrisisBanner,
                recoveredByRetry: false,
                historyEntryID: nil
            )
        }

        let analysisResult = decodedResult.normalized(for: template)
        let entry = HistoryEntry(
            inputThought: thought,
            result: analysisResult,
            providerName: resolver.selectedProvider.displayName,
            modelName: resolver.selectedModel.name,
            moodTag: ReframeUseCase.moodTagForHistory(base: mood, isAkathisia: isAkathisia),
            therapyTemplate: template,
            analysisDepth: globalSettings.analysisDepth,
            responseStyle: globalSettings.responseStyle
        )
        #if !SKIP
        modelContext.insert(entry)

        let moodScore = MoodTagPicker.score(for: mood)
        let checkin = MoodCheckIn(moodScore: moodScore, moodLabel: mood)
        modelContext.insert(checkin)

        try? modelContext.save()
        #endif
        return ReframeUseCaseOutput(
            result: analysisResult,
            errorMessage: nil,
            showCrisisBanner: showCrisisBanner,
            recoveredByRetry: rawResult.metadata.recoveredByRetry,
            historyEntryID: entry.id
        )
    }

    /// 历史列表展示：勾选 Akathisia 时在心情后标注。
    private static func moodTagForHistory(base: String, isAkathisia: Bool) -> String {
        guard isAkathisia else { return base }
        if base == PromptBuilder.akathisiaMoodTag { return base }
        return "\(base)（Akathisia）"
    }
}

#if SKIP
public class ModelContext {}
#endif

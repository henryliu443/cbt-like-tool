import Foundation
import SwiftData

struct ReframeUseCaseOutput {
    let result: AnalysisResult?
    let errorMessage: String?
    let showCrisisBanner: Bool
    let recoveredByRetry: Bool
    let historyProviderName: String?
    let historyModelName: String?
    let moodTag: String?
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
        globalSettings: GlobalSettings
    ) async -> ReframeUseCaseOutput {
        let riskLevel = detectRiskLevel(thought)
        let responseStrategy = routeStrategy(level: riskLevel)
        let showCrisisBanner = (riskLevel == .high)
        let template = globalSettings.thinkingTemplate

        // 高风险：本地关键词已判定，不调用远端 API（避免安全策略无有效输出且产生费用）
        if shouldUseLocalCrisisOnly(thought) {
            let analysisResult = CrisisLocalSupport.analysisResult.normalized(for: template)
            return ReframeUseCaseOutput(
                result: analysisResult,
                errorMessage: nil,
                showCrisisBanner: showCrisisBanner,
                recoveredByRetry: false,
                historyProviderName: CrisisLocalSupport.historyProviderName,
                historyModelName: CrisisLocalSupport.historyModelName,
                moodTag: ReframeUseCase.moodTagForHistory(base: mood, isAkathisia: isAkathisia)
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
                historyProviderName: nil,
                historyModelName: nil,
                moodTag: nil
            )
        }
        guard let decodedResult = rawResult.result else {
            return ReframeUseCaseOutput(
                result: nil,
                errorMessage: "分析失败，请稍后重试",
                showCrisisBanner: showCrisisBanner,
                recoveredByRetry: false,
                historyProviderName: nil,
                historyModelName: nil,
                moodTag: nil
            )
        }

        let analysisResult = decodedResult.normalized(for: template)
        return ReframeUseCaseOutput(
            result: analysisResult,
            errorMessage: nil,
            showCrisisBanner: showCrisisBanner,
            recoveredByRetry: rawResult.metadata.recoveredByRetry,
            historyProviderName: resolver.selectedProvider.displayName,
            historyModelName: resolver.selectedModel.name,
            moodTag: ReframeUseCase.moodTagForHistory(base: mood, isAkathisia: isAkathisia)
        )
    }

    /// 历史列表展示：勾选 Akathisia 时在心情后标注。
    private static func moodTagForHistory(base: String, isAkathisia: Bool) -> String {
        guard isAkathisia else { return base }
        if base == PromptBuilder.akathisiaMoodTag { return base }
        return "\(base)（Akathisia）"
    }
}

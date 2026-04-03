import Foundation

/// Encodes thought + mood + risk strategy for engines (`input` JSON string).
struct AnalysisInputEnvelope: Codable {
    let thought: String
    let mood: String
    let strategy: ResponseStrategy
}

struct AnalysisRunMetadata {
    let attemptCount: Int
    let recoveredByRetry: Bool

    static let `default` = AnalysisRunMetadata(attemptCount: 1, recoveredByRetry: false)
}

struct AnalysisPipelineOutput {
    let result: AnalysisResult?
    let metadata: AnalysisRunMetadata
    let errorMessage: String?
}

/// Central entry for reframe analysis: ViewModels call this instead of AI services.
@MainActor
final class AnalysisPipeline {
    private let router = EngineRouter()
    private let provider: LLMProvider
    private let settingsViewModel: SettingsViewModel

    init(provider: LLMProvider, settingsViewModel: SettingsViewModel) {
        self.provider = provider
        self.settingsViewModel = settingsViewModel
    }

    /// Single entry for reframe analysis. `input` must be JSON-encoded `AnalysisInputEnvelope` (thought, mood, risk strategy).
    func run(input: String, settings: GlobalSettings) async -> AnalysisPipelineOutput {
        guard let data = input.data(using: .utf8),
              let envelope = try? JSONDecoder().decode(AnalysisInputEnvelope.self, from: data) else {
            return AnalysisPipelineOutput(
                result: nil,
                metadata: .default,
                errorMessage: "分析请求格式错误，请稍后重试"
            )
        }

        let engine = router.resolve(settings: settings)
        do {
            let raw = try await engine.generateRaw(
                input: input,
                settings: settings,
                provider: provider
            )
            var result = safeDecode(raw.text, strategy: envelope.strategy)
            if settings.thinkingTemplate == .socratic && envelope.strategy != .crisis {
                do {
                    result = try SocraticPipelineValidation.applyingSanitizedQuestions(result)
                } catch {
                    let serviceError = AIServiceError.classify(error)
                    return AnalysisPipelineOutput(
                        result: nil,
                        metadata: AnalysisRunMetadata(
                            attemptCount: raw.attemptCount,
                            recoveredByRetry: raw.recoveredByRetry
                        ),
                        errorMessage: serviceError.userFacingMessage
                    )
                }
            }
            return AnalysisPipelineOutput(
                result: result,
                metadata: AnalysisRunMetadata(
                    attemptCount: raw.attemptCount,
                    recoveredByRetry: raw.recoveredByRetry
                ),
                errorMessage: nil
            )
        } catch {
            let serviceError = AIServiceError.classify(error)
            return AnalysisPipelineOutput(
                result: nil,
                metadata: .default,
                errorMessage: serviceError.userFacingMessage
            )
        }
    }

    /// Thought journal pattern analysis — routed here so ViewModels do not call `AIServiceFactory` directly.
    func analyzeThoughtPatterns(entries: [ThoughtEntry]) async throws -> ThoughtPatternReport {
        let service = AIServiceFactory.service(for: settingsViewModel.selectedProvider)
        return try await service.analyzeThoughtPatterns(
            thoughts: entries,
            model: settingsViewModel.selectedModel
        )
    }

    private func safeDecode(_ raw: String, strategy: ResponseStrategy) -> AnalysisResult {
        if let data = raw.data(using: .utf8),
           let decoded = try? JSONDecoder().decode(AnalysisResult.self, from: data) {
            return decoded
        }
        if let parsed = try? parseReframeOutput(raw, strategy: strategy) {
            return parsed
        }
        return AnalysisResult(
            distortion: "error",
            alternative: raw,
            action: "请尝试重新分析"
        )
    }
}

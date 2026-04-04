import Foundation

struct AnalysisEngineRequest {
    let envelope: AnalysisInputEnvelope
    let settings: GlobalSettings

    func llmRequest(template: PromptTemplate) -> ReframeLLMRequest {
        ReframeLLMRequest(
            thought: envelope.thought,
            mood: envelope.mood,
            hasAkathisia: envelope.hasAkathisia,
            mode: settings.analysisDepth.reframeMode,
            style: settings.responseStyle.legacyResponseStyle,
            template: template,
            strategy: envelope.strategy
        )
    }
}

protocol AnalysisEngine {
    var promptTemplate: PromptTemplate { get }
    func generateRaw(request: AnalysisEngineRequest, provider: LLMProvider) async throws -> LLMGenerationOutput
}

extension AnalysisEngine {
    func generateRaw(request: AnalysisEngineRequest, provider: LLMProvider) async throws -> LLMGenerationOutput {
        let llmRequest = request.llmRequest(template: promptTemplate)
        let data = try JSONEncoder().encode(llmRequest)
        guard let prompt = String(data: data, encoding: .utf8) else {
            throw AIServiceError.parseError("encode request")
        }
        return try await provider.generate(prompt: prompt)
    }
}

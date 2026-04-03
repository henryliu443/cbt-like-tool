import Foundation

final class SocraticEngine: AnalysisEngine {
    func generateRaw(
        input: String,
        settings: GlobalSettings,
        provider: LLMProvider
    ) async throws -> String {
        let envelope = try JSONDecoder().decode(AnalysisInputEnvelope.self, from: Data(input.utf8))
        let req = ReframeLLMRequest(
            thought: envelope.thought,
            mood: envelope.mood,
            mode: settings.analysisDepth.reframeMode,
            style: settings.responseStyle.legacyResponseStyle,
            template: PromptTemplate.socratic,
            strategy: envelope.strategy
        )
        let data = try JSONEncoder().encode(req)
        guard let prompt = String(data: data, encoding: .utf8) else {
            throw AIServiceError.parseError("encode request")
        }
        return try await provider.generate(prompt: prompt)
    }
}

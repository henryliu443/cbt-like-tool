import Foundation

protocol AnalysisEngine {
    func generateRaw(
        input: String,
        settings: GlobalSettings,
        provider: LLMProvider
    ) async throws -> LLMGenerationOutput
}

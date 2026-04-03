import Foundation

/// Wire-format request for `AIServiceProtocol.reframe` (encoded as JSON string for `LLMProvider.generate`).
struct ReframeLLMRequest: Codable {
    let thought: String
    let mood: String
    let mode: ReframeMode
    let style: ResponseStyle
    let template: PromptTemplate
    let strategy: ResponseStrategy
}

/// Bridges `LLMProvider` to existing `AIServiceFactory` + `reframe` without modifying service implementations.
@MainActor
final class AIServiceLLMProvider: LLMProvider {
    private let settingsViewModel: SettingsViewModel

    init(settingsViewModel: SettingsViewModel) {
        self.settingsViewModel = settingsViewModel
    }

    func generate(prompt: String) async throws -> String {
        guard let data = prompt.data(using: .utf8) else {
            throw AIServiceError.parseError("invalid prompt encoding")
        }
        let req = try JSONDecoder().decode(ReframeLLMRequest.self, from: data)
        let service = AIServiceFactory.service(for: settingsViewModel.selectedProvider)
        let result = try await service.reframe(
            thought: req.thought,
            mood: req.mood,
            model: settingsViewModel.selectedModel,
            mode: req.mode,
            style: req.style,
            template: req.template,
            strategy: req.strategy
        )
        let out = try JSONEncoder().encode(result)
        guard let s = String(data: out, encoding: .utf8) else {
            throw AIServiceError.invalidResponse
        }
        return s
    }
}

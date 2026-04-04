import Foundation

/// Wire-format request for `AIServiceProtocol.reframe` (encoded as JSON string for `LLMProvider.generate`).
struct ReframeLLMRequest: Codable {
    let thought: String
    let mood: String
    let hasAkathisia: Bool
    let mode: ReframeMode
    let style: ResponseStyle
    let template: PromptTemplate
    let strategy: ResponseStrategy

    enum CodingKeys: String, CodingKey {
        case thought, mood, hasAkathisia, mode, style, template, strategy
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        thought = try c.decode(String.self, forKey: .thought)
        mood = try c.decode(String.self, forKey: .mood)
        hasAkathisia = try c.decodeIfPresent(Bool.self, forKey: .hasAkathisia) ?? false
        mode = try c.decode(ReframeMode.self, forKey: .mode)
        style = try c.decode(ResponseStyle.self, forKey: .style)
        template = try c.decode(PromptTemplate.self, forKey: .template)
        strategy = try c.decode(ResponseStrategy.self, forKey: .strategy)
    }

    init(
        thought: String,
        mood: String,
        hasAkathisia: Bool,
        mode: ReframeMode,
        style: ResponseStyle,
        template: PromptTemplate,
        strategy: ResponseStrategy
    ) {
        self.thought = thought
        self.mood = mood
        self.hasAkathisia = hasAkathisia
        self.mode = mode
        self.style = style
        self.template = template
        self.strategy = strategy
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(thought, forKey: .thought)
        try c.encode(mood, forKey: .mood)
        try c.encode(hasAkathisia, forKey: .hasAkathisia)
        try c.encode(mode, forKey: .mode)
        try c.encode(style, forKey: .style)
        try c.encode(template, forKey: .template)
        try c.encode(strategy, forKey: .strategy)
    }
}

/// Bridges `LLMProvider` to existing `AIServiceFactory` + `reframe` without modifying service implementations.
@MainActor
final class AIServiceLLMProvider: LLMProvider {
    private let settingsViewModel: SettingsViewModel

    init(settingsViewModel: SettingsViewModel) {
        self.settingsViewModel = settingsViewModel
    }

    func generate(prompt: String) async throws -> LLMGenerationOutput {
        guard let data = prompt.data(using: .utf8) else {
            throw AIServiceError.parseError("invalid prompt encoding")
        }
        let req = try JSONDecoder().decode(ReframeLLMRequest.self, from: data)
        let service = AIServiceFactory.service(for: settingsViewModel.selectedProvider)

        let maxAttempts = (req.template == .socratic && req.strategy != .crisis) ? 3 : 2
        let retried = try await ReframeRetryExecutor.run(maxAttempts: maxAttempts) {
            var result = try await service.reframe(
                thought: req.thought,
                mood: req.mood,
                hasAkathisia: req.hasAkathisia,
                model: settingsViewModel.selectedModel,
                mode: req.mode,
                style: req.style,
                template: req.template,
                strategy: req.strategy
            )
            if req.template == .socratic && req.strategy != .crisis {
                result = try SocraticPipelineValidation.applyingSanitizedQuestions(result)
            }
            if req.strategy != .crisis {
                try ReframeOutputGate.validate(result, template: req.template)
            }
            return result
        }

        let out = try JSONEncoder().encode(retried.value)
        guard let s = String(data: out, encoding: .utf8) else {
            throw AIServiceError.invalidResponse
        }
        return LLMGenerationOutput(
            text: s,
            attemptCount: retried.attemptCount,
            recoveredByRetry: retried.recoveredByRetry
        )
    }
}

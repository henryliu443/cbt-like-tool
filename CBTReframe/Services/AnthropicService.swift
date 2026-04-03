import Foundation

struct AnthropicService: AIServiceProtocol {
    let provider = AIProvider.anthropic

    func reframe(
        thought: String,
        mood: String,
        model: AIModel,
        mode: ReframeMode,
        style: ResponseStyle,
        template: PromptTemplate,
        strategy: ResponseStrategy
    ) async throws -> AnalysisResult {
        guard let apiKey = KeychainManager.shared.load(key: provider.rawValue),
              !apiKey.isEmpty else {
            throw AIServiceError.noAPIKey
        }

        let systemPrompt = PromptBuilder.buildSystemPrompt(mode: mode, style: style, template: template, strategy: strategy)
        let userPrompt = PromptBuilder.buildUserPrompt(thought: thought, mood: mood)

        let body: [String: Any] = [
            "model": model.id,
            "max_tokens": strategy == .crisis ? 512 : 1024,
            "system": systemPrompt,
            "messages": [
                ["role": "user", "content": userPrompt],
            ],
        ]

        var request = URLRequest(url: URL(string: provider.baseURL)!)
        request.httpMethod = "POST"
        request.setValue(apiKey, forHTTPHeaderField: "x-api-key")
        request.setValue("2023-06-01", forHTTPHeaderField: "anthropic-version")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        request.timeoutInterval = 60

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw AIServiceError.invalidResponse
        }

        switch httpResponse.statusCode {
        case 200: break
        case 401: throw AIServiceError.invalidKey
        case 429: throw AIServiceError.rateLimited
        default: throw AIServiceError.invalidResponse
        }

        return try parseAnthropicResponse(data, strategy: strategy)
    }

    func analyzeThoughtPatterns(
        thoughts: [ThoughtEntry],
        model: AIModel
    ) async throws -> ThoughtPatternReport {
        guard let apiKey = KeychainManager.shared.load(key: provider.rawValue),
              !apiKey.isEmpty else {
            throw AIServiceError.noAPIKey
        }

        let body: [String: Any] = [
            "model": model.id,
            "max_tokens": 1400,
            "system": PromptBuilder.thoughtPatternSystemPrompt,
            "messages": [
                ["role": "user", "content": PromptBuilder.buildThoughtPatternUserPrompt(thoughts: thoughts)],
            ],
        ]

        var request = URLRequest(url: URL(string: provider.baseURL)!)
        request.httpMethod = "POST"
        request.setValue(apiKey, forHTTPHeaderField: "x-api-key")
        request.setValue("2023-06-01", forHTTPHeaderField: "anthropic-version")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        request.timeoutInterval = 60

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw AIServiceError.invalidResponse
        }

        switch httpResponse.statusCode {
        case 200: break
        case 401: throw AIServiceError.invalidKey
        case 429: throw AIServiceError.rateLimited
        default: throw AIServiceError.invalidResponse
        }

        return try parseAnthropicThoughtPatternResponse(data)
    }

    private func parseAnthropicResponse(_ data: Data, strategy: ResponseStrategy) throws -> AnalysisResult {
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let content = json["content"] as? [[String: Any]],
              let textBlock = content.first(where: { $0["type"] as? String == "text" }),
              let text = textBlock["text"] as? String else {
            throw AIServiceError.invalidResponse
        }

        return try parseReframeOutput(text, strategy: strategy)
    }

    private func parseAnthropicThoughtPatternResponse(_ data: Data) throws -> ThoughtPatternReport {
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let content = json["content"] as? [[String: Any]],
              let textBlock = content.first(where: { $0["type"] as? String == "text" }),
              let text = textBlock["text"] as? String else {
            throw AIServiceError.invalidResponse
        }

        return try parseThoughtPatternContent(text)
    }
}

import Foundation

struct DeepSeekService: AIServiceProtocol {
    let provider = AIProvider.deepseek

    func reframe(
        thought: String,
        model: AIModel,
        mode: ReframeMode,
        style: ResponseStyle,
        template: PromptTemplate
    ) async throws -> AnalysisResult {
        guard let apiKey = KeychainManager.shared.load(key: provider.rawValue),
              !apiKey.isEmpty else {
            throw AIServiceError.noAPIKey
        }

        let isReasoner = model.id.contains("reasoner")

        let systemPrompt = PromptBuilder.buildSystemPrompt(mode: mode, style: style, template: template)
        let userPrompt = PromptBuilder.buildUserPrompt(thought: thought)

        var body: [String: Any] = [
            "model": model.id,
            "max_tokens": isReasoner ? 4096 : 1024,
        ]

        if isReasoner {
            body["messages"] = [
                ["role": "user", "content": "\(systemPrompt)\n\n\(userPrompt)"],
            ]
        } else {
            body["messages"] = [
                ["role": "system", "content": systemPrompt],
                ["role": "user", "content": userPrompt],
            ]
            body["temperature"] = 0.7
        }

        var request = URLRequest(url: URL(string: provider.baseURL)!)
        request.httpMethod = "POST"
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        request.timeoutInterval = isReasoner ? 120 : 60

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw AIServiceError.invalidResponse
        }

        if httpResponse.statusCode != 200 {
            let bodyStr = String(data: data, encoding: .utf8) ?? ""
            print("[CBTReframe][DeepSeek] HTTP \(httpResponse.statusCode): \(bodyStr)")
        }

        switch httpResponse.statusCode {
        case 200: break
        case 401: throw AIServiceError.invalidKey
        case 429: throw AIServiceError.rateLimited
        case 400:
            let bodyStr = String(data: data, encoding: .utf8) ?? ""
            if bodyStr.contains("Insufficient Balance") || bodyStr.contains("insufficient") {
                throw AIServiceError.parseError("DeepSeek 账户余额不足，请充值后重试")
            }
            throw AIServiceError.invalidResponse
        default: throw AIServiceError.invalidResponse
        }

        return try parseDeepSeekResponse(data)
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
            "messages": [
                ["role": "system", "content": PromptBuilder.thoughtPatternSystemPrompt],
                ["role": "user", "content": PromptBuilder.buildThoughtPatternUserPrompt(thoughts: thoughts)],
            ],
            "max_tokens": 1600,
            "temperature": 0.3,
        ]

        var request = URLRequest(url: URL(string: provider.baseURL)!)
        request.httpMethod = "POST"
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
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

        return try parseDeepSeekThoughtPatternResponse(data)
    }

    private func parseDeepSeekResponse(_ data: Data) throws -> AnalysisResult {
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let choices = json["choices"] as? [[String: Any]],
              let message = choices.first?["message"] as? [String: Any] else {
            let raw = String(data: data, encoding: .utf8) ?? "nil"
            print("[CBTReframe][DeepSeek] Cannot parse top-level: \(raw.prefix(500))")
            throw AIServiceError.invalidResponse
        }

        let content = message["content"] as? String
        let reasoningContent = message["reasoning_content"] as? String

        let textToParse: String
        if let content = content, !content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            textToParse = content
        } else if let reasoning = reasoningContent, !reasoning.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            textToParse = reasoning
        } else {
            print("[CBTReframe][DeepSeek] message fields: \(message.keys)")
            throw AIServiceError.invalidResponse
        }

        return try parseJSONContent(textToParse)
    }

    private func parseDeepSeekThoughtPatternResponse(_ data: Data) throws -> ThoughtPatternReport {
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let choices = json["choices"] as? [[String: Any]],
              let message = choices.first?["message"] as? [String: Any],
              let content = message["content"] as? String else {
            throw AIServiceError.invalidResponse
        }

        return try parseThoughtPatternContent(content)
    }
}

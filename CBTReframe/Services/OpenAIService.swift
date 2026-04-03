import Foundation

struct OpenAIService: AIServiceProtocol {
    let provider = AIProvider.openai

    func reframe(
        thought: String,
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
        let userPrompt = PromptBuilder.buildUserPrompt(thought: thought)

        let body: [String: Any] = [
            "model": model.id,
            "messages": [
                ["role": "system", "content": systemPrompt],
                ["role": "user", "content": userPrompt],
            ],
            "temperature": 0.7,
            "max_tokens": strategy == .crisis ? 512 : 1024,
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

        return try parseOpenAIResponse(data, strategy: strategy)
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
            "temperature": 0.3,
            "max_tokens": 1400,
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

        return try parseOpenAIThoughtPatternResponse(data)
    }

    private func parseOpenAIResponse(_ data: Data, strategy: ResponseStrategy) throws -> AnalysisResult {
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let choices = json["choices"] as? [[String: Any]],
              let message = choices.first?["message"] as? [String: Any],
              let content = message["content"] as? String else {
            throw AIServiceError.invalidResponse
        }

        return try parseReframeOutput(content, strategy: strategy)
    }

    private func parseOpenAIThoughtPatternResponse(_ data: Data) throws -> ThoughtPatternReport {
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let choices = json["choices"] as? [[String: Any]],
              let message = choices.first?["message"] as? [String: Any],
              let content = message["content"] as? String else {
            throw AIServiceError.invalidResponse
        }

        return try parseThoughtPatternContent(content)
    }
}

func parseReframeOutput(_ content: String, strategy: ResponseStrategy) throws -> AnalysisResult {
    if strategy == .crisis {
        return parsePlainTextCrisisResponse(content)
    }
    return try parseJSONContent(content)
}

func parsePlainTextCrisisResponse(_ content: String) -> AnalysisResult {
    var text = content
        .replacingOccurrences(of: "```", with: "")
        .trimmingCharacters(in: .whitespacesAndNewlines)
    if text.isEmpty {
        text = "你愿意说出来，这本身就很不容易。你值得被认真对待，也有人愿意陪伴你度过这段艰难的时刻。"
    }
    return AnalysisResult(
        distortion: "支持与陪伴",
        alternative: text,
        action: "若情绪持续或加重，请向信任的人求助，或联系当地心理援助热线与专业医疗机构。"
    )
}

func parseJSONContent(_ content: String) throws -> AnalysisResult {
    var text = content
        .replacingOccurrences(of: "```json", with: "")
        .replacingOccurrences(of: "```", with: "")
        .trimmingCharacters(in: .whitespacesAndNewlines)

    if let startIdx = text.firstIndex(of: "{"),
       let endIdx = text.lastIndex(of: "}") {
        text = String(text[startIdx...endIdx])
    }

    guard let data = text.data(using: .utf8) else {
        throw AIServiceError.parseError("无法转换响应文本")
    }

    if let decoded = try? JSONDecoder().decode(AnalysisResult.self, from: data) {
        return decoded
    }

    if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
        let distortion = json["distortion"] as? String
            ?? json["认知扭曲"] as? String
            ?? json["cognitive_distortion"] as? String
            ?? "未识别"
        let alternative = json["alternative"] as? String
            ?? json["替代想法"] as? String
            ?? json["alternative_thought"] as? String
            ?? "暂无替代想法"
        let action = json["action"] as? String
            ?? json["建议行动"] as? String
            ?? json["小行动"] as? String
            ?? json["suggested_action"] as? String
            ?? "暂无建议行动"

        return AnalysisResult(
            distortion: distortion,
            alternative: alternative,
            action: action
        )
    }

    let lines = content.components(separatedBy: .newlines).filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
    if lines.count >= 3 {
        return AnalysisResult(
            distortion: lines[0].trimmingCharacters(in: .whitespacesAndNewlines),
            alternative: lines[1].trimmingCharacters(in: .whitespacesAndNewlines),
            action: lines[2].trimmingCharacters(in: .whitespacesAndNewlines)
        )
    }

    return AnalysisResult(
        distortion: "AI 分析",
        alternative: content,
        action: "请尝试重新分析"
    )
}

func parseThoughtPatternContent(_ content: String) throws -> ThoughtPatternReport {
    var text = content
        .replacingOccurrences(of: "```json", with: "")
        .replacingOccurrences(of: "```", with: "")
        .trimmingCharacters(in: .whitespacesAndNewlines)

    if let startIdx = text.firstIndex(of: "{"),
       let endIdx = text.lastIndex(of: "}") {
        text = String(text[startIdx...endIdx])
    }

    guard let data = text.data(using: .utf8) else {
        throw AIServiceError.parseError("无法转换模式分析文本")
    }

    do {
        return try JSONDecoder().decode(ThoughtPatternReport.self, from: data)
    } catch {
        throw AIServiceError.parseError("模式分析 JSON 解析失败")
    }
}

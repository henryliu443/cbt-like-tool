import Foundation

/// Google Gemini：`generateContent`（API Key 来自 Google AI Studio / Vertex）
struct GeminiService: AIServiceProtocol {
    let provider = AIProvider.gemini

    /// 与 `URLSession.shared` 分离；弱网访问 Google 时需更长「等首包 / 等下一 chunk」与整次传输上限，避免半截 JSON。
    private static let generateSession: URLSession = {
        let config = URLSessionConfiguration.ephemeral
        config.timeoutIntervalForRequest = 600
        config.timeoutIntervalForResource = 1200
        config.waitsForConnectivity = true
        return URLSession(configuration: config)
    }()

    func reframe(
        thought: String,
        mood: String,
        hasAkathisia: Bool,
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

        let systemPrompt = PromptBuilder.buildSystemPrompt(
            mode: mode,
            style: style,
            template: template,
            strategy: strategy,
            mood: mood,
            hasAkathisia: hasAkathisia
        )
        let userPrompt = PromptBuilder.buildUserPrompt(thought: thought, mood: mood, hasAkathisia: hasAkathisia)

        let body: [String: Any] = [
            "systemInstruction": [
                "parts": [["text": systemPrompt]],
            ],
            "contents": [
                [
                    "role": "user",
                    "parts": [["text": userPrompt]],
                ],
            ],
            "generationConfig": [
                "temperature": 0.7,
                // 中文 JSON + actions 数组易顶满 1024，表现为合法 HTTP 200 但 JSON 半截、解析失败
                "maxOutputTokens": strategy == .crisis ? 512 : 4096,
            ] as [String: Any],
        ]

        let (data, response) = try await performGenerateContent(modelId: model.id, apiKey: apiKey, body: body)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw AIServiceError.invalidResponse
        }

        switch httpResponse.statusCode {
        case 200: break
        case 401, 403: throw AIServiceError.invalidKey
        case 429: throw AIServiceError.httpStatus(429)
        case 500, 502, 503, 504: throw AIServiceError.httpStatus(httpResponse.statusCode)
        default:
            if let msg = geminiErrorMessage(from: data) {
                NSLog("Gemini HTTP \(httpResponse.statusCode): \(msg)")
            }
            throw AIServiceError.httpStatus(httpResponse.statusCode)
        }

        return try parseGeminiGenerateResponse(data, strategy: strategy)
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
            "systemInstruction": [
                "parts": [["text": PromptBuilder.thoughtPatternSystemPrompt]],
            ],
            "contents": [
                [
                    "role": "user",
                    "parts": [["text": PromptBuilder.buildThoughtPatternUserPrompt(thoughts: thoughts)]],
                ],
            ],
            "generationConfig": [
                "temperature": 0.3,
                "maxOutputTokens": 1400,
            ] as [String: Any],
        ]

        let (data, response) = try await performGenerateContent(modelId: model.id, apiKey: apiKey, body: body)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw AIServiceError.invalidResponse
        }

        switch httpResponse.statusCode {
        case 200: break
        case 401, 403: throw AIServiceError.invalidKey
        case 429: throw AIServiceError.httpStatus(429)
        case 500, 502, 503, 504: throw AIServiceError.httpStatus(httpResponse.statusCode)
        default: throw AIServiceError.httpStatus(httpResponse.statusCode)
        }

        let text = try extractGeminiText(from: data)
        return try parseThoughtPatternContent(text)
    }

    private func performGenerateContent(modelId: String, apiKey: String, body: [String: Any]) async throws -> (Data, URLResponse) {
        guard var components = URLComponents(string: "\(provider.baseURL)/models/\(modelId):generateContent") else {
            throw AIServiceError.invalidResponse
        }
        components.queryItems = [URLQueryItem(name: "key", value: apiKey)]
        guard let url = components.url else {
            throw AIServiceError.invalidResponse
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        request.timeoutInterval = 600

        return try await Self.generateSession.data(for: request)
    }

    private func geminiErrorMessage(from data: Data) -> String? {
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let err = json["error"] as? [String: Any] else { return nil }
        return err["message"] as? String ?? err["status"] as? String
    }

    private func parseGeminiGenerateResponse(_ data: Data, strategy: ResponseStrategy) throws -> AnalysisResult {
        let text = try extractGeminiText(from: data)
        let sanitized = LLMJSONSanitizer.sanitizeForJSONObject(text)
        return try parseReframeOutput(sanitized, strategy: strategy)
    }

    private func extractGeminiText(from data: Data) throws -> String {
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw AIServiceError.invalidResponse
        }
        if let err = json["error"] as? [String: Any], let msg = err["message"] as? String {
            throw AIServiceError.parseError(msg)
        }
        guard let candidates = json["candidates"] as? [[String: Any]],
              let first = candidates.first,
              let content = first["content"] as? [String: Any],
              let parts = content["parts"] as? [[String: Any]] else {
            throw AIServiceError.invalidResponse
        }
        let text = parts.compactMap { $0["text"] as? String }.joined()
        guard !text.trimmingCharacters(in: CharacterSet.whitespacesAndNewlines).isEmpty else {
            throw AIServiceError.invalidResponse
        }
        return text
    }
}

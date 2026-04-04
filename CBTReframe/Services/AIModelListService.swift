import Foundation

enum AIModelListError: LocalizedError {
    case invalidURL
    case httpStatus(Int)
    case decodeFailed

    var errorDescription: String? {
        switch self {
        case .invalidURL: return "无效的模型列表地址"
        case .httpStatus(let c): return "服务器返回 \(c)"
        case .decodeFailed: return "无法解析模型列表"
        }
    }
}

/// 从各厂商官方 API 拉取可用模型；失败时由上层回退到 `AIProvider.fallbackModels`。
enum AIModelListService {

    static func fetchModels(provider: AIProvider, apiKey: String) async throws -> [AIModel] {
        let trimmed = apiKey.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return [] }

        switch provider {
        case .local:
            return AIProvider.local.fallbackModels
        case .openai:
            return try await fetchOpenAICompatible(
                baseURL: "https://api.openai.com/v1",
                apiKey: trimmed,
                filter: isOpenAIChatModel,
                displayName: prettyGenericName
            )
        case .anthropic:
            return try await fetchAnthropic(apiKey: trimmed)
        case .deepseek:
            return try await fetchOpenAICompatible(
                baseURL: "https://api.deepseek.com/v1",
                apiKey: trimmed,
                filter: { $0.lowercased().contains("deepseek") },
                displayName: prettyGenericName
            )
        case .gemini:
            return try await fetchGemini(apiKey: trimmed)
        case .kimi:
            return try await fetchOpenAICompatible(
                baseURL: "https://api.moonshot.cn/v1",
                apiKey: trimmed,
                filter: {
                    let l = $0.lowercased()
                    return l.contains("moonshot") || l.contains("kimi")
                },
                displayName: prettyGenericName
            )
        }
    }

    // MARK: - OpenAI-compatible /models

    private struct OpenAIModelsResponse: Decodable {
        struct Item: Decodable { let id: String }
        let data: [Item]
    }

    private static func fetchOpenAICompatible(
        baseURL: String,
        apiKey: String,
        filter: (String) -> Bool,
        displayName: (String) -> String
    ) async throws -> [AIModel] {
        guard let url = URL(string: "\(baseURL)/models") else { throw AIModelListError.invalidURL }
        var request = URLRequest(url: url)
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = 30

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw AIModelListError.decodeFailed }
        switch http.statusCode {
        case 200: break
        case 401, 403: throw AIModelListError.httpStatus(http.statusCode)
        default: throw AIModelListError.httpStatus(http.statusCode)
        }

        let decoded = try JSONDecoder().decode(OpenAIModelsResponse.self, from: data)
        let models = decoded.data
            .map(\.id)
            .filter(filter)
            .map { AIModel(id: $0, name: displayName($0)) }
            .sorted { $0.id < $1.id }
        return models
    }

    private static func isOpenAIChatModel(_ id: String) -> Bool {
        let l = id.lowercased()
        if l.contains("embedding") || l.contains("whisper") || l.contains("tts") { return false }
        if l.contains("moderation") || l.contains("dall-e") || l.contains("davinci") { return false }
        if l.contains("babbage") || l.contains("ada-") || l.contains("audio") { return false }
        if l.hasPrefix("gpt-") || l.hasPrefix("chatgpt-") { return true }
        if l.hasPrefix("o1") || l.hasPrefix("o3") || l.hasPrefix("o4") { return true }
        return false
    }

    private static func prettyGenericName(_ id: String) -> String {
        id.split(separator: "-").map(\.capitalized).joined(separator: " ")
    }

    // MARK: - Anthropic

    private static func fetchAnthropic(apiKey: String) async throws -> [AIModel] {
        guard let url = URL(string: "https://api.anthropic.com/v1/models") else { throw AIModelListError.invalidURL }
        var request = URLRequest(url: url)
        request.setValue(apiKey, forHTTPHeaderField: "x-api-key")
        request.setValue("2023-06-01", forHTTPHeaderField: "anthropic-version")
        request.timeoutInterval = 30

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw AIModelListError.decodeFailed }

        if http.statusCode == 404 {
            return []
        }
        switch http.statusCode {
        case 200: break
        case 401, 403: throw AIModelListError.httpStatus(http.statusCode)
        default: throw AIModelListError.httpStatus(http.statusCode)
        }

        if let decoded = try? JSONDecoder().decode(OpenAIModelsResponse.self, from: data) {
            return decoded.data
                .map(\.id)
                .filter { $0.lowercased().contains("claude") }
                .map { AIModel(id: $0, name: prettyGenericName($0)) }
                .sorted { $0.id < $1.id }
        }
        throw AIModelListError.decodeFailed
    }

    // MARK: - Gemini

    private struct GeminiModelsResponse: Decodable {
        struct Model: Decodable {
            let name: String
            let displayName: String?
            let supportedGenerationMethods: [String]?
        }
        let models: [Model]?
    }

    private static func fetchGemini(apiKey: String) async throws -> [AIModel] {
        guard var components = URLComponents(string: "https://generativelanguage.googleapis.com/v1beta/models") else {
            throw AIModelListError.invalidURL
        }
        components.queryItems = [URLQueryItem(name: "key", value: apiKey)]
        guard let url = components.url else { throw AIModelListError.invalidURL }

        var request = URLRequest(url: url)
        request.timeoutInterval = 45

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw AIModelListError.decodeFailed }
        switch http.statusCode {
        case 200: break
        case 401, 403: throw AIModelListError.httpStatus(http.statusCode)
        default: throw AIModelListError.httpStatus(http.statusCode)
        }

        let decoded = try JSONDecoder().decode(GeminiModelsResponse.self, from: data)
        let items = decoded.models ?? []
        let models: [AIModel] = items.compactMap { m in
            guard m.name.lowercased().contains("gemini") else { return nil }
            let methods = m.supportedGenerationMethods ?? []
            guard methods.contains("generateContent") else { return nil }
            let id = m.name.hasPrefix("models/")
                ? String(m.name.dropFirst("models/".count))
                : m.name
            let label: String
            if id == "gemini-flash-latest" {
                label = "Gemini Flash Latest"
            } else if m.displayName?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty == false {
                label = m.displayName!
            } else {
                label = prettyGenericName(id)
            }
            return AIModel(id: id, name: label)
        }
        .sorted { $0.name < $1.name }
        return models
    }
}

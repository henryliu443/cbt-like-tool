import Foundation

enum AIServiceError: LocalizedError {
    case noAPIKey
    case invalidResponse
    case networkError(Error)
    case rateLimited
    case invalidKey
    case parseError(String)

    var errorDescription: String? {
        switch self {
        case .noAPIKey:
            return "请先在设置中填写 API Key"
        case .invalidResponse:
            return "AI 返回了无效的响应，请稍后重试"
        case .networkError(let error):
            return "网络错误：\(error.localizedDescription)"
        case .rateLimited:
            return "请求过于频繁，请稍后再试"
        case .invalidKey:
            return "API Key 无效，请检查设置"
        case .parseError(let detail):
            return "解析响应失败：\(detail)"
        }
    }
}

protocol AIServiceProtocol {
    var provider: AIProvider { get }

    func reframe(
        thought: String,
        model: AIModel,
        mode: ReframeMode,
        style: ResponseStyle,
        template: PromptTemplate
    ) async throws -> AnalysisResult

    func analyzeThoughtPatterns(
        thoughts: [ThoughtEntry],
        model: AIModel
    ) async throws -> ThoughtPatternReport
}

struct AIServiceFactory {
    static func service(for provider: AIProvider) -> AIServiceProtocol {
        switch provider {
        case .openai:
            return OpenAIService()
        case .anthropic:
            return AnthropicService()
        case .deepseek:
            return DeepSeekService()
        case .local:
            return LocalAnalysisService()
        }
    }
}

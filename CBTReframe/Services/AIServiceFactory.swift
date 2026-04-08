import Foundation

struct AIServiceFactory {
    static func service(for provider: AIProvider) -> AIServiceProtocol {
        switch provider {
        case .openai:
            return OpenAIService()
        case .anthropic:
            return AnthropicService()
        case .deepseek:
            return DeepSeekService()
        case .gemini:
            return GeminiService()
        case .kimi:
            return MoonshotService()
        case .local:
            return LocalAnalysisService()
        }
    }
}

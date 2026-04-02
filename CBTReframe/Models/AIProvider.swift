import Foundation

enum AIProvider: String, CaseIterable, Codable, Identifiable {
    case openai = "OpenAI"
    case anthropic = "Anthropic"
    case deepseek = "DeepSeek"
    case local = "本地（离线）"

    var id: String { rawValue }

    var displayName: String { rawValue }

    var requiresAPIKey: Bool {
        self != .local
    }

    var availableModels: [AIModel] {
        switch self {
        case .openai:
            return [
                AIModel(id: "gpt-4.1", name: "GPT-4.1"),
                AIModel(id: "gpt-4.1-mini", name: "GPT-4.1 Mini"),
                AIModel(id: "gpt-4.1-nano", name: "GPT-4.1 Nano"),
                AIModel(id: "gpt-4o", name: "GPT-4o"),
                AIModel(id: "gpt-4o-mini", name: "GPT-4o Mini"),
            ]
        case .anthropic:
            return [
                AIModel(id: "claude-sonnet-4-20250514", name: "Claude Sonnet 4"),
                AIModel(id: "claude-3-5-haiku-20241022", name: "Claude 3.5 Haiku"),
            ]
        case .deepseek:
            return [
                AIModel(id: "deepseek-chat", name: "DeepSeek Chat"),
                AIModel(id: "deepseek-reasoner", name: "DeepSeek Reasoner"),
            ]
        case .local:
            return [
                AIModel(id: "local", name: "内置分析"),
            ]
        }
    }

    var defaultModel: AIModel {
        availableModels[0]
    }

    var baseURL: String {
        switch self {
        case .openai:
            return "https://api.openai.com/v1/chat/completions"
        case .anthropic:
            return "https://api.anthropic.com/v1/messages"
        case .deepseek:
            return "https://api.deepseek.com/v1/chat/completions"
        case .local:
            return ""
        }
    }
}

struct AIModel: Codable, Identifiable, Hashable {
    let id: String
    let name: String
}

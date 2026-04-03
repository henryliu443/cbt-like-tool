import Foundation

enum AIProvider: String, CaseIterable, Codable, Identifiable {
    case openai = "OpenAI"
    case anthropic = "Anthropic"
    case deepseek = "DeepSeek"
    case gemini = "Google Gemini"
    case kimi = "Kimi (Moonshot)"
    case local = "本地（离线）"

    var id: String { rawValue }

    var displayName: String { rawValue }

    var requiresAPIKey: Bool {
        self != .local
    }

    /// 网络拉取失败或未配置 Key 时的兜底列表。
    var fallbackModels: [AIModel] {
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
        case .gemini:
            return [
                AIModel(id: "gemini-2.0-flash", name: "Gemini 2.0 Flash"),
                AIModel(id: "gemini-2.0-flash-lite", name: "Gemini 2.0 Flash-Lite"),
                AIModel(id: "gemini-1.5-flash", name: "Gemini 1.5 Flash"),
                AIModel(id: "gemini-1.5-pro", name: "Gemini 1.5 Pro"),
            ]
        case .kimi:
            return [
                AIModel(id: "moonshot-v1-8k", name: "Moonshot v1 8K"),
                AIModel(id: "moonshot-v1-32k", name: "Moonshot v1 32K"),
                AIModel(id: "kimi-k2-turbo-preview", name: "Kimi K2 Turbo"),
                AIModel(id: "kimi-k2-thinking-preview", name: "Kimi K2 Thinking"),
            ]
        case .local:
            return [
                AIModel(id: "local", name: "内置分析"),
            ]
        }
    }

    var defaultModel: AIModel {
        fallbackModels[0]
    }

    var baseURL: String {
        switch self {
        case .openai:
            return "https://api.openai.com/v1/chat/completions"
        case .anthropic:
            return "https://api.anthropic.com/v1/messages"
        case .deepseek:
            return "https://api.deepseek.com/v1/chat/completions"
        case .gemini:
            return "https://generativelanguage.googleapis.com/v1beta"
        case .kimi:
            return "https://api.moonshot.cn/v1/chat/completions"
        case .local:
            return ""
        }
    }
}

struct AIModel: Codable, Identifiable, Hashable {
    let id: String
    let name: String
}

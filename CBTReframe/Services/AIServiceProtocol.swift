import Foundation

enum AIServiceError: LocalizedError {
    case noAPIKey
    case invalidResponse
    case networkError(Error)
    case rateLimited
    case invalidKey
    case httpStatus(Int)
    case parseError(String)
    /// 苏格拉底模式：JSON 中 `questions` 缺失、不足或无效；可触发一次自动重试。
    case invalidSocraticOutput
    /// 结构化 JSON 已解析但字段不可用（如元信息占位、主字段全空）；可触发自动重试。
    case invalidStructuredOutput(String)

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
        case .httpStatus(let code):
            return "服务返回异常（\(code)）"
        case .parseError(let detail):
            return "解析响应失败：\(detail)"
        case .invalidSocraticOutput:
            return "模型未返回有效的引导问题"
        case .invalidStructuredOutput(let detail):
            return detail
        }
    }

    var userFacingMessage: String {
        switch self {
        case .noAPIKey:
            return "请先在设置中填写 API Key"
        case .invalidResponse:
            return "AI 返回了无效的响应，请稍后重试"
        case .networkError(let error):
            if let urlError = error as? URLError {
                switch urlError.code {
                case .notConnectedToInternet, .networkConnectionLost:
                    return "网络连接中断，请检查网络后重试"
                case .timedOut:
                    return "请求超时，请稍后重试"
                default:
                    return "网络波动，请稍后重试"
                }
            }
            return "网络错误，请稍后重试"
        case .rateLimited:
            return "请求过于频繁，请稍后再试"
        case .invalidKey:
            return "API Key 无效或无权限，请检查设置"
        case .httpStatus(let code):
            switch code {
            case 400:
                return "请求参数或模型配置有误，请检查后重试"
            case 401, 403:
                return "API Key 无效或无权限，请检查设置"
            case 429:
                return "请求过于频繁，请稍后再试"
            case 500, 502, 503, 504:
                return "服务暂时不可用，请稍后重试"
            default:
                return "服务返回异常（\(code)），请稍后重试"
            }
        case .parseError(let detail):
            return detail
        case .invalidSocraticOutput:
            return "模型未返回有效的引导问题，请重试或换用其他服务商。"
        case .invalidStructuredOutput(let detail):
            return detail
        }
    }

    var isRetriable: Bool {
        switch self {
        case .rateLimited:
            return true
        case .httpStatus(let code):
            return code == 429 || code == 500 || code == 502 || code == 503 || code == 504
        case .networkError(let error):
            guard let urlError = error as? URLError else { return false }
            switch urlError.code {
            case .timedOut, .notConnectedToInternet, .networkConnectionLost, .cannotConnectToHost, .cannotFindHost, .dnsLookupFailed:
                return true
            default:
                return false
            }
        case .invalidSocraticOutput, .invalidStructuredOutput:
            return true
        case .noAPIKey, .invalidResponse, .invalidKey, .parseError:
            return false
        }
    }

    static func classify(_ error: Error) -> AIServiceError {
        if let serviceError = error as? AIServiceError {
            return serviceError
        }
        if error is URLError {
            return .networkError(error)
        }
        return .networkError(error)
    }
}

protocol AIServiceProtocol {
    var provider: AIProvider { get }

    func reframe(
        thought: String,
        mood: String,
        hasAkathisia: Bool,
        model: AIModel,
        mode: ReframeMode,
        style: ResponseStyle,
        template: PromptTemplate,
        strategy: ResponseStrategy
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
        case .gemini:
            return GeminiService()
        case .kimi:
            return MoonshotService()
        case .local:
            return LocalAnalysisService()
        }
    }
}

// MARK: - Socratic JSON gate (pipeline)

/// 苏格拉底模式在 JSON 模式下必须产出至少两条有效引导问题；与 `PromptTemplate.socratic` 约定一致。
enum SocraticPipelineValidation {
    static let minimumQuestionCount = 2
    static let minimumQuestionLength = 3

    /// 与 `AnalysisResult.normalized(for: .socratic)` 中从 `alternative` 拆行补问题的逻辑对齐。
    static func sanitizedQuestions(from result: AnalysisResult) throws -> [String] {
        var qs = result.questions ?? []
        if qs.isEmpty {
            let alt = result.alternative.trimmingCharacters(in: .whitespacesAndNewlines)
            if !alt.isEmpty {
                qs = alt.split(separator: "\n").map { String($0).trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
            }
        }
        let trimmed = qs.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
        guard trimmed.count >= minimumQuestionCount else {
            throw AIServiceError.invalidSocraticOutput
        }
        for q in trimmed {
            guard q.count >= minimumQuestionLength else {
                throw AIServiceError.invalidSocraticOutput
            }
        }
        return trimmed
    }

    static func applyingSanitizedQuestions(_ result: AnalysisResult) throws -> AnalysisResult {
        let qs = try sanitizedQuestions(from: result)
        return AnalysisResult(
            id: result.id,
            distortion: result.distortion,
            alternative: result.alternative,
            action: result.action,
            questions: qs,
            actions: result.actions,
            stateAssessment: result.stateAssessment
        )
    }
}

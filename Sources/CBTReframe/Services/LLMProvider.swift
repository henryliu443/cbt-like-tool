import Foundation

struct LLMGenerationOutput {
    let text: String
    let attemptCount: Int
    let recoveredByRetry: Bool
}

/// 原始文本生成抽象；本应用主流程使用 `AIServiceProtocol.reframe`（结构化提示 + JSON 解析）。
/// 若将来接入仅支持 completion 的端点，可实现本协议并在适配层再调用 `parseReframeOutput`。
protocol LLMProvider {
    func generate(prompt: String) async throws -> LLMGenerationOutput
}

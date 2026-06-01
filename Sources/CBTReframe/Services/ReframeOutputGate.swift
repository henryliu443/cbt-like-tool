import Foundation

/// 远程模型偶发返回「可解析但不可用」的 JSON（如 distortion 填元信息、主字段全空），避免 UI 显示「暂无」占位却像成功。
enum ReframeOutputGate {
    /// 模型把「状态说明」写进 distortion 的常见占位，不应作为认知扭曲展示。
    private static let metaDistortionPhrases: Set<String> = [
        "有返回结果", "无返回结果", "返回结果", "有结果", "无结果",
        "解析成功", "输出成功", "生成成功", "成功返回", "正常返回",
        "json", "ok", "n/a", "na", "none", "success", "done",
    ]

    static func validate(_ result: AnalysisResult, template: PromptTemplate) throws {
        switch template {
        case .cbtReframe:
            try validateCBT(result)
        case .behavioral:
            try validateBehavioral(result)
        case .socratic:
            break
        }
    }

    private static func validateCBT(_ r: AnalysisResult) throws {
        let dist = r.distortion.trimmingCharacters(in: .whitespacesAndNewlines)
        let alt = r.alternative.trimmingCharacters(in: .whitespacesAndNewlines)
        let act = r.action.trimmingCharacters(in: .whitespacesAndNewlines)
        let extraActs = (r.actions ?? []).map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }

        if isMetaDistortion(dist) {
            throw AIServiceError.invalidStructuredOutput(
                "模型把「\(dist)」填进了认知扭曲字段，无法作为有效解读。请重试或更换模型。"
            )
        }

        if alt.isEmpty && act.isEmpty && extraActs.isEmpty {
            throw AIServiceError.invalidStructuredOutput(
                "模型未返回有效的替代想法与行动建议，请重试或更换模型。"
            )
        }
    }

    private static func validateBehavioral(_ r: AnalysisResult) throws {
        let dist = r.distortion.trimmingCharacters(in: .whitespacesAndNewlines)
        let act = r.action.trimmingCharacters(in: .whitespacesAndNewlines)
        let extraActs = (r.actions ?? []).map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }

        if isMetaDistortion(dist) {
            throw AIServiceError.invalidStructuredOutput(
                "模型返回了无效的解读字段，请重试或更换模型。"
            )
        }

        if act.isEmpty && extraActs.isEmpty {
            throw AIServiceError.invalidStructuredOutput(
                "模型未返回可立即执行的小行动，请重试或更换模型。"
            )
        }
    }

    private static func isMetaDistortion(_ raw: String) -> Bool {
        let t = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !t.isEmpty else { return false }
        if metaDistortionPhrases.contains(t) { return true }
        let low = t.lowercased()
        if metaDistortionPhrases.contains(low) { return true }
        return false
    }
}

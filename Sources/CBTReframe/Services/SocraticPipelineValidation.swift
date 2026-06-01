import Foundation

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

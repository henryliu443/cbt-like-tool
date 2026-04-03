import Foundation

struct AnalysisResult: Codable, Identifiable, Equatable {
    var id: UUID
    let distortion: String
    let alternative: String
    let action: String
    /// 苏格拉底模式：分步问题（无单一「标准答案」）。
    var questions: [String]?
    /// 多条行动建议（可选）；单条主行动仍用 `action`。
    var actions: [String]?
    /// 行为激活：对当前状态的简短评估。
    var stateAssessment: String?

    init(
        id: UUID = UUID(),
        distortion: String,
        alternative: String,
        action: String,
        questions: [String]? = nil,
        actions: [String]? = nil,
        stateAssessment: String? = nil
    ) {
        self.id = id
        self.distortion = distortion
        self.alternative = alternative
        self.action = action
        self.questions = questions
        self.actions = actions
        self.stateAssessment = stateAssessment
    }

    enum CodingKeys: String, CodingKey {
        case distortion, alternative, action, questions, actions, stateAssessment
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.id = UUID()
        self.distortion = try container.decodeIfPresent(String.self, forKey: .distortion) ?? "未识别"
        self.alternative = try container.decodeIfPresent(String.self, forKey: .alternative) ?? ""
        self.action = try container.decodeIfPresent(String.self, forKey: .action) ?? ""
        self.questions = try container.decodeIfPresent([String].self, forKey: .questions)
        self.actions = try container.decodeIfPresent([String].self, forKey: .actions)
        self.stateAssessment = try container.decodeIfPresent(String.self, forKey: .stateAssessment)
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(distortion, forKey: .distortion)
        try container.encode(alternative, forKey: .alternative)
        try container.encode(action, forKey: .action)
        try container.encodeIfPresent(questions, forKey: .questions)
        try container.encodeIfPresent(actions, forKey: .actions)
        try container.encodeIfPresent(stateAssessment, forKey: .stateAssessment)
    }

    /// 解析或占位后保证各疗法视图都有可读内容。
    func normalized(for template: ThinkingTemplate) -> AnalysisResult {
        let tid = id
        let dist = distortion.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "未识别" : distortion
        let alt = alternative
        let act = action.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? (template == .behavioral ? "先从小行动开始。" : "暂无替代想法")
            : action
        switch template {
        case .cbt:
            return AnalysisResult(
                id: tid,
                distortion: dist,
                alternative: alt.isEmpty ? "暂无替代想法" : alternative,
                action: action.isEmpty ? "暂无建议行动" : action,
                questions: questions,
                actions: actions,
                stateAssessment: stateAssessment
            )
        case .socratic:
            var qs = questions ?? []
            if qs.isEmpty, !alternative.isEmpty {
                qs = alternative.split(separator: "\n").map { String($0).trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
            }
            if qs.isEmpty {
                qs = ["模型未返回有效问题，请重试或换用其他服务商。"]
            }
            return AnalysisResult(
                id: tid,
                distortion: dist == "未识别" ? "苏格拉底提问" : dist,
                alternative: alternative.isEmpty ? "请结合下列问题逐步反思（无标准答案）。" : alternative,
                action: action.isEmpty ? "写下你对第一个问题的回答。" : action,
                questions: qs,
                actions: actions,
                stateAssessment: stateAssessment
            )
        case .behavioral:
            let state = stateAssessment?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true
                ? nil
                : stateAssessment
            return AnalysisResult(
                id: tid,
                distortion: dist == "未识别" ? "行为聚焦" : dist,
                alternative: alternative.isEmpty ? "先关注下一步可执行的小行动。" : alternative,
                action: action.isEmpty ? "选择一个 5 分钟内可完成的小步骤。" : action,
                questions: questions,
                actions: actions,
                stateAssessment: state ?? "（可先简短描述你现在的精力与情绪）"
            )
        }
    }

    static let empty = AnalysisResult(
        distortion: "",
        alternative: "",
        action: ""
    )
}

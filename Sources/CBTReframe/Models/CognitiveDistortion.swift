import Foundation

enum CognitiveDistortion: String, CaseIterable, Codable, Identifiable {
    case catastrophizing = "灾难化"
    case blackAndWhite = "非黑即白"
    case overgeneralization = "过度概括"
    case mindReading = "读心术"
    case emotionalReasoning = "情绪化推理"
    case shouldStatement = "应该思维"
    case labeling = "贴标签"
    case personalization = "个人化"
    case fortuneTelling = "预言未来"
    case mentalFilter = "心理过滤"

    var id: String { rawValue }

    static func detect(from text: String) -> CognitiveDistortion? {
        let lower = text.lowercased()
        let map: [(CognitiveDistortion, [String])] = [
            (.catastrophizing, ["灾难", "完蛋", "最糟", "catastroph"]),
            (.blackAndWhite, ["非黑即白", "要么", "完全", "all or nothing"]),
            (.overgeneralization, ["总是", "从来", "每次", "过度概括"]),
            (.mindReading, ["别人一定", "他们觉得", "读心"]),
            (.emotionalReasoning, ["我感觉", "所以事实", "情绪化推理"]),
            (.shouldStatement, ["应该", "必须", "不能这样"]),
            (.labeling, ["我是废物", "没用", "标签"]),
            (.personalization, ["都怪我", "我的错", "个人化"]),
            (.fortuneTelling, ["肯定会失败", "一定会", "预言"]),
            (.mentalFilter, ["只看到", "忽略好的", "过滤"]),
        ]
        for (kind, words) in map where words.contains(where: { lower.contains($0.lowercased()) }) {
            return kind
        }
        return nil
    }

    var educationTip: String {
        switch self {
        case .catastrophizing: return "把最坏结果当成必然。试着列出三种更现实的可能。"
        case .blackAndWhite: return "世界常常不是0或1，尝试找出中间地带。"
        case .overgeneralization: return "一次失败不等于一直失败，回看反例。"
        case .mindReading: return "你无法直接读心，先用事实验证。"
        case .emotionalReasoning: return "感觉很真实，但不一定等于事实。"
        case .shouldStatement: return "把“必须”改成“我希望”，降低自我苛责。"
        case .labeling: return "行为不等于身份，用具体描述替代标签。"
        case .personalization: return "很多结果受多因素影响，不必全归因于自己。"
        case .fortuneTelling: return "未来未发生，改成“可能”并准备备选方案。"
        case .mentalFilter: return "同时记录负面与正面证据，避免单一过滤。"
        }
    }
}

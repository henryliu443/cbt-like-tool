import Foundation

enum ReframeMode: String, CaseIterable, Codable, Identifiable {
    case quick = "快速"
    case balanced = "平衡"
    case deep = "深度"

    var id: String { rawValue }

    var description: String {
        switch self {
        case .quick: return "简短回复，适合快速调整"
        case .balanced: return "标准 CBT 分析"
        case .deep: return "深入的认知行为治疗分析"
        }
    }

    var icon: String {
        switch self {
        case .quick: return "hare"
        case .balanced: return "scalemass"
        case .deep: return "brain.head.profile"
        }
    }
}

enum ResponseStyle: String, CaseIterable, Codable, Identifiable {
    case brief = "简洁"
    case coach = "教练式"
    case warm = "温暖支持"

    var id: String { rawValue }

    var description: String {
        switch self {
        case .brief: return "直接给出分析结果"
        case .coach: return "像教练一样引导你思考"
        case .warm: return "温柔、有同理心的回应"
        }
    }
}

enum PromptTemplate: String, CaseIterable, Codable, Identifiable {
    case cbtReframe = "CBT 重构"
    case socratic = "苏格拉底提问"
    case behavioral = "行为激活"

    var id: String { rawValue }

    var shortLabel: String {
        switch self {
        case .cbtReframe: return "重构想法"
        case .socratic: return "引导反思"
        case .behavioral: return "行动起来"
        }
    }

    var description: String {
        switch self {
        case .cbtReframe: return "识别认知扭曲，提供替代想法"
        case .socratic: return "通过提问引导你自己发现答案"
        case .behavioral: return "聚焦下一步行动"
        }
    }

    var icon: String {
        switch self {
        case .cbtReframe: return "arrow.triangle.2.circlepath"
        case .socratic: return "questionmark.bubble"
        case .behavioral: return "figure.walk"
        }
    }

    static func suggest(for text: String) -> PromptTemplate? {
        let t = text.lowercased()

        let questionWords = ["为什么", "怎么", "是不是", "难道", "凭什么", "到底"]
        if questionWords.contains(where: { t.contains($0) }) {
            return .socratic
        }

        let stuckWords = ["不想动", "没有动力", "拖延", "懒", "躺平", "提不起", "什么都不想做", "没精力", "累"]
        if stuckWords.contains(where: { t.contains($0) }) {
            return .behavioral
        }

        let distortionWords = ["总是", "永远", "一定", "肯定", "全都", "没人", "所有人", "不可能",
                               "觉得自己", "我不行", "我太", "完蛋", "糟糕", "失败"]
        if distortionWords.contains(where: { t.contains($0) }) {
            return .cbtReframe
        }

        return nil
    }
}

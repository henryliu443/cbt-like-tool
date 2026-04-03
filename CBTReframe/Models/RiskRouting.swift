import Foundation

// MARK: - Risk

enum RiskLevel: Equatable {
    case safe
    case low
    case medium
    case high
}

// MARK: - Strategy

enum ResponseStrategy: Equatable, Codable {
    case cbtNormal
    case cbtGentle
    case crisis
}

// MARK: - Lexicon (规则 v1，可后续换 embedding / 二次模型)

struct RiskKeyword: Sendable {
    let word: String
    let weight: Int
}

enum RiskLexicon {
    /// 命中任一则直接视为高风险并本地拦截，不依赖累计分数（避免「单条高危词但总分不够」仍去打 API）。
    static let immediateBlockPhrases: [String] = [
        "自杀", "轻生", "结束生命", "了结", "割腕", "跳楼", "死了算了", "去死", "不想活",
        "kill myself", "suicide", "end my life", "want to die",
    ]

    /// 关键词命中即累加对应权重；同一词在文中多次出现仍只加一次（按词表项计）。
    static let keywords: [RiskKeyword] = [
        RiskKeyword(word: "自杀", weight: 10),
        RiskKeyword(word: "结束生命", weight: 10),
        RiskKeyword(word: "kill myself", weight: 10),
        RiskKeyword(word: "suicide", weight: 10),
        RiskKeyword(word: "want to die", weight: 9),
        RiskKeyword(word: "end my life", weight: 10),

        RiskKeyword(word: "不想活", weight: 6),
        RiskKeyword(word: "活着没意思", weight: 5),
        RiskKeyword(word: "死了算了", weight: 8),
        RiskKeyword(word: "去死", weight: 8),
        RiskKeyword(word: "轻生", weight: 10),
        RiskKeyword(word: "了结", weight: 10),
        RiskKeyword(word: "跳楼", weight: 8),
        RiskKeyword(word: "割腕", weight: 8),

        RiskKeyword(word: "很累", weight: 2),
        RiskKeyword(word: "撑不住", weight: 4),
    ]
}

func calculateRiskScore(_ text: String) -> Int {
    let lower = text.lowercased()
    var score = 0
    for keyword in RiskLexicon.keywords {
        if lower.contains(keyword.word.lowercased()) {
            score += keyword.weight
        }
    }
    return score
}

func hasImmediateCrisisKeyword(_ text: String) -> Bool {
    let lower = text.lowercased()
    for phrase in RiskLexicon.immediateBlockPhrases {
        if lower.contains(phrase.lowercased()) {
            return true
        }
    }
    return false
}

func detectRiskLevel(_ text: String) -> RiskLevel {
    if hasImmediateCrisisKeyword(text) {
        return .high
    }
    let score = calculateRiskScore(text)
    switch score {
    case 0..<3: return .safe
    case 3..<6: return .low
    case 6..<10: return .medium
    default: return .high
    }
}

func routeStrategy(level: RiskLevel) -> ResponseStrategy {
    switch level {
    case .safe, .low:
        return .cbtNormal
    case .medium:
        return .cbtGentle
    case .high:
        return .crisis
    }
}

func isJSONMode(_ strategy: ResponseStrategy) -> Bool {
    strategy != .crisis
}

/// 高风险内容：不调用任何远端 LLM（避免安全拒答、空回复与 API 费用），仅用本地固定支持文案。
func shouldUseLocalCrisisOnly(_ text: String) -> Bool {
    routeStrategy(level: detectRiskLevel(text)) == .crisis
}

// MARK: - 本地危机回复（与远端无关，单一路径供 UI / 历史记录使用）

enum CrisisLocalSupport {
    static let analysisResult = AnalysisResult(
        distortion: "支持与陪伴",
        alternative: "听起来你正在承受很大的痛苦，你愿意说出来已经很不容易。你值得被认真对待，不必独自扛下所有。若你感到难以承受，请尽量联系你信任的人陪伴在身边；紧急情况请拨打当地急救或心理危机热线。",
        action: "若情绪持续或加重，请向信任的人求助，或联系当地心理援助热线与专业医疗机构。"
    )

    static let historyProviderName = "本地"
    static let historyModelName = "危机支持"
}

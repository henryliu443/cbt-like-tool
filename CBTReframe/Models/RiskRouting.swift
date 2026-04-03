import Foundation

// MARK: - Risk

enum RiskLevel: Equatable {
    case safe
    case low
    case medium
    case high
}

// MARK: - Strategy

enum ResponseStrategy: Equatable {
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

func detectRiskLevel(_ text: String) -> RiskLevel {
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

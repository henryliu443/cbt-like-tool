import Foundation
import Combine

// MARK: - Therapy / analysis dimensions (orthogonal to API provider)

/// 选择方式：与「思维模板」一一对应，便于语义化与历史展示。
enum ActionMode: String, CaseIterable, Codable, Identifiable {
    case reframe
    case reflect
    case act

    var id: String { rawValue }

    var shortLabel: String {
        switch self {
        case .reframe: return "重构想法"
        case .reflect: return "引导反思"
        case .act: return "行动起来"
        }
    }
}

enum ThinkingTemplate: String, CaseIterable, Codable, Identifiable {
    case cbt
    case socratic
    case behavioral

    var id: String { rawValue }

    var promptTemplate: PromptTemplate {
        switch self {
        case .cbt: return .cbtReframe
        case .socratic: return .socratic
        case .behavioral: return .behavioral
        }
    }

    var actionMode: ActionMode {
        switch self {
        case .cbt: return .reframe
        case .socratic: return .reflect
        case .behavioral: return .act
        }
    }

    var displayName: String { promptTemplate.rawValue }

    var shortLabel: String { promptTemplate.shortLabel }

    var description: String { promptTemplate.description }

    var icon: String { promptTemplate.icon }

    /// 历史记录等处的简短标签。
    var historyTag: String {
        switch self {
        case .cbt: return "CBT"
        case .socratic: return "苏格拉底"
        case .behavioral: return "行为"
        }
    }

    static func from(_ legacy: PromptTemplate) -> ThinkingTemplate {
        switch legacy {
        case .cbtReframe: return .cbt
        case .socratic: return .socratic
        case .behavioral: return .behavioral
        }
    }

    static func suggest(for text: String) -> ThinkingTemplate? {
        guard let p = PromptTemplate.suggest(for: text) else { return nil }
        return ThinkingTemplate.from(p)
    }

    /// 分析深度：仅影响篇幅、步骤数、语气强度，不改变疗法类型。
    enum AnalysisDepth: String, CaseIterable, Codable, Identifiable {
        case fast
        case balanced
        case deep

        var id: String { rawValue }

        var displayName: String {
            switch self {
            case .fast: return "快速"
            case .balanced: return "平衡"
            case .deep: return "深度"
            }
        }

        var description: String {
            switch self {
            case .fast: return "简短回复，适合快速调整"
            case .balanced: return "标准分析"
            case .deep: return "更详细的推理与步骤"
            }
        }

        var icon: String {
            switch self {
            case .fast: return "hare"
            case .balanced: return "scalemass"
            case .deep: return "brain.head.profile"
            }
        }

        var reframeMode: ReframeMode {
            switch self {
            case .fast: return .quick
            case .balanced: return .balanced
            case .deep: return .deep
            }
        }

        static func from(_ legacy: ReframeMode) -> AnalysisDepth {
            switch legacy {
            case .quick: return .fast
            case .balanced: return .balanced
            case .deep: return .deep
            }
        }
    }

    /// 回应风格：仅影响语气与措辞，不改变 CBT / 苏格拉底 / 行为激活 类型。
    enum AppResponseStyle: String, CaseIterable, Codable, Identifiable {
        case concise
        case coach
        case supportive

        var id: String { rawValue }

        var displayName: String {
            switch self {
            case .concise: return "简洁"
            case .coach: return "教练式"
            case .supportive: return "温暖支持"
            }
        }

        var description: String {
            switch self {
            case .concise: return "直接给出分析结果"
            case .coach: return "像教练一样引导你思考"
            case .supportive: return "温柔、有同理心的回应"
            }
        }

        var legacyResponseStyle: ResponseStyle {
            switch self {
            case .concise: return .brief
            case .coach: return .coach
            case .supportive: return .warm
            }
        }

        static func from(_ legacy: ResponseStyle) -> ThinkingTemplate.AppResponseStyle {
            switch legacy {
            case .brief: return .concise
            case .coach: return .coach
            case .warm: return .supportive
            }
        }
    }
}

// MARK: - Global persisted settings

final class GlobalSettings: ObservableObject {
    @Published var thinkingTemplate: ThinkingTemplate {
        didSet { persist() }
    }

    @Published var analysisDepth: ThinkingTemplate.AnalysisDepth {
        didSet { persist() }
    }

    @Published var responseStyle: ThinkingTemplate.AppResponseStyle {
        didSet { persist() }
    }

    var actionMode: ActionMode { thinkingTemplate.actionMode }

    private enum Keys {
        static let thinkingTemplate = "global.thinkingTemplate"
        static let analysisDepth = "global.analysisDepth"
        static let responseStyle = "global.responseStyle"
        /// 迁移旧键
        static let legacyPromptTemplate = "promptTemplate"
        static let legacyReframeMode = "reframeMode"
        static let legacyResponseStyle = "responseStyle"
    }

    init() {
        let defaults = UserDefaults.standard

        if let raw = defaults.string(forKey: Keys.thinkingTemplate),
           let t = ThinkingTemplate(rawValue: raw) {
            self.thinkingTemplate = t
        } else if let legacy = defaults.string(forKey: Keys.legacyPromptTemplate),
                  let pt = PromptTemplate(rawValue: legacy) {
            self.thinkingTemplate = ThinkingTemplate.from(pt)
        } else {
            self.thinkingTemplate = .cbt
        }

        if let raw = defaults.string(forKey: Keys.analysisDepth),
           let d = ThinkingTemplate.AnalysisDepth(rawValue: raw) {
            self.analysisDepth = d
        } else if let legacyRaw = defaults.string(forKey: Keys.legacyReframeMode),
                  let rm = ReframeMode(rawValue: legacyRaw) {
            self.analysisDepth = ThinkingTemplate.AnalysisDepth.from(rm)
        } else {
            self.analysisDepth = .balanced
        }

        if let raw = defaults.string(forKey: Keys.responseStyle),
           let s = ThinkingTemplate.AppResponseStyle(rawValue: raw) {
            self.responseStyle = s
        } else if let legacyRaw = defaults.string(forKey: Keys.legacyResponseStyle),
                  let rs = ResponseStyle(rawValue: legacyRaw) {
            self.responseStyle = ThinkingTemplate.AppResponseStyle.from(rs)
        } else {
            self.responseStyle = .supportive
        }

        persist()
    }

    func resetToDefaults() {
        thinkingTemplate = .cbt
        analysisDepth = .balanced
        responseStyle = .supportive
        persist()
    }

    private func persist() {
        let d = UserDefaults.standard
        d.set(thinkingTemplate.rawValue, forKey: Keys.thinkingTemplate)
        d.set(analysisDepth.rawValue, forKey: Keys.analysisDepth)
        d.set(responseStyle.rawValue, forKey: Keys.responseStyle)
    }
}

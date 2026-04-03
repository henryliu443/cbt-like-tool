import Foundation
import SwiftData

private struct HistoryResultExtras: Codable {
    var questions: [String]?
    var actions: [String]?
    var stateAssessment: String?
}

@Model
final class HistoryEntry {
    var id: UUID
    var inputThought: String
    var distortion: String
    var alternative: String
    var action: String
    var isFavorite: Bool
    var createdAt: Date
    var providerName: String
    var modelName: String
    /// 用户选择的心情标签（如「焦虑」），用于解读语境；旧数据可能为空。
    var moodTag: String = ""
    /// 生成时的思维模板（`ThinkingTemplate.rawValue`），旧数据为空。
    var therapyTemplateRaw: String = ""
    /// `ThinkingTemplate.AnalysisDepth.rawValue`
    var analysisDepthRaw: String = ""
    /// `ThinkingTemplate.AppResponseStyle.rawValue`
    var responseStyleRaw: String = ""
    /// 可选：问题列表、多条行动、状态评估等（JSON）。
    var resultExtrasJSON: String = ""

    init(
        inputThought: String,
        result: AnalysisResult,
        providerName: String = "",
        modelName: String = "",
        moodTag: String = "",
        therapyTemplate: ThinkingTemplate? = nil,
        analysisDepth: ThinkingTemplate.AnalysisDepth? = nil,
        responseStyle: ThinkingTemplate.AppResponseStyle? = nil
    ) {
        self.id = UUID()
        self.inputThought = inputThought
        self.distortion = result.distortion
        self.alternative = result.alternative
        self.action = result.action
        self.isFavorite = false
        self.createdAt = Date()
        self.providerName = providerName
        self.modelName = modelName
        self.moodTag = moodTag
        self.therapyTemplateRaw = therapyTemplate?.rawValue ?? ""
        self.analysisDepthRaw = analysisDepth?.rawValue ?? ""
        self.responseStyleRaw = responseStyle?.rawValue ?? ""

        let extras = HistoryResultExtras(
            questions: result.questions,
            actions: result.actions,
            stateAssessment: result.stateAssessment
        )
        if extras.questions != nil || extras.actions != nil || extras.stateAssessment != nil,
           let data = try? JSONEncoder().encode(extras),
           let s = String(data: data, encoding: .utf8) {
            self.resultExtrasJSON = s
        } else {
            self.resultExtrasJSON = ""
        }
    }

    var analysisResult: AnalysisResult {
        var extras: HistoryResultExtras?
        if !resultExtrasJSON.isEmpty, let data = resultExtrasJSON.data(using: .utf8) {
            extras = try? JSONDecoder().decode(HistoryResultExtras.self, from: data)
        }
        return AnalysisResult(
            distortion: distortion,
            alternative: alternative,
            action: action,
            questions: extras?.questions,
            actions: extras?.actions,
            stateAssessment: extras?.stateAssessment
        )
    }

    var thinkingTemplate: ThinkingTemplate? {
        guard !therapyTemplateRaw.isEmpty else { return nil }
        return ThinkingTemplate(rawValue: therapyTemplateRaw)
    }

    var analysisDepth: ThinkingTemplate.AnalysisDepth? {
        guard !analysisDepthRaw.isEmpty else { return nil }
        return ThinkingTemplate.AnalysisDepth(rawValue: analysisDepthRaw)
    }

    var responseStyle: ThinkingTemplate.AppResponseStyle? {
        guard !responseStyleRaw.isEmpty else { return nil }
        return ThinkingTemplate.AppResponseStyle(rawValue: responseStyleRaw)
    }
}

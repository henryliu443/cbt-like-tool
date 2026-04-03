import Foundation
import SwiftData

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

    init(
        inputThought: String,
        result: AnalysisResult,
        providerName: String = "",
        modelName: String = "",
        moodTag: String = ""
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
    }

    var analysisResult: AnalysisResult {
        AnalysisResult(
            distortion: distortion,
            alternative: alternative,
            action: action
        )
    }
}

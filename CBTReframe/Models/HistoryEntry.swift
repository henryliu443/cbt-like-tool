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

    init(
        inputThought: String,
        result: AnalysisResult,
        providerName: String = "",
        modelName: String = ""
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
    }

    var analysisResult: AnalysisResult {
        AnalysisResult(
            distortion: distortion,
            alternative: alternative,
            action: action
        )
    }
}

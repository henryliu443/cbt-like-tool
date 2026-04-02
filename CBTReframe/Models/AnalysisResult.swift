import Foundation

struct AnalysisResult: Codable, Identifiable, Equatable {
    var id = UUID()
    let distortion: String
    let alternative: String
    let action: String

    static let empty = AnalysisResult(
        distortion: "",
        alternative: "",
        action: ""
    )
}

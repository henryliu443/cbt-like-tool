import Foundation

struct AnalysisResult: Codable, Identifiable, Equatable {
    var id: UUID
    let distortion: String
    let alternative: String
    let action: String

    init(distortion: String, alternative: String, action: String) {
        self.id = UUID()
        self.distortion = distortion
        self.alternative = alternative
        self.action = action
    }

    enum CodingKeys: String, CodingKey {
        case distortion, alternative, action
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.id = UUID()
        self.distortion = try container.decode(String.self, forKey: .distortion)
        self.alternative = try container.decode(String.self, forKey: .alternative)
        self.action = try container.decode(String.self, forKey: .action)
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(distortion, forKey: .distortion)
        try container.encode(alternative, forKey: .alternative)
        try container.encode(action, forKey: .action)
    }

    static let empty = AnalysisResult(
        distortion: "",
        alternative: "",
        action: ""
    )
}

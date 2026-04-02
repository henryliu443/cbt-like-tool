import Foundation
import SwiftData

@Model
final class ThoughtEntry {
    var id: UUID
    var content: String
    var situation: String
    var emotion: String
    var intensity: Int
    var distortionTag: String
    var isProcessed: Bool
    var createdAt: Date

    init(
        content: String,
        situation: String = "",
        emotion: String = "",
        intensity: Int = 5,
        distortionTag: String = "",
        isProcessed: Bool = false
    ) {
        self.id = UUID()
        self.content = content
        self.situation = situation
        self.emotion = emotion
        self.intensity = intensity
        self.distortionTag = distortionTag
        self.isProcessed = isProcessed
        self.createdAt = Date()
    }
}

struct ThoughtPatternReport: Codable {
    let topDistortions: [DistortionCount]
    let overallPattern: String
    let suggestion: String

    struct DistortionCount: Codable, Identifiable {
        var id: String { name }
        let name: String
        let count: Int
        let example: String
    }
}

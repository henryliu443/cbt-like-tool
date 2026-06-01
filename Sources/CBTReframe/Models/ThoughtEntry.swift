import Foundation
#if !SKIP
import SwiftData
#endif

#if !SKIP
@Model
#endif
final class ThoughtEntry: @unchecked Sendable {
    var id: UUID
    var content: String
    var situation: String
    var emotion: String
    var intensity: Int
    var beliefBefore: Int
    var beliefAfter: Int
    var evidenceFor: String
    var evidenceAgainst: String
    var balancedThought: String
    var distortionTag: String
    var isProcessed: Bool
    var createdAt: Date

    init(
        content: String,
        situation: String = "",
        emotion: String = "",
        intensity: Int = 5,
        beliefBefore: Int = 50,
        beliefAfter: Int = 50,
        evidenceFor: String = "",
        evidenceAgainst: String = "",
        balancedThought: String = "",
        distortionTag: String = "",
        isProcessed: Bool = false
    ) {
        self.id = UUID()
        self.content = content
        self.situation = situation
        self.emotion = emotion
        self.intensity = intensity
        self.beliefBefore = beliefBefore
        self.beliefAfter = beliefAfter
        self.evidenceFor = evidenceFor
        self.evidenceAgainst = evidenceAgainst
        self.balancedThought = balancedThought
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

#if !SKIP
@Model
#endif
final class MoodCheckIn: @unchecked Sendable {
    var id: UUID
    var createdAt: Date
    var moodScore: Int
    var moodLabel: String
    var note: String

    init(moodScore: Int, moodLabel: String, note: String = "") {
        self.id = UUID()
        self.createdAt = Date()
        self.moodScore = moodScore
        self.moodLabel = moodLabel
        self.note = note
    }
}

import Foundation
import SwiftData

@Model
final class MoodCheckIn {
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

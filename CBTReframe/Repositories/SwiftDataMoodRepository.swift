#if !SKIP
import Foundation
import SwiftData

@MainActor
final class SwiftDataMoodRepository: MoodRepository {
    private let context: ModelContext
    
    init(context: ModelContext) {
        self.context = context
    }
    
    func fetchAll() async throws -> [MoodCheckIn] {
        let descriptor = FetchDescriptor<MoodCheckIn>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        return try context.fetch(descriptor)
    }
    
    func insert(_ checkIn: MoodCheckIn) async throws {
        context.insert(checkIn)
        try context.save()
    }
    
    func deleteAll() async throws {
        try context.delete(model: MoodCheckIn.self)
        try context.save()
    }
}
#endif

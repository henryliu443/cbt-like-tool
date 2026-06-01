#if !SKIP
import Foundation
import SwiftData

@MainActor
final class SwiftDataMoodRepository: MoodRepository {
    private let container: ModelContainer
    
    init(container: ModelContainer) {
        self.container = container
    }
    
    func fetchAll() async throws -> [MoodCheckIn] {
        let descriptor = FetchDescriptor<MoodCheckIn>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        return try container.mainContext.fetch(descriptor)
    }
    
    func insert(_ checkIn: MoodCheckIn) async throws {
        container.mainContext.insert(checkIn)
        try container.mainContext.save()
    }
    
    func deleteAll() async throws {
        try container.mainContext.delete(model: MoodCheckIn.self)
        try container.mainContext.save()
    }
}
#endif

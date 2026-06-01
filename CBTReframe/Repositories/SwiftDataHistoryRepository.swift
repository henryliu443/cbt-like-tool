#if !SKIP
import Foundation
import SwiftData

@MainActor
final class SwiftDataHistoryRepository: HistoryRepository {
    private let container: ModelContainer
    
    init(container: ModelContainer) {
        self.container = container
    }
    
    func fetchAll() async throws -> [HistoryEntry] {
        let descriptor = FetchDescriptor<HistoryEntry>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        return try container.mainContext.fetch(descriptor)
    }
    
    func insert(_ entry: HistoryEntry) async throws {
        container.mainContext.insert(entry)
        try container.mainContext.save()
    }
    
    func toggleFavorite(_ entry: HistoryEntry) async throws {
        entry.isFavorite.toggle()
        try container.mainContext.save()
    }
    
    func deleteAll() async throws {
        try container.mainContext.delete(model: HistoryEntry.self)
        try container.mainContext.save()
    }
}
#endif

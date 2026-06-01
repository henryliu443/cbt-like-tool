#if !SKIP
import Foundation
import SwiftData

@MainActor
final class SwiftDataHistoryRepository: HistoryRepository {
    private let context: ModelContext
    
    init(context: ModelContext) {
        self.context = context
    }
    
    func fetchAll() async throws -> [HistoryEntry] {
        let descriptor = FetchDescriptor<HistoryEntry>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        return try context.fetch(descriptor)
    }
    
    func insert(_ entry: HistoryEntry) async throws {
        context.insert(entry)
        try context.save()
    }
    
    func toggleFavorite(_ entry: HistoryEntry) async throws {
        entry.isFavorite.toggle()
        try context.save()
    }
    
    func deleteAll() async throws {
        try context.delete(model: HistoryEntry.self)
        try context.save()
    }
}
#endif

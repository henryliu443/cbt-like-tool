#if !SKIP
import Foundation
import SwiftData

@MainActor
final class SwiftDataThoughtRepository: ThoughtRepository {
    private let context: ModelContext
    
    init(context: ModelContext) {
        self.context = context
    }
    
    func fetchAll() async throws -> [ThoughtEntry] {
        let descriptor = FetchDescriptor<ThoughtEntry>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        return try context.fetch(descriptor)
    }
    
    func insert(_ entry: ThoughtEntry) async throws {
        context.insert(entry)
        try context.save()
    }
    
    func deleteAll() async throws {
        try context.delete(model: ThoughtEntry.self)
        try context.save()
    }
}
#endif

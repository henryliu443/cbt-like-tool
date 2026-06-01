#if !SKIP
import Foundation
import SwiftData

@MainActor
final class SwiftDataThoughtRepository: ThoughtRepository {
    private let container: ModelContainer
    
    init(container: ModelContainer) {
        self.container = container
    }
    
    func fetchAll() async throws -> [ThoughtEntry] {
        let descriptor = FetchDescriptor<ThoughtEntry>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        return try container.mainContext.fetch(descriptor)
    }
    
    func insert(_ entry: ThoughtEntry) async throws {
        container.mainContext.insert(entry)
        try container.mainContext.save()
    }
    
    func deleteAll() async throws {
        try container.mainContext.delete(model: ThoughtEntry.self)
        try container.mainContext.save()
    }
}
#endif

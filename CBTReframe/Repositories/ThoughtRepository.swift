import Foundation

protocol ThoughtRepository {
    func fetchAll() async throws -> [ThoughtEntry]
    func insert(_ entry: ThoughtEntry) async throws
    func deleteAll() async throws
}

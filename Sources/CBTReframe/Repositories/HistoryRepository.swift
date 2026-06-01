import Foundation

@MainActor
protocol HistoryRepository {
    func fetchAll() async throws -> [HistoryEntry]
    func insert(_ entry: HistoryEntry) async throws
    func toggleFavorite(_ entry: HistoryEntry) async throws
    func deleteAll() async throws
}

import Foundation

@MainActor
protocol MoodRepository {
    func fetchAll() async throws -> [MoodCheckIn]
    func insert(_ checkIn: MoodCheckIn) async throws
    func deleteAll() async throws
}

#if SKIP
import Foundation

@MainActor
final class MockHistoryRepository: HistoryRepository {
    private var entries: [HistoryEntry] = []

    func fetchAll() async throws -> [HistoryEntry] {
        return entries
    }

    func insert(_ entry: HistoryEntry) async throws {
        entries.append(entry)
    }

    func toggleFavorite(_ entry: HistoryEntry) async throws {
        if let index = entries.firstIndex(where: { $0.id == entry.id }) {
            entries[index].isFavorite.toggle()
        }
    }

    func deleteAll() async throws {
        entries.removeAll()
    }
}

@MainActor
final class MockThoughtRepository: ThoughtRepository {
    private var entries: [ThoughtEntry] = []

    func fetchAll() async throws -> [ThoughtEntry] {
        return entries
    }

    func insert(_ entry: ThoughtEntry) async throws {
        entries.append(entry)
    }

    func deleteAll() async throws {
        entries.removeAll()
    }
}

@MainActor
final class MockMoodRepository: MoodRepository {
    private var checkIns: [MoodCheckIn] = []

    func fetchAll() async throws -> [MoodCheckIn] {
        return checkIns
    }

    func insert(_ checkIn: MoodCheckIn) async throws {
        checkIns.append(checkIn)
    }

    func deleteAll() async throws {
        checkIns.removeAll()
    }
}
#endif

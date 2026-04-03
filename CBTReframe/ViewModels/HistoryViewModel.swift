import Foundation
import SwiftData
import SwiftUI

@Observable
final class HistoryViewModel {
    var searchText: String = ""
    var showFavoritesOnly: Bool = false

    func filteredEntries(_ entries: [HistoryEntry]) -> [HistoryEntry] {
        var result = entries
        if showFavoritesOnly {
            result = result.filter { $0.isFavorite }
        }
        if !searchText.isEmpty {
            let query = searchText.lowercased()
            result = result.filter {
                $0.inputThought.lowercased().contains(query) ||
                $0.distortion.lowercased().contains(query) ||
                $0.alternative.lowercased().contains(query)
            }
        }
        return result.sorted { $0.createdAt > $1.createdAt }
    }

    func groupedByDate(_ entries: [HistoryEntry]) -> [(String, [HistoryEntry])] {
        let filtered = filteredEntries(entries)
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.locale = Locale(identifier: "zh_CN")

        let grouped = Dictionary(grouping: filtered) { entry in
            formatter.string(from: entry.createdAt)
        }

        return grouped
            .sorted { lhs, rhs in
                guard let lDate = lhs.value.first?.createdAt,
                      let rDate = rhs.value.first?.createdAt else { return false }
                return lDate > rDate
            }
    }

    func weeklyStats(_ entries: [HistoryEntry]) -> (count: Int, favoriteCount: Int) {
        let weekAgo = Calendar.current.date(byAdding: .day, value: -7, to: Date()) ?? Date()
        let thisWeek = entries.filter { $0.createdAt >= weekAgo }
        let favorites = thisWeek.filter { $0.isFavorite }
        return (thisWeek.count, favorites.count)
    }

    func toggleFavorite(_ entry: HistoryEntry, modelContext: ModelContext) {
        entry.isFavorite.toggle()
        try? modelContext.save()
    }
}

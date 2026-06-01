import Foundation
import SwiftData
import SwiftUI

struct DailyMoodPoint: Identifiable {
    let day: Date
    let avgScore: Double
    let moodLabel: String
    let count: Int
    var id: Date { day }
}

@MainActor
@Observable
final class MoodInsightsViewModel {
    private let moodRepository: MoodRepository
    private let historyRepository: HistoryRepository
    
    var checkins: [MoodCheckIn] = []
    var historyEntries: [HistoryEntry] = []
    
    init(moodRepository: MoodRepository, historyRepository: HistoryRepository) {
        self.moodRepository = moodRepository
        self.historyRepository = historyRepository
    }
    
    func loadData() async {
        do {
            checkins = try await moodRepository.fetchAll()
            historyEntries = try await historyRepository.fetchAll()
        } catch {
            print("Failed to load insights data: \(error)")
        }
    }
    
    func points(for range: Int) -> [DailyMoodPoint] {
        let cutoff = Calendar.current.date(byAdding: .day, value: -range, to: Date()) ?? .distantPast
        let validLabels = Set(MoodTagPicker.sharedMoods.map(\.label))
        let filtered = checkins.filter {
            $0.createdAt >= cutoff && validLabels.contains($0.moodLabel)
        }
        let grouped = Dictionary(grouping: filtered) { Calendar.current.startOfDay(for: $0.createdAt) }

        return grouped.keys.sorted().compactMap { day in
            guard let items = grouped[day], !items.isEmpty else { return nil }
            let avg = Double(items.map(\.moodScore).reduce(0, +)) / Double(items.count)
            let label = items
                .reduce(into: [String: Int]()) { dict, item in
                    dict[item.moodLabel, default: 0] += 1
                }
                .max(by: { $0.value < $1.value })?
                .key ?? "未记录"
            return DailyMoodPoint(day: day, avgScore: avg, moodLabel: label, count: items.count)
        }
    }
    
    func analysisCounts(for range: Int) -> [(Date, Int)] {
        let grouped = Dictionary(grouping: historyEntries.filter {
            $0.createdAt >= (Calendar.current.date(byAdding: .day, value: -range, to: Date()) ?? .distantPast)
        }) { Calendar.current.startOfDay(for: $0.createdAt) }
        return grouped.keys.sorted().map { ($0, grouped[$0]?.count ?? 0) }
    }
    
    func moodBreakdown(for range: Int) -> [(label: String, emoji: String, count: Int, pct: Double)] {
        let cutoff = Calendar.current.date(byAdding: .day, value: -range, to: Date()) ?? .distantPast
        let filtered = checkins.filter { $0.createdAt >= cutoff }
        let total = max(filtered.count, 1)
        let grouped = Dictionary(grouping: filtered, by: \.moodLabel)
        return grouped
            .map { (label: $0.key, emoji: MoodTagPicker.emoji(for: $0.key), count: $0.value.count, pct: Double($0.value.count) / Double(total) * 100) }
            .sorted { $0.count > $1.count }
    }
    
    func avgScore(for points: [DailyMoodPoint]) -> Double {
        guard !points.isEmpty else { return 0 }
        return points.map(\.avgScore).reduce(0, +) / Double(points.count)
    }
}

import SwiftUI
import SwiftData
import Charts

struct MoodInsightsView: View {
    @Query(sort: \MoodCheckIn.createdAt, order: .reverse) private var checkins: [MoodCheckIn]
    @Query(sort: \HistoryEntry.createdAt, order: .reverse) private var historyEntries: [HistoryEntry]
    @State private var range: Int = 30

    private struct DailyMoodPoint: Identifiable {
        let day: Date
        let avgScore: Double
        let moodLabel: String
        let count: Int
        var id: Date { day }
    }

    private var points: [DailyMoodPoint] {
        let cutoff = Calendar.current.date(byAdding: .day, value: -range, to: Date()) ?? .distantPast
        let filtered = checkins.filter { $0.createdAt >= cutoff }
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

    var body: some View {
        NavigationStack {
            List {
                Section("时间范围") {
                    Picker("范围", selection: $range) {
                        Text("7天").tag(7)
                        Text("30天").tag(30)
                        Text("90天").tag(90)
                    }
                    .pickerStyle(.segmented)
                }

                Section("情绪趋势") {
                    if points.isEmpty {
                        Text("暂无心情签到数据")
                            .foregroundStyle(.secondary)
                    } else {
                        Chart(points) { item in
                            LineMark(
                                x: .value("日期", item.day),
                                y: .value("心情", item.avgScore)
                            )
                            .foregroundStyle(Color("AccentColor"))
                            PointMark(
                                x: .value("日期", item.day),
                                y: .value("心情", item.avgScore)
                            )
                            .foregroundStyle(Color("AccentColor"))
                        }
                        .frame(height: 220)

                        ForEach(points.reversed()) { item in
                            HStack(spacing: 10) {
                                Text(MoodTagPicker.emoji(for: item.moodLabel))
                                Text(item.moodLabel)
                                    .font(.subheadline.weight(.medium))
                                Spacer()
                                Text(item.day, style: .date)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                Text(String(format: "%.1f/10", item.avgScore))
                                    .font(.caption.monospacedDigit())
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }

                Section("本期分析次数") {
                    let grouped = Dictionary(grouping: historyEntries.filter {
                        $0.createdAt >= (Calendar.current.date(byAdding: .day, value: -range, to: Date()) ?? .distantPast)
                    }) { Calendar.current.startOfDay(for: $0.createdAt) }
                    let counts = grouped.keys.sorted().map { ($0, grouped[$0]?.count ?? 0) }
                    Chart(counts, id: \.0) { item in
                        BarMark(
                            x: .value("日期", item.0),
                            y: .value("次数", item.1)
                        )
                    }
                    .frame(height: 180)
                }
            }
            .navigationTitle("情绪趋势")
        }
    }
}

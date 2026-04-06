import SwiftUI
import SwiftData
import Charts

struct MoodInsightsView: View {
    @Query(sort: \MoodCheckIn.createdAt, order: .reverse) private var checkins: [MoodCheckIn]
    @Query(sort: \HistoryEntry.createdAt, order: .reverse) private var historyEntries: [HistoryEntry]
    @State private var range: Int = 30

    private var points: [MoodCheckIn] {
        let cutoff = Calendar.current.date(byAdding: .day, value: -range, to: Date()) ?? .distantPast
        return checkins.filter { $0.createdAt >= cutoff }.sorted { $0.createdAt < $1.createdAt }
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
                                x: .value("日期", item.createdAt),
                                y: .value("心情", item.moodScore)
                            )
                            .foregroundStyle(Color("AccentColor"))
                            PointMark(
                                x: .value("日期", item.createdAt),
                                y: .value("心情", item.moodScore)
                            )
                            .foregroundStyle(Color("AccentColor"))
                        }
                        .frame(height: 220)
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

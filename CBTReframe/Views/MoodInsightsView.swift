import SwiftUI
import SwiftData
import Charts

@Observable
class MoodInsightsViewModel {
    var points: [DailyMoodPoint] = []
    var analysisCounts: [(Date, Int)] = []

    func calculate(checkins: [MoodCheckIn], historyEntries: [HistoryEntry], range: Int) {
        let cutoff = Calendar.current.date(byAdding: .day, value: -range, to: Date()) ?? .distantPast
        let validLabels = Set(MoodTagPicker.sharedMoods.map(\.label))
        
        let filteredCheckins = checkins.filter {
            $0.createdAt >= cutoff && validLabels.contains($0.moodLabel)
        }
        let groupedCheckins = Dictionary(grouping: filteredCheckins) { Calendar.current.startOfDay(for: $0.createdAt) }
        
        self.points = groupedCheckins.keys.sorted().compactMap { day in
            guard let items = groupedCheckins[day], !items.isEmpty else { return nil }
            let avg = Double(items.map(\.moodScore).reduce(0, +)) / Double(items.count)
            let label = items
                .reduce(into: [String: Int]()) { dict, item in
                    dict[item.moodLabel, default: 0] += 1
                }
                .max(by: { $0.value < $1.value })?
                .key ?? "未记录"
            return DailyMoodPoint(day: day, avgScore: avg, moodLabel: label, count: items.count)
        }
        
        let filteredEntries = historyEntries.filter { $0.createdAt >= cutoff }
        let groupedEntries = Dictionary(grouping: filteredEntries) { Calendar.current.startOfDay(for: $0.createdAt) }
        self.analysisCounts = groupedEntries.keys.sorted().map { ($0, groupedEntries[$0]?.count ?? 0) }
    }
}

struct DailyMoodPoint: Identifiable {
    let day: Date
    let avgScore: Double
    let moodLabel: String
    let count: Int
    var id: Date { day }
}

struct MoodInsightsView: View {
    @Query(sort: \MoodCheckIn.createdAt, order: .reverse) private var checkins: [MoodCheckIn]
    @Query(sort: \HistoryEntry.createdAt, order: .reverse) private var historyEntries: [HistoryEntry]
    @State private var range: Int = 30
    @State private var viewModel = MoodInsightsViewModel()

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
                    if viewModel.points.isEmpty {
                        Text("暂无心情签到数据")
                            .foregroundStyle(.secondary)
                    } else {
                        Chart(viewModel.points) { item in
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

                        ForEach(viewModel.points.reversed()) { item in
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
                    Chart(viewModel.analysisCounts, id: \.0) { item in
                        BarMark(
                            x: .value("日期", item.0),
                            y: .value("次数", item.1)
                        )
                    }
                    .frame(height: 180)
                }
            }
            .navigationTitle("情绪趋势")
            .onChange(of: range) { _, newValue in
                viewModel.calculate(checkins: checkins, historyEntries: historyEntries, range: newValue)
            }
            .onChange(of: checkins) { _, _ in
                viewModel.calculate(checkins: checkins, historyEntries: historyEntries, range: range)
            }
            .onChange(of: historyEntries) { _, _ in
                viewModel.calculate(checkins: checkins, historyEntries: historyEntries, range: range)
            }
            .onAppear {
                viewModel.calculate(checkins: checkins, historyEntries: historyEntries, range: range)
            }
        }
    }
}

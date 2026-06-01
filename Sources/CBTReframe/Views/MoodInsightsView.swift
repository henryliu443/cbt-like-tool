import SwiftUI
#if !SKIP
import Charts
#endif

struct MoodInsightsView: View {
    @Bindable var viewModel: MoodInsightsViewModel
    @State private var range: Int = 30
    
    var body: some View {
        let currentPoints = viewModel.points(for: range)
        let currentAvgScore = viewModel.avgScore(for: currentPoints)
        let currentBreakdown = viewModel.moodBreakdown(for: range)
        let currentCounts = viewModel.analysisCounts(for: range)
        
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

                if !currentPoints.isEmpty {
                    Section("概览") {
                        HStack(spacing: 0) {
                            VStack(spacing: 4) {
                                Text(String(format: "%.1f", currentAvgScore))
#if !SKIP
                                    .font(.title.bold().monospacedDigit())
#else
                                    .font(.title.bold())
#endif
                                    .foregroundStyle(Color("AccentColor"))
                                Text("平均心情")
                                    .font(.caption2)
                                    .foregroundStyle(Color("TextSecondary"))
                            }
                            .frame(maxWidth: .infinity)

                            Rectangle().fill(Color.gray.opacity(0.2)).frame(width: 1, height: 36)

                            VStack(spacing: 4) {
                                Text("\(viewModel.checkins.filter { $0.createdAt >= (Calendar.current.date(byAdding: .day, value: -range, to: Date()) ?? .distantPast) }.count)")
#if !SKIP
                                    .font(.title.bold().monospacedDigit())
#else
                                    .font(.title.bold())
#endif
                                    .foregroundStyle(Color("TextPrimary"))
                                Text("签到次数")
                                    .font(.caption2)
                                    .foregroundStyle(Color("TextSecondary"))
                            }
                            .frame(maxWidth: .infinity)

                            Rectangle().fill(Color.gray.opacity(0.2)).frame(width: 1, height: 36)

                            VStack(spacing: 4) {
                                Text("\(currentPoints.count)")
#if !SKIP
                                    .font(.title.bold().monospacedDigit())
#else
                                    .font(.title.bold())
#endif
                                    .foregroundStyle(Color("TextPrimary"))
                                Text("活跃天数")
                                    .font(.caption2)
                                    .foregroundStyle(Color("TextSecondary"))
                            }
                            .frame(maxWidth: .infinity)
                        }
                        .padding(.vertical, 6)
                    }
                }

                Section("情绪趋势") {
                    if currentPoints.isEmpty {
                        VStack(spacing: 8) {
                            Image(systemName: "chart.line.uptrend.xyaxis")
                                .font(.largeTitle)
                                .foregroundStyle(Color("TextSecondary").opacity(0.3))
                            Text("还没有心情数据")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                            Text("完成一次分析后，心情会自动记录")
                                .font(.caption)
                                .foregroundStyle(Color("TextSecondary"))
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 20)
                    } else {
                        MoodTrendChartView(points: currentPoints)

                        ForEach(currentPoints.suffix(7).reversed()) { item in
                            HStack(spacing: 10) {
                                Text(MoodTagPicker.emoji(for: item.moodLabel))
                                Text(item.moodLabel)
                                    .font(.subheadline.weight(.medium))
                                Spacer()
                                Text(item.day, style: .date)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                Text(String(format: "%.1f/10", item.avgScore))
#if !SKIP
                                    .font(.caption.monospacedDigit())
#else
                                    .font(.caption)
#endif
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }

                if !currentBreakdown.isEmpty {
                    Section("情绪分布") {
                        ForEach(currentBreakdown, id: \.label) { item in
                            HStack(spacing: 10) {
                                Text(item.emoji)
                                Text(item.label)
                                    .font(.subheadline.weight(.medium))
                                Spacer()
                                Text("\(item.count) 次")
#if !SKIP
                                    .font(.caption.monospacedDigit())
#else
                                    .font(.caption)
#endif
                                    .foregroundStyle(Color("TextSecondary"))
                                Text(String(format: "%.0f%%", item.pct))
#if !SKIP
                                    .font(.caption.bold().monospacedDigit())
#else
                                    .font(.caption.bold())
#endif
                                    .foregroundStyle(Color("AccentColor"))
                                    .frame(width: 36, alignment: .trailing)
                            }
                        }
                    }
                }

                Section("本期分析次数") {
                    if currentCounts.isEmpty {
                        Text("暂无分析数据")
                            .foregroundStyle(.secondary)
                    } else {
                        AnalysisCountChartView(counts: currentCounts)
                    }
                }
            }
            .navigationTitle("情绪趋势")
            .onAppear {
                Task {
                    await viewModel.loadData()
                }
            }
        }
    }
}

struct MoodTrendChartView: View {
    let points: [DailyMoodPoint]
    
    var body: some View {
        #if !SKIP
        Chart(points) { item in
            LineMark(
                x: .value("日期", item.day),
                y: .value("心情", item.avgScore)
            )
            .foregroundStyle(Color("AccentColor"))
            .interpolationMethod(.catmullRom)
            AreaMark(
                x: .value("日期", item.day),
                y: .value("心情", item.avgScore)
            )
            .foregroundStyle(
                LinearGradient(
                    colors: [Color("AccentColor").opacity(0.2), Color("AccentColor").opacity(0.02)],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .interpolationMethod(.catmullRom)
            PointMark(
                x: .value("日期", item.day),
                y: .value("心情", item.avgScore)
            )
            .foregroundStyle(Color("AccentColor"))
            .annotation(position: .top, spacing: 4) {
                Text(MoodTagPicker.emoji(for: item.moodLabel))
                    .font(.caption2)
            }
        }
        .chartYScale(domain: 0...10)
        .frame(height: 220)
        #else
        AndroidMoodTrendChart(points: points)
        #endif
    }
}

struct AnalysisCountChartView: View {
    let counts: [(Date, Int)]
    
    var body: some View {
        #if !SKIP
        Chart(counts, id: \.0) { item in
            BarMark(
                x: .value("日期", item.0),
                y: .value("次数", item.1)
            )
            .foregroundStyle(Color("AccentColor").opacity(0.7))
            .cornerRadius(4)
        }
        .frame(height: 180)
        #else
        AndroidAnalysisCountChart(counts: counts)
        #endif
    }
}

#if SKIP
struct AndroidMoodTrendChart: View {
    let points: [DailyMoodPoint]
    
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(alignment: .bottom, spacing: 12) {
                ForEach(points) { item in
                    VStack(spacing: 4) {
                        Text(MoodTagPicker.emoji(for: item.moodLabel))
                            .font(.caption2)
                        
                        VStack {
                            Spacer()
                            RoundedRectangle(cornerRadius: 3)
                                .fill(Color("AccentColor"))
                                .frame(width: 8, height: max(CGFloat(item.avgScore) * 15, 2))
                        }
                        .frame(height: 160)
                        
                        Text(String(format: "%.1f", item.avgScore))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
        .frame(height: 220)
    }
}

struct AndroidAnalysisCountChart: View {
    let counts: [(Date, Int)]
    
    var body: some View {
        let maxCount = counts.map { $0.1 }.max() ?? 1
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(alignment: .bottom, spacing: 16) {
                ForEach(0..<counts.count, id: \.self) { i in
                    let item = counts[i]
                    VStack(spacing: 4) {
                        Text("\(item.1)")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        
                        VStack {
                            Spacer()
                            RoundedRectangle(cornerRadius: 4)
                                .fill(Color("AccentColor").opacity(0.7))
                                .frame(width: 16, height: max(CGFloat(item.1) / CGFloat(maxCount) * 120, 2))
                        }
                        .frame(height: 120)
                        
                        Text(dayAbbreviation(for: item.0))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
        .frame(height: 180)
    }
    
    private func dayAbbreviation(for date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MM-dd"
        return formatter.string(from: date)
    }
}
#endif

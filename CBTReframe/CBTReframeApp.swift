import SwiftUI
import SwiftData
import Charts

@main
struct CBTReframeApp: App {
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = false
    @State private var settingsViewModel = SettingsViewModel()
    @StateObject private var globalSettings = GlobalSettings()

    let container: ModelContainer

    init() {
        let schema = Schema([HistoryEntry.self, ThoughtEntry.self, MoodCheckIn.self])
        let config = ModelConfiguration(isStoredInMemoryOnly: false)
        do {
            container = try ModelContainer(for: schema, configurations: [config])
        } catch {
            NSLog("SwiftData initialization failed: \(error.localizedDescription)")
            container = try! ModelContainer(for: schema, configurations: [ModelConfiguration(isStoredInMemoryOnly: true)])
        }
    }

    var body: some Scene {
        WindowGroup {
            if hasCompletedOnboarding {
                MainTabView(settingsViewModel: settingsViewModel, globalSettings: globalSettings)
                    .environmentObject(globalSettings)
            } else {
                OnboardingView(
                    settingsViewModel: settingsViewModel,
                    hasCompletedOnboarding: $hasCompletedOnboarding
                )
                .environmentObject(globalSettings)
            }
        }
        .modelContainer(container)
    }
}

private struct ExercisesView: View {
    private let items: [ExerciseGuide] = [
        ExerciseGuide(
            title: "4-7-8 呼吸",
            subtitle: "用呼吸先把身体慢下来",
            steps: [
                "坐稳，双脚踩地，肩膀放松。",
                "鼻吸 4 秒：1、2、3、4。",
                "屏息 7 秒：1 到 7，尽量轻松。",
                "嘴慢慢呼气 8 秒，像吹蜡烛。",
                "重复 4 轮，结束后观察心跳和紧张感。"
            ]
        ),
        ExerciseGuide(
            title: "渐进式肌肉放松",
            subtitle: "一组一组松开紧绷的肌肉",
            steps: [
                "从脚开始，绷紧 5 秒，再放松 10 秒。",
                "小腿、大腿、腹部、肩膀依次重复。",
                "每次放松时，注意“变软”的感觉。",
                "最后做 2 次深呼吸，结束。"
            ]
        ),
        ExerciseGuide(
            title: "5-4-3-2-1 感官着陆",
            subtitle: "把注意力拉回当下",
            steps: [
                "看见 5 样东西，并说出名称。",
                "摸到 4 个触感（衣服、桌子、手机等）。",
                "听见 3 种声音。",
                "闻到 2 种气味（或回忆 2 种熟悉味道）。",
                "感受 1 个当下的身体感觉（呼吸、脚踩地）。"
            ]
        ),
        ExerciseGuide(
            title: "身体扫描",
            subtitle: "从头到脚做一次内在巡检",
            steps: [
                "闭眼或垂眼，先注意呼吸 3 次。",
                "从头皮到肩颈，找出最紧的地方。",
                "每找到一处，就慢呼气并放松那一处。",
                "继续到胸、腹、腿、脚，直到全身。",
                "最后问自己：此刻紧张度从 0-10 是几分？"
            ]
        )
    ]

    var body: some View {
        NavigationStack {
            List(items) { item in
                NavigationLink {
                    ExerciseGuideView(guide: item)
                } label: {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(item.title)
                            .font(.headline)
                        Text(item.subtitle)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 4)
                }
            }
            .navigationTitle("练习")
        }
    }
}

private struct ExerciseGuide: Identifiable {
    let id = UUID()
    let title: String
    let subtitle: String
    let steps: [String]
}

private struct ExerciseGuideView: View {
    let guide: ExerciseGuide
    @State private var stepIndex: Int = 0
    @State private var isDone = false

    private var currentStep: String {
        guide.steps[min(stepIndex, guide.steps.count - 1)]
    }

    var body: some View {
        VStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 8) {
                Text(guide.subtitle)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Text("第 \(stepIndex + 1) / \(guide.steps.count) 步")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color("AccentColor"))
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Text(currentStep)
                .font(.title3.weight(.medium))
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
                .background(Color("CardBackground"))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

            HStack(spacing: 10) {
                Button("上一步") {
                    stepIndex = max(0, stepIndex - 1)
                    isDone = false
                }
                .buttonStyle(.bordered)
                .disabled(stepIndex == 0)

                Button(stepIndex == guide.steps.count - 1 ? "完成" : "下一步") {
                    if stepIndex == guide.steps.count - 1 {
                        isDone = true
                    } else {
                        stepIndex += 1
                    }
                }
                .buttonStyle(.borderedProminent)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if isDone {
                Text("已完成一轮练习，做得很好。")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color("AccentColor"))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            Spacer()
        }
        .padding()
        .navigationTitle(guide.title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct MoodInsightsView: View {
    @Query(sort: \MoodCheckIn.createdAt, order: .reverse) private var checkins: [MoodCheckIn]
    @Query(sort: \HistoryEntry.createdAt, order: .reverse) private var historyEntries: [HistoryEntry]
    @State private var range: Int = 30

    private struct DailyMoodPoint: Identifiable {
        let day: Date
        let avgScore: Double
        let moodLabel: String
        var id: Date { day }
    }

    private var points: [DailyMoodPoint] {
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
            return DailyMoodPoint(day: day, avgScore: avg, moodLabel: label)
        }
    }

    private var moodBreakdown: [(label: String, emoji: String, count: Int, pct: Double)] {
        let cutoff = Calendar.current.date(byAdding: .day, value: -range, to: Date()) ?? .distantPast
        let filtered = checkins.filter { $0.createdAt >= cutoff }
        let total = max(filtered.count, 1)
        let grouped = Dictionary(grouping: filtered, by: \.moodLabel)
        return grouped
            .map { (label: $0.key, emoji: MoodTagPicker.emoji(for: $0.key), count: $0.value.count, pct: Double($0.value.count) / Double(total) * 100) }
            .sorted { $0.count > $1.count }
    }

    private var avgScore: Double {
        guard !points.isEmpty else { return 0 }
        return points.map(\.avgScore).reduce(0, +) / Double(points.count)
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

                if !points.isEmpty {
                    Section("概览") {
                        HStack(spacing: 0) {
                            VStack(spacing: 4) {
                                Text(String(format: "%.1f", avgScore))
                                    .font(.title.bold().monospacedDigit())
                                    .foregroundStyle(Color("AccentColor"))
                                Text("平均心情")
                                    .font(.caption2)
                                    .foregroundStyle(Color("TextSecondary"))
                            }
                            .frame(maxWidth: .infinity)

                            Rectangle().fill(Color(.separator).opacity(0.2)).frame(width: 1, height: 36)

                            VStack(spacing: 4) {
                                Text("\(checkins.filter { $0.createdAt >= (Calendar.current.date(byAdding: .day, value: -range, to: Date()) ?? .distantPast) }.count)")
                                    .font(.title.bold().monospacedDigit())
                                    .foregroundStyle(Color("TextPrimary"))
                                Text("签到次数")
                                    .font(.caption2)
                                    .foregroundStyle(Color("TextSecondary"))
                            }
                            .frame(maxWidth: .infinity)

                            Rectangle().fill(Color(.separator).opacity(0.2)).frame(width: 1, height: 36)

                            VStack(spacing: 4) {
                                Text("\(points.count)")
                                    .font(.title.bold().monospacedDigit())
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
                    if points.isEmpty {
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

                        ForEach(points.suffix(7).reversed()) { item in
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

                if !moodBreakdown.isEmpty {
                    Section("情绪分布") {
                        ForEach(moodBreakdown, id: \.label) { item in
                            HStack(spacing: 10) {
                                Text(item.emoji)
                                Text(item.label)
                                    .font(.subheadline.weight(.medium))
                                Spacer()
                                Text("\(item.count) 次")
                                    .font(.caption.monospacedDigit())
                                    .foregroundStyle(Color("TextSecondary"))
                                Text(String(format: "%.0f%%", item.pct))
                                    .font(.caption.bold().monospacedDigit())
                                    .foregroundStyle(Color("AccentColor"))
                                    .frame(width: 36, alignment: .trailing)
                            }
                        }
                    }
                }

                Section("本期分析次数") {
                    let grouped = Dictionary(grouping: historyEntries.filter {
                        $0.createdAt >= (Calendar.current.date(byAdding: .day, value: -range, to: Date()) ?? .distantPast)
                    }) { Calendar.current.startOfDay(for: $0.createdAt) }
                    let counts = grouped.keys.sorted().map { ($0, grouped[$0]?.count ?? 0) }
                    if counts.isEmpty {
                        Text("暂无分析数据")
                            .foregroundStyle(.secondary)
                    } else {
                        Chart(counts, id: \.0) { item in
                            BarMark(
                                x: .value("日期", item.0),
                                y: .value("次数", item.1)
                            )
                            .foregroundStyle(Color("AccentColor").opacity(0.7))
                            .cornerRadius(4)
                        }
                        .frame(height: 180)
                    }
                }
            }
            .navigationTitle("情绪趋势")
        }
    }
}

struct MainTabView: View {
    @Bindable var settingsViewModel: SettingsViewModel
    @ObservedObject var globalSettings: GlobalSettings
    @StateObject private var session: AppSession
    @State private var historyViewModel = HistoryViewModel()
    @State private var selectedTab = 0

    init(settingsViewModel: SettingsViewModel, globalSettings: GlobalSettings) {
        self.settingsViewModel = settingsViewModel
        self.globalSettings = globalSettings
        _session = StateObject(wrappedValue: AppSession(settings: settingsViewModel, globalSettings: globalSettings))
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            HomeView(viewModel: session.reframeViewModel)
                .environmentObject(globalSettings)
                .tabItem {
                    Label("首页", systemImage: "brain.head.profile")
                }
                .tag(0)

            ThoughtJournalView(viewModel: session.journalViewModel)
                .tabItem {
                    Label("记录", systemImage: "square.and.pencil")
                }
                .tag(1)

            HistoryView(viewModel: historyViewModel, settingsViewModel: settingsViewModel)
                .tabItem {
                    Label("历史", systemImage: "clock.arrow.circlepath")
                }
                .tag(2)

            MoodInsightsView()
                .tabItem {
                    Label("趋势", systemImage: "chart.line.uptrend.xyaxis")
                }
                .tag(3)

            ExercisesView()
                .tabItem {
                    Label("练习", systemImage: "figure.mind.and.body")
                }
                .tag(4)

            SettingsView(viewModel: settingsViewModel)
                .environmentObject(globalSettings)
                .tabItem {
                    Label("设置", systemImage: "gearshape")
                }
                .tag(5)
        }
        .tint(Color("AccentColor"))
    }
}

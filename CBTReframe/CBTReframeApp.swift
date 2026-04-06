import SwiftUI
import SwiftData

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

private struct MoodInsightsView: View {
    @Query(sort: \MoodCheckIn.createdAt, order: .reverse) private var checkins: [MoodCheckIn]
    var body: some View {
        NavigationStack {
            List {
                Section("最近心情签到") {
                    if checkins.isEmpty {
                        Text("暂无数据").foregroundStyle(.secondary)
                    } else {
                        ForEach(checkins.prefix(20), id: \.id) { item in
                            HStack {
                                Text(item.moodLabel)
                                Spacer()
                                Text("\(item.moodScore)/10")
                                Text(item.createdAt, style: .date).foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            }
            .navigationTitle("情绪趋势")
        }
    }
}

private struct ExercisesView: View {
    private let items: [(String, String)] = [
        ("4-7-8 呼吸", "吸气4秒，屏息7秒，呼气8秒"),
        ("渐进式肌肉放松", "从脚到头逐段放松"),
        ("5-4-3-2-1 感官着陆", "回到当下的地面技巧"),
        ("身体扫描", "从头到脚觉察紧绷和放松"),
    ]

    var body: some View {
        NavigationStack {
            List(items, id: \.0) { item in
                VStack(alignment: .leading, spacing: 6) {
                    Text(item.0).font(.headline)
                    Text(item.1).font(.subheadline).foregroundStyle(.secondary)
                }
                .padding(.vertical, 4)
            }
            .navigationTitle("练习")
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

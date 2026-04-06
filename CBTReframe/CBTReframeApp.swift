import SwiftUI
import SwiftData
import OSLog

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
            AppLogger.data.error("SwiftData initialization failed: \(error.localizedDescription, privacy: .public)")
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

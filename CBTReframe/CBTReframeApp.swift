import SwiftUI
import SwiftData

@main
struct CBTReframeApp: App {
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = false
    @State private var settingsViewModel = SettingsViewModel()

    let container: ModelContainer

    init() {
        let schema = Schema([HistoryEntry.self, ThoughtEntry.self])
        let config = ModelConfiguration(isStoredInMemoryOnly: false)
        do {
            container = try ModelContainer(for: schema, configurations: [config])
        } catch {
            print("[CBTReframe] SwiftData schema error, resetting database: \(error)")
            let urls = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)
            if let appSupport = urls.first {
                let storeURL = appSupport.appendingPathComponent("default.store")
                for suffix in ["", "-shm", "-wal"] {
                    let fileURL = storeURL.deletingLastPathComponent().appendingPathComponent(storeURL.lastPathComponent + suffix)
                    try? FileManager.default.removeItem(at: fileURL)
                }
            }
            container = try! ModelContainer(for: schema, configurations: [config])
        }
    }

    var body: some Scene {
        WindowGroup {
            if hasCompletedOnboarding {
                MainTabView(settingsViewModel: settingsViewModel)
            } else {
                OnboardingView(
                    settingsViewModel: settingsViewModel,
                    hasCompletedOnboarding: $hasCompletedOnboarding
                )
            }
        }
        .modelContainer(container)
    }
}

struct MainTabView: View {
    @Bindable var settingsViewModel: SettingsViewModel
    @State private var reframeViewModel: ReframeViewModel
    @State private var historyViewModel = HistoryViewModel()
    @State private var journalViewModel: ThoughtJournalViewModel
    @State private var selectedTab = 0

    init(settingsViewModel: SettingsViewModel) {
        self.settingsViewModel = settingsViewModel
        self._reframeViewModel = State(initialValue: ReframeViewModel(settings: settingsViewModel))
        self._journalViewModel = State(initialValue: ThoughtJournalViewModel(settings: settingsViewModel))
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            HomeView(viewModel: reframeViewModel)
                .tabItem {
                    Label("首页", systemImage: "brain.head.profile")
                }
                .tag(0)

            ThoughtJournalView(viewModel: journalViewModel)
                .tabItem {
                    Label("记录", systemImage: "square.and.pencil")
                }
                .tag(1)

            HistoryView(viewModel: historyViewModel, settingsViewModel: settingsViewModel)
                .tabItem {
                    Label("历史", systemImage: "clock.arrow.circlepath")
                }
                .tag(2)

            SettingsView(viewModel: settingsViewModel)
                .tabItem {
                    Label("设置", systemImage: "gearshape")
                }
                .tag(3)
        }
        .tint(Color("AccentColor"))
    }
}

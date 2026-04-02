import SwiftUI
import SwiftData

@main
struct CBTReframeApp: App {
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = false
    @State private var settingsViewModel = SettingsViewModel()

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
        .modelContainer(for: HistoryEntry.self)
    }
}

struct MainTabView: View {
    @Bindable var settingsViewModel: SettingsViewModel
    @State private var reframeViewModel: ReframeViewModel
    @State private var historyViewModel = HistoryViewModel()
    @State private var selectedTab = 0

    init(settingsViewModel: SettingsViewModel) {
        self.settingsViewModel = settingsViewModel
        self._reframeViewModel = State(initialValue: ReframeViewModel(settings: settingsViewModel))
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            HomeView(viewModel: reframeViewModel)
                .tabItem {
                    Label("首页", systemImage: "brain.head.profile")
                }
                .tag(0)

            HistoryView(viewModel: historyViewModel)
                .tabItem {
                    Label("历史", systemImage: "clock.arrow.circlepath")
                }
                .tag(1)

            SettingsView(viewModel: settingsViewModel)
                .tabItem {
                    Label("设置", systemImage: "gearshape")
                }
                .tag(2)
        }
        .tint(Color("AccentColor"))
    }
}

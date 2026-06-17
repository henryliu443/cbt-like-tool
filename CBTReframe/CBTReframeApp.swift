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

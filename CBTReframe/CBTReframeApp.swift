import SwiftUI
import SwiftData
import Charts

@main
struct CBTReframeApp: App {
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = false
    @State private var settingsViewModel: SettingsViewModel
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
        
        let histRepo = SwiftDataHistoryRepository(context: container.mainContext)
        let thoughtRepo = SwiftDataThoughtRepository(context: container.mainContext)
        let moodRepo = SwiftDataMoodRepository(context: container.mainContext)
        
        _settingsViewModel = State(wrappedValue: SettingsViewModel(
            historyRepository: histRepo,
            thoughtRepository: thoughtRepo,
            moodRepository: moodRepo
        ))
    }

    var body: some Scene {
        WindowGroup {
            if hasCompletedOnboarding {
                MainTabView(settingsViewModel: settingsViewModel, globalSettings: globalSettings, container: container)
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

// Removed duplicate MoodInsightsView

struct MainTabView: View {
    @Bindable var settingsViewModel: SettingsViewModel
    @ObservedObject var globalSettings: GlobalSettings
    @StateObject private var session: AppSession
    @State private var historyViewModel: HistoryViewModel
    @State private var moodInsightsViewModel: MoodInsightsViewModel
    @State private var selectedTab = 0

    init(settingsViewModel: SettingsViewModel, globalSettings: GlobalSettings, container: ModelContainer) {
        self.settingsViewModel = settingsViewModel
        self.globalSettings = globalSettings
        _session = StateObject(wrappedValue: AppSession(settings: settingsViewModel, globalSettings: globalSettings))
        
        let histRepo = SwiftDataHistoryRepository(context: container.mainContext)
        let moodRepo = SwiftDataMoodRepository(context: container.mainContext)
        _historyViewModel = State(wrappedValue: HistoryViewModel(historyRepository: histRepo))
        _moodInsightsViewModel = State(wrappedValue: MoodInsightsViewModel(moodRepository: moodRepo, historyRepository: histRepo))
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

            MoodInsightsView(viewModel: moodInsightsViewModel)
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

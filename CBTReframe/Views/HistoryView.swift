import SwiftUI
import SwiftData
import LocalAuthentication

struct HistoryView: View {
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \HistoryEntry.createdAt, order: .reverse) private var allEntries: [HistoryEntry]
    @Bindable var viewModel: HistoryViewModel
    @Bindable var settingsViewModel: SettingsViewModel
    @State private var isUnlocked = false
    @State private var authErrorMessage: String?
    @State private var hasAttemptedAuth = false

    private var needsAuth: Bool {
        settingsViewModel.useFaceID && !isUnlocked
    }

    var body: some View {
        NavigationStack {
            Group {
                if needsAuth {
                    lockedState
                } else if allEntries.isEmpty {
                    emptyState
                } else {
                    listContent
                }
            }
            .navigationTitle("历史记录")
            .searchable(text: $viewModel.searchText, prompt: "搜索想法...")
            .toolbar {
                if !needsAuth {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            withAnimation { viewModel.showFavoritesOnly.toggle() }
                        } label: {
                            Image(systemName: viewModel.showFavoritesOnly ? "star.fill" : "star")
                                .foregroundStyle(viewModel.showFavoritesOnly ? .yellow : Color("TextSecondary"))
                        }
                    }
                }
            }
        }
        .onAppear {
            if settingsViewModel.useFaceID && !hasAttemptedAuth {
                hasAttemptedAuth = true
                Task { await authenticateIfNeeded() }
            } else if !settingsViewModel.useFaceID {
                isUnlocked = true
            }
        }
        .onChange(of: settingsViewModel.useFaceID) { _, newValue in
            if !newValue {
                isUnlocked = true
                authErrorMessage = nil
            } else {
                isUnlocked = false
                hasAttemptedAuth = false
            }
        }
        .onChange(of: scenePhase) { _, newPhase in
            if settingsViewModel.useFaceID && newPhase == .background {
                isUnlocked = false
                hasAttemptedAuth = false
            }
        }
    }

    private var lockedState: some View {
        VStack(spacing: 18) {
            Image(systemName: "lock.shield")
                .font(.system(size: 44))
                .foregroundStyle(Color("AccentColor"))

            Text("历史记录已锁定")
                .font(.headline)
                .foregroundStyle(Color("TextPrimary"))

            Text("点击下方按钮使用 Face ID 解锁")
                .font(.subheadline)
                .foregroundStyle(Color("TextSecondary"))
                .multilineTextAlignment(.center)

            if let authErrorMessage, !authErrorMessage.isEmpty {
                Text(authErrorMessage)
                    .font(.caption)
                    .foregroundStyle(.red)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }

            Button {
                Task { await authenticateIfNeeded() }
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "faceid")
                    Text("Face ID 解锁")
                }
                .font(.headline)
                .foregroundStyle(.white)
                .padding(.horizontal, 28)
                .padding(.vertical, 14)
                .background(Color("AccentColor"))
                .clipShape(Capsule())
            }
            .padding(.top, 8)

            Button("跳过，暂时关闭 Face ID") {
                settingsViewModel.useFaceID = false
                isUnlocked = true
            }
            .font(.caption)
            .foregroundStyle(Color("TextSecondary"))
            .padding(.top, 4)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(24)
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "clock.arrow.circlepath")
                .font(.system(size: 48))
                .foregroundStyle(Color("TextSecondary").opacity(0.4))
            Text("还没有记录")
                .font(.headline)
                .foregroundStyle(Color("TextSecondary"))
            Text("完成你的第一次思维重构后\n记录会出现在这里")
                .font(.subheadline)
                .foregroundStyle(Color("TextSecondary").opacity(0.7))
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var groupedEntries: [(String, [HistoryEntry])] {
        viewModel.groupedByDate(allEntries)
    }

    private var listContent: some View {
        List {
            weeklyReviewSection

            ForEach(groupedEntries, id: \.0) { pair in
                Section(pair.0) {
                    ForEach(pair.1, id: \.id) { entry in
                        HistoryRowView(entry: entry, viewModel: viewModel)
                    }
                    .onDelete { offsets in
                        deleteEntries(entries: pair.1, at: offsets)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
    }

    private var weeklyReviewSection: some View {
        Section {
            let stats = viewModel.weeklyStats(allEntries)
            HStack(spacing: 24) {
                statItem(value: "\(stats.count)", label: "本周分析", icon: "brain.head.profile", color: Color("AccentColor"))
                Divider().frame(height: 40)
                statItem(value: "\(stats.favoriteCount)", label: "收藏洞察", icon: "star.fill", color: .yellow)
                Divider().frame(height: 40)
                statItem(value: "\(allEntries.count)", label: "总记录", icon: "clock", color: Color("TextSecondary"))
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
        } header: {
            Text("本周回顾")
        }
    }

    private func statItem(value: String, label: String, icon: String, color: Color) -> some View {
        VStack(spacing: 6) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(color)
            Text(value)
                .font(.title2.bold())
                .foregroundStyle(Color("TextPrimary"))
            Text(label)
                .font(.caption2)
                .foregroundStyle(Color("TextSecondary"))
        }
        .frame(maxWidth: .infinity)
    }

    private func deleteEntries(entries: [HistoryEntry], at offsets: IndexSet) {
        for index in offsets {
            modelContext.delete(entries[index])
        }
        try? modelContext.save()
    }

    @MainActor
    private func authenticateIfNeeded() async {
        guard settingsViewModel.useFaceID, !isUnlocked else { return }

        let context = LAContext()
        var authError: NSError?

        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &authError) else {
            authErrorMessage = "当前设备不支持 Face ID，可在设置中关闭此选项。"
            return
        }

        do {
            let success = try await context.evaluatePolicy(
                .deviceOwnerAuthenticationWithBiometrics,
                localizedReason: "解锁你的历史记录"
            )
            if success {
                isUnlocked = true
                authErrorMessage = nil
            }
        } catch {
            authErrorMessage = "验证失败，请再试一次"
        }
    }
}

struct HistoryRowView: View {
    @Environment(\.modelContext) private var modelContext
    let entry: HistoryEntry
    @Bindable var viewModel: HistoryViewModel
    @State private var isExpanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(entry.inputThought)
                        .font(.subheadline)
                        .lineLimit(isExpanded ? nil : 2)
                        .foregroundStyle(Color("TextPrimary"))

                    HStack(spacing: 8) {
                        Text(entry.distortion)
                            .font(.caption)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Color("AccentColor").opacity(0.1))
                            .foregroundStyle(Color("AccentColor"))
                            .clipShape(Capsule())

                        if !entry.providerName.isEmpty {
                            Text(entry.providerName)
                                .font(.caption2)
                                .foregroundStyle(Color("TextSecondary"))
                        }

                        Text(entry.createdAt, style: .time)
                            .font(.caption2)
                            .foregroundStyle(Color("TextSecondary"))
                    }
                }

                Spacer()

                Button {
                    viewModel.toggleFavorite(entry, modelContext: modelContext)
                } label: {
                    Image(systemName: entry.isFavorite ? "star.fill" : "star")
                        .foregroundStyle(entry.isFavorite ? .yellow : Color("TextSecondary").opacity(0.4))
                        .font(.title3)
                }
                .buttonStyle(.plain)
            }

            if isExpanded {
                VStack(alignment: .leading, spacing: 10) {
                    expandedRow(label: "替代想法", value: entry.alternative, icon: "lightbulb")
                    expandedRow(label: "建议行动", value: entry.action, icon: "figure.walk")
                }
                .padding(.top, 4)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .contentShape(Rectangle())
        .onTapGesture {
            withAnimation(.spring(response: 0.3)) {
                isExpanded.toggle()
            }
        }
    }

    private func expandedRow(label: String, value: String, icon: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundStyle(Color("AccentColor"))
                .frame(width: 16)
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(Color("TextSecondary"))
                Text(value)
                    .font(.caption)
                    .foregroundStyle(Color("TextPrimary"))
            }
        }
    }
}

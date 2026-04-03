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
            .toolbar {
                if !needsAuth && !allEntries.isEmpty {
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
        ScrollView {
            LazyVStack(spacing: 16) {
                weeklyReviewCard

                ForEach(groupedEntries, id: \.0) { pair in
                    VStack(alignment: .leading, spacing: 10) {
                        Text(pair.0)
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Color("TextSecondary"))
                            .padding(.horizontal, 4)

                        ForEach(pair.1, id: \.id) { entry in
                            HistoryRowView(entry: entry, viewModel: viewModel)
                                .padding(14)
                                .background(Color("CardBackground"))
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                                .shadow(color: .black.opacity(0.03), radius: 4, y: 2)
                        }
                    }
                }
            }
            .padding(.horizontal)
            .padding(.top, 8)
            .padding(.bottom, 40)
        }
    }

    private var weeklyReviewCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("本周回顾")
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color("TextSecondary"))
                .padding(.horizontal, 4)

            HStack(spacing: 0) {
                let stats = viewModel.weeklyStats(allEntries)
                statItem(value: "\(stats.count)", label: "本周分析", icon: "brain.head.profile", color: Color("AccentColor"))
                statItem(value: "\(stats.favoriteCount)", label: "收藏", icon: "star.fill", color: .yellow)
                statItem(value: "\(allEntries.count)", label: "总记录", icon: "clock", color: Color("TextSecondary"))
            }
            .padding(.vertical, 12)
            .background(Color("CardBackground"))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .shadow(color: .black.opacity(0.03), radius: 4, y: 2)
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
    @State private var copiedToast = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                Text(entry.inputThought)
                    .font(.subheadline)
                    .lineLimit(isExpanded ? nil : 2)
                    .foregroundStyle(Color("TextPrimary"))

                Spacer(minLength: 12)

                Button {
                    viewModel.toggleFavorite(entry, modelContext: modelContext)
                } label: {
                    Image(systemName: entry.isFavorite ? "star.fill" : "star")
                        .foregroundStyle(entry.isFavorite ? .yellow : Color("TextSecondary").opacity(0.3))
                        .font(.body)
                }
                .buttonStyle(.plain)
            }

            HStack(spacing: 6) {
                Text(entry.distortion)
                    .font(.caption2.weight(.medium))
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .padding(.horizontal, 7)
                    .padding(.vertical, 3)
                    .background(Color("AccentColor").opacity(0.1))
                    .foregroundStyle(Color("AccentColor"))
                    .clipShape(Capsule())

                if !entry.providerName.isEmpty {
                    Text(entry.providerName)
                        .font(.caption2)
                        .foregroundStyle(Color("TextSecondary"))
                }

                Spacer()

                Text(entry.createdAt, style: .time)
                    .font(.caption2)
                    .foregroundStyle(Color("TextSecondary"))
            }

            if isExpanded {
                VStack(alignment: .leading, spacing: 10) {
                    Divider()

                    Text("替代想法")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color("TextSecondary"))
                    Text(entry.alternative)
                        .font(.subheadline)
                        .foregroundStyle(Color("TextPrimary"))

                    Text("建议行动")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color("TextSecondary"))
                    Text(entry.action)
                        .font(.subheadline)
                        .foregroundStyle(Color("TextPrimary"))

                    Divider()

                    HStack(spacing: 16) {
                        Button {
                            UIPasteboard.general.string = buildText()
                            UIImpactFeedbackGenerator(style: .light).impactOccurred()
                            withAnimation { copiedToast = true }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                                withAnimation { copiedToast = false }
                            }
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: copiedToast ? "checkmark" : "doc.on.doc")
                                Text(copiedToast ? "已复制" : "复制")
                            }
                            .font(.caption)
                            .foregroundStyle(Color("AccentColor"))
                        }
                        .buttonStyle(.plain)

                        Button {
                            UIPasteboard.general.string = buildText() + "\n\n请帮我进一步分析这个想法。"
                            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                                let url = URL(string: "chatgpt://")!
                                if UIApplication.shared.canOpenURL(url) {
                                    UIApplication.shared.open(url)
                                } else {
                                    UIApplication.shared.open(URL(string: "https://chat.openai.com")!)
                                }
                            }
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: "paperplane.fill")
                                Text("发到 ChatGPT")
                            }
                            .font(.caption)
                            .foregroundStyle(Color("AccentColor"))
                        }
                        .buttonStyle(.plain)

                        Spacer()
                    }
                }
                .transition(.opacity)
            }
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
        .onTapGesture {
            withAnimation(.easeInOut(duration: 0.25)) {
                isExpanded.toggle()
            }
        }
    }

    private func buildText() -> String {
        """
        我的想法：\(entry.inputThought)
        认知扭曲：\(entry.distortion)
        替代想法：\(entry.alternative)
        建议行动：\(entry.action)
        """
    }
}

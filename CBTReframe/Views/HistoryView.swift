import SwiftUI
import SwiftData
import LocalAuthentication
import UIKit

struct HistoryView: View {
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \HistoryEntry.createdAt, order: .reverse) private var allEntries: [HistoryEntry]
    @Bindable var viewModel: HistoryViewModel
    @Bindable var settingsViewModel: SettingsViewModel
    @State private var isUnlocked = false
    @State private var authErrorMessage: String?
    @State private var hasAttemptedAuth = false
    @State private var isPresentingHistoryExport = false
    @State private var historyExportItems: [Any] = []
    @State private var showExportOptions = false

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
            .searchable(text: $viewModel.searchText, prompt: "搜索想法、心情、扭曲、行动…")
            .toolbar {
                if !needsAuth && !allEntries.isEmpty {
                    ToolbarItem(placement: .topBarTrailing) {
                        HStack(spacing: 16) {
                            Button {
                                showExportOptions = true
                            } label: {
                                Image(systemName: "square.and.arrow.up")
                                    .foregroundStyle(Color("AccentColor"))
                            }
                            .accessibilityLabel("导出当前列表为 JSON")

                            Button {
                                withAnimation { viewModel.showFavoritesOnly.toggle() }
                            } label: {
                                Image(systemName: viewModel.showFavoritesOnly ? "star.fill" : "star")
                                    .foregroundStyle(viewModel.showFavoritesOnly ? .yellow : Color("TextSecondary"))
                            }
                            .accessibilityLabel(viewModel.showFavoritesOnly ? "显示全部" : "仅显示收藏")
                        }
                    }
                }
            }
            .sheet(isPresented: $isPresentingHistoryExport, onDismiss: { historyExportItems = [] }) {
                HistoryExportActivityView(items: historyExportItems)
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
        .confirmationDialog("导出格式", isPresented: $showExportOptions) {
            Button("JSON") { export(format: "json") }
            Button("CSV") { export(format: "csv") }
            Button("PDF") { export(format: "pdf") }
            Button("取消", role: .cancel) {}
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
                            HistoryRowView(
                                entry: entry,
                                viewModel: viewModel,
                                onShare: { shareEntry(entry) },
                                onDelete: { deleteEntry(entry) }
                            )
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
                statDivider
                statItem(value: "\(stats.favoriteCount)", label: "收藏", icon: "star.fill", color: .yellow)
                statDivider
                statItem(value: "\(allEntries.count)", label: "总记录", icon: "clock", color: Color("TextSecondary"))
                statDivider
                statItem(value: topMoodEmoji, label: "常见心情", icon: "", color: .clear)
            }
            .padding(.vertical, 12)
            .background(Color("CardBackground"))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .shadow(color: .black.opacity(0.03), radius: 4, y: 2)
        }
    }

    private var statDivider: some View {
        Rectangle()
            .fill(Color(.separator).opacity(0.15))
            .frame(width: 1, height: 36)
    }

    private var topMoodEmoji: String {
        let moods = allEntries.prefix(50).map(\.moodTag).filter { !$0.isEmpty }
        guard !moods.isEmpty else { return "–" }
        let counts = Dictionary(grouping: moods, by: { $0 }).mapValues(\.count)
        let top = counts.max(by: { $0.value < $1.value })?.key ?? "–"
        return MoodTagPicker.emoji(for: top)
    }

    private func statItem(value: String, label: String, icon: String, color: Color) -> some View {
        VStack(spacing: 6) {
            if icon.isEmpty {
                Text(value)
                    .font(.title2)
            } else {
                Image(systemName: icon)
                    .font(.title3)
                    .foregroundStyle(color)
            }
            if !icon.isEmpty {
                Text(value)
                    .font(.title2.bold())
                    .foregroundStyle(Color("TextPrimary"))
            }
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

    private func export(format: String) {
        let subset = viewModel.filteredEntries(allEntries)
        let url: URL?
        switch format {
        case "csv":
            url = HistoryExportService.makeTemporaryCSVFile(entries: subset)
        case "pdf":
            url = HistoryExportService.makeTemporaryPDFFile(entries: subset)
        default:
            url = HistoryExportService.makeTemporaryJSONFile(entries: subset)
        }
        if let url {
            historyExportItems = [url]
            isPresentingHistoryExport = true
        }
    }

    private func shareEntry(_ entry: HistoryEntry) {
        historyExportItems = [buildShareText(for: entry)]
        isPresentingHistoryExport = true
    }

    private func deleteEntry(_ entry: HistoryEntry) {
        modelContext.delete(entry)
        try? modelContext.save()
    }

    private func buildShareText(for entry: HistoryEntry) -> String {
        var text = ""
        if !entry.moodTag.isEmpty {
            text += "心情：\(entry.moodTag)\n"
        }
        if let depth = entry.analysisDepth {
            text += "深度：\(depth.displayName)\n"
        }
        text += "我的想法：\(entry.inputThought)\n\n"

        let template = entry.thinkingTemplate ?? .cbt
        switch template {
        case .cbt:
            text += """
            认知扭曲：\(entry.distortion)
            替代想法：\(entry.alternative)
            建议行动：\(entry.action)
            """
        case .socratic:
            let qs = (entry.analysisResult.questions ?? [])
                .enumerated()
                .map { "\($0.offset + 1). \($0.element)" }
                .joined(separator: "\n")
            text += """
            引导问题：
            \(qs)

            说明：\(entry.alternative)
            反思练习：\(entry.action)
            """
        case .behavioral:
            text += """
            状态：\(entry.analysisResult.stateAssessment ?? entry.distortion)
            下一步：\(entry.action)
            积极视角：\(entry.alternative)
            """
        }
        return text
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
    var onShare: (() -> Void)?
    var onDelete: (() -> Void)?
    @State private var isExpanded = false

    private var displayTemplate: ThinkingTemplate {
        entry.thinkingTemplate ?? .cbt
    }

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
                Text("[\(displayTemplate.historyTag)]")
                    .font(.caption2.weight(.semibold))
                    .lineLimit(1)
                    .padding(.horizontal, 7)
                    .padding(.vertical, 3)
                    .background(Color("AccentColor").opacity(0.12))
                    .foregroundStyle(Color("AccentColor"))
                    .clipShape(Capsule())

                if !entry.moodTag.isEmpty {
                    Text("[\(entry.moodTag)]")
                        .font(.caption2.weight(.medium))
                        .lineLimit(1)
                        .padding(.horizontal, 7)
                        .padding(.vertical, 3)
                        .background(Color("TextSecondary").opacity(0.08))
                        .foregroundStyle(Color("TextSecondary"))
                        .clipShape(Capsule())
                }

                if let depth = entry.analysisDepth {
                    Text("[\(depth.displayName)]")
                        .font(.caption2.weight(.medium))
                        .lineLimit(1)
                        .padding(.horizontal, 7)
                        .padding(.vertical, 3)
                        .background(Color("TextSecondary").opacity(0.06))
                        .foregroundStyle(Color("TextSecondary"))
                        .clipShape(Capsule())
                }

                Text(entry.distortion)
                    .font(.caption2.weight(.medium))
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .padding(.horizontal, 7)
                    .padding(.vertical, 3)
                    .background(Color("AccentColor").opacity(0.06))
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

                    ResultCardView(
                        result: entry.analysisResult,
                        template: displayTemplate,
                        inputThought: entry.inputThought,
                        moodTag: entry.moodTag,
                        analysisDepthLabel: entry.analysisDepth?.displayName ?? "",
                        historyEntryID: entry.id
                    )
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
        .contextMenu {
            Button {
                onShare?()
            } label: {
                Label("分享", systemImage: "square.and.arrow.up")
            }

            Button(role: .destructive) {
                onDelete?()
            } label: {
                Label("移除", systemImage: "trash")
            }
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            Button {
                onShare?()
            } label: {
                Label("分享", systemImage: "square.and.arrow.up")
            }
            .tint(Color("AccentColor"))

            Button(role: .destructive) {
                onDelete?()
            } label: {
                Label("移除", systemImage: "trash")
            }
        }
    }
}

// MARK: - V2.1 导出分享

private struct HistoryExportActivityView: UIViewControllerRepresentable {
    var items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

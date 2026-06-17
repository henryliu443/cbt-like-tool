import SwiftUI
import Combine
import SwiftData

// MARK: - Model

struct FollowUpMessage: Identifiable, Codable {
    enum Role: String, Codable {
        case assistant
        case user
    }

    let id: UUID
    let role: Role
    let text: String

    init(id: UUID = UUID(), role: Role, text: String) {
        self.id = id
        self.role = role
        self.text = text
    }
}

// MARK: - ViewModel

@MainActor
final class FollowUpChatViewModel: ObservableObject {
    @Published var messages: [FollowUpMessage] = []
    @Published var isReplying = false
    @Published var draftText: String = ""

    private var modelContext: ModelContext?
    private let initialThought: String
    private let initialResult: AnalysisResult
    private let template: ThinkingTemplate
    private let moodTag: String
    private let historyEntryID: UUID?

    init(
        initialThought: String,
        initialResult: AnalysisResult,
        template: ThinkingTemplate,
        moodTag: String,
        historyEntryID: UUID?
    ) {
        self.initialThought = initialThought
        self.initialResult = initialResult
        self.template = template
        self.moodTag = moodTag
        self.historyEntryID = historyEntryID
        self.messages = Self.bootstrapMessages(initialThought: initialThought, initialResult: initialResult)
    }

    func setModelContext(_ context: ModelContext) {
        guard modelContext == nil else { return }
        modelContext = context
        loadPersistedMessages()
    }

    var canSend: Bool {
        !draftText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    func sendMessage() async {
        let text = draftText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }

        messages.append(FollowUpMessage(role: .user, text: text))
        draftText = ""
        persistMessages()

        isReplying = true
        defer { isReplying = false }

        let provider = Self.resolvedProvider()
        let model = Self.resolvedModel(for: provider)
        let service = AIServiceFactory.service(for: provider)

        let mode: ReframeMode = .balanced
        let style: ResponseStyle = .warm
        let strategy = routeStrategy(level: detectRiskLevel(text))

        do {
            let result = try await service.reframe(
                thought: buildFollowUpThought(for: text),
                mood: moodTag.isEmpty ? "未填写" : moodTag,
                hasAkathisia: false,
                model: model,
                mode: mode,
                style: style,
                template: template.promptTemplate,
                strategy: strategy
            )
            messages.append(FollowUpMessage(role: .assistant, text: Self.responseText(from: result, template: template)))
        } catch {
            let fallback = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            messages.append(FollowUpMessage(role: .assistant, text: "这次没有成功生成回复：\(fallback)"))
        }
        persistMessages()
    }

    // MARK: - Persistence

    private func loadPersistedMessages() {
        guard let id = historyEntryID, let ctx = modelContext else { return }
        let fetch = FetchDescriptor<HistoryEntry>(predicate: #Predicate { $0.id == id })
        guard let entry = try? ctx.fetch(fetch).first else { return }
        let json = entry.followUpMessagesJSON.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !json.isEmpty, let data = json.data(using: .utf8),
              let decoded = try? JSONDecoder().decode([FollowUpMessage].self, from: data),
              !decoded.isEmpty else { return }
        messages = decoded
    }

    private func persistMessages() {
        guard let id = historyEntryID, let ctx = modelContext else { return }
        let fetch = FetchDescriptor<HistoryEntry>(predicate: #Predicate { $0.id == id })
        guard let entry = try? ctx.fetch(fetch).first,
              let data = try? JSONEncoder().encode(messages),
              let text = String(data: data, encoding: .utf8) else { return }
        entry.followUpMessagesJSON = text
        try? ctx.save()
    }

    // MARK: - Prompt (template-aware, WAS hardcoded as "基于 CBT")

    private func buildFollowUpThought(for userInput: String) -> String {
        let recent = messages.suffix(6).map { msg in
            let role = msg.role == .user ? "用户" : "助手"
            return "\(role)：\(msg.text)"
        }.joined(separator: "\n")

        let instruction: String = switch template {
        case .cbt:
            "请基于 CBT 框架给出简短、具体、可执行的回应。"
        case .socratic:
            "请基于苏格拉底提问法引导用户反思，用提问代替直接答案。"
        case .behavioral:
            "请聚焦于行为激活，给出可立即执行的一小步建议。"
        }

        return """
        原始想法：\(initialThought)
        上一轮结论：\(initialResult.alternative)
        对话上下文：
        \(recent)

        用户继续追问：\(userInput)
        \(instruction)
        """
    }

    // MARK: - Provider / Model resolution

    private static func resolvedProvider() -> AIProvider {
        let raw = UserDefaults.standard.string(forKey: "selectedProvider") ?? AIProvider.gemini.rawValue
        return AIProvider(rawValue: raw) ?? .local
    }

    private static func resolvedModel(for provider: AIProvider) -> AIModel {
        let id = UserDefaults.standard.string(forKey: "selectedModelId") ?? provider.defaultModel.id
        return provider.fallbackModels.first(where: { $0.id == id }) ?? provider.defaultModel
    }

    // MARK: - Helpers

    private static func bootstrapMessages(initialThought: String, initialResult: AnalysisResult) -> [FollowUpMessage] {
        [
            FollowUpMessage(role: .assistant, text: "你刚刚的原始想法：\(initialThought)"),
            FollowUpMessage(role: .assistant, text: "上一轮结论：\(initialResult.alternative)")
        ]
    }

    private static func responseText(from result: AnalysisResult, template: ThinkingTemplate) -> String {
        switch template {
        case .cbt:
            return "换个角度：\(result.alternative)\n\n你现在可以先做：\(result.action)"
        case .socratic:
            if let qs = result.questions, !qs.isEmpty {
                return qs.prefix(3).enumerated().map { "\($0.offset + 1). \($0.element)" }.joined(separator: "\n")
            }
            return result.alternative.isEmpty ? result.action : result.alternative
        case .behavioral:
            return "下一步建议：\(result.action)\n\n积极视角：\(result.alternative)"
        }
    }
}

// MARK: - View

struct FollowUpChatView: View {
    @Environment(\.modelContext) private var modelContext
    @StateObject private var viewModel: FollowUpChatViewModel
    @FocusState private var isInputFocused: Bool

    init(
        initialThought: String,
        initialResult: AnalysisResult,
        template: ThinkingTemplate,
        moodTag: String,
        historyEntryID: UUID?
    ) {
        _viewModel = StateObject(wrappedValue: FollowUpChatViewModel(
            initialThought: initialThought,
            initialResult: initialResult,
            template: template,
            moodTag: moodTag,
            historyEntryID: historyEntryID
        ))
    }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(viewModel.messages) { message in
                        HStack {
                            if message.role == .user { Spacer(minLength: 48) }
                            Text(message.text)
                                .font(.body)
                                .foregroundStyle(message.role == .user ? Color.white : Color("TextPrimary"))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 10)
                                .background(message.role == .user ? Color("AccentColor") : Color(.secondarySystemGroupedBackground))
                                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                                .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(message.role == .user ? Color.clear : Color(.separator).opacity(0.25), lineWidth: 1))
                                .frame(maxWidth: 280, alignment: message.role == .user ? .trailing : .leading)
                                .id(message.id)
                            if message.role != .user { Spacer(minLength: 48) }
                        }
                        .frame(maxWidth: .infinity)
                    }
                    if viewModel.isReplying {
                        HStack { Spacer(); ProgressView().padding(); Spacer() }
                    }
                }
                .padding(.horizontal, 14)
                .padding(.top, 12)
                .padding(.bottom, 12)
            }
            .background(Color(.systemGroupedBackground))
            .safeAreaInset(edge: .bottom) {
                HStack(spacing: 8) {
                    TextField("继续追问…", text: $viewModel.draftText, axis: .vertical)
                        .lineLimit(1...4)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .background(Color(.secondarySystemBackground))
                        .clipShape(Capsule())
                        .focused($isInputFocused)
                    Button {
                        Task { await viewModel.sendMessage() }
                    } label: {
                        Image(systemName: "arrow.up.circle.fill")
                            .font(.system(size: 30))
                            .foregroundStyle(viewModel.canSend && !viewModel.isReplying ? Color("AccentColor") : Color.gray.opacity(0.45))
                    }
                    .disabled(!viewModel.canSend || viewModel.isReplying)
                }
                .padding(.horizontal, 12)
                .padding(.top, 8)
                .padding(.bottom, 8)
                .background(.ultraThinMaterial)
            }
            .onChange(of: viewModel.messages.count) { _, _ in
                guard let last = viewModel.messages.last else { return }
                withAnimation(.easeOut(duration: 0.2)) { proxy.scrollTo(last.id, anchor: .bottom) }
            }
        }
        .navigationTitle("继续探索")
        .onAppear { viewModel.setModelContext(modelContext) }
    }
}

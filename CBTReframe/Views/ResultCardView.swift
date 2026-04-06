import SwiftUI
import SwiftData

struct ResultCardView: View {
    let result: AnalysisResult
    var template: ThinkingTemplate = .cbt
    var inputThought: String = ""
    /// 历史记录等场景：复制到剪贴板时附在文首（首页留空即可）。
    var moodTag: String = ""
    var analysisDepthLabel: String = ""
    var historyEntryID: UUID?
    @State private var copiedToast = false
    @State private var showFollowUp = false
    @State private var reveal = false
    @State private var collapsedSections: Set<String> = []

    var body: some View {
        VStack(spacing: 0) {
            keyInsightHeader
            if keyInsightText != nil {
                sectionDivider()
            }
            switch template {
            case .cbt:
                cbtContent
            case .socratic:
                socraticContent
            case .behavioral:
                behavioralContent
            }
            sectionDivider()
            actionBar
            continueExplorationFooter
        }
        .background(Color("CardBackground"))
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .shadow(color: .black.opacity(0.06), radius: 12, y: 6)
        .overlay(alignment: .top) {
            if copiedToast {
                HStack(spacing: 6) {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(.green)
                    Text("已复制到剪贴板")
                        .font(.caption.weight(.medium))
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(.ultraThinMaterial)
                .clipShape(Capsule())
                .offset(y: -12)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .sheet(isPresented: $showFollowUp) {
            NavigationStack {
                FollowUpChatView(
                    initialThought: inputThought,
                    initialResult: result,
                    template: template,
                    moodTag: moodTag,
                    historyEntryID: historyEntryID
                )
            }
        }
        .onAppear {
            withAnimation(.easeInOut(duration: 0.35)) {
                reveal = true
            }
        }
    }

    @ViewBuilder
    private var keyInsightHeader: some View {
        if let insight = keyInsightText {
            VStack(alignment: .leading, spacing: 8) {
                Text("关键提醒")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color("TextSecondary"))
                    .textCase(.uppercase)
                Text(insight)
                    .font(.body.weight(.medium))
                    .foregroundStyle(Color("TextPrimary"))
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(Color("AccentColor").opacity(0.08))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(Color("AccentColor").opacity(0.18), lineWidth: 1)
            )
            .padding(16)
        }
    }

    private var cbtContent: some View {
        Group {
            resultSection(
                icon: "exclamationmark.triangle",
                iconColor: .orange,
                label: "认知扭曲",
                value: result.distortion
            )
            sectionDivider()
            resultSection(
                icon: "lightbulb",
                iconColor: Color("AccentColor"),
                label: "替代想法",
                value: result.alternative
            )
            sectionDivider()
            resultSection(
                icon: "figure.walk",
                iconColor: .green,
                label: "建议行动",
                value: result.action
            )
            if let extra = result.actions, !extra.isEmpty {
                sectionDivider()
                resultSection(
                    icon: "list.bullet",
                    iconColor: .teal,
                    label: "更多行动",
                    value: extra.joined(separator: "\n")
                )
            }
        }
    }

    private var socraticContent: some View {
        Group {
            if !result.distortion.isEmpty {
                resultSection(
                    icon: "eye",
                    iconColor: .orange,
                    label: "视角提示",
                    value: result.distortion
                )
                sectionDivider()
            }
            socraticQuestionsSection
            if !result.alternative.isEmpty {
                sectionDivider()
                resultSection(
                    icon: "quote.bubble",
                    iconColor: Color("AccentColor"),
                    label: "说明",
                    value: result.alternative
                )
            }
            sectionDivider()
            resultSection(
                icon: "pencil.line",
                iconColor: .green,
                label: "反思练习",
                value: result.action
            )
        }
    }

    private var socraticQuestionsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "questionmark.circle")
                    .font(.subheadline)
                    .foregroundStyle(Color("AccentColor"))
                    .frame(width: 28, height: 28)
                    .background(Color("AccentColor").opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 8))

                Text("引导问题")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color("TextSecondary"))
                    .textCase(.uppercase)
            }

            let qs = result.questions ?? []
            if qs.isEmpty {
                Text("（无问题列表）")
                    .font(.body)
                    .foregroundStyle(Color("TextSecondary"))
            } else {
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(Array(qs.enumerated()), id: \.offset) { idx, q in
                        HStack(alignment: .top, spacing: 8) {
                            Text("\(idx + 1).")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(Color("AccentColor"))
                                .frame(width: 20, alignment: .leading)
                            Text(q)
                                .font(.body)
                                .foregroundStyle(Color("TextPrimary"))
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .opacity(reveal ? 1 : 0.1)
        .offset(y: reveal ? 0 : 8)
        .animation(.easeOut(duration: 0.3), value: reveal)
    }

    private var behavioralContent: some View {
        Group {
            resultSection(
                icon: "heart.text.square",
                iconColor: .orange,
                label: "当前状态",
                value: result.stateAssessment ?? result.distortion
            )
            sectionDivider()
            resultSection(
                icon: "arrow.forward.circle",
                iconColor: Color("AccentColor"),
                label: "下一步行动",
                value: result.action
            )
            if !result.alternative.isEmpty {
                sectionDivider()
                resultSection(
                    icon: "leaf",
                    iconColor: .green,
                    label: "积极视角",
                    value: result.alternative
                )
            }
        }
    }

    private var actionBar: some View {
        HStack(spacing: 0) {
            actionButton(icon: "doc.on.doc", label: "复制") {
                copyResultToClipboard()
            }

            Rectangle()
                .fill(Color(.separator).opacity(0.2))
                .frame(width: 1, height: 28)

            actionButton(icon: "paperplane.fill", label: "发到 ChatGPT") {
                sendToChatGPT()
            }

            Rectangle()
                .fill(Color(.separator).opacity(0.2))
                .frame(width: 1, height: 28)

            actionButton(icon: collapsedSections.isEmpty ? "arrow.up.left.and.arrow.down.right" : "arrow.down.right.and.arrow.up.left", label: collapsedSections.isEmpty ? "收起分段" : "展开分段") {
                withAnimation(.easeInOut(duration: 0.2)) {
                    if collapsedSections.isEmpty {
                        collapsedSections = allSectionLabels
                    } else {
                        collapsedSections.removeAll()
                    }
                }
            }
        }
        .padding(.vertical, 4)
    }

    private var continueExplorationFooter: some View {
        Button {
            showFollowUp = true
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "arrow.right.circle.fill")
                Text("继续探索")
                    .font(.subheadline.weight(.semibold))
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .foregroundStyle(Color("AccentColor"))
            .background(Color("AccentColor").opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(Color("AccentColor").opacity(0.2), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 16)
        .padding(.bottom, 16)
    }

    private func actionButton(icon: String, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.caption)
                Text(label)
                    .font(.caption.weight(.medium))
            }
            .foregroundStyle(Color("AccentColor"))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
        }
        .buttonStyle(.plain)
    }

    private func buildClipboardText() -> String {
        var text = ""
        let mood = moodTag.trimmingCharacters(in: .whitespacesAndNewlines)
        if !mood.isEmpty {
            text += "心情：\(mood)\n"
        }
        let depth = analysisDepthLabel.trimmingCharacters(in: .whitespacesAndNewlines)
        if !depth.isEmpty {
            text += "深度：\(depth)\n"
        }
        if !inputThought.isEmpty {
            text += "我的想法：\(inputThought)\n\n"
        }
        switch template {
        case .cbt:
            text += """
            认知扭曲：\(result.distortion)
            替代想法：\(result.alternative)
            建议行动：\(result.action)
            """
            if let a = result.actions, !a.isEmpty {
                text += "\n更多行动：\(a.joined(separator: "；"))"
            }
        case .socratic:
            let qs = (result.questions ?? []).enumerated().map { "\($0.offset + 1). \($0.element)" }.joined(separator: "\n")
            text += """
            引导问题：
            \(qs)

            说明：\(result.alternative)
            反思练习：\(result.action)
            """
        case .behavioral:
            text += """
            状态：\(result.stateAssessment ?? result.distortion)
            下一步：\(result.action)
            积极视角：\(result.alternative)
            """
        }
        text += "\n\n请帮我进一步分析这个想法，给出更多角度和建议。"
        return text
    }

    private func copyResultToClipboard() {
        UIPasteboard.general.string = buildClipboardText()
        let impact = UIImpactFeedbackGenerator(style: .light)
        impact.impactOccurred()
        withAnimation(.spring(response: 0.3)) { copiedToast = true }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            withAnimation { copiedToast = false }
        }
    }

    private func sendToChatGPT() {
        UIPasteboard.general.string = buildClipboardText()
        let impact = UIImpactFeedbackGenerator(style: .medium)
        impact.impactOccurred()
        withAnimation(.spring(response: 0.3)) { copiedToast = true }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            ExternalAIAppLauncher.openChatGPT()
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                withAnimation { copiedToast = false }
            }
        }
    }

    private func resultSection(icon: String, iconColor: Color, label: String, value: String) -> some View {
        let isCollapsed = collapsedSections.contains(label)
        return VStack(alignment: .leading, spacing: 8) {
            Button {
                withAnimation(.easeInOut(duration: 0.2)) {
                    if isCollapsed {
                        collapsedSections.remove(label)
                    } else {
                        collapsedSections.insert(label)
                    }
                }
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: icon)
                        .font(.subheadline)
                        .foregroundStyle(iconColor)
                        .frame(width: 28, height: 28)
                        .background(iconColor.opacity(0.12))
                        .clipShape(RoundedRectangle(cornerRadius: 8))

                    Text(label)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color("TextSecondary"))
                        .textCase(.uppercase)

                    Spacer()
                    Image(systemName: isCollapsed ? "chevron.down" : "chevron.up")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color("TextSecondary"))
                }
            }
            .buttonStyle(.plain)

            if !isCollapsed {
                Text(value)
                    .font(.body)
                    .foregroundStyle(Color("TextPrimary"))
                    .fixedSize(horizontal: false, vertical: true)
                    .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
    }

    private func sectionDivider() -> some View {
        Rectangle()
            .fill(Color(.separator).opacity(0.2))
            .frame(height: 1)
            .padding(.horizontal, 16)
    }

    private var keyInsightText: String? {
        let source: String
        switch template {
        case .cbt:
            source = result.alternative
        case .socratic:
            source = result.alternative.isEmpty ? result.action : result.alternative
        case .behavioral:
            source = result.action
        }
        let sentence = source
            .split(whereSeparator: { $0 == "。" || $0 == "！" || $0 == "？" || $0 == "\n" })
            .first?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return sentence.isEmpty ? nil : sentence
    }

    private var allSectionLabels: Set<String> {
        switch template {
        case .cbt:
            var labels: [String] = ["认知扭曲", "替代想法", "建议行动"]
            if let extra = result.actions, !extra.isEmpty {
                labels.append("更多行动")
            }
            return Set(labels)
        case .socratic:
            return Set(["视角提示", "反思练习", "说明"])
        case .behavioral:
            var labels: [String] = ["当前状态", "下一步行动"]
            if !result.alternative.isEmpty {
                labels.append("积极视角")
            }
            return Set(labels)
        }
    }
}

private struct FollowUpChatView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \HistoryEntry.createdAt, order: .reverse) private var historyEntries: [HistoryEntry]
    let initialThought: String
    let initialResult: AnalysisResult
    let template: ThinkingTemplate
    let moodTag: String
    let historyEntryID: UUID?
    @State private var messages: [FollowUpMessage] = []
    @State private var draft: String = ""
    @State private var isReplying = false
    @FocusState private var isInputFocused: Bool

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(messages) { message in
                        HStack {
                            if message.role == .user {
                                Spacer(minLength: 48)
                            }

                            Text(message.text)
                                .font(.body)
                                .foregroundStyle(message.role == .user ? Color.white : Color("TextPrimary"))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 10)
                                .background(
                                    message.role == .user
                                        ? Color("AccentColor")
                                        : Color(.secondarySystemGroupedBackground)
                                )
                                .clipShape(
                                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                                        .stroke(
                                            message.role == .user
                                                ? Color.clear
                                                : Color(.separator).opacity(0.25),
                                            lineWidth: 1
                                        )
                                )
                                .frame(maxWidth: 280, alignment: message.role == .user ? .trailing : .leading)
                                .id(message.id)

                            if message.role != .user {
                                Spacer(minLength: 48)
                            }
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.top, 12)
                .padding(.bottom, 12)
            }
            .background(Color(.systemGroupedBackground))
            .safeAreaInset(edge: .bottom) {
                HStack(spacing: 8) {
                    TextField("继续追问…", text: $draft, axis: .vertical)
                        .lineLimit(1...4)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .background(Color(.secondarySystemBackground))
                        .clipShape(Capsule())
                        .focused($isInputFocused)

                    Button {
                        Task { await sendMessage() }
                    } label: {
                        Image(systemName: "arrow.up.circle.fill")
                            .font(.system(size: 30))
                            .foregroundStyle(canSend ? Color("AccentColor") : Color.gray.opacity(0.45))
                    }
                    .disabled(!canSend || isReplying)
                }
                .padding(.horizontal, 12)
                .padding(.top, 8)
                .padding(.bottom, 8)
                .background(.ultraThinMaterial)
            }
            .onChange(of: messages.count) { _, _ in
                guard let last = messages.last else { return }
                withAnimation(.easeOut(duration: 0.2)) {
                    proxy.scrollTo(last.id, anchor: .bottom)
                }
            }
            .onAppear {
                loadOrBootstrapMessages()
            }
        }
        .navigationTitle("继续探索")
    }

    private var canSend: Bool {
        !draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private func loadOrBootstrapMessages() {
        if let entry = currentHistoryEntry,
           !entry.followUpMessagesJSON.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
           let data = entry.followUpMessagesJSON.data(using: .utf8),
           let decoded = try? JSONDecoder().decode([FollowUpMessage].self, from: data),
           !decoded.isEmpty {
            messages = decoded
            return
        }
        messages = [
            FollowUpMessage(role: .assistant, text: "你刚刚的原始想法：\(initialThought)"),
            FollowUpMessage(role: .assistant, text: "上一轮结论：\(initialResult.alternative)")
        ]
        persistMessages()
    }

    private var currentHistoryEntry: HistoryEntry? {
        guard let id = historyEntryID else { return nil }
        return historyEntries.first(where: { $0.id == id })
    }

    private func persistMessages() {
        guard let entry = currentHistoryEntry,
              let data = try? JSONEncoder().encode(messages),
              let text = String(data: data, encoding: .utf8) else { return }
        entry.followUpMessagesJSON = text
        try? modelContext.save()
    }

    private func responseText(from result: AnalysisResult) -> String {
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

    private func resolvedProvider() -> AIProvider {
        let raw = UserDefaults.standard.string(forKey: "selectedProvider") ?? AIProvider.gemini.rawValue
        return AIProvider(rawValue: raw) ?? .local
    }

    private func resolvedModel(for provider: AIProvider) -> AIModel {
        let id = UserDefaults.standard.string(forKey: "selectedModelId") ?? provider.defaultModel.id
        return provider.fallbackModels.first(where: { $0.id == id }) ?? provider.defaultModel
    }

    private func buildFollowUpThought(for userInput: String) -> String {
        let recent = messages.suffix(6).map { msg in
            let role = msg.role == .user ? "用户" : "助手"
            return "\(role)：\(msg.text)"
        }.joined(separator: "\n")
        return """
        原始想法：\(initialThought)
        上一轮结论：\(initialResult.alternative)
        对话上下文：
        \(recent)

        用户继续追问：\(userInput)
        请基于 CBT 给出简短、具体、可执行的回应。
        """
    }

    private func sendMessage() async {
        let text = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        messages.append(FollowUpMessage(role: .user, text: text))
        draft = ""
        persistMessages()

        isReplying = true
        defer { isReplying = false }

        let provider = resolvedProvider()
        let model = resolvedModel(for: provider)
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
            messages.append(FollowUpMessage(role: .assistant, text: responseText(from: result)))
        } catch {
            let fallback = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
            messages.append(FollowUpMessage(role: .assistant, text: "这次没有成功生成回复：\(fallback)"))
        }
        persistMessages()
    }
}

private struct FollowUpMessage: Identifiable, Codable {
    enum Role: String, Codable {
        case assistant
        case user
    }

    let id = UUID()
    let role: Role
    let text: String
}

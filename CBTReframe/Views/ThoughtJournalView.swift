import SwiftUI
import SwiftData

struct ThoughtJournalView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \ThoughtEntry.createdAt, order: .reverse) private var entries: [ThoughtEntry]
    @Bindable var viewModel: ThoughtJournalViewModel

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                if entries.isEmpty && viewModel.patternReport == nil {
                    emptyState
                } else {
                    mainContent
                }

                addButton
            }
            .navigationTitle("想法记录")
            .sheet(isPresented: $viewModel.showAddSheet) {
                AddThoughtSheet(viewModel: viewModel)
            }
            .toolbar {
                if !entries.isEmpty {
                    ToolbarItem(placement: .topBarLeading) {
                        Button {
                            sendAllToChatGPT()
                        } label: {
                            Label("ChatGPT", systemImage: "paperplane.fill")
                        }
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            Task {
                                await viewModel.analyzePatterns(entries: entries, modelContext: modelContext)
                            }
                        } label: {
                            if viewModel.isAnalyzing {
                                ProgressView()
                            } else {
                                Label("整理", systemImage: "wand.and.stars")
                            }
                        }
                        .disabled(viewModel.isAnalyzing)
                    }
                }
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "square.and.pencil")
                .font(.system(size: 48))
                .foregroundStyle(Color("TextSecondary").opacity(0.4))
            Text("记录你的自动想法")
                .font(.headline)
                .foregroundStyle(Color("TextSecondary"))
            Text("当脑海中闪过负面想法时\n随手记下来，积累后让 AI 帮你整理")
                .font(.subheadline)
                .foregroundStyle(Color("TextSecondary").opacity(0.7))
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var mainContent: some View {
        List {
            if let report = viewModel.patternReport {
                patternReportSection(report)
            }

            if let error = viewModel.errorMessage {
                Section {
                    HStack(spacing: 8) {
                        Image(systemName: "exclamationmark.circle")
                            .foregroundStyle(.red)
                        Text(error)
                            .font(.subheadline)
                    }
                }
            }

            let unprocessed = entries.filter { !$0.isProcessed }
            if !unprocessed.isEmpty {
                Section {
                    ForEach(unprocessed, id: \.id) { entry in
                        ThoughtRowView(entry: entry)
                    }
                    .onDelete { offsets in
                        deleteEntries(from: unprocessed, at: offsets)
                    }
                } header: {
                    HStack {
                        Text("待整理（\(unprocessed.count)条）")
                        Spacer()
                        if unprocessed.count >= 3 {
                            Image(systemName: "sparkles")
                                .foregroundStyle(Color("AccentColor"))
                                .font(.caption)
                            Text("可以整理了")
                                .font(.caption)
                                .foregroundStyle(Color("AccentColor"))
                        }
                    }
                }
            }

            let processed = entries.filter { $0.isProcessed }
            if !processed.isEmpty {
                Section("已整理") {
                    ForEach(processed, id: \.id) { entry in
                        ThoughtRowView(entry: entry)
                    }
                    .onDelete { offsets in
                        deleteEntries(from: processed, at: offsets)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
    }

    private func patternReportSection(_ report: ThoughtPatternReport) -> some View {
        Section {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 8) {
                    Image(systemName: "chart.bar.doc.horizontal")
                        .foregroundStyle(Color("AccentColor"))
                        .font(.title3)
                    Text("思维模式分析")
                        .font(.headline)
                        .foregroundStyle(Color("TextPrimary"))
                }

                ForEach(report.topDistortions) { distortion in
                    HStack(spacing: 10) {
                        Text("\(distortion.count)")
                            .font(.title2.bold())
                            .foregroundStyle(Color("AccentColor"))
                            .frame(width: 36)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(distortion.name)
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(Color("TextPrimary"))
                            Text("「\(distortion.example)」")
                                .font(.caption)
                                .foregroundStyle(Color("TextSecondary"))
                                .lineLimit(2)
                        }
                    }
                }

                Divider()

                VStack(alignment: .leading, spacing: 6) {
                    Label("整体模式", systemImage: "brain.head.profile")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color("TextSecondary"))
                    Text(report.overallPattern)
                        .font(.subheadline)
                        .foregroundStyle(Color("TextPrimary"))
                }

                VStack(alignment: .leading, spacing: 6) {
                    Label("建议", systemImage: "lightbulb")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.orange)
                    Text(report.suggestion)
                        .font(.subheadline)
                        .foregroundStyle(Color("TextPrimary"))
                }
            }
            .padding(.vertical, 4)
        } header: {
            Text("最近分析")
        }
    }

    private var addButton: some View {
        Button {
            viewModel.showAddSheet = true
        } label: {
            Image(systemName: "plus")
                .font(.title2.weight(.semibold))
                .foregroundStyle(.white)
                .frame(width: 56, height: 56)
                .background(
                    LinearGradient(
                        colors: [Color("GradientStart"), Color("GradientEnd")],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .clipShape(Circle())
                .shadow(color: Color("AccentColor").opacity(0.3), radius: 8, y: 4)
        }
        .padding(.trailing, 20)
        .padding(.bottom, 20)
    }

    private func sendAllToChatGPT() {
        let unprocessed = entries.filter { !$0.isProcessed }
        let target = unprocessed.isEmpty ? entries : unprocessed

        let thoughtsList = target.enumerated().map { idx, entry in
            var line = "\(idx + 1). \(entry.content)"
            if !entry.emotion.isEmpty { line += "（\(entry.emotion)）" }
            return line
        }.joined(separator: "\n")

        let prompt = """
        我最近记录了一些自动想法，请帮我分析其中的认知扭曲模式，并给出整体建议：

        \(thoughtsList)

        请分别识别每条想法的认知扭曲类型，然后总结我的整体思维倾向，给出改善建议。
        """

        UIPasteboard.general.string = prompt
        let impact = UIImpactFeedbackGenerator(style: .medium)
        impact.impactOccurred()

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            ExternalAIAppLauncher.openChatGPT()
        }
    }

    private func deleteEntries(from list: [ThoughtEntry], at offsets: IndexSet) {
        for index in offsets {
            modelContext.delete(list[index])
        }
        try? modelContext.save()
    }
}

struct ThoughtRowView: View {
    let entry: ThoughtEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(entry.content)
                .font(.subheadline)
                .foregroundStyle(Color("TextPrimary"))

            HStack(spacing: 8) {
                if !entry.emotion.isEmpty {
                    Text(entry.emotion)
                        .font(.caption2)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color("AccentColor").opacity(0.1))
                        .foregroundStyle(Color("AccentColor"))
                        .clipShape(Capsule())
                }

                if !entry.distortionTag.isEmpty {
                    Text(entry.distortionTag)
                        .font(.caption2)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.orange.opacity(0.1))
                        .foregroundStyle(.orange)
                        .clipShape(Capsule())
                }

                Spacer()

                Text(entry.createdAt, style: .relative)
                    .font(.caption2)
                    .foregroundStyle(Color("TextSecondary"))
            }
        }
        .padding(.vertical, 4)
    }
}

struct AddThoughtSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @Bindable var viewModel: ThoughtJournalViewModel
    @FocusState private var isInputFocused: Bool

    private let emotions = ["😔 低落", "😰 焦虑", "😤 愤怒", "😟 担忧", "😞 失望", "🫠 疲惫", "😶 麻木", "😨 恐惧"]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    VStack(alignment: .leading, spacing: 8) {
                        Label("脑海中闪过的想法", systemImage: "bubble.left.and.text.bubble.right")
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(Color("TextSecondary"))

                        TextEditor(text: $viewModel.quickInput)
                            .focused($isInputFocused)
                            .frame(minHeight: 100)
                            .scrollContentBackground(.hidden)
                            .padding(12)
                            .background(Color("CardBackground"))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color(.separator).opacity(0.3), lineWidth: 1)
                            )
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Label("当时的情境（可选）", systemImage: "location")
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(Color("TextSecondary"))

                        TextField("例如：开会时被老板批评", text: $viewModel.situation)
                            .padding(12)
                            .background(Color("CardBackground"))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Label("情绪", systemImage: "heart")
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(Color("TextSecondary"))

                        LazyVGrid(columns: [GridItem(.adaptive(minimum: 80))], spacing: 8) {
                            ForEach(emotions, id: \.self) { emotion in
                                Button {
                                    withAnimation(.spring(response: 0.3)) {
                                        viewModel.selectedEmotion = viewModel.selectedEmotion == emotion ? "" : emotion
                                    }
                                } label: {
                                    Text(emotion)
                                        .font(.caption)
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 8)
                                        .frame(maxWidth: .infinity)
                                        .background(
                                            viewModel.selectedEmotion == emotion
                                                ? Color("AccentColor").opacity(0.15)
                                                : Color("CardBackground")
                                        )
                                        .foregroundStyle(
                                            viewModel.selectedEmotion == emotion
                                                ? Color("AccentColor")
                                                : Color("TextPrimary")
                                        )
                                        .clipShape(RoundedRectangle(cornerRadius: 10))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 10)
                                                .stroke(
                                                    viewModel.selectedEmotion == emotion
                                                        ? Color("AccentColor").opacity(0.3)
                                                        : Color(.separator).opacity(0.2),
                                                    lineWidth: 1
                                                )
                                        )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Label("强度", systemImage: "gauge.medium")
                                .font(.subheadline.weight(.medium))
                                .foregroundStyle(Color("TextSecondary"))
                            Spacer()
                            Text("\(Int(viewModel.intensity))/10")
                                .font(.caption.bold())
                                .foregroundStyle(Color("AccentColor"))
                        }

                        Slider(value: $viewModel.intensity, in: 1...10, step: 1)
                            .tint(Color("AccentColor"))
                    }
                }
                .padding()
            }
            .navigationTitle("记录想法")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") {
                        viewModel.quickCapture(modelContext: modelContext)
                        dismiss()
                    }
                    .disabled(viewModel.quickInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    .bold()
                }
            }
            .onAppear { isInputFocused = true }
        }
        .presentationDetents([.medium, .large])
    }
}

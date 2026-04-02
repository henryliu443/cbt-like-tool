import Foundation
import SwiftUI
import SwiftData

@Observable
final class ThoughtJournalViewModel {
    var quickInput: String = ""
    var situation: String = ""
    var selectedEmotion: String = ""
    var intensity: Double = 5
    var showAddSheet: Bool = false
    var isAnalyzing: Bool = false
    var patternReport: ThoughtPatternReport?
    var errorMessage: String?

    var settings: SettingsViewModel

    init(settings: SettingsViewModel) {
        self.settings = settings
    }

    @MainActor
    func quickCapture(modelContext: ModelContext) {
        let text = quickInput.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }

        let entry = ThoughtEntry(
            content: text,
            situation: situation,
            emotion: selectedEmotion,
            intensity: Int(intensity)
        )
        modelContext.insert(entry)
        try? modelContext.save()

        let impact = UIImpactFeedbackGenerator(style: .light)
        impact.impactOccurred()

        quickInput = ""
        situation = ""
        selectedEmotion = ""
        intensity = 5
        showAddSheet = false
    }

    @MainActor
    func analyzePatterns(entries: [ThoughtEntry], modelContext: ModelContext) async {
        let unprocessed = entries.filter { !$0.isProcessed }
        guard !unprocessed.isEmpty else {
            errorMessage = "没有待整理的想法"
            return
        }

        isAnalyzing = true
        errorMessage = nil

        let thoughtsList = unprocessed.enumerated().map { idx, entry in
            var line = "\(idx + 1). \"\(entry.content)\""
            if !entry.emotion.isEmpty { line += "（情绪: \(entry.emotion)）" }
            if !entry.situation.isEmpty { line += "（情境: \(entry.situation)）" }
            return line
        }.joined(separator: "\n")

        let prompt = """
        请分析以下自动想法列表，找出认知扭曲模式：

        \(thoughtsList)

        请按以下 JSON 格式输出，不要输出其他内容：
        {"topDistortions": [{"name": "扭曲类型名", "count": 出现次数, "example": "最典型的一条原文"}], "overallPattern": "整体思维模式总结（2-3句话）", "suggestion": "改善建议（1-2句话）"}
        注意：topDistortions 最多列出3种最常见的扭曲类型。键名必须是英文。值用中文。
        """

        do {
            let service = AIServiceFactory.service(for: settings.selectedProvider)
            let model = settings.selectedModel
            let result = try await service.reframe(
                thought: prompt,
                model: model,
                mode: .balanced,
                style: .coach,
                template: .cbtReframe
            )

            let reportText = """
            {"topDistortions": [{"name": "\(result.distortion)", "count": \(unprocessed.count), "example": "\(unprocessed.first?.content ?? "")"}], "overallPattern": "\(result.alternative)", "suggestion": "\(result.action)"}
            """

            if let data = reportText.data(using: .utf8),
               let report = try? JSONDecoder().decode(ThoughtPatternReport.self, from: data) {
                patternReport = report
            } else {
                patternReport = ThoughtPatternReport(
                    topDistortions: [
                        ThoughtPatternReport.DistortionCount(
                            name: result.distortion,
                            count: unprocessed.count,
                            example: unprocessed.first?.content ?? ""
                        )
                    ],
                    overallPattern: result.alternative,
                    suggestion: result.action
                )
            }

            for entry in unprocessed {
                entry.isProcessed = true
                entry.distortionTag = result.distortion
            }
            try? modelContext.save()

            let feedback = UINotificationFeedbackGenerator()
            feedback.notificationOccurred(.success)

        } catch let error as AIServiceError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = "分析失败：\(error.localizedDescription)"
        }

        isAnalyzing = false
    }
}

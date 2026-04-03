import Foundation
import SwiftUI
import SwiftData

@MainActor
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
    private let pipeline: AnalysisPipeline

    init(settings: SettingsViewModel, pipeline: AnalysisPipeline) {
        self.settings = settings
        self.pipeline = pipeline
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

        do {
            patternReport = try await pipeline.analyzeThoughtPatterns(entries: unprocessed)

            for entry in unprocessed {
                entry.isProcessed = true
                entry.distortionTag = patternReport?.topDistortions.first?.name ?? ""
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

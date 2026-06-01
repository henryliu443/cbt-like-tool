import Foundation
import SwiftUI
#if !SKIP
import SwiftData
#endif

@MainActor
@Observable
final class ThoughtJournalViewModel {
    var quickInput: String = ""
    var situation: String = ""
    var selectedEmotion: String = ""
    var intensity: Double = 5
    var beliefBefore: Double = 50
    var evidenceFor: String = ""
    var evidenceAgainst: String = ""
    var showAddSheet: Bool = false
    var isAnalyzing: Bool = false
    var patternReport: ThoughtPatternReport?
    var errorMessage: String?

    private let thoughtPatternPipeline: ThoughtPatternPipeline

    init(thoughtPatternPipeline: ThoughtPatternPipeline) {
        self.thoughtPatternPipeline = thoughtPatternPipeline
    }

    #if !SKIP
    @MainActor
    func quickCapture(modelContext: ModelContext) {
        _quickCapture(context: modelContext)
    }
    #else
    @MainActor
    func quickCapture() {
        _quickCapture(context: nil)
    }
    #endif

    @MainActor
    private func _quickCapture(context: Any?) {
        let text = quickInput.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }

        let entry = ThoughtEntry(
            content: text,
            situation: situation,
            emotion: selectedEmotion,
            intensity: Int(intensity),
            beliefBefore: Int(beliefBefore),
            evidenceFor: evidenceFor,
            evidenceAgainst: evidenceAgainst
        )
        #if !SKIP
        if let ctx = context as? ModelContext {
            ctx.insert(entry)
            try? ctx.save()
        }
        #endif

        HapticManager.shared.impact(style: .light)

        quickInput = ""
        situation = ""
        selectedEmotion = ""
        intensity = 5
        beliefBefore = 50
        evidenceFor = ""
        evidenceAgainst = ""
        showAddSheet = false
    }

    #if !SKIP
    @MainActor
    func analyzePatterns(entries: [ThoughtEntry], modelContext: ModelContext) async {
        await _analyzePatterns(entries: entries, context: modelContext)
    }
    #else
    @MainActor
    func analyzePatterns(entries: [ThoughtEntry]) async {
        await _analyzePatterns(entries: entries, context: nil)
    }
    #endif

    @MainActor
    private func _analyzePatterns(entries: [ThoughtEntry], context: Any?) async {
        let unprocessed = entries.filter { !$0.isProcessed }
        guard !unprocessed.isEmpty else {
            errorMessage = "没有待整理的想法"
            return
        }

        isAnalyzing = true
        errorMessage = nil

        do {
            patternReport = try await thoughtPatternPipeline.analyze(entries: unprocessed)

            for entry in unprocessed {
                entry.isProcessed = true
                entry.distortionTag = patternReport?.topDistortions.first?.name ?? ""
                entry.balancedThought = patternReport?.suggestion ?? ""
                entry.beliefAfter = max(10, entry.beliefBefore - 20)
            }
            #if !SKIP
            if let ctx = context as? ModelContext {
                try? ctx.save()
            }
            #endif

            HapticManager.success()

        } catch let error as AIServiceError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = "分析失败：\(error.localizedDescription)"
        }

        isAnalyzing = false
    }
}

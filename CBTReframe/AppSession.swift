import Foundation
import SwiftUI

@MainActor
final class AppSession: ObservableObject {
    let reframeViewModel: ReframeViewModel
    let journalViewModel: ThoughtJournalViewModel

    init(settings: SettingsViewModel, globalSettings: GlobalSettings) {
        let llmProvider = AIServiceLLMProvider(settingsViewModel: settings)
        let pipeline = AnalysisPipeline(provider: llmProvider, settingsViewModel: settings)
        reframeViewModel = ReframeViewModel(settings: settings, globalSettings: globalSettings, pipeline: pipeline)
        journalViewModel = ThoughtJournalViewModel(settings: settings, pipeline: pipeline)
    }
}

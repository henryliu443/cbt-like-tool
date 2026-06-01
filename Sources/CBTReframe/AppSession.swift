import Foundation
import SwiftUI

@MainActor
final class AppSession: ObservableObject {
    let reframeViewModel: ReframeViewModel
    let journalViewModel: ThoughtJournalViewModel

    init(settings: SettingsViewModel, globalSettings: GlobalSettings) {
        let resolver = SettingsAIProviderResolver(settingsViewModel: settings)
        let llmProvider = AIServiceLLMProvider(resolver: resolver)
        let reframePipeline = ReframePipeline(provider: llmProvider)
        let reframeUseCase = ReframeUseCase(pipeline: reframePipeline, resolver: resolver)
        let streakService = StreakService()
        let thoughtPatternPipeline = ThoughtPatternPipeline(resolver: resolver)
        reframeViewModel = ReframeViewModel(
            globalSettings: globalSettings,
            resolver: resolver,
            reframeUseCase: reframeUseCase,
            streakService: streakService
        )
        journalViewModel = ThoughtJournalViewModel(thoughtPatternPipeline: thoughtPatternPipeline)
    }
}

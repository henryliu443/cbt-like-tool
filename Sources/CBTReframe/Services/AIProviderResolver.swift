import Foundation

@MainActor
protocol AIProviderResolver {
    var selectedProvider: AIProvider { get }
    var selectedModel: AIModel { get }
}

@MainActor
struct SettingsAIProviderResolver: AIProviderResolver {
    private let settingsViewModel: SettingsViewModel

    init(settingsViewModel: SettingsViewModel) {
        self.settingsViewModel = settingsViewModel
    }

    var selectedProvider: AIProvider { settingsViewModel.selectedProvider }
    var selectedModel: AIModel { settingsViewModel.selectedModel }
}

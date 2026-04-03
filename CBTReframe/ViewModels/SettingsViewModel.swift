import Foundation
import SwiftUI
import SwiftData

@Observable
final class SettingsViewModel {
    var selectedProvider: AIProvider {
        didSet {
            UserDefaults.standard.set(selectedProvider.rawValue, forKey: "selectedProvider")
            let models = selectedProvider.availableModels
            if !models.contains(where: { $0.id == selectedModelId }) {
                selectedModelId = selectedProvider.defaultModel.id
            }
            loadAPIKey()
        }
    }

    var selectedModelId: String {
        didSet {
            UserDefaults.standard.set(selectedModelId, forKey: "selectedModelId")
        }
    }

    var reframeMode: ReframeMode {
        didSet {
            UserDefaults.standard.set(reframeMode.rawValue, forKey: "reframeMode")
        }
    }

    var responseStyle: ResponseStyle {
        didSet {
            UserDefaults.standard.set(responseStyle.rawValue, forKey: "responseStyle")
        }
    }

    var promptTemplate: PromptTemplate {
        didSet {
            UserDefaults.standard.set(promptTemplate.rawValue, forKey: "promptTemplate")
        }
    }

    var apiKeyInput: String = ""
    var useFaceID: Bool {
        didSet {
            UserDefaults.standard.set(useFaceID, forKey: "useFaceID")
        }
    }

    var selectedModel: AIModel {
        selectedProvider.availableModels.first { $0.id == selectedModelId }
            ?? selectedProvider.defaultModel
    }

    init() {
        let providerRaw = UserDefaults.standard.string(forKey: "selectedProvider") ?? AIProvider.local.rawValue
        self.selectedProvider = AIProvider(rawValue: providerRaw) ?? .local

        let modelId = UserDefaults.standard.string(forKey: "selectedModelId") ?? ""
        self.selectedModelId = modelId.isEmpty ? (AIProvider(rawValue: providerRaw) ?? .local).defaultModel.id : modelId

        let modeRaw = UserDefaults.standard.string(forKey: "reframeMode") ?? ReframeMode.balanced.rawValue
        self.reframeMode = ReframeMode(rawValue: modeRaw) ?? .balanced

        let styleRaw = UserDefaults.standard.string(forKey: "responseStyle") ?? ResponseStyle.warm.rawValue
        self.responseStyle = ResponseStyle(rawValue: styleRaw) ?? .warm

        let templateRaw = UserDefaults.standard.string(forKey: "promptTemplate") ?? PromptTemplate.cbtReframe.rawValue
        self.promptTemplate = PromptTemplate(rawValue: templateRaw) ?? .cbtReframe

        self.useFaceID = UserDefaults.standard.bool(forKey: "useFaceID")

        loadAPIKey()
    }

    func loadAPIKey() {
        apiKeyInput = KeychainManager.shared.load(key: selectedProvider.rawValue) ?? ""
    }

    func saveAPIKey() {
        let trimmed = apiKeyInput.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            KeychainManager.shared.delete(key: selectedProvider.rawValue)
        } else {
            KeychainManager.shared.save(key: selectedProvider.rawValue, value: trimmed)
        }
    }

    func clearAllData(modelContext: ModelContext) {
        KeychainManager.shared.deleteAll()
        apiKeyInput = ""

        if let historyEntries = try? modelContext.fetch(FetchDescriptor<HistoryEntry>()) {
            for entry in historyEntries {
                modelContext.delete(entry)
            }
        }

        if let thoughtEntries = try? modelContext.fetch(FetchDescriptor<ThoughtEntry>()) {
            for entry in thoughtEntries {
                modelContext.delete(entry)
            }
        }

        try? modelContext.save()

        let domain = Bundle.main.bundleIdentifier ?? "com.cbt.reframe"
        UserDefaults.standard.removePersistentDomain(forName: domain)
        selectedProvider = .local
        selectedModelId = AIProvider.local.defaultModel.id
        reframeMode = .balanced
        responseStyle = .warm
        promptTemplate = .cbtReframe
        useFaceID = false
    }

    var hasAPIKey: Bool {
        guard selectedProvider.requiresAPIKey else { return true }
        let key = KeychainManager.shared.load(key: selectedProvider.rawValue) ?? ""
        return !key.isEmpty
    }
}

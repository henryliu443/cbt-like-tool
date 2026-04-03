import Foundation
import SwiftUI
import SwiftData
import UIKit

@MainActor
@Observable
final class SettingsViewModel {
    private static let modelCacheKeyPrefix = "cachedModelList."

    /// 从 API 拉取并持久化后的模型；无缓存时用 `fallbackModels`。
    private var modelCache: [String: [AIModel]] = [:]

    var isRefreshingModels = false
    var modelsListError: String?

    var selectedProvider: AIProvider {
        didSet {
            UserDefaults.standard.set(selectedProvider.rawValue, forKey: "selectedProvider")
            let models = resolvedModels(for: selectedProvider)
            if !models.contains(where: { $0.id == selectedModelId }) {
                selectedModelId = models.first?.id ?? selectedProvider.fallbackModels.first!.id
            }
            loadAPIKey()
            Task { await refreshModels() }
        }
    }

    var selectedModelId: String {
        didSet {
            UserDefaults.standard.set(selectedModelId, forKey: "selectedModelId")
        }
    }

    var apiKeyInput: String = ""
    var isSavingAPIKey = false
    var useFaceID: Bool {
        didSet {
            UserDefaults.standard.set(useFaceID, forKey: "useFaceID")
        }
    }

    func resolvedModels(for provider: AIProvider) -> [AIModel] {
        if provider == .local { return AIProvider.local.fallbackModels }
        if let cached = modelCache[provider.rawValue], !cached.isEmpty {
            return cached
        }
        return provider.fallbackModels
    }

    var selectedModel: AIModel {
        let list = resolvedModels(for: selectedProvider)
        return list.first { $0.id == selectedModelId }
            ?? selectedProvider.fallbackModels.first!
    }

    init() {
        let providerRaw = UserDefaults.standard.string(forKey: "selectedProvider") ?? AIProvider.local.rawValue
        let provider = AIProvider(rawValue: providerRaw) ?? .local
        self.selectedProvider = provider

        let modelId = UserDefaults.standard.string(forKey: "selectedModelId") ?? ""
        self.selectedModelId = modelId.isEmpty ? provider.defaultModel.id : modelId

        self.useFaceID = UserDefaults.standard.bool(forKey: "useFaceID")

        loadModelCacheFromDisk()
        let list = resolvedModels(for: selectedProvider)
        if !list.contains(where: { $0.id == selectedModelId }) {
            selectedModelId = list.first?.id ?? provider.fallbackModels.first!.id
        }

        loadAPIKey()
        Task { await refreshModels() }
    }

    private func loadModelCacheFromDisk() {
        for p in AIProvider.allCases where p.requiresAPIKey {
            let key = Self.modelCacheKeyPrefix + p.rawValue
            guard let data = UserDefaults.standard.data(forKey: key),
                  let models = try? JSONDecoder().decode([AIModel].self, from: data) else { continue }
            modelCache[p.rawValue] = models
        }
    }

    private func persistModelCache(_ models: [AIModel], for provider: AIProvider) {
        modelCache[provider.rawValue] = models
        if let data = try? JSONEncoder().encode(models) {
            UserDefaults.standard.set(data, forKey: Self.modelCacheKeyPrefix + provider.rawValue)
        }
    }

    func loadAPIKey() {
        apiKeyInput = KeychainManager.shared.load(key: selectedProvider.rawValue) ?? ""
    }

    /// 从服务商拉取最新模型列表（需已在 Keychain 中保存有效 Key）。
    func refreshModels() async {
        guard selectedProvider.requiresAPIKey else {
            modelsListError = nil
            return
        }
        let key = KeychainManager.shared.load(key: selectedProvider.rawValue) ?? ""
        let trimmed = key.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            modelsListError = nil
            return
        }

        isRefreshingModels = true
        modelsListError = nil
        defer { isRefreshingModels = false }

        do {
            let models = try await AIModelListService.fetchModels(provider: selectedProvider, apiKey: trimmed)
            if !models.isEmpty {
                persistModelCache(models, for: selectedProvider)
                if !models.contains(where: { $0.id == selectedModelId }) {
                    selectedModelId = models[0].id
                }
            }
        } catch {
            modelsListError = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    func saveAPIKey() {
        let trimmed = apiKeyInput.trimmingCharacters(in: .whitespacesAndNewlines)
        let providerKey = selectedProvider.rawValue
        guard !isSavingAPIKey else { return }
        isSavingAPIKey = true
        Task {
            await Task.detached {
                if trimmed.isEmpty {
                    KeychainManager.shared.delete(key: providerKey)
                } else {
                    KeychainManager.shared.save(key: providerKey, value: trimmed)
                }
            }.value
            isSavingAPIKey = false
            let feedback = UINotificationFeedbackGenerator()
            feedback.notificationOccurred(.success)
            await refreshModels()
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

        for p in AIProvider.allCases where p.requiresAPIKey {
            UserDefaults.standard.removeObject(forKey: Self.modelCacheKeyPrefix + p.rawValue)
        }
        modelCache.removeAll()

        let domain = Bundle.main.bundleIdentifier ?? "com.cbt.reframe"
        UserDefaults.standard.removePersistentDomain(forName: domain)
        selectedProvider = .local
        selectedModelId = AIProvider.local.defaultModel.id
        useFaceID = false
    }

    var hasAPIKey: Bool {
        guard selectedProvider.requiresAPIKey else { return true }
        let key = KeychainManager.shared.load(key: selectedProvider.rawValue) ?? ""
        return !key.isEmpty
    }
}

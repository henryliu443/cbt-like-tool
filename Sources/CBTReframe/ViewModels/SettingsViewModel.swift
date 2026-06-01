import Foundation
import SwiftUI
#if !SKIP
import SwiftData
import LocalAuthentication
#else
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.lang.CharSequence
#endif

@MainActor
protocol ReminderScheduling {
    func requestPermission() async -> Bool
    func scheduleDailyReminder(hour: Int, minute: Int) async
    func cancelDailyReminder()
}

@MainActor
struct DefaultReminderScheduler: ReminderScheduling {
    func requestPermission() async -> Bool {
        await ReminderService.requestPermission()
    }

    func scheduleDailyReminder(hour: Int, minute: Int) async {
        await ReminderService.scheduleDailyReminder(hour: hour, minute: minute)
    }

    func cancelDailyReminder() {
        ReminderService.cancelDailyReminder()
    }
}

@MainActor
@Observable
final class SettingsViewModel {
    private let historyRepository: HistoryRepository
    private let thoughtRepository: ThoughtRepository
    private let moodRepository: MoodRepository

    private static let modelCacheKeyPrefix = "cachedModelList."
    private let reminderScheduler: ReminderScheduling
    private var reminderSyncTask: Task<Void, Never>?

    /// 从 API 拉取并持久化后的模型；无缓存时用 `fallbackModels`。
    private var modelCache: [String: [AIModel]] = [:]

    var isRefreshingModels = false
    var modelsListError: String?

    var selectedProvider: AIProvider {
        didSet {
            UserDefaults.standard.set(selectedProvider.rawValue, forKey: "selectedProvider")
            let models = resolvedModels(for: selectedProvider)
            if !models.contains(where: { $0.id == selectedModelId }) {
                selectedModelId = selectedProvider.resolveDefaultModelId(from: models)
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
    var dailyReminderEnabled: Bool {
        didSet {
            UserDefaults.standard.set(dailyReminderEnabled, forKey: "dailyReminderEnabled")
            syncReminderSchedule()
        }
    }
    var reminderHour: Int {
        didSet {
            UserDefaults.standard.set(reminderHour, forKey: "reminderHour")
            syncReminderSchedule()
        }
    }
    var reminderMinute: Int {
        didSet {
            UserDefaults.standard.set(reminderMinute, forKey: "reminderMinute")
            syncReminderSchedule()
        }
    }
    var hasAcceptedDisclaimer: Bool {
        didSet {
            UserDefaults.standard.set(hasAcceptedDisclaimer, forKey: "hasAcceptedDisclaimer")
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

    @MainActor
    init(
        historyRepository: HistoryRepository,
        thoughtRepository: ThoughtRepository,
        moodRepository: MoodRepository,
        reminderScheduler: ReminderScheduling? = nil
    ) {
        self.historyRepository = historyRepository
        self.thoughtRepository = thoughtRepository
        self.moodRepository = moodRepository
        self.reminderScheduler = reminderScheduler ?? DefaultReminderScheduler()
        let providerRaw = UserDefaults.standard.string(forKey: "selectedProvider") ?? AIProvider.gemini.rawValue
        let provider = AIProvider(rawValue: providerRaw) ?? .local
        self.selectedProvider = provider

        let modelId = UserDefaults.standard.string(forKey: "selectedModelId") ?? ""
        self.selectedModelId = modelId.isEmpty ? provider.defaultModel.id : modelId

        self.useFaceID = UserDefaults.standard.bool(forKey: "useFaceID")
        self.dailyReminderEnabled = UserDefaults.standard.bool(forKey: "dailyReminderEnabled")
        self.reminderHour = UserDefaults.standard.object(forKey: "reminderHour") as? Int ?? 21
        self.reminderMinute = UserDefaults.standard.object(forKey: "reminderMinute") as? Int ?? 0
        self.hasAcceptedDisclaimer = UserDefaults.standard.bool(forKey: "hasAcceptedDisclaimer")

        loadModelCacheFromDisk()
        let list = resolvedModels(for: selectedProvider)
        if !list.contains(where: { $0.id == selectedModelId }) {
            selectedModelId = provider.resolveDefaultModelId(from: list)
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
                    selectedModelId = selectedProvider.resolveDefaultModelId(from: models)
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
            HapticManager.success()
            await refreshModels()
        }
    }

    func clearAllData() {
        KeychainManager.shared.deleteAll()
        apiKeyInput = ""

        Task { @MainActor in
            try? await historyRepository.deleteAll()
            try? await thoughtRepository.deleteAll()
            try? await moodRepository.deleteAll()
        }

        for p in AIProvider.allCases where p.requiresAPIKey {
            UserDefaults.standard.removeObject(forKey: Self.modelCacheKeyPrefix + p.rawValue)
        }
        modelCache.removeAll()

        #if !SKIP
        let domain = Bundle.main.bundleIdentifier ?? "com.cbt.reframe"
        UserDefaults.standard.removePersistentDomain(forName: domain)
        #endif
        selectedProvider = .local
        selectedModelId = AIProvider.local.defaultModel.id
        useFaceID = false
        dailyReminderEnabled = false
        reminderHour = 21
        reminderMinute = 0
        hasAcceptedDisclaimer = false
    }

    var hasAPIKey: Bool {
        guard selectedProvider.requiresAPIKey else { return true }
        let key = KeychainManager.shared.load(key: selectedProvider.rawValue) ?? ""
        return !key.isEmpty
    }

    func authenticateWithBiometrics(reason: String) async -> Bool {
        #if !SKIP
        let context = LAContext()
        var error: NSError?
        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
            return false
        }
        do {
            return try await context.evaluatePolicy(
                .deviceOwnerAuthenticationWithBiometrics,
                localizedReason: reason
            )
        } catch {
            return false
        }
        #else
        return await withCheckedContinuation { continuation in
            // Fallback for Android via Skip
            // Assuming we can retrieve the current FragmentActivity. In a complete Skip App, 
            // the activity is accessible or injected. Here we provide the standard BiometricPrompt structure.
            guard let activity = Skip.App.currentActivity as? androidx.fragment.app.FragmentActivity else {
                continuation.resume(returning: false)
                return
            }
            
            let callback = BiometricCallback(continuation)
            
            let executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
            let prompt = androidx.biometric.BiometricPrompt(activity, executor, callback)
            let promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authentication Required")
                .setSubtitle(reason)
                .setNegativeButtonText("Cancel")
                .build()
                
            prompt.authenticate(promptInfo)
        }
        #endif
    }

    private func syncReminderSchedule() {
        reminderSyncTask?.cancel()
        let enabled = dailyReminderEnabled
        let hour = reminderHour
        let minute = reminderMinute
        let scheduler = reminderScheduler

        reminderSyncTask = Task { @MainActor in
            if enabled {
                _ = await scheduler.requestPermission()
                guard !Task.isCancelled else { return }
                await scheduler.scheduleDailyReminder(hour: hour, minute: minute)
            } else {
                scheduler.cancelDailyReminder()
            }
        }
    }
}

#if SKIP
class BiometricCallback: androidx.biometric.BiometricPrompt.AuthenticationCallback {
    let cont: CheckedContinuation<Bool, Never>
    var didResume = false
    init(_ cont: CheckedContinuation<Bool, Never>) { self.cont = cont }
    
    override func onAuthenticationError(_ errorCode: Int32, _ errString: java.lang.CharSequence) {
        if !didResume { didResume = true; cont.resume(returning: false) }
    }
    override func onAuthenticationSucceeded(_ result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
        if !didResume { didResume = true; cont.resume(returning: true) }
    }
    override func onAuthenticationFailed() {
        // Do nothing on fail, let the user retry
    }
}
#endif

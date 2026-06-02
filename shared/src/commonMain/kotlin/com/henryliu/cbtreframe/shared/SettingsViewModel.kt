package com.henryliu.cbtreframe.shared

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val selectedProvider: AIProvider = AIProvider.GEMINI,
    val selectedModelId: String = "",
    val apiKeyInput: String = "",
    val isSavingAPIKey: Boolean = false,
    val isRefreshingModels: Boolean = false,
    val modelsListError: String? = null,
    val useFaceID: Boolean = false,
    val dailyReminderEnabled: Boolean = false,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
    val hasAcceptedDisclaimer: Boolean = false,
    val resolvedModels: List<AIModel> = emptyList(),
    val hasAPIKey: Boolean = false,
) {
    val selectedModel: AIModel
        get() = resolvedModels.firstOrNull { it.modelName == selectedModelId }
            ?: resolvedModels.firstOrNull()
            ?: AIModel.GEMINI_FLASH_LATEST
}

interface KeychainProvider {
    fun load(key: String): String?
    fun save(key: String, value: String)
    fun delete(key: String)
    fun deleteAll()
}

interface ReminderScheduler {
    suspend fun requestPermission(): Boolean
    suspend fun scheduleDailyReminder(hour: Int, minute: Int)
    fun cancelDailyReminder()
}

class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val keychainProvider: KeychainProvider,
    private val biometricAuthProvider: BiometricAuthProvider,
    private val reminderScheduler: ReminderScheduler,
    private val modelFetcher: ModelFetcher,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var reminderSyncJob: Job? = null

    init {
        // Load persisted state
        val provider = settingsManager.getSelectedProvider()
        val modelId = settingsManager.getSelectedModelId().let {
            it.ifEmpty { provider.defaultModelId() }
        }

        val models = loadResolvedModels(provider)

        val savedModelId = if (models.any { m -> m.modelName == modelId }) modelId
            else provider.defaultModelId()

        _uiState.value = _uiState.value.copy(
            selectedProvider = provider,
            selectedModelId = savedModelId,
            apiKeyInput = keychainProvider.load(provider.name) ?: "",
            useFaceID = settingsManager.getUseFaceID(),
            dailyReminderEnabled = settingsManager.getDailyReminderEnabled(),
            reminderHour = settingsManager.getReminderHour(),
            reminderMinute = settingsManager.getReminderMinute(),
            hasAcceptedDisclaimer = settingsManager.getHasAcceptedDisclaimer(),
            resolvedModels = models,
            hasAPIKey = computeHasAPIKey(provider),
        )

        scope.launch { refreshModels() }
    }

    fun clear() {
        scope.cancel()
    }

    // ── Provider selection ─────────────────────────────────────────────

    fun selectProvider(provider: AIProvider) {
        val models = loadResolvedModels(provider)
        val modelId = if (models.any { it.modelName == _uiState.value.selectedModelId })
            _uiState.value.selectedModelId
        else
            provider.defaultModelId()

        settingsManager.setSelectedProvider(provider)
        settingsManager.setSelectedModelId(modelId)

        _uiState.value = _uiState.value.copy(
            selectedProvider = provider,
            selectedModelId = modelId,
            apiKeyInput = keychainProvider.load(provider.name) ?: "",
            resolvedModels = models,
            hasAPIKey = computeHasAPIKey(provider),
        )

        scope.launch { refreshModels() }
    }

    fun selectModel(modelId: String) {
        settingsManager.setSelectedModelId(modelId)
        _uiState.value = _uiState.value.copy(selectedModelId = modelId)
    }

    // ── API Key ────────────────────────────────────────────────────────

    fun updateApiKeyInput(value: String) {
        _uiState.value = _uiState.value.copy(apiKeyInput = value)
    }

    fun saveAPIKey() {
        val trimmed = _uiState.value.apiKeyInput.trim()
        val provider = _uiState.value.selectedProvider
        val providerKey = provider.name

        if (_uiState.value.isSavingAPIKey) return

        _uiState.value = _uiState.value.copy(isSavingAPIKey = true)

        scope.launch {
            withContext(Dispatchers.Default) {
                if (trimmed.isEmpty()) {
                    keychainProvider.delete(providerKey)
                } else {
                    keychainProvider.save(providerKey, trimmed)
                }
            }
            _uiState.value = _uiState.value.copy(
                isSavingAPIKey = false,
                hasAPIKey = computeHasAPIKey(provider),
            )
            refreshModels()
        }
    }

    // ── Model refresh ──────────────────────────────────────────────────

    suspend fun refreshModels() {
        val provider = _uiState.value.selectedProvider
        if (!provider.requiresApiKey()) {
            val fallback = provider.fallbackModels()
            _uiState.value = _uiState.value.copy(
                resolvedModels = fallback,
                modelsListError = null,
            )
            return
        }

        val key = (keychainProvider.load(provider.name) ?: "").trim()
        if (key.isEmpty()) {
            _uiState.value = _uiState.value.copy(modelsListError = null)
            return
        }

        _uiState.value = _uiState.value.copy(
            isRefreshingModels = true,
            modelsListError = null,
        )

        try {
            val models = modelFetcher.fetchModels(provider, key)
            if (models.isNotEmpty()) {
                settingsManager.setCachedModels(provider, models)
                val currentModelId = _uiState.value.selectedModelId
                val newModelId = if (models.any { it.modelName == currentModelId }) currentModelId
                    else provider.defaultModelId()
                settingsManager.setSelectedModelId(newModelId)

                _uiState.value = _uiState.value.copy(
                    resolvedModels = models,
                    selectedModelId = newModelId,
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                modelsListError = e.message ?: "Unknown error",
            )
        } finally {
            _uiState.value = _uiState.value.copy(isRefreshingModels = false)
        }
    }

    // ── Toggles ────────────────────────────────────────────────────────

    fun setUseFaceID(enabled: Boolean) {
        settingsManager.setUseFaceID(enabled)
        _uiState.value = _uiState.value.copy(useFaceID = enabled)
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        settingsManager.setDailyReminderEnabled(enabled)
        _uiState.value = _uiState.value.copy(dailyReminderEnabled = enabled)
        syncReminderSchedule()
    }

    fun setReminderHour(hour: Int) {
        settingsManager.setReminderHour(hour)
        _uiState.value = _uiState.value.copy(reminderHour = hour)
        syncReminderSchedule()
    }

    fun setReminderMinute(minute: Int) {
        settingsManager.setReminderMinute(minute)
        _uiState.value = _uiState.value.copy(reminderMinute = minute)
        syncReminderSchedule()
    }

    fun setHasAcceptedDisclaimer(accepted: Boolean) {
        settingsManager.setHasAcceptedDisclaimer(accepted)
        _uiState.value = _uiState.value.copy(hasAcceptedDisclaimer = accepted)
    }

    // ── Face ID auth ───────────────────────────────────────────────────

    suspend fun authenticateWithBiometrics(reason: String): Boolean {
        return biometricAuthProvider.authenticate(reason, "")
    }

    val canUseBiometrics: Boolean
        get() = biometricAuthProvider.canAuthenticate()

    // ── Clear all ──────────────────────────────────────────────────────

    fun clearAllData(onClearDatabase: suspend () -> Unit) {
        keychainProvider.deleteAll()
        settingsManager.clearAll()
        _uiState.value = SettingsUiState(
            selectedProvider = AIProvider.LOCAL,
            selectedModelId = AIProvider.LOCAL.defaultModelId(),
            resolvedModels = AIProvider.LOCAL.fallbackModels(),
        )
        scope.launch { onClearDatabase() }
    }

    // ── Private helpers ────────────────────────────────────────────────

    private fun loadResolvedModels(provider: AIProvider): List<AIModel> {
        if (provider == AIProvider.LOCAL) return provider.fallbackModels()
        val cached = settingsManager.getCachedModels(provider)
        return if (cached.isNullOrEmpty()) provider.fallbackModels() else cached
    }

    private fun computeHasAPIKey(provider: AIProvider): Boolean {
        if (!provider.requiresApiKey()) return true
        val key = keychainProvider.load(provider.name) ?: ""
        return key.isNotEmpty()
    }

    private fun syncReminderSchedule() {
        reminderSyncJob?.cancel()
        val enabled = _uiState.value.dailyReminderEnabled
        val hour = _uiState.value.reminderHour
        val minute = _uiState.value.reminderMinute

        reminderSyncJob = scope.launch {
            if (enabled) {
                val granted = reminderScheduler.requestPermission()
                if (!granted || !isActive) return@launch
                reminderScheduler.scheduleDailyReminder(hour, minute)
            } else {
                reminderScheduler.cancelDailyReminder()
            }
        }
    }
}

// ── Extension helpers ─────────────────────────────────────────────────────

fun AIProvider.requiresApiKey(): Boolean = this != AIProvider.LOCAL

fun AIProvider.defaultModelId(): String {
    return when (this) {
        AIProvider.DEEPSEEK -> AIModel.DEEPSEEK_CHAT.modelName
        AIProvider.OPENAI -> AIModel.GPT_4O.modelName
        AIProvider.ANTHROPIC -> AIModel.CLAUDE_SONNET_4.modelName
        AIProvider.GEMINI -> AIModel.GEMINI_FLASH_LATEST.modelName
        AIProvider.KIMI -> AIModel.MOONSHOT_V1_8K.modelName
        AIProvider.LOCAL -> AIModel.LOCAL_BUILTIN.modelName
    }
}

fun AIProvider.fallbackModels(): List<AIModel> {
    return when (this) {
        AIProvider.DEEPSEEK -> listOf(AIModel.DEEPSEEK_CHAT, AIModel.DEEPSEEK_REASONER)
        AIProvider.OPENAI -> listOf(AIModel.GPT_4O)
        AIProvider.ANTHROPIC -> listOf(AIModel.CLAUDE_SONNET_4, AIModel.CLAUDE_3_5_HAIKU)
        AIProvider.GEMINI -> listOf(AIModel.GEMINI_FLASH_LATEST, AIModel.GEMINI_2_5_FLASH, AIModel.GEMINI_2_0_FLASH, AIModel.GEMINI_1_5_PRO)
        AIProvider.KIMI -> listOf(AIModel.MOONSHOT_V1_8K, AIModel.MOONSHOT_V1_32K, AIModel.KIMI_K2_TURBO, AIModel.KIMI_K2_THINKING)
        AIProvider.LOCAL -> listOf(AIModel.LOCAL_BUILTIN)
    }
}

// ── Model fetching interface ───────────────────────────────────────────────

interface ModelFetcher {
    suspend fun fetchModels(provider: AIProvider, apiKey: String): List<AIModel>
}

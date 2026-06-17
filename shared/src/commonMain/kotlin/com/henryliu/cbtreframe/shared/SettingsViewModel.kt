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
    val modelInvalidated: Boolean = false,
    val invalidatedModelName: String? = null,
) {
    val selectedModel: AIModel
        get() = resolvedModels.firstOrNull { it.modelName == selectedModelId }
            ?: resolvedModels.firstOrNull()
            ?: AIModel(AIProvider.GEMINI, "gemini-flash-latest", "Gemini Flash Latest")
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
        println("SELECT_PROVIDER provider=$provider")
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

        println("SELECT_PROVIDER launching refreshModels")
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

    suspend fun validateAndFetchModels(provider: AIProvider, apiKey: String): Result<Unit> {
        return try {
            println("INIT-1 start")
            // 1. Save provider and API key
            println("INIT: saving provider")
            settingsManager.setSelectedProvider(provider)
            println("INIT-2 provider saved")
            val providerKey = provider.name
            if (provider.requiresApiKey()) {
                if (apiKey.isBlank()) {
                    return Result.failure(Exception("API Key 不能为空"))
                }
                println("INIT: saving api key")
                withContext(Dispatchers.Default) {
                    keychainProvider.save(providerKey, apiKey.trim())
                }
            } else {
                println("INIT: saving api key")
                withContext(Dispatchers.Default) {
                    keychainProvider.delete(providerKey)
                }
            }
            println("INIT-3 key saved")

            // Sync ViewModel UI State locally
            _uiState.value = _uiState.value.copy(
                selectedProvider = provider,
                apiKeyInput = apiKey,
                hasAPIKey = computeHasAPIKey(provider)
            )
            println("INIT-4 ui updated")

            // 2. Fetch models & cache
            if (provider.requiresApiKey()) {
                println("INIT-5 before fetchModels")
                val models = withTimeout(15000) {
                    modelFetcher.fetchModels(provider, apiKey.trim())
                }
                println("INIT-6 after fetchModels")
                if (models.isEmpty()) {
                    return Result.failure(Exception("未能获取到任何可用模型，请检查配置"))
                }
                println("INIT: caching models")
                settingsManager.setCachedModels(provider, models)
                val currentModelId = _uiState.value.selectedModelId
                val newModelId = if (models.any { it.modelName == currentModelId }) currentModelId
                    else provider.resolveDefaultModelId(models)

                _uiState.value = _uiState.value.copy(
                    resolvedModels = models,
                    selectedModelId = newModelId,
                    modelsListError = null
                )
            } else {
                val fallback = provider.fallbackModels()
                val newModelId = provider.resolveDefaultModelId(fallback)
                _uiState.value = _uiState.value.copy(
                    resolvedModels = fallback,
                    selectedModelId = newModelId,
                    modelsListError = null
                )
            }

            println("INIT-7 success")
            Result.success(Unit)
        } catch (e: Exception) {
            println("INIT: validateAndFetchModels failed: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            _uiState.value = _uiState.value.copy(
                modelsListError = e.message ?: "Unknown error"
            )
            Result.failure(e)
        }
    }

    fun completeOnboarding(selectedModelId: String) {
        settingsManager.setSelectedModelId(selectedModelId)
        settingsManager.setHasAcceptedDisclaimer(true)
        _uiState.value = _uiState.value.copy(
            selectedModelId = selectedModelId,
            hasAcceptedDisclaimer = true
        )
    }

    // ── Model refresh ──────────────────────────────────────────────────

    suspend fun refreshModels() {
        println("REFRESH: enter")
        val provider = _uiState.value.selectedProvider
        println("REFRESH: provider=$provider")
        println("REFRESH: requiresApiKey=${provider.requiresApiKey()}")
        if (!provider.requiresApiKey()) {
            val fallback = provider.fallbackModels()
            _uiState.value = _uiState.value.copy(
                resolvedModels = fallback,
                modelsListError = null,
            )
            return
        }

        val key = (keychainProvider.load(provider.name) ?: "").trim()
        println("REFRESH: keyLoaded length=${key.length}")
        if (key.isEmpty()) {
            _uiState.value = _uiState.value.copy(modelsListError = null)
            return
        }

        _uiState.value = _uiState.value.copy(
            isRefreshingModels = true,
            modelsListError = null,
        )

        try {
            println("REFRESH: before fetchModels")
            val models = modelFetcher.fetchModels(provider, key)
            println("REFRESH: after fetchModels count=${models.size}")
            if (models.isNotEmpty()) {
                settingsManager.setCachedModels(provider, models)
                val currentModelId = _uiState.value.selectedModelId
                val wasInvalidated = currentModelId.isNotEmpty() && models.none { it.modelName == currentModelId }

                val newModelId = if (!wasInvalidated && models.any { it.modelName == currentModelId }) {
                    currentModelId
                } else {
                    provider.resolveDefaultModelId(models)
                }

                settingsManager.setSelectedModelId(newModelId)

                _uiState.value = _uiState.value.copy(
                    resolvedModels = models,
                    selectedModelId = newModelId,
                    modelInvalidated = wasInvalidated,
                    invalidatedModelName = if (wasInvalidated) currentModelId else null
                )
            }
            println("REFRESH: success path finished")
        } catch (e: Exception) {
            println("REFRESH: exception=${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            _uiState.value = _uiState.value.copy(
                modelsListError = e.message ?: "Unknown error",
            )
        } finally {
            println("REFRESH: finally")
            _uiState.value = _uiState.value.copy(isRefreshingModels = false)
        }
    }

    fun dismissModelInvalidationBanner() {
        _uiState.value = _uiState.value.copy(modelInvalidated = false, invalidatedModelName = null)
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
    return fallbackModels().first().modelName
}

fun AIProvider.resolveDefaultModelId(models: List<AIModel>): String {
    val firstId = models.firstOrNull()?.modelName ?: defaultModelId()
    if (this != AIProvider.GEMINI) return firstId
    
    val priority = listOf(
        "gemini-flash-latest",
        "gemini-2.5-flash",
        "gemini-2.0-flash",
        "gemini-2.0-flash-lite",
        "gemini-1.5-flash"
    )
    val idSet = models.map { it.modelName }.toSet()
    for (id in priority) {
        if (idSet.contains(id)) return id
    }
    return firstId
}

fun AIProvider.fallbackModels(): List<AIModel> {
    return when (this) {
        AIProvider.OPENAI -> listOf(
            AIModel(this, "gpt-4.1", "GPT-4.1"),
            AIModel(this, "gpt-4.1-mini", "GPT-4.1 Mini"),
            AIModel(this, "gpt-4.1-nano", "GPT-4.1 Nano"),
            AIModel(this, "gpt-4o", "GPT-4o"),
            AIModel(this, "gpt-4o-mini", "GPT-4o Mini")
        )
        AIProvider.ANTHROPIC -> listOf(
            AIModel(this, "claude-sonnet-4-20250514", "Claude Sonnet 4"),
            AIModel(this, "claude-3-5-haiku-20241022", "Claude 3.5 Haiku")
        )
        AIProvider.DEEPSEEK -> listOf(
            AIModel(this, "deepseek-chat", "DeepSeek Chat"),
            AIModel(this, "deepseek-reasoner", "DeepSeek Reasoner")
        )
        AIProvider.GEMINI -> listOf(
            AIModel(this, "gemini-flash-latest", "Gemini Flash Latest"),
            AIModel(this, "gemini-2.5-flash", "Gemini 2.5 Flash"),
            AIModel(this, "gemini-2.0-flash", "Gemini 2.0 Flash"),
            AIModel(this, "gemini-2.0-flash-lite", "Gemini 2.0 Flash-Lite"),
            AIModel(this, "gemini-1.5-flash", "Gemini 1.5 Flash"),
            AIModel(this, "gemini-1.5-pro", "Gemini 1.5 Pro")
        )
        AIProvider.KIMI -> listOf(
            AIModel(this, "moonshot-v1-8k", "Moonshot v1 8K"),
            AIModel(this, "moonshot-v1-32k", "Moonshot v1 32K"),
            AIModel(this, "kimi-k2-turbo-preview", "Kimi K2 Turbo"),
            AIModel(this, "kimi-k2-thinking-preview", "Kimi K2 Thinking")
        )
        AIProvider.LOCAL -> listOf(
            AIModel(this, "local", "内置分析")
        )
    }
}

// ── Model fetching interface ───────────────────────────────────────────────

interface ModelFetcher {
    suspend fun fetchModels(provider: AIProvider, apiKey: String): List<AIModel>
}

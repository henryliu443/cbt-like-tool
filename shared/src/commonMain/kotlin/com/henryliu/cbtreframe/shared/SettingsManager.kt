package com.henryliu.cbtreframe.shared

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.builtins.ListSerializer

class SettingsManager(private val settings: Settings) {
    companion object {
        private const val KEY_SELECTED_PROVIDER = "selectedProvider"
        private const val KEY_SELECTED_MODEL_ID = "selectedModelId"
        private const val KEY_USE_FACE_ID = "useFaceID"
        private const val KEY_DAILY_REMINDER_ENABLED = "dailyReminderEnabled"
        private const val KEY_REMINDER_HOUR = "reminderHour"
        private const val KEY_REMINDER_MINUTE = "reminderMinute"
        private const val KEY_HAS_ACCEPTED_DISCLAIMER = "hasAcceptedDisclaimer"
        private const val KEY_MODEL_CACHE_PREFIX = "cachedModelList."
    }

    private val json = Json { ignoreUnknownKeys = true }

    // ── Provider ──────────────────────────────────────────────────────────

    fun getSelectedProvider(): AIProvider {
        val raw = settings.getString(KEY_SELECTED_PROVIDER, AIProvider.GEMINI.name)
        return try { AIProvider.valueOf(raw) } catch (_: Exception) { AIProvider.LOCAL }
    }

    fun setSelectedProvider(provider: AIProvider) {
        settings.putString(KEY_SELECTED_PROVIDER, provider.name)
    }

    // ── Model ID ──────────────────────────────────────────────────────────

    fun getSelectedModelId(): String {
        return settings.getString(KEY_SELECTED_MODEL_ID, "")
    }

    fun setSelectedModelId(modelId: String) {
        settings.putString(KEY_SELECTED_MODEL_ID, modelId)
    }

    // ── Face ID ───────────────────────────────────────────────────────────

    fun getUseFaceID(): Boolean {
        return settings.getBoolean(KEY_USE_FACE_ID, false)
    }

    fun setUseFaceID(enabled: Boolean) {
        settings.putBoolean(KEY_USE_FACE_ID, enabled)
    }

    // ── Daily Reminder ────────────────────────────────────────────────────

    fun getDailyReminderEnabled(): Boolean {
        return settings.getBoolean(KEY_DAILY_REMINDER_ENABLED, false)
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_DAILY_REMINDER_ENABLED, enabled)
    }

    fun getReminderHour(): Int {
        return settings.getInt(KEY_REMINDER_HOUR, 21)
    }

    fun setReminderHour(hour: Int) {
        settings.putInt(KEY_REMINDER_HOUR, hour)
    }

    fun getReminderMinute(): Int {
        return settings.getInt(KEY_REMINDER_MINUTE, 0)
    }

    fun setReminderMinute(minute: Int) {
        settings.putInt(KEY_REMINDER_MINUTE, minute)
    }

    // ── Disclaimer ────────────────────────────────────────────────────────

    fun getHasAcceptedDisclaimer(): Boolean {
        return settings.getBoolean(KEY_HAS_ACCEPTED_DISCLAIMER, false)
    }

    fun setHasAcceptedDisclaimer(accepted: Boolean) {
        settings.putBoolean(KEY_HAS_ACCEPTED_DISCLAIMER, accepted)
    }

    // ── Model Cache ───────────────────────────────────────────────────────

    fun getCachedModels(provider: AIProvider): List<AIModel>? {
        val key = KEY_MODEL_CACHE_PREFIX + provider.name
        val raw = settings.getStringOrNull(key) ?: return null
        return try {
            json.decodeFromString<List<AIModelInfo>>(raw).map { info ->
                FallbackModels.entries.firstOrNull { it.modelName == info.modelName }
                    ?: info.toAIModel(provider)
            }
        } catch (_: Exception) { null }
    }

    fun setCachedModels(provider: AIProvider, models: List<AIModel>) {
        val key = KEY_MODEL_CACHE_PREFIX + provider.name
        val infos = models.map { AIModelInfo(it.modelName) }
        settings.putString(key, json.encodeToString(infos))
    }

    fun clearAll() {
        settings.clear()
    }

    fun remove(key: String) {
        settings.remove(key)
    }

    fun getString(key: String, default: String = ""): String {
        return settings.getString(key, default)
    }

    fun putString(key: String, value: String) {
        settings.putString(key, value)
    }

    fun loadSettings(): GlobalSettings {
        val templateRaw = settings.getString("thinkingTemplate", ThinkingTemplate.cbt.name)
        val template = ThinkingTemplate.entries.firstOrNull { it.name == templateRaw } ?: ThinkingTemplate.cbt

        val depthRaw = settings.getString("analysisDepth", ThinkingTemplate.AnalysisDepth.balanced.name)
        val depth = ThinkingTemplate.AnalysisDepth.entries.firstOrNull { it.name == depthRaw } ?: ThinkingTemplate.AnalysisDepth.balanced

        val styleRaw = settings.getString("responseStyle", ThinkingTemplate.AppResponseStyle.supportive.name)
        val style = ThinkingTemplate.AppResponseStyle.entries.firstOrNull { it.name == styleRaw } ?: ThinkingTemplate.AppResponseStyle.supportive

        return GlobalSettings(
            thinkingTemplate = template,
            analysisDepth = depth,
            responseStyle = style
        )
    }
}

@kotlinx.serialization.Serializable
private data class AIModelInfo(
    val modelName: String,
) {
    fun toAIModel(provider: AIProvider): AIModel {
        return FallbackModels.entries.firstOrNull { it.provider == provider && it.modelName == modelName }
            ?: AIModel(provider, modelName, prettyGenericName(modelName))
    }
}

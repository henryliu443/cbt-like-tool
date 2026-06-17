package com.henryliu.cbtreframe.shared

import kotlinx.serialization.Serializable

enum class AIProvider {
    DEEPSEEK,
    OPENAI,
    ANTHROPIC,
    GEMINI,
    KIMI,
    LOCAL
}

@Serializable
data class AIModel(
    val provider: AIProvider,
    val modelName: String,
    val displayName: String,
    val isReasoning: Boolean = false,
    val isPremium: Boolean = false,
)

object ModelDisplayDictionary {
    private val DEEPSEEK_CHAT = AIModel(AIProvider.DEEPSEEK, "deepseek-chat", "DeepSeek Chat")
    private val DEEPSEEK_REASONER = AIModel(AIProvider.DEEPSEEK, "deepseek-reasoner", "DeepSeek Reasoner", isReasoning = true)
    private val GPT_4O = AIModel(AIProvider.OPENAI, "gpt-4o", "GPT-4o")
    private val GPT_4O_MINI = AIModel(AIProvider.OPENAI, "gpt-4o-mini", "GPT-4o Mini")
    private val GPT_4_1 = AIModel(AIProvider.OPENAI, "gpt-4.1", "GPT-4.1")
    private val GPT_4_1_MINI = AIModel(AIProvider.OPENAI, "gpt-4.1-mini", "GPT-4.1 Mini")
    private val GPT_4_1_NANO = AIModel(AIProvider.OPENAI, "gpt-4.1-nano", "GPT-4.1 Nano")
    private val CLAUDE_SONNET_4 = AIModel(AIProvider.ANTHROPIC, "claude-sonnet-4-20250514", "Claude Sonnet 4")
    private val CLAUDE_3_5_HAIKU = AIModel(AIProvider.ANTHROPIC, "claude-3-5-haiku-20241022", "Claude 3.5 Haiku")
    private val GEMINI_FLASH_LATEST = AIModel(AIProvider.GEMINI, "gemini-flash-latest", "Gemini Flash Latest")
    private val GEMINI_2_5_FLASH = AIModel(AIProvider.GEMINI, "gemini-2.5-flash", "Gemini 2.5 Flash")
    private val GEMINI_2_0_FLASH = AIModel(AIProvider.GEMINI, "gemini-2.0-flash", "Gemini 2.0 Flash")
    private val GEMINI_2_0_FLASH_LITE = AIModel(AIProvider.GEMINI, "gemini-2.0-flash-lite", "Gemini 2.0 Flash-Lite")
    private val GEMINI_1_5_PRO = AIModel(AIProvider.GEMINI, "gemini-1.5-pro", "Gemini 1.5 Pro", isPremium = true)
    private val GEMINI_1_5_FLASH = AIModel(AIProvider.GEMINI, "gemini-1.5-flash", "Gemini 1.5 Flash")
    private val MOONSHOT_V1_8K = AIModel(AIProvider.KIMI, "moonshot-v1-8k", "Moonshot v1 8K")
    private val MOONSHOT_V1_32K = AIModel(AIProvider.KIMI, "moonshot-v1-32k", "Moonshot v1 32K")
    private val KIMI_K2_TURBO = AIModel(AIProvider.KIMI, "kimi-k2-turbo-preview", "Kimi K2 Turbo")
    private val KIMI_K2_THINKING = AIModel(AIProvider.KIMI, "kimi-k2-thinking-preview", "Kimi K2 Thinking", isReasoning = true)
    val LOCAL_BUILTIN = AIModel(AIProvider.LOCAL, "local", "内置分析")

    val entries = listOf(
        DEEPSEEK_CHAT,
        DEEPSEEK_REASONER,
        GPT_4O,
        GPT_4O_MINI,
        GPT_4_1,
        GPT_4_1_MINI,
        GPT_4_1_NANO,
        CLAUDE_SONNET_4,
        CLAUDE_3_5_HAIKU,
        GEMINI_FLASH_LATEST,
        GEMINI_2_5_FLASH,
        GEMINI_2_0_FLASH,
        GEMINI_2_0_FLASH_LITE,
        GEMINI_1_5_PRO,
        GEMINI_1_5_FLASH,
        MOONSHOT_V1_8K,
        MOONSHOT_V1_32K,
        KIMI_K2_TURBO,
        KIMI_K2_THINKING,
        LOCAL_BUILTIN
    )

    fun getDisplayName(provider: AIProvider, modelId: String): String? {
        return entries.firstOrNull { it.provider == provider && it.modelName.equals(modelId, ignoreCase = true) }?.displayName
    }
}

fun prettyGenericName(id: String): String {
    return id.split("-").joinToString(" ") { part ->
        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

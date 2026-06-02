package com.henryliu.cbtreframe.shared

enum class AIProvider {
    DEEPSEEK,
    OPENAI,
    ANTHROPIC,
    GEMINI,
    KIMI,
    LOCAL
}

enum class AIModel(val provider: AIProvider, val modelName: String) {
    DEEPSEEK_CHAT(AIProvider.DEEPSEEK, "deepseek-chat"),
    DEEPSEEK_REASONER(AIProvider.DEEPSEEK, "deepseek-reasoner"),
    GPT_4O(AIProvider.OPENAI, "gpt-4o"),
    CLAUDE_SONNET_4(AIProvider.ANTHROPIC, "claude-sonnet-4-20250514"),
    CLAUDE_3_5_HAIKU(AIProvider.ANTHROPIC, "claude-3-5-haiku-20241022"),
    GEMINI_FLASH_LATEST(AIProvider.GEMINI, "gemini-flash-latest"),
    GEMINI_2_5_FLASH(AIProvider.GEMINI, "gemini-2.5-flash"),
    GEMINI_2_0_FLASH(AIProvider.GEMINI, "gemini-2.0-flash"),
    GEMINI_1_5_PRO(AIProvider.GEMINI, "gemini-1.5-pro"),
    MOONSHOT_V1_8K(AIProvider.KIMI, "moonshot-v1-8k"),
    MOONSHOT_V1_32K(AIProvider.KIMI, "moonshot-v1-32k"),
    KIMI_K2_TURBO(AIProvider.KIMI, "kimi-k2-turbo-preview"),
    KIMI_K2_THINKING(AIProvider.KIMI, "kimi-k2-thinking-preview"),
    LOCAL_BUILTIN(AIProvider.LOCAL, "local")
}

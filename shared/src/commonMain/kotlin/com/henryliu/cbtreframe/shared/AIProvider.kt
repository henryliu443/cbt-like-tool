package com.henryliu.cbtreframe.shared

enum class AIProvider {
    DEEPSEEK,
    OPENAI
}

enum class AIModel(val provider: AIProvider, val modelName: String) {
    DEEPSEEK_V4_PRO(AIProvider.DEEPSEEK, "deepseek-v4-pro"),
    GPT_4O(AIProvider.OPENAI, "gpt-4o")
}

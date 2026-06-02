package com.henryliu.cbtreframe.shared

class DefaultModelFetcher : ModelFetcher {
    override suspend fun fetchModels(provider: AIProvider, apiKey: String): List<AIModel> {
        return provider.fallbackModels()
    }
}

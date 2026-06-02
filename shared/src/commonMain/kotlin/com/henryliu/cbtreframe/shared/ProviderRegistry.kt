package com.henryliu.cbtreframe.shared

import io.ktor.client.HttpClient

/**
 * Collapsed Service: AIServiceFactory + AIProviderResolver.
 *
 * Returns the correct [AIServiceProtocol] for a given [AIProvider].
 * All 6 providers are wired for KMP.
 */
object ProviderRegistry {

    /**
     * Resolve the [AIServiceProtocol] for [provider].
     *
     * @param httpClient Shared Ktor HttpClient for network-based providers.
     * @param apiKeyProvider Lambda that returns the API key for a given provider string key.
     * @throws IllegalArgumentException if the provider is not yet implemented.
     */
    fun resolve(
        provider: AIProvider,
        httpClient: HttpClient,
        apiKeyProvider: suspend (String) -> String?,
    ): AIServiceProtocol {
        return when (provider) {
            AIProvider.DEEPSEEK -> DeepSeekService(
                httpClient = httpClient,
                apiKeyProvider = { apiKeyProvider("DeepSeek") },
            )
            AIProvider.OPENAI -> OpenAIService(
                httpClient = httpClient,
                apiKeyProvider = { apiKeyProvider("OpenAI") },
            )
            AIProvider.ANTHROPIC -> AnthropicService(
                httpClient = httpClient,
                apiKeyProvider = { apiKeyProvider("Anthropic") },
            )
            AIProvider.GEMINI -> GeminiService(
                httpClient = httpClient,
                apiKeyProvider = { apiKeyProvider("Google Gemini") },
            )
            AIProvider.KIMI -> MoonshotService(
                httpClient = httpClient,
                apiKeyProvider = { apiKeyProvider("Kimi (Moonshot)") },
            )
            AIProvider.LOCAL -> LocalAnalysisService()
        }
    }
}

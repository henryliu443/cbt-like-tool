package com.henryliu.cbtreframe.shared

import io.ktor.client.HttpClient

/**
 * Collapsed Service: ReframePipeline + EngineRouter + Engine formatting.
 *
 * Single public entry point that delegates to [ProviderRegistry] and
 * [ValidatedReframeClient] so callers don't need to know about the
 * internal layering.
 */
object ReframeOrchestrator {

    /**
     * Run the full reframe pipeline for a single thought.
     *
     * 1. Resolves the correct [AIServiceProtocol] via [ProviderRegistry].
     * 2. Wraps it in [ValidatedReframeClient] for retry + output gating.
     * 3. Executes and returns the validated [AnalysisResult].
     */
    suspend fun runReframe(
        thought: String,
        mood: String,
        hasAkathisia: Boolean,
        model: AIModel,
        settings: GlobalSettings,
        strategy: ResponseStrategy,
        httpClient: HttpClient,
        apiKeyProvider: suspend (String) -> String?,
    ): AnalysisResult {
        val provider = model.provider
        val service = ProviderRegistry.resolve(provider, httpClient, apiKeyProvider)
        val client = ValidatedReframeClient(service)

        return client.executeWithRetryAndValidation(
            thought = thought,
            mood = mood,
            hasAkathisia = hasAkathisia,
            model = model,
            depth = settings.analysisDepth,
            style = settings.responseStyle,
            template = settings.thinkingTemplate,
            strategy = strategy,
        )
    }

    /**
     * Run pattern analysis for a list of thought entries.
     */
    suspend fun runPatternAnalysis(
        thoughts: List<ThoughtEntry>,
        model: AIModel,
        httpClient: HttpClient,
        apiKeyProvider: suspend (String) -> String?,
    ): ThoughtPatternReport {
        val service = ProviderRegistry.resolve(model.provider, httpClient, apiKeyProvider)
        return service.analyzeThoughtPatterns(thoughts, model)
    }
}

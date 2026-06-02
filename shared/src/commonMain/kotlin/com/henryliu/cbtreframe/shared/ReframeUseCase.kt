package com.henryliu.cbtreframe.shared

import com.benasher44.uuid.uuid4
import io.ktor.client.HttpClient

data class ReframeUseCaseOutput(
    val result: AnalysisResult,
    val showCrisisBanner: Boolean,
    val historyEntryID: String,
    val recoveredByRetry: Boolean = false,
)

class ReframeUseCase(
    private val orchestrator: ReframeOrchestrator,
    private val historyRepository: HistoryRepository,
    private val httpClient: HttpClient,
    private val apiKeyProvider: suspend (String) -> String?,
) {
    suspend fun analyze(
        thought: String,
        mood: String,
        hasAkathisia: Boolean = false,
        provider: AIProvider,
        modelName: String,
        settings: GlobalSettings,
    ): ReframeUseCaseOutput {
        val level = detectRiskLevel(thought)
        val strategy = routeStrategy(level)

        val result: AnalysisResult
        val showCrisisBanner: Boolean
        val providerNameForHistory: String
        val modelNameForHistory: String

        if (shouldUseLocalCrisisOnly(thought)) {
            result = CrisisLocalSupport.analysisResult
            showCrisisBanner = true
            providerNameForHistory = CrisisLocalSupport.historyProviderName
            modelNameForHistory = CrisisLocalSupport.historyModelName
        } else {
            val model = resolveAIModel(provider, modelName)
            result = orchestrator.runReframe(
                thought = thought,
                mood = mood,
                hasAkathisia = hasAkathisia,
                model = model,
                settings = settings,
                strategy = strategy,
                httpClient = httpClient,
                apiKeyProvider = apiKeyProvider,
            )
            showCrisisBanner = false
            providerNameForHistory = provider.name
            modelNameForHistory = model.modelName
        }

        val id = uuid4().toString()
        val timestamp = currentTimeMillis()

        historyRepository.addHistory(
            id = id,
            inputThought = thought,
            distortion = result.distortion,
            alternative = result.alternative,
            action = result.action,
            isFavorite = 0L,
            createdAt = timestamp,
            providerName = providerNameForHistory,
            modelName = modelNameForHistory,
            moodTag = mood,
            therapyTemplateRaw = settings.thinkingTemplate.name,
            analysisDepthRaw = settings.analysisDepth.name,
            responseStyleRaw = settings.responseStyle.name,
            resultExtrasJSON = "{}",
            followUpMessagesJSON = "[]"
        )

        return ReframeUseCaseOutput(
            result = result,
            showCrisisBanner = showCrisisBanner,
            historyEntryID = id,
        )
    }

    private fun resolveAIModel(provider: AIProvider, modelName: String): AIModel {
        return AIModel.entries.firstOrNull { it.provider == provider && it.modelName == modelName }
            ?: error("Unknown model: provider=${provider.name}, modelName=$modelName")
    }

    private fun currentTimeMillis(): Long {
        return kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    }
}

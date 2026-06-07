package com.henryliu.cbtreframe.shared

import com.benasher44.uuid.uuid4
import io.ktor.client.HttpClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


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

        val template = settings.thinkingTemplate
        val normalizedResult = result.normalized(template)

        val extras = HistoryResultExtras(
            questions = normalizedResult.questions,
            actions = normalizedResult.actions,
            stateAssessment = normalizedResult.stateAssessment
        )
        val resultExtrasJSON = Json.encodeToString(extras)

        val id = uuid4().toString()
        val timestamp = currentTimeMillis()
        
        val finalMoodTag = moodTagForHistory(mood, hasAkathisia)

        historyRepository.addHistory(
            id = id,
            inputText = thought,
            aiResponse = "${normalizedResult.distortion}\n\n${normalizedResult.alternative}\n\n${normalizedResult.action}",
            timestamp = timestamp,
            distortion = normalizedResult.distortion,
            alternative = normalizedResult.alternative,
            action = normalizedResult.action,
            isFavorite = 0L,
            providerName = providerNameForHistory,
            modelName = modelNameForHistory,
            moodTag = finalMoodTag,
            therapyTemplateRaw = template.name,
            analysisDepthRaw = settings.analysisDepth.name,
            responseStyleRaw = settings.responseStyle.name,
            resultExtrasJSON = resultExtrasJSON,
            followUpMessagesJSON = ""
        )

        return ReframeUseCaseOutput(
            result = normalizedResult,
            showCrisisBanner = showCrisisBanner,
            historyEntryID = id,
        )
    }

    private fun moodTagForHistory(base: String, isAkathisia: Boolean): String {
        if (!isAkathisia) return base
        if (base == PromptBuilder.akathisiaMoodTag) return base
        return "$base（Akathisia）"
    }

    private fun resolveAIModel(provider: AIProvider, modelName: String): AIModel {
        return FallbackModels.entries.firstOrNull { it.provider == provider && it.modelName == modelName }
            ?: AIModel(provider, modelName, prettyGenericName(modelName))
    }

    data class StreamUseCaseOutput(
        val stream: kotlinx.coroutines.flow.Flow<String>,
        val showCrisisBanner: Boolean,
        val historyEntryID: String,
        val providerNameForHistory: String,
        val modelNameForHistory: String,
        val strategy: ResponseStrategy,
        val finalResult: kotlinx.coroutines.CompletableDeferred<AnalysisResult>
    )

    fun streamAnalyze(
        thought: String,
        mood: String,
        hasAkathisia: Boolean = false,
        provider: AIProvider,
        modelName: String,
        settings: GlobalSettings,
    ): StreamUseCaseOutput {
        val level = detectRiskLevel(thought)
        val strategy = routeStrategy(level)
        val id = uuid4().toString()

        var showCrisisBanner = false
        var providerNameForHistory = provider.name
        var modelNameForHistory = modelName

        val baseStream = if (shouldUseLocalCrisisOnly(thought)) {
            showCrisisBanner = true
            providerNameForHistory = CrisisLocalSupport.historyProviderName
            modelNameForHistory = CrisisLocalSupport.historyModelName
            kotlinx.coroutines.flow.flow {
                val result = CrisisLocalSupport.analysisResult
                val text = buildString {
                    appendLine("认知扭曲：${result.distortion}")
                    appendLine("替代想法：${result.alternative}")
                    appendLine("建议行动：${result.action}")
                }
                emit(text)
            }
        } else {
            val model = resolveAIModel(provider, modelName)
            modelNameForHistory = model.modelName
            orchestrator.streamRunReframe(
                thought = thought,
                mood = mood,
                hasAkathisia = hasAkathisia,
                model = model,
                settings = settings,
                strategy = strategy,
                httpClient = httpClient,
                apiKeyProvider = apiKeyProvider,
            )
        }

        val finalResultDeferred = kotlinx.coroutines.CompletableDeferred<AnalysisResult>()

        // We wrap the stream to intercept and build the final string,
        // then save to history DB when it finishes.
        val hookedFlow = kotlinx.coroutines.flow.flow {
            val sb = StringBuilder()
            try {
                baseStream.collect { chunk ->
                    sb.append(chunk)
                    emit(chunk)
                }
            } finally {
                val fullText = sb.toString()
                if (fullText.isNotBlank()) {
                    val parsedResult = try {
                        parseReframeOutput(fullText, strategy)
                    } catch(e: Exception) {
                        AnalysisResult(
                            distortion = "AI 分析完成",
                            alternative = fullText.replace(Regex("[{}\"\\[\\]]"), "").trim(),
                            action = ""
                        )
                    }
                    val template = settings.thinkingTemplate
                    val normalizedResult = parsedResult.normalized(template)
                    
                    finalResultDeferred.complete(normalizedResult)
                    
                    val extras = HistoryResultExtras(
                        questions = normalizedResult.questions,
                        actions = normalizedResult.actions,
                        stateAssessment = normalizedResult.stateAssessment
                    )
                    val resultExtrasJSON = Json.encodeToString(extras)
                    val finalMoodTag = moodTagForHistory(mood, hasAkathisia)
                    
                    historyRepository.addHistory(
                        id = id,
                        inputText = thought,
                        aiResponse = "${normalizedResult.distortion}\n\n${normalizedResult.alternative}\n\n${normalizedResult.action}",
                        timestamp = currentTimeMillis(),
                        distortion = normalizedResult.distortion,
                        alternative = normalizedResult.alternative,
                        action = normalizedResult.action,
                        isFavorite = 0L,
                        providerName = providerNameForHistory,
                        modelName = modelNameForHistory,
                        moodTag = finalMoodTag,
                        therapyTemplateRaw = template.name,
                        analysisDepthRaw = settings.analysisDepth.name,
                        responseStyleRaw = settings.responseStyle.name,
                        resultExtrasJSON = resultExtrasJSON,
                        followUpMessagesJSON = ""
                    )
                } else {
                    finalResultDeferred.completeExceptionally(IllegalStateException("Empty stream"))
                }
            }
        }

        return StreamUseCaseOutput(
            stream = hookedFlow,
            showCrisisBanner = showCrisisBanner,
            historyEntryID = id,
            providerNameForHistory = providerNameForHistory,
            modelNameForHistory = modelNameForHistory,
            strategy = strategy,
            finalResult = finalResultDeferred
        )
    }

    suspend fun analyzePatterns(
        thoughts: List<ThoughtEntry>,
        provider: AIProvider,
        modelName: String,
    ): ThoughtPatternReport {
        val model = resolveAIModel(provider, modelName)
        return orchestrator.runPatternAnalysis(
            thoughts = thoughts,
            model = model,
            httpClient = httpClient,
            apiKeyProvider = apiKeyProvider,
        )
    }

    private fun currentTimeMillis(): Long {
        return kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    }
}

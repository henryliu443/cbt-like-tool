package com.henryliu.cbtreframe.shared

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.request.preparePost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MoonshotService(
    private val httpClient: HttpClient,
    private val apiKeyProvider: suspend () -> String?,
) : AIServiceProtocol {
    override val provider: AIProvider = AIProvider.KIMI
    private val baseUrl = "https://api.moonshot.cn/v1/chat/completions"

    override suspend fun reframe(
        thought: String,
        mood: String,
        hasAkathisia: Boolean,
        model: AIModel,
        depth: ThinkingTemplate.AnalysisDepth,
        style: ThinkingTemplate.AppResponseStyle,
        template: ThinkingTemplate,
        strategy: ResponseStrategy,
    ): AnalysisResult {
        val apiKey = apiKeyProvider() ?: throw AIServiceError.NoAPIKey()

        val systemPrompt = PromptBuilder.buildSystemPrompt(
            template = template,
            strategy = strategy,
            depth = depth,
            style = style,
            mood = mood,
            hasAkathisia = hasAkathisia,
        )
        val userPrompt = PromptBuilder.buildUserPrompt(thought, mood, hasAkathisia)

        val body = ChatCompletionBody(
            model = model.modelName,
            messages = listOf(
                ChatCompletionMessage(role = "system", content = systemPrompt),
                ChatCompletionMessage(role = "user", content = userPrompt),
            ),
            temperature = 0.7,
            maxTokens = if (strategy == ResponseStrategy.crisis) 512 else 1024,
        )

        val response = httpClient.post(baseUrl) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatCompletionBody.serializer(), body))
        }

        when (response.status.value) {
            200 -> { /* ok */ }
            401 -> throw AIServiceError.InvalidKey()
            429 -> throw AIServiceError.RateLimited()
            else -> throw AIServiceError.InvalidResponse()
        }

        val data = response.bodyAsText()
        return parseMoonshotResponse(data, strategy)
    }

    override fun streamReframe(
        thought: String,
        mood: String,
        hasAkathisia: Boolean,
        model: AIModel,
        depth: ThinkingTemplate.AnalysisDepth,
        style: ThinkingTemplate.AppResponseStyle,
        template: ThinkingTemplate,
        strategy: ResponseStrategy,
    ): Flow<String> = flow {
        val apiKey = apiKeyProvider() ?: throw AIServiceError.NoAPIKey()

        val systemPrompt = PromptBuilder.buildSystemPrompt(
            template = template,
            strategy = strategy,
            depth = depth,
            style = style,
            mood = mood,
            hasAkathisia = hasAkathisia,
        )
        val userPrompt = PromptBuilder.buildUserPrompt(thought, mood, hasAkathisia)

        val body = ChatCompletionBody(
            model = model.modelName,
            messages = listOf(
                ChatCompletionMessage(role = "system", content = systemPrompt),
                ChatCompletionMessage(role = "user", content = userPrompt),
            ),
            temperature = 0.7,
            maxTokens = if (strategy == ResponseStrategy.crisis) 512 else 1024,
            stream = true,
        )

        httpClient.preparePost(baseUrl) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatCompletionBody.serializer(), body))
        }.execute { response ->
            response.streamSSE().collect { emit(it) }
        }
    }

    override suspend fun analyzeThoughtPatterns(
        thoughts: List<ThoughtEntry>,
        model: AIModel,
    ): ThoughtPatternReport {
        val apiKey = apiKeyProvider() ?: throw AIServiceError.NoAPIKey()

        val body = ChatCompletionBody(
            model = model.modelName,
            messages = listOf(
                ChatCompletionMessage(role = "system", content = PromptBuilder.thoughtPatternSystemPrompt),
                ChatCompletionMessage(
                    role = "user",
                    content = PromptBuilder.buildThoughtPatternUserPrompt(thoughts)
                ),
            ),
            temperature = 0.3,
            maxTokens = 1400,
        )

        val response = httpClient.post(baseUrl) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatCompletionBody.serializer(), body))
        }

        when (response.status.value) {
            200 -> { /* ok */ }
            401 -> throw AIServiceError.InvalidKey()
            429 -> throw AIServiceError.RateLimited()
            else -> throw AIServiceError.InvalidResponse()
        }

        val data = response.bodyAsText()
        return parseMoonshotThoughtPatternResponse(data)
    }

    private fun parseMoonshotResponse(data: String, strategy: ResponseStrategy): AnalysisResult {
        val resp = parseChatCompletionResponse(data) ?: throw AIServiceError.InvalidResponse()
        val content = resp.choices.firstOrNull()?.message?.content
            ?: throw AIServiceError.InvalidResponse()
        return parseReframeOutput(content, strategy)
    }

    private fun parseMoonshotThoughtPatternResponse(data: String): ThoughtPatternReport {
        val resp = parseChatCompletionResponse(data) ?: throw AIServiceError.InvalidResponse()
        val content = resp.choices.firstOrNull()?.message?.content
            ?: throw AIServiceError.InvalidResponse()
        return parseThoughtPatternContent(content)
    }
}

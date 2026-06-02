package com.henryliu.cbtreframe.shared

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class ChatCompletionBody(
    val model: String,
    val messages: List<ChatCompletionMessage>,
    val temperature: Double = 0.7,
    @SerialName("max_tokens")
    val maxTokens: Int = 1024,
)

@Serializable
data class ChatCompletionMessage(
    val role: String,
    val content: String,
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChatCompletionChoice> = emptyList(),
)

@Serializable
data class ChatCompletionChoice(
    val message: ChatCompletionMessage? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

// ── OpenAI-compatible Chat Completions implementation ─────────────────────

class OpenAIService(
    private val httpClient: HttpClient,
    private val apiKeyProvider: suspend () -> String?,
) : AIServiceProtocol {
    override val provider: AIProvider = AIProvider.OPENAI
    private val baseUrl = "https://api.openai.com/v1/chat/completions"

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

        return handleResponseAndParse(response, strategy)
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

        return handleThoughtPatternResponseAndParse(response)
    }

    internal suspend fun handleResponseAndParse(response: io.ktor.client.statement.HttpResponse, strategy: ResponseStrategy): AnalysisResult {
        when (response.status.value) {
            200 -> { /* ok */ }
            401, 403 -> throw AIServiceError.InvalidKey()
            429 -> throw AIServiceError.HttpStatus(429)
            in 500..599 -> throw AIServiceError.HttpStatus(response.status.value)
            else -> throw AIServiceError.HttpStatus(response.status.value)
        }

        val data = response.bodyAsText()
        return parseOpenAIResponse(data, strategy)
    }

    internal suspend fun handleThoughtPatternResponseAndParse(response: io.ktor.client.statement.HttpResponse): ThoughtPatternReport {
        when (response.status.value) {
            200 -> { /* ok */ }
            401, 403 -> throw AIServiceError.InvalidKey()
            429 -> throw AIServiceError.HttpStatus(429)
            in 500..599 -> throw AIServiceError.HttpStatus(response.status.value)
            else -> throw AIServiceError.HttpStatus(response.status.value)
        }

        val data = response.bodyAsText()
        return parseOpenAIThoughtPatternResponse(data)
    }

    internal fun parseOpenAIResponse(data: String, strategy: ResponseStrategy): AnalysisResult {
        val resp = parseChatCompletionResponse(data) ?: throw AIServiceError.InvalidResponse()
        val content = resp.choices.firstOrNull()?.message?.content
            ?: throw AIServiceError.InvalidResponse()
        return parseReframeOutput(content, strategy)
    }

    internal fun parseOpenAIThoughtPatternResponse(data: String): ThoughtPatternReport {
        val resp = parseChatCompletionResponse(data) ?: throw AIServiceError.InvalidResponse()
        val content = resp.choices.firstOrNull()?.message?.content
            ?: throw AIServiceError.InvalidResponse()
        return parseThoughtPatternContent(content)
    }
}

// ── Shared parse helpers ────────────────────────────────────────────────

internal val json = Json { ignoreUnknownKeys = true; isLenient = true }

internal fun parseChatCompletionResponse(data: String): ChatCompletionResponse? {
    return try {
        json.decodeFromString(ChatCompletionResponse.serializer(), data)
    } catch (_: Exception) {
        null
    }
}


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

@Serializable
data class AnthropicBody(
    val model: String,
    @SerialName("max_tokens")
    val maxTokens: Int,
    val system: String,
    val messages: List<AnthropicMessage>,
)

@Serializable
data class AnthropicMessage(
    val role: String,
    val content: String,
)

@Serializable
data class AnthropicContentBlock(
    val type: String,
    val text: String? = null,
)

@Serializable
data class AnthropicResponse(
    val content: List<AnthropicContentBlock> = emptyList(),
)

class AnthropicService(
    private val httpClient: HttpClient,
    private val apiKeyProvider: suspend () -> String?,
) : AIServiceProtocol {
    override val provider: AIProvider = AIProvider.ANTHROPIC
    private val baseUrl = "https://api.anthropic.com/v1/messages"

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

        val body = AnthropicBody(
            model = model.modelName,
            maxTokens = if (strategy == ResponseStrategy.crisis) 512 else 1024,
            system = systemPrompt,
            messages = listOf(
                AnthropicMessage(role = "user", content = userPrompt),
            ),
        )

        val response = httpClient.post(baseUrl) {
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(anthropicJson.encodeToString(AnthropicBody.serializer(), body))
        }

        when (response.status.value) {
            200 -> { /* ok */ }
            401 -> throw AIServiceError.InvalidKey()
            429 -> throw AIServiceError.RateLimited()
            else -> throw AIServiceError.InvalidResponse()
        }

        val data = response.bodyAsText()
        return parseAnthropicResponse(data, strategy)
    }

    override suspend fun analyzeThoughtPatterns(
        thoughts: List<ThoughtEntry>,
        model: AIModel,
    ): ThoughtPatternReport {
        val apiKey = apiKeyProvider() ?: throw AIServiceError.NoAPIKey()

        val body = AnthropicBody(
            model = model.modelName,
            maxTokens = 1400,
            system = PromptBuilder.thoughtPatternSystemPrompt,
            messages = listOf(
                AnthropicMessage(
                    role = "user",
                    content = PromptBuilder.buildThoughtPatternUserPrompt(thoughts),
                ),
            ),
        )

        val response = httpClient.post(baseUrl) {
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(anthropicJson.encodeToString(AnthropicBody.serializer(), body))
        }

        when (response.status.value) {
            200 -> { /* ok */ }
            401 -> throw AIServiceError.InvalidKey()
            429 -> throw AIServiceError.RateLimited()
            else -> throw AIServiceError.InvalidResponse()
        }

        val data = response.bodyAsText()
        return parseAnthropicThoughtPatternResponse(data)
    }

    private fun parseAnthropicResponse(data: String, strategy: ResponseStrategy): AnalysisResult {
        val resp = parseAnthropicResponseJson(data) ?: throw AIServiceError.InvalidResponse()
        val textBlock = resp.content.firstOrNull { it.type == "text" }
            ?: throw AIServiceError.InvalidResponse()
        val text = textBlock.text ?: throw AIServiceError.InvalidResponse()
        return parseReframeOutput(text, strategy)
    }

    private fun parseAnthropicThoughtPatternResponse(data: String): ThoughtPatternReport {
        val resp = parseAnthropicResponseJson(data) ?: throw AIServiceError.InvalidResponse()
        val textBlock = resp.content.firstOrNull { it.type == "text" }
            ?: throw AIServiceError.InvalidResponse()
        val text = textBlock.text ?: throw AIServiceError.InvalidResponse()
        return parseThoughtPatternContent(text)
    }

    private fun parseAnthropicResponseJson(data: String): AnthropicResponse? {
        return try {
            anthropicJson.decodeFromString(AnthropicResponse.serializer(), data)
        } catch (_: Exception) {
            null
        }
    }
}

private val anthropicJson = Json { ignoreUnknownKeys = true; isLenient = true }

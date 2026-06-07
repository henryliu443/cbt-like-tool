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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class GeminiGenerateBody(
    @SerialName("systemInstruction")
    val systemInstruction: GeminiSystemInstruction? = null,
    val contents: List<GeminiContent>,
    @SerialName("generationConfig")
    val generationConfig: GeminiGenerationConfig? = null,
)

@Serializable
data class GeminiSystemInstruction(
    val parts: List<GeminiPart>,
)

@Serializable
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>,
)

@Serializable
data class GeminiPart(
    val text: String,
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Double = 0.7,
    @SerialName("maxOutputTokens")
    val maxOutputTokens: Int = 1024,
)

@Serializable
data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val error: GeminiError? = null,
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
)

@Serializable
data class GeminiError(
    val message: String? = null,
    val status: String? = null,
)

class GeminiService(
    private val httpClient: HttpClient,
    private val apiKeyProvider: suspend () -> String?,
) : AIServiceProtocol {
    override val provider: AIProvider = AIProvider.GEMINI
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"

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

        val body = GeminiGenerateBody(
            systemInstruction = GeminiSystemInstruction(
                parts = listOf(GeminiPart(text = systemPrompt)),
            ),
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = userPrompt)),
                ),
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7,
                maxOutputTokens = if (strategy == ResponseStrategy.crisis) 512 else 4096,
            ),
        )

        val (data, statusCode) = performGenerateContent(model.modelName, apiKey, body)

        when (statusCode) {
            200 -> { /* ok */ }
            401, 403 -> throw AIServiceError.InvalidKey()
            429 -> throw AIServiceError.HttpStatus(429)
            in 500..599 -> throw AIServiceError.HttpStatus(statusCode)
            else -> {
                val errMsg = geminiErrorMessage(data)
                if (errMsg != null) {
                    println("Gemini HTTP $statusCode: $errMsg")
                }
                throw AIServiceError.HttpStatus(statusCode)
            }
        }

        return parseGeminiGenerateResponse(data, strategy)
    }

    override suspend fun analyzeThoughtPatterns(
        thoughts: List<ThoughtEntry>,
        model: AIModel,
    ): ThoughtPatternReport {
        val apiKey = apiKeyProvider() ?: throw AIServiceError.NoAPIKey()

        val body = GeminiGenerateBody(
            systemInstruction = GeminiSystemInstruction(
                parts = listOf(GeminiPart(text = PromptBuilder.thoughtPatternSystemPrompt)),
            ),
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = PromptBuilder.buildThoughtPatternUserPrompt(thoughts))),
                ),
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.3,
                maxOutputTokens = 1400,
            ),
        )

        val (data, statusCode) = performGenerateContent(model.modelName, apiKey, body)

        when (statusCode) {
            200 -> { /* ok */ }
            401, 403 -> throw AIServiceError.InvalidKey()
            429 -> throw AIServiceError.HttpStatus(429)
            in 500..599 -> throw AIServiceError.HttpStatus(statusCode)
            else -> throw AIServiceError.HttpStatus(statusCode)
        }

        val text = extractGeminiText(data)
        return parseThoughtPatternContent(text)
    }

    private suspend fun performGenerateContent(
        modelId: String,
        apiKey: String,
        body: GeminiGenerateBody,
    ): Pair<String, Int> {
        val url = "$baseUrl/models/$modelId:generateContent?key=$apiKey"

        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(geminiJson.encodeToString(GeminiGenerateBody.serializer(), body))
        }

        return response.bodyAsText() to response.status.value
    }

    private fun geminiErrorMessage(data: String): String? {
        return try {
            val resp = geminiJson.decodeFromString(GeminiGenerateResponse.serializer(), data)
            resp.error?.message ?: resp.error?.status
        } catch (_: Exception) {
            null
        }
    }

    private fun parseGeminiGenerateResponse(data: String, strategy: ResponseStrategy): AnalysisResult {
        val text = extractGeminiText(data)
        return parseReframeOutput(text, strategy)
    }

    private fun extractGeminiText(data: String): String {
        val jsonObj = try {
            geminiJson.parseToJsonElement(data).jsonObject
        } catch (_: Exception) {
            throw AIServiceError.InvalidResponse()
        }

        val error = jsonObj["error"]?.jsonObject
        if (error != null) {
            val msg = error["message"]?.jsonPrimitive?.contentOrNull ?: "unknown"
            throw AIServiceError.ParseError(msg)
        }

        val candidates = jsonObj["candidates"]?.jsonArray
        val firstCandidate = candidates?.firstOrNull()?.jsonObject
            ?: throw AIServiceError.InvalidResponse()
        val content = firstCandidate["content"]?.jsonObject
            ?: throw AIServiceError.InvalidResponse()
        val parts = content["parts"]?.jsonArray
            ?: throw AIServiceError.InvalidResponse()

        val text = parts.mapNotNull { part ->
            part.jsonObject["text"]?.jsonPrimitive?.contentOrNull
        }.joinToString("")

        if (text.isBlank()) {
            throw AIServiceError.InvalidResponse()
        }

        return text
    }
}

private val geminiJson = Json { ignoreUnknownKeys = true; isLenient = true }

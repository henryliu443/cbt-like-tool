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
import io.ktor.client.request.preparePost
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.currentCoroutineContext
@Serializable
data class ChatCompletionBody(
    val model: String,
    val messages: List<ChatCompletionMessage>,
    val temperature: Double? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    @SerialName("max_completion_tokens")
    val maxCompletionTokens: Int? = null,
    @SerialName("reasoning_effort")
    val reasoningEffort: String? = null,
    val stream: Boolean = false,
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
    val delta: ChatCompletionDelta? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

@Serializable
data class ChatCompletionDelta(
    val content: String? = null,
    @SerialName("reasoning_content")
    val reasoningContent: String? = null,
)

// ── OpenAI-compatible Chat Completions implementation ─────────────────────

class OpenAIService(
    private val httpClient: HttpClient,
    private val apiKeyProvider: suspend () -> String?,
) : AIServiceProtocol {
    override val provider: AIProvider = AIProvider.OPENAI
    private val baseUrl = "https://api.openai.com/v1/chat/completions"

    private fun supportsReasoningEffort(modelName: String) = modelName.startsWith("o1") || modelName.startsWith("o3")

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

        val isO1OrO3 = supportsReasoningEffort(model.modelName)
        val maxTokensVal = if (strategy == ResponseStrategy.crisis) 512 else 1024

        val body = ChatCompletionBody(
            model = model.modelName,
            messages = listOf(
                ChatCompletionMessage(role = "system", content = systemPrompt),
                ChatCompletionMessage(role = "user", content = userPrompt),
            ),
            temperature = if (isO1OrO3) null else 0.7,
            maxTokens = if (isO1OrO3) null else maxTokensVal,
            maxCompletionTokens = if (isO1OrO3) maxTokensVal else null,
            reasoningEffort = if (isO1OrO3) {
                when (depth) {
                    ThinkingTemplate.AnalysisDepth.fast -> "low"
                    ThinkingTemplate.AnalysisDepth.balanced -> "medium"
                    ThinkingTemplate.AnalysisDepth.deep -> "high"
                }
            } else null,
            stream = false,
        )

        val response = httpClient.post(baseUrl) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatCompletionBody.serializer(), body))
        }

        return handleResponseAndParse(response, strategy)
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

        val isO1OrO3 = supportsReasoningEffort(model.modelName)
        val maxTokensVal = if (strategy == ResponseStrategy.crisis) 512 else 1024

        val body = ChatCompletionBody(
            model = model.modelName,
            messages = listOf(
                ChatCompletionMessage(role = "system", content = systemPrompt),
                ChatCompletionMessage(role = "user", content = userPrompt),
            ),
            temperature = if (isO1OrO3) null else 0.7,
            maxTokens = if (isO1OrO3) null else maxTokensVal,
            maxCompletionTokens = if (isO1OrO3) maxTokensVal else null,
            reasoningEffort = if (isO1OrO3) {
                when (depth) {
                    ThinkingTemplate.AnalysisDepth.fast -> "low"
                    ThinkingTemplate.AnalysisDepth.balanced -> "medium"
                    ThinkingTemplate.AnalysisDepth.deep -> "high"
                }
            } else null,
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

        val isO1OrO3 = supportsReasoningEffort(model.modelName)

        val body = ChatCompletionBody(
            model = model.modelName,
            messages = listOf(
                ChatCompletionMessage(role = "system", content = PromptBuilder.thoughtPatternSystemPrompt),
                ChatCompletionMessage(
                    role = "user",
                    content = PromptBuilder.buildThoughtPatternUserPrompt(thoughts)
                ),
            ),
            temperature = if (isO1OrO3) null else 0.3,
            maxTokens = if (isO1OrO3) null else 1400,
            maxCompletionTokens = if (isO1OrO3) 1400 else null,
            reasoningEffort = if (isO1OrO3) "high" else null,
            stream = false,
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

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
internal val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

internal fun parseChatCompletionResponse(data: String): ChatCompletionResponse? {
    return try {
        json.decodeFromString(ChatCompletionResponse.serializer(), data)
    } catch (_: Exception) {
        null
    }
}

internal fun io.ktor.client.statement.HttpResponse.streamSSE(): Flow<String> = flow {
    val statusCode = this@streamSSE.status.value
    if (statusCode !in 200..299) {
        val bodyStr = this@streamSSE.bodyAsText()
        if (bodyStr.contains("Insufficient Balance", ignoreCase = true) ||
            bodyStr.contains("insufficient", ignoreCase = true)
        ) {
            throw AIServiceError.ParseError("账户余额不足，请充值后重试")
        }
        throw AIServiceError.HttpStatus(statusCode)
    }

    val channel: ByteReadChannel = this@streamSSE.bodyAsChannel()
    while (currentCoroutineContext().isActive && !channel.isClosedForRead) {
        val line = channel.readUTF8Line() ?: break
        if (line.isBlank()) continue
        if (line.startsWith("data: ")) {
            val data = line.removePrefix("data: ").trim()
            if (data == "[DONE]") break
            if (data.isEmpty()) continue
            try {
                val chunk = json.decodeFromString<ChatCompletionResponse>(data)
                val choice = chunk.choices.firstOrNull()
                val content = choice?.delta?.content
                if (!content.isNullOrEmpty()) {
                    emit(content)
                }
            } catch (_: Exception) {}
        }
    }
}


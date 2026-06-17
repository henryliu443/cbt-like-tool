package com.henryliu.cbtreframe.shared

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import io.ktor.client.request.preparePost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DeepSeekService(
    private val httpClient: HttpClient,
    private val apiKeyProvider: suspend () -> String?,
) : AIServiceProtocol {
    override val provider: AIProvider = AIProvider.DEEPSEEK
    private val baseUrl = "https://api.deepseek.com/v1/chat/completions"

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

        val isReasoner = model.modelName.contains("reasoner")
        val useJSON = isJsonMode(strategy)

        val systemPrompt = PromptBuilder.buildSystemPrompt(
            template = template,
            strategy = strategy,
            depth = depth,
            style = style,
            mood = mood,
            hasAkathisia = hasAkathisia,
        )
        val userPrompt = PromptBuilder.buildUserPrompt(thought, mood, hasAkathisia)

        val messages: List<ChatCompletionMessage>
        if (isReasoner) {
            val combined = if (useJSON) {
                "$systemPrompt\n\n${PromptBuilder.reasonerAdditionalInstructions()}\n\n$userPrompt"
            } else {
                "$systemPrompt\n\n$userPrompt"
            }
            messages = listOf(
                ChatCompletionMessage(role = "user", content = combined),
            )
        } else {
            messages = listOf(
                ChatCompletionMessage(role = "system", content = systemPrompt),
                ChatCompletionMessage(role = "user", content = userPrompt),
            )
        }

        val body = ChatCompletionBody(
            model = model.modelName,
            messages = messages,
            temperature = if (isReasoner) 0.0 else 0.7,
            maxTokens = when {
                isReasoner -> 8192
                strategy == ResponseStrategy.crisis -> 512
                else -> 2048
            },
        )

        val response = httpClient.post(baseUrl) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatCompletionBody.serializer(), body))
        }

        val statusCode = response.status.value
        val data = response.bodyAsText()

        if (statusCode != 200) {
            // Log the body for debugging
            println("DeepSeek HTTP $statusCode: ${data.take(500)}")
        }

        when (statusCode) {
            200 -> { /* ok */ }
            401, 403 -> throw AIServiceError.InvalidKey()
            429 -> throw AIServiceError.HttpStatus(429)
            400 -> {
                if (data.contains("Insufficient Balance", ignoreCase = true) ||
                    data.contains("insufficient", ignoreCase = true)
                ) {
                    throw AIServiceError.ParseError("DeepSeek 账户余额不足，请充值后重试")
                }
                throw AIServiceError.HttpStatus(400)
            }
            in 500..599 -> throw AIServiceError.HttpStatus(statusCode)
            else -> throw AIServiceError.HttpStatus(statusCode)
        }

        return parseDeepSeekResponse(data, strategy)
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

        val isReasoner = model.modelName.contains("reasoner")
        val useJSON = isJsonMode(strategy)

        val systemPrompt = PromptBuilder.buildSystemPrompt(
            template = template,
            strategy = strategy,
            depth = depth,
            style = style,
            mood = mood,
            hasAkathisia = hasAkathisia,
        )
        val userPrompt = PromptBuilder.buildUserPrompt(thought, mood, hasAkathisia)

        val messages: List<ChatCompletionMessage>
        if (isReasoner) {
            val combined = if (useJSON) {
                "$systemPrompt\n\n${PromptBuilder.reasonerAdditionalInstructions()}\n\n$userPrompt"
            } else {
                "$systemPrompt\n\n$userPrompt"
            }
            messages = listOf(
                ChatCompletionMessage(role = "user", content = combined),
            )
        } else {
            messages = listOf(
                ChatCompletionMessage(role = "system", content = systemPrompt),
                ChatCompletionMessage(role = "user", content = userPrompt),
            )
        }

        val body = ChatCompletionBody(
            model = model.modelName,
            messages = messages,
            temperature = if (isReasoner) 0.0 else 0.7,
            maxTokens = when {
                isReasoner -> 8192
                strategy == ResponseStrategy.crisis -> 512
                else -> 2048
            },
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
            maxTokens = 1600,
        )

        val response = httpClient.post(baseUrl) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatCompletionBody.serializer(), body))
        }

        when (response.status.value) {
            200 -> { /* ok */ }
            401, 403 -> throw AIServiceError.InvalidKey()
            429 -> throw AIServiceError.HttpStatus(429)
            in 500..599 -> throw AIServiceError.HttpStatus(response.status.value)
            else -> throw AIServiceError.HttpStatus(response.status.value)
        }

        val data = response.bodyAsText()
        return parseDeepSeekThoughtPatternResponse(data)
    }

    private fun parseDeepSeekResponse(data: String, strategy: ResponseStrategy): AnalysisResult {
        val resp = parseChatCompletionResponse(data) ?: run {
            println("DeepSeek cannot parse top-level: ${data.take(500)}")
            throw AIServiceError.InvalidResponse()
        }

        val choice = resp.choices.firstOrNull()
        val message = choice?.message ?: throw AIServiceError.InvalidResponse()

        val finish = choice.finishReason

        val content = message.content
        if (content.isNullOrBlank()) {
            println("DeepSeek empty content. finish=$finish")
            throw AIServiceError.ParseError(
                "DeepSeek Reasoner 未返回最终正文（推理可能占满 token）。请重试，或改用「DeepSeek Chat」；若仍失败请检查账户额度与 API 文档。"
            )
        }

        if (finish == "length" && strategy != ResponseStrategy.crisis && isJsonMode(strategy)) {
            throw AIServiceError.ParseError("DeepSeek 输出因长度被截断，无法保证完整 JSON。请缩短输入或重试。")
        }

        return parseReframeOutput(content, strategy)
    }

    private fun parseDeepSeekThoughtPatternResponse(data: String): ThoughtPatternReport {
        val resp = parseChatCompletionResponse(data) ?: throw AIServiceError.InvalidResponse()
        val content = resp.choices.firstOrNull()?.message?.content
            ?: throw AIServiceError.InvalidResponse()
        return parseThoughtPatternContent(content)
    }
}

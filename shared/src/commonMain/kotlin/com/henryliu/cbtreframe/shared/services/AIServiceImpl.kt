package com.henryliu.cbtreframe.shared.services

import com.henryliu.cbtreframe.shared.AIModel
import com.henryliu.cbtreframe.shared.AIProvider
import com.henryliu.cbtreframe.shared.KeychainProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

interface AIService {
    fun streamReframe(input: String, model: AIModel): Flow<String>
}

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    @SerialName("max_tokens")
    val maxTokens: Int = 2048,
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
data class ChatResponseChunk(
    val choices: List<ChatChoice> = emptyList(),
)

@Serializable
data class ChatChoice(
    val delta: ChatDelta,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

@Serializable
data class ChatDelta(
    val content: String? = null,
    @SerialName("reasoning_content")
    val reasoningContent: String? = null,
)

class AIServiceImpl(
    private val httpClient: HttpClient,
    private val keychainProvider: KeychainProvider,
) : AIService {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun streamReframe(input: String, model: AIModel): Flow<String> = flow {
        val apiKey = keychainProvider.load(model.provider.name)
            ?: throw Exception("API Key not set")

        val isDeepSeek = model.provider == AIProvider.DEEPSEEK
        val url = if (isDeepSeek) {
            "https://api.deepseek.com/chat/completions"
        } else {
            "https://api.openai.com/v1/chat/completions"
        }

        val isReasoner = model.modelName.contains("reasoner")

        val requestBody = ChatRequest(
            model = model.modelName,
            messages = listOf(
                ChatMessage(
                    role = "system",
                    content = "You are a helpful CBT therapist. Reframe the user's negative thoughts into positive, constructive perspectives. Keep it concise.",
                ),
                ChatMessage(
                    role = "user",
                    content = input,
                ),
            ),
            stream = true,
            maxTokens = if (isReasoner) 8192 else 2048,
        )

        httpClient.preparePost(url) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatRequest.serializer(), requestBody))
        }.execute { response ->
            val statusCode = response.status.value
            if (statusCode !in 200..299) {
                val bodyStr = response.bodyAsText()
                if (bodyStr.contains("Insufficient Balance", ignoreCase = true) ||
                    bodyStr.contains("insufficient", ignoreCase = true)
                ) {
                    throw Exception("DeepSeek 账户余额不足，请充值后重试")
                }
                throw Exception("HTTP Error: $statusCode $bodyStr")
            }

            val channel: ByteReadChannel = response.bodyAsChannel()
            var hasReasoning = false
            var hasContent = false
            var finishReason: String? = null

            while (currentCoroutineContext().isActive && !channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break

                // SSE spec: empty lines are boundary separators — skip them
                if (line.isBlank()) continue

                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()

                    // DeepSeek (and OpenAI) signal end-of-stream with [DONE]
                    if (data == "[DONE]") break

                    if (data.isEmpty()) continue

                    try {
                        val chunk = json.decodeFromString<ChatResponseChunk>(data)
                        val choice = chunk.choices.firstOrNull()
                        val delta = choice?.delta

                        if (choice?.finishReason != null) {
                            finishReason = choice.finishReason
                        }

                        // Track reasoning content (DeepSeek R1) but do not emit
                        if (!delta?.reasoningContent.isNullOrEmpty()) {
                            hasReasoning = true
                        }

                        // Emit content deltas to the flow
                        val content = delta?.content
                        if (!content.isNullOrEmpty()) {
                            hasContent = true
                            emit(content)
                        }
                    } catch (_: Exception) {
                        // Gracefully skip chunks that fail to parse as JSON
                        // (e.g. partial chunks or malformed lines)
                    }
                }
                // Lines without "data: " prefix are ignored per SSE spec
            }

            // Post-stream validation for DeepSeek Reasoner edge cases
            if (!hasContent && hasReasoning) {
                throw Exception(
                    "DeepSeek Reasoner 未返回最终正文（推理可能占满 token）。" +
                    "请重试，或改用「DeepSeek Chat」；若仍失败请检查账户额度与 API 文档。"
                )
            }
            if (finishReason == "length" && !hasContent) {
                throw Exception(
                    "DeepSeek 输出因长度被截断，无法保证完整内容。请缩短输入或重试。"
                )
            }
        }
    }
}

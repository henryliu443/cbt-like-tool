package com.henryliu.cbtreframe.shared

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.isActive
import kotlinx.coroutines.currentCoroutineContext

interface AIService {
    fun streamReframe(input: String, model: AIModel): Flow<String>
}

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    @kotlinx.serialization.SerialName("max_tokens")
    val maxTokens: Int = 2048
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponseChunk(
    val choices: List<ChatChoice> = emptyList()
)

@Serializable
data class ChatChoice(
    val delta: ChatDelta,
    @kotlinx.serialization.SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class ChatDelta(
    val content: String? = null,
    @kotlinx.serialization.SerialName("reasoning_content")
    val reasoningContent: String? = null
)

class AIServiceImpl(
    private val httpClient: HttpClient,
    private val keychainProvider: KeychainProvider
) : AIService {

    private val json = Json { ignoreUnknownKeys = true }

    override fun streamReframe(input: String, model: AIModel): Flow<String> = flow {
        val apiKey = keychainProvider.load(model.provider.name) ?: throw Exception("API Key not set")
        
        val isDeepSeek = model.provider == AIProvider.DEEPSEEK
        val url = if (isDeepSeek) {
            "https://api.deepseek.com/chat/completions"
        } else {
            "https://api.openai.com/v1/chat/completions"
        }

        val requestBody = ChatRequest(
            model = model.modelName,
            messages = listOf(
                ChatMessage(
                    role = "system",
                    content = "You are a helpful CBT therapist. Reframe the user's negative thoughts into positive, constructive perspectives. Keep it concise."
                ),
                ChatMessage(
                    role = "user",
                    content = input
                )
            ),
            stream = true,
            maxTokens = if (model.modelName.contains("reasoner")) 8192 else 2048
        )

        httpClient.preparePost(url) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatRequest.serializer(), requestBody))
        }.execute { response ->
            if (response.status.value !in 200..299) {
                val bodyStr = response.bodyAsText()
                if (bodyStr.contains("Insufficient Balance", ignoreCase = true) || bodyStr.contains("insufficient", ignoreCase = true)) {
                    throw Exception("DeepSeek 账户余额不足，请充值后重试")
                }
                throw Exception("HTTP Error: ${response.status.value} $bodyStr")
            }

            val channel: ByteReadChannel = response.bodyAsChannel()
            var hasReasoning = false
            var hasContent = false
            var finishReason: String? = null

            while (currentCoroutineContext().isActive && !channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    if (data.isNotEmpty()) {
                        try {
                            val chunk = json.decodeFromString<ChatResponseChunk>(data)
                            val choice = chunk.choices.firstOrNull()
                            val delta = choice?.delta
                            
                            if (choice?.finishReason != null) {
                                finishReason = choice.finishReason
                            }
                            
                            if (!delta?.reasoningContent.isNullOrEmpty()) {
                                hasReasoning = true
                                // Optionally, emit a special marker or just wait. 
                                // For CBT reframe, users don't usually want to see the reasoning mixed with content.
                            }
                            
                            val content = delta?.content
                            if (!content.isNullOrEmpty()) {
                                hasContent = true
                                emit(content)
                            }
                        } catch (e: Exception) {
                            // Ignore json parsing errors for partial chunks
                        }
                    }
                }
            }

            if (!hasContent && hasReasoning) {
                throw Exception("DeepSeek Reasoner 未返回最终正文（推理可能占满 token）。请重试，或改用「DeepSeek Chat」；若仍失败请检查账户额度与 API 文档。")
            }
            if (finishReason == "length" && !hasContent) {
                throw Exception("DeepSeek 输出因长度被截断，无法保证完整内容。请缩短输入或重试。")
            }
        }
    }
}

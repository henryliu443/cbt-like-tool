package com.henryliu.cbtreframe.shared

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import io.ktor.client.HttpClient
import io.ktor.client.request.post
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

interface AIService {
    fun streamReframe(input: String, model: AIModel): Flow<String>
}

class AIServiceImpl(private val httpClient: HttpClient) : AIService {
    // Note: We use a simple flow for the MVP to simulate or do real network calls.
    // For MVP Phase 1, if Ktor bodyAsChannel is too complex immediately, we can simulate SSE
    // But the plan says: "优先使用 Ktor Client 底层的 bodyAsChannel() 手动硬解析大模型流式响应"
    
    override fun streamReframe(input: String, model: AIModel): Flow<String> = flow {
        // Mocking for now, we will add real network parsing once this compiles.
        val words = "Thinking about: $input... This is a reframed thought from ${model.modelName}.".split(" ")
        for (word in words) {
            emit("$word ")
            delay(100)
        }
    }
}

fun createAIService(): AIService = AIServiceImpl(HttpClient())

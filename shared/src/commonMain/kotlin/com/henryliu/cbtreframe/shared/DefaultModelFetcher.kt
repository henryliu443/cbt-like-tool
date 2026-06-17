package com.henryliu.cbtreframe.shared

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

class DefaultModelFetcher(private val httpClient: HttpClient) : ModelFetcher {

    override suspend fun fetchModels(provider: AIProvider, apiKey: String): List<AIModel> {
        println("MODEL_FETCH ENTER provider=$provider")
        val trimmed = apiKey.trim()
        println("MODEL_FETCH APIKEY_EMPTY=${trimmed.isEmpty()}")
        if (trimmed.isEmpty()) return emptyList()

        val result = when (provider) {
            AIProvider.LOCAL -> listOf(ModelDisplayDictionary.LOCAL_BUILTIN)
            AIProvider.OPENAI -> fetchOpenAICompatible(
                baseURL = "https://api.openai.com/v1",
                apiKey = trimmed,
                provider = provider,
                filter = ::isOpenAIChatModel
            )
            AIProvider.DEEPSEEK -> fetchOpenAICompatible(
                baseURL = "https://api.deepseek.com/v1",
                apiKey = trimmed,
                provider = provider,
                filter = { it.lowercase().contains("deepseek") }
            )
            AIProvider.KIMI -> fetchOpenAICompatible(
                baseURL = "https://api.moonshot.cn/v1",
                apiKey = trimmed,
                provider = provider,
                filter = {
                    val l = it.lowercase()
                    l.contains("moonshot") || l.contains("kimi")
                }
            )
            AIProvider.ANTHROPIC -> fetchAnthropic(trimmed)
            AIProvider.GEMINI -> fetchGemini(trimmed)
        }

        println("MODEL_FETCH EXIT provider=$provider count=${result.size}")
        return result
    }

    @Serializable
    private data class OpenAIModelItem(val id: String)

    @Serializable
    private data class OpenAIModelsResponse(val data: List<OpenAIModelItem>)

    @Serializable
    private data class GeminiModelItem(
        val name: String,
        val displayName: String? = null,
        val supportedGenerationMethods: List<String>? = null
    )

    @Serializable
    private data class GeminiModelsResponse(val models: List<GeminiModelItem>? = null)

    private suspend fun fetchOpenAICompatible(
        baseURL: String,
        apiKey: String,
        provider: AIProvider,
        filter: (String) -> Boolean
    ): List<AIModel> {
        println("OPENAI_FETCH START baseURL=$baseURL")
        val response = httpClient.get("$baseURL/models") {
            header("Authorization", "Bearer $apiKey")
        }
        if (response.status.value != 200) {
            throw AIServiceError.HttpStatus(response.status.value)
        }
        val decoded = response.body<OpenAIModelsResponse>()
        println("OPENAI_FETCH RESPONSE status=${response.status.value} models=${decoded.data.size}")
        return decoded.data
            .map { it.id }
            .filter(filter)
            .map { id ->
                val displayName = ModelDisplayDictionary.getDisplayName(provider, id)
                AIModel(provider, id, displayName ?: prettyGenericName(id))
            }
            .sortedBy { it.modelName }
    }

    private suspend fun fetchAnthropic(apiKey: String): List<AIModel> {
        println("ANTHROPIC_FETCH START")
        val response = httpClient.get("https://api.anthropic.com/v1/models") {
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
        }
        if (response.status.value == 404) {
            return emptyList()
        }
        if (response.status.value != 200) {
            throw AIServiceError.HttpStatus(response.status.value)
        }
        val decoded = response.body<OpenAIModelsResponse>()
        println("ANTHROPIC_FETCH RESPONSE status=${response.status.value} models=${decoded.data.size}")
        return decoded.data
            .map { it.id }
            .filter { it.lowercase().contains("claude") }
            .map { id ->
                val displayName = ModelDisplayDictionary.getDisplayName(AIProvider.ANTHROPIC, id)
                AIModel(AIProvider.ANTHROPIC, id, displayName ?: prettyGenericName(id))
            }
            .sortedBy { it.modelName }
    }

    private suspend fun fetchGemini(apiKey: String): List<AIModel> {
        println("GEMINI_FETCH START")
        val response = httpClient.get("https://generativelanguage.googleapis.com/v1beta/models") {
            parameter("key", apiKey)
        }
        if (response.status.value != 200) {
            throw AIServiceError.HttpStatus(response.status.value)
        }
        val decoded = response.body<GeminiModelsResponse>()
        println("GEMINI_FETCH RESPONSE status=${response.status.value} models=${decoded.models?.size ?: 0}")
        val items = decoded.models ?: emptyList()
        return items.filter { m ->
            m.name.lowercase().contains("gemini") &&
            m.supportedGenerationMethods?.contains("generateContent") == true
        }.map { m ->
            val id = if (m.name.startsWith("models/")) m.name.removePrefix("models/") else m.name
            val label = if (id == "gemini-flash-latest") {
                "Gemini Flash Latest"
            } else if (!m.displayName.isNullOrBlank()) {
                m.displayName.trim()
            } else {
                prettyGenericName(id)
            }
            val displayName = ModelDisplayDictionary.getDisplayName(AIProvider.GEMINI, id)
            AIModel(AIProvider.GEMINI, id, displayName ?: label)
        }.sortedBy { it.displayName }
    }

    private fun isOpenAIChatModel(id: String): Boolean {
        val l = id.lowercase()
        if (l.contains("embedding") || l.contains("whisper") || l.contains("tts")) return false
        if (l.contains("moderation") || l.contains("dall-e") || l.contains("davinci")) return false
        if (l.contains("babbage") || l.contains("ada-") || l.contains("audio")) return false
        if (l.startsWith("gpt-") || l.startsWith("chatgpt-")) return true
        if (l.startsWith("o1") || l.startsWith("o3") || l.startsWith("o4")) return true
        return false
    }
}

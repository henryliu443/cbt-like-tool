#!/bin/bash
cat > JsonTest.kt <<'KOTLIN'
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class ChatCompletionMessage(
    val role: String,
    val content: String,
)

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

fun main() {
    val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }
    
    val bodyO1 = ChatCompletionBody(
        model = "o1",
        messages = emptyList(),
        temperature = null,
        maxTokens = null,
        maxCompletionTokens = 1024,
        reasoningEffort = "high",
        stream = false
    )
    
    println(json.encodeToString(ChatCompletionBody.serializer(), bodyO1))
}
KOTLIN

cat > build.gradle <<'GRADLE'
plugins {
    id 'org.jetbrains.kotlin.jvm' version '1.9.22'
    id 'org.jetbrains.kotlin.plugin.serialization' version '1.9.22'
    id 'application'
}
repositories { mavenCentral() }
dependencies {
    implementation 'org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3'
}
application {
    mainClass = 'JsonTestKt'
}
GRADLE

gradle run -q

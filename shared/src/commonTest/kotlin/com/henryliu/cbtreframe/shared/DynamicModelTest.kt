package com.henryliu.cbtreframe.shared

import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class DynamicModelTest {

    private fun supportsReasoningEffort(modelName: String) = modelName.startsWith("o1") || modelName.startsWith("o3")

    private fun createBody(modelName: String, depth: ThinkingTemplate.AnalysisDepth): ChatCompletionBody {
        val isO1OrO3 = supportsReasoningEffort(modelName)
        val maxTokensVal = 1024
        return ChatCompletionBody(
            model = modelName,
            messages = listOf(
                ChatCompletionMessage(role = "user", content = "test")
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
    }

    @Test
    fun testOpenAIReasoningEffortMapping() {
        // o1 model
        val o1Body = createBody("o1-preview", ThinkingTemplate.AnalysisDepth.deep)
        val o1Json = json.encodeToString(o1Body)

        assertTrue(o1Json.contains("\"reasoning_effort\":\"high\""), "o1 model should have reasoning_effort=high")
        assertFalse(o1Json.contains("\"temperature\":"), "o1 model should NOT have temperature")
        assertFalse(o1Json.contains("\"max_tokens\":"), "o1 model should NOT have max_tokens")
        assertTrue(o1Json.contains("\"max_completion_tokens\":1024"), "o1 model should have max_completion_tokens")

        // gpt-4o model
        val gpt4oBody = createBody("gpt-4o", ThinkingTemplate.AnalysisDepth.deep)
        val gpt4oJson = json.encodeToString(gpt4oBody)
        
        assertTrue(gpt4oJson.contains("\"temperature\":0.7"), "gpt-4o should have temperature")
        assertTrue(gpt4oJson.contains("\"max_tokens\":1024"), "gpt-4o should have max_tokens")
        assertFalse(gpt4oJson.contains("\"max_completion_tokens\":"), "gpt-4o should NOT have max_completion_tokens")
        assertFalse(gpt4oJson.contains("\"reasoning_effort\":"), "gpt-4o should NOT have reasoning_effort")

        // o3 model
        val o3Body = createBody("o3-mini", ThinkingTemplate.AnalysisDepth.balanced)
        val o3Json = json.encodeToString(o3Body)
        
        assertTrue(o3Json.contains("\"reasoning_effort\":\"medium\""), "o3 model should have reasoning_effort=medium")
        assertFalse(o3Json.contains("\"temperature\":"), "o3 model should NOT have temperature")
        assertFalse(o3Json.contains("\"max_tokens\":"), "o3 model should NOT have max_tokens")
        assertTrue(o3Json.contains("\"max_completion_tokens\":1024"), "o3 model should have max_completion_tokens")
    }
}

package com.henryliu.cbtreframe.shared

import kotlinx.coroutines.delay

/**
 * Collapsed Service: Retry Runner + Output Gate + Socratic Validation.
 *
 * Wraps an [AIServiceProtocol] implementation with retry logic and
 * post-processing to ensure output quality before returning results.
 */
class ValidatedReframeClient(
    private val service: AIServiceProtocol,
) {
    companion object {
        /** Retry budget for standard (non-Socratic) calls. */
        const val MAX_RETRIES_DEFAULT = 2
        /** Retry budget for Socratic template calls. */
        const val MAX_RETRIES_SOCRATIC = 3
        /** Backoff base in ms (linear: attempt * BACKOFF_MS). */
        const val BACKOFF_MS = 800L
    }

    /**
     * Execute [reframe] with retry logic, then run validation / sanitisation
     * on the returned [AnalysisResult].
     */
    suspend fun executeWithRetryAndValidation(
        thought: String,
        mood: String,
        hasAkathisia: Boolean,
        model: AIModel,
        depth: ThinkingTemplate.AnalysisDepth,
        style: ThinkingTemplate.AppResponseStyle,
        template: ThinkingTemplate,
        strategy: ResponseStrategy,
    ): AnalysisResult {
        val maxRetries = if (template == ThinkingTemplate.socratic) MAX_RETRIES_SOCRATIC else MAX_RETRIES_DEFAULT

        var lastError: Throwable? = null

        for (attempt in 0..maxRetries) {
            try {
                val raw = service.reframe(
                    thought = thought,
                    mood = mood,
                    hasAkathisia = hasAkathisia,
                    model = model,
                    depth = depth,
                    style = style,
                    template = template,
                    strategy = strategy,
                )
                return sanitize(raw, template)
            } catch (e: Exception) {
                lastError = e
                if (attempt < maxRetries) {
                    delay(BACKOFF_MS * (attempt + 1))
                }
            }
        }

        throw lastError ?: IllegalStateException("Retry exhausted with no captured error")
    }

    // ---- output gate / sanitisation ----

    private fun sanitize(raw: AnalysisResult, template: ThinkingTemplate): AnalysisResult {
        val cleaned = raw.normalized(template)
        return validateFields(cleaned, template)
    }

    /** Ensure every non-null text field is meaningfully present. */
    private fun validateFields(result: AnalysisResult, template: ThinkingTemplate): AnalysisResult {
        return when (template) {
            ThinkingTemplate.socratic -> validateSocratic(result)
            else -> {
                // CBT & behavioral: ensure key fields aren't blank placeholders
                result.copy(
                    distortion = requireMeaningful(result.distortion, "未识别"),
                    alternative = requireMeaningful(result.alternative, "暂无替代想法"),
                    action = requireMeaningful(result.action, "暂无建议行动"),
                )
            }
        }
    }

    /** Socratic-specific: questions list must be usable, action must be safe. */
    private fun validateSocratic(result: AnalysisResult): AnalysisResult {
        val questions = result.questions?.filter { it.isNotBlank() && !isPlaceholder(it) } ?: emptyList()

        val safeAction = if (result.action.isBlank() || isPlaceholder(result.action)) {
            "尝试回答上面的第一个问题，写下你想到的任何内容。"
        } else {
            result.action
        }

        return result.copy(
            questions = questions.ifEmpty {
                listOf(AnalysisResult.SocraticPlaceholderQuestion)
            },
            action = safeAction,
        )
    }

    private fun requireMeaningful(value: String, fallback: String): String =
        if (value.isBlank() || isPlaceholder(value)) fallback else value

    private fun isPlaceholder(text: String): Boolean {
        val lower = text.trim().lowercase()
        return lower == "n/a" ||
            lower == "-" ||
            lower == "null" ||
            lower == "none" ||
            lower.startsWith("（") && lower.endsWith("）") ||
            lower.startsWith("请稍等") ||
            lower.startsWith("wait") ||
            lower == "empty"
    }
}

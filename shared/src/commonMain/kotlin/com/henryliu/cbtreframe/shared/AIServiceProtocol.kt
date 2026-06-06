package com.henryliu.cbtreframe.shared

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface AIServiceProtocol {
    val provider: AIProvider

    suspend fun reframe(
        thought: String,
        mood: String,
        hasAkathisia: Boolean,
        model: AIModel,
        depth: ThinkingTemplate.AnalysisDepth,
        style: ThinkingTemplate.AppResponseStyle,
        template: ThinkingTemplate,
        strategy: ResponseStrategy,
    ): AnalysisResult

    fun streamReframe(
        thought: String,
        mood: String,
        hasAkathisia: Boolean,
        model: AIModel,
        depth: ThinkingTemplate.AnalysisDepth,
        style: ThinkingTemplate.AppResponseStyle,
        template: ThinkingTemplate,
        strategy: ResponseStrategy,
    ): Flow<String> = flow {
        val result = reframe(thought, mood, hasAkathisia, model, depth, style, template, strategy)
        val text = buildString {
            appendLine("认知扭曲：${result.distortion}")
            appendLine("替代想法：${result.alternative}")
            appendLine("建议行动：${result.action}")
        }
        emit(text)
    }

    suspend fun analyzeThoughtPatterns(
        thoughts: List<ThoughtEntry>,
        model: AIModel,
    ): ThoughtPatternReport
}

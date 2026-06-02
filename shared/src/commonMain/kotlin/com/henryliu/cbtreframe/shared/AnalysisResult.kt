package com.henryliu.cbtreframe.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class AnalysisResult(
    val distortion: String,
    val alternative: String,
    val action: String,
    val questions: List<String>? = null,
    val actions: List<String>? = null,
    @SerialName("state_assessment")
    val stateAssessment: String? = null
) {
    companion object {
        const val SocraticPlaceholderQuestion = "模型未返回有效问题，请重试或换用其他服务商。"

        val Empty = AnalysisResult(
            distortion = "",
            alternative = "",
            action = ""
        )

        fun empty(): AnalysisResult = Empty
    }

    fun normalized(template: ThinkingTemplate): AnalysisResult {
        val dist = if (distortion.isBlank()) "未识别" else distortion
        val alt = alternative

        return when (template) {
            ThinkingTemplate.cbt -> {
                AnalysisResult(
                    distortion = dist,
                    alternative = alt.ifBlank { "暂无替代想法" },
                    action = action.ifBlank { "暂无建议行动" },
                    questions = questions,
                    actions = actions,
                    stateAssessment = stateAssessment
                )
            }
            ThinkingTemplate.socratic -> {
                var qs = questions ?: emptyList()
                if (qs.isEmpty() && alternative.isNotBlank()) {
                    qs = alternative.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                }
                if (qs.isEmpty()) {
                    qs = listOf(SocraticPlaceholderQuestion)
                }
                val onlyPlaceholderQuestion = qs.size == 1 && qs[0] == SocraticPlaceholderQuestion
                val resolvedAction: String = if (action.isBlank()) {
                    if (onlyPlaceholderQuestion) {
                        "暂无有效引导问题，请先重试或换用其他服务商后再记录反思。"
                    } else {
                        "写下你对第一个问题的回答。"
                    }
                } else {
                    action
                }
                AnalysisResult(
                    distortion = if (dist == "未识别") "苏格拉底提问" else dist,
                    alternative = alt.ifBlank { "请结合下列问题逐步反思（无标准答案）。" },
                    action = resolvedAction,
                    questions = qs,
                    actions = actions,
                    stateAssessment = stateAssessment
                )
            }
            ThinkingTemplate.behavioral -> {
                val state = stateAssessment?.takeIf { it.isNotBlank() }
                AnalysisResult(
                    distortion = if (dist == "未识别") "行为聚焦" else dist,
                    alternative = alt.ifBlank { "先关注下一步可执行的小行动。" },
                    action = action.ifBlank { "选择一个 5 分钟内可完成的小步骤。" },
                    questions = questions,
                    actions = actions,
                    stateAssessment = state ?: "（可先简短描述你现在的精力与情绪）"
                )
            }
        }
    }
}

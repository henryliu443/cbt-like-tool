package com.henryliu.cbtreframe.ui

import com.henryliu.cbtreframe.shared.ThinkingTemplate
import com.henryliu.cbtreframe.shared.AIProvider

fun ThinkingTemplate.AnalysisDepth.displayName(): String = when (this) {
    ThinkingTemplate.AnalysisDepth.fast -> "快速"
    ThinkingTemplate.AnalysisDepth.balanced -> "平衡"
    ThinkingTemplate.AnalysisDepth.deep -> "深度"
}

fun ThinkingTemplate.AppResponseStyle.displayName(): String = when (this) {
    ThinkingTemplate.AppResponseStyle.concise -> "简洁直接"
    ThinkingTemplate.AppResponseStyle.coach -> "教练引导"
    ThinkingTemplate.AppResponseStyle.supportive -> "温暖支持"
}

fun ThinkingTemplate.displayName(): String = when (this) {
    ThinkingTemplate.cbt -> "CBT 认知行为"
    ThinkingTemplate.socratic -> "苏格拉底提问"
    ThinkingTemplate.behavioral -> "行为激活"
}

fun AIProvider.displayName(): String = when (this) {
    AIProvider.DEEPSEEK -> "DeepSeek"
    AIProvider.OPENAI -> "OpenAI"
    AIProvider.ANTHROPIC -> "Anthropic"
    AIProvider.GEMINI -> "Google Gemini"
    AIProvider.KIMI -> "月之暗面 Kimi"
    AIProvider.LOCAL -> "本地模型"
}

fun com.henryliu.cbtreframe.shared.ThinkingTemplate.AnalysisDepth.description(): String = when (this) {
    com.henryliu.cbtreframe.shared.ThinkingTemplate.AnalysisDepth.fast -> "快速分析核心问题"
    com.henryliu.cbtreframe.shared.ThinkingTemplate.AnalysisDepth.balanced -> "平衡的深度与广度"
    com.henryliu.cbtreframe.shared.ThinkingTemplate.AnalysisDepth.deep -> "深入探讨各层面"
}

fun com.henryliu.cbtreframe.shared.ThinkingTemplate.AppResponseStyle.description(): String = when (this) {
    com.henryliu.cbtreframe.shared.ThinkingTemplate.AppResponseStyle.concise -> "直接给出现实建议"
    com.henryliu.cbtreframe.shared.ThinkingTemplate.AppResponseStyle.coach -> "提供引导与反馈"
    com.henryliu.cbtreframe.shared.ThinkingTemplate.AppResponseStyle.supportive -> "给予充分共情与支持"
}

fun com.henryliu.cbtreframe.shared.ThinkingTemplate.description(): String = when (this) {
    com.henryliu.cbtreframe.shared.ThinkingTemplate.cbt -> "认知行为疗法，识别认知扭曲"
    com.henryliu.cbtreframe.shared.ThinkingTemplate.socratic -> "通过层层追问发现真理"
    com.henryliu.cbtreframe.shared.ThinkingTemplate.behavioral -> "鼓励行动打破负向循环"
}

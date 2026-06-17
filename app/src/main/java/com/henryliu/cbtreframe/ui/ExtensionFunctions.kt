package com.henryliu.cbtreframe.ui

import com.henryliu.cbtreframe.shared.ThinkingTemplate
import com.henryliu.cbtreframe.shared.AIProvider

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.ui.graphics.vector.ImageVector

val ThinkingTemplate.icon: ImageVector
    get() = when (this) {
        ThinkingTemplate.cbt -> Icons.Default.Lightbulb
        ThinkingTemplate.socratic -> Icons.Default.QuestionAnswer
        ThinkingTemplate.behavioral -> Icons.Default.DirectionsRun
    }

val ThinkingTemplate.label: String
    get() = when (this) {
        ThinkingTemplate.cbt -> "CBT\n标准"
        ThinkingTemplate.socratic -> "苏格拉底\n提问"
        ThinkingTemplate.behavioral -> "行为\n激活"
    }

fun ThinkingTemplate.AnalysisDepth.displayName(): String = when (this) {
    ThinkingTemplate.AnalysisDepth.fast -> "快速"
    ThinkingTemplate.AnalysisDepth.balanced -> "平衡"
    ThinkingTemplate.AnalysisDepth.deep -> "深度"
}

fun ThinkingTemplate.AppResponseStyle.displayName(): String = when (this) {
    ThinkingTemplate.AppResponseStyle.concise -> "简洁"
    ThinkingTemplate.AppResponseStyle.coach -> "教练式"
    ThinkingTemplate.AppResponseStyle.supportive -> "温暖支持"
}

fun ThinkingTemplate.displayName(): String = when (this) {
    ThinkingTemplate.cbt -> "CBT 重构"
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
    com.henryliu.cbtreframe.shared.ThinkingTemplate.AnalysisDepth.fast -> "简短回复，适合快速调整"
    com.henryliu.cbtreframe.shared.ThinkingTemplate.AnalysisDepth.balanced -> "标准分析"
    com.henryliu.cbtreframe.shared.ThinkingTemplate.AnalysisDepth.deep -> "更详细的推理与步骤"
}

fun com.henryliu.cbtreframe.shared.ThinkingTemplate.AppResponseStyle.description(): String = when (this) {
    com.henryliu.cbtreframe.shared.ThinkingTemplate.AppResponseStyle.concise -> "直接给出分析结果"
    com.henryliu.cbtreframe.shared.ThinkingTemplate.AppResponseStyle.coach -> "像教练一样引导你思考"
    com.henryliu.cbtreframe.shared.ThinkingTemplate.AppResponseStyle.supportive -> "温柔、有同理心的回应"
}

fun com.henryliu.cbtreframe.shared.ThinkingTemplate.description(): String = when (this) {
    com.henryliu.cbtreframe.shared.ThinkingTemplate.cbt -> "识别认知扭曲，提供替代想法"
    com.henryliu.cbtreframe.shared.ThinkingTemplate.socratic -> "通过提问引导你自己发现答案"
    com.henryliu.cbtreframe.shared.ThinkingTemplate.behavioral -> "聚焦下一步行动"
}

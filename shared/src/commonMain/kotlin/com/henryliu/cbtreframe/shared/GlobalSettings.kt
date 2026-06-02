package com.henryliu.cbtreframe.shared

import kotlinx.serialization.Serializable

@Serializable
data class GlobalSettings(
    val thinkingTemplate: ThinkingTemplate = ThinkingTemplate.cbt,
    val analysisDepth: ThinkingTemplate.AnalysisDepth = ThinkingTemplate.AnalysisDepth.balanced,
    val responseStyle: ThinkingTemplate.AppResponseStyle = ThinkingTemplate.AppResponseStyle.supportive,
) {
    companion object {
        val Default = GlobalSettings()
    }
}

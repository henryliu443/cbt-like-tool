package com.henryliu.cbtreframe.shared

import kotlinx.serialization.Serializable

@Serializable
enum class ThinkingTemplate {
    cbt,
    socratic,
    behavioral;

    @Serializable
    enum class AnalysisDepth {
        fast,
        balanced,
        deep;
    }

    @Serializable
    enum class AppResponseStyle {
        concise,
        coach,
        supportive;
    }
}

package com.henryliu.cbtreframe.shared

import kotlinx.serialization.Serializable

@Serializable
data class ThoughtEntry(
    val id: String,
    val content: String,
    val situation: String = "",
    val emotion: String = "",
    val intensity: Int = 5,
    val beliefBefore: Int = 50,
    val beliefAfter: Int = 50,
    val evidenceFor: String = "",
    val evidenceAgainst: String = "",
    val balancedThought: String = "",
    val distortionTag: String = "",
    val isProcessed: Boolean = false,
    val createdAt: Long = 0L, // epoch millis
)

@Serializable
data class ThoughtPatternReport(
    val topDistortions: List<DistortionCount>,
    val overallPattern: String,
    val suggestion: String,
) {
    @Serializable
    data class DistortionCount(
        val name: String,
        val count: Int,
        val example: String,
    )
}

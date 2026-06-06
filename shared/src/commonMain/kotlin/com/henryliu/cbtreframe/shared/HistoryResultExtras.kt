package com.henryliu.cbtreframe.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class HistoryResultExtras(
    val questions: List<String>? = null,
    val actions: List<String>? = null,
    @SerialName("state_assessment")
    val stateAssessment: String? = null
)

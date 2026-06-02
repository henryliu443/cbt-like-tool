package com.henryliu.cbtreframe.shared.models

data class HistoryEntry(
    val id: String,
    val inputText: String,
    val timestamp: Long,
    val aiResponse: String?
)

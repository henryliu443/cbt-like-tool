package com.henryliu.cbtreframe.shared.models

data class MoodCheckIn(
    val id: String,
    val emotion: String?,
    val intensity: Int?,
    val timestamp: Long
)

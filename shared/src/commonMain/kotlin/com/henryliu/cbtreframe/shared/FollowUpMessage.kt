package com.henryliu.cbtreframe.shared

import kotlinx.serialization.Serializable

@Serializable
data class FollowUpMessage(
    val id: String = com.benasher44.uuid.uuid4().toString(),
    val role: String,
    val text: String
)

package com.henryliu.cbtreframe.shared.viewmodels

sealed interface StreamingState {
    data object Idle : StreamingState
    data class Receiving(val text: String) : StreamingState
    data object Completed : StreamingState
    data class Failed(val error: String) : StreamingState
}

package com.henryliu.cbtreframe.shared.viewmodels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class HomeViewModel {

    private val _streamingState = MutableStateFlow<StreamingState?>(null)
    val streamingState: StateFlow<StreamingState?> = _streamingState.asStateFlow()

    fun analyzeThought(thought: String) {
        // Dummy implementation for analyzing thought
    }
}

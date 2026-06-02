package com.henryliu.cbtreframe.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import com.benasher44.uuid.uuid4

class ReframeViewModel(
    private val aiService: AIService,
    private val historyRepository: HistoryRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _uiState = MutableStateFlow(ReframeUiState())
    val uiState: StateFlow<ReframeUiState> = _uiState.asStateFlow()

    fun reframe(input: String, model: AIModel) {
        if (input.isBlank()) return
        
        _uiState.value = _uiState.value.copy(isLoading = true, response = "")
        
        scope.launch {
            var finalResponse = ""
            aiService.streamReframe(input, model)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
                .onCompletion {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // Save to history upon completion
                    if (finalResponse.isNotBlank() && _uiState.value.error == null) {
                        historyRepository.addHistory(
                            id = uuid4().toString(),
                            originalThought = input,
                            reframedThought = finalResponse,
                            modelName = model.modelName,
                            timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                        )
                    }
                }
                .collect { chunk ->
                    finalResponse += chunk
                    _uiState.value = _uiState.value.copy(
                        response = finalResponse
                    )
                }
        }
    }
}

data class ReframeUiState(
    val response: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

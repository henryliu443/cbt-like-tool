package com.henryliu.cbtreframe.shared

import com.henryliu.cbtreframe.shared.db.HistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: HistoryRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    val history: StateFlow<List<HistoryEntity>> = repository.getHistory()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteItem(id: String) {
        scope.launch(Dispatchers.IO) {
            repository.deleteHistory(id)
        }
    }
}

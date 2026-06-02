package com.henryliu.cbtreframe.shared.viewmodels

import com.henryliu.cbtreframe.shared.db.HistoryEntity
import com.henryliu.cbtreframe.shared.HistoryRepository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.datetime.*

data class HistoryUiState(
    val searchText: String = "",
    val groupedEntries: List<DateGroup> = emptyList(),
)

data class DateGroup(
    val dateLabel: String,
    val entries: List<HistoryEntity>,
)

class HistoryViewModel(
    private val repository: HistoryRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) {
    private val _searchText = MutableStateFlow("")

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    val history: StateFlow<List<HistoryEntity>> = repository.getHistory()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    init {
        scope.launch {
            combine(
                history,
                _searchText
            ) { entries, search ->
                computeState(entries, search)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun clear() {
        scope.cancel()
    }

    fun setSearchText(text: String) {
        _searchText.value = text
    }

    fun deleteItem(id: String) {
        scope.launch {
            repository.deleteHistory(id)
        }
    }

    private fun computeState(
        entries: List<HistoryEntity>,
        searchText: String,
    ): HistoryUiState {
        var filtered = entries
        if (searchText.isNotBlank()) {
            val query = searchText.lowercase()
            filtered = filtered.filter { entry ->
                entry.inputText.lowercase().contains(query) ||
                (entry.aiResponse?.lowercase()?.contains(query) ?: false)
            }
        }
        val sorted = filtered.sortedByDescending { it.timestamp }
        val grouped = groupByDate(sorted)

        return HistoryUiState(
            searchText = searchText,
            groupedEntries = grouped,
        )
    }

    private fun groupByDate(entries: List<HistoryEntity>): List<DateGroup> {
        val grouped = entries.groupBy { entry ->
            val instant = Instant.fromEpochMilliseconds(entry.timestamp)
            val date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
            formatDate(date)
        }
        return grouped.map { (label, list) -> DateGroup(label, list) }
    }

    private fun formatDate(date: LocalDate): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return when {
            date == now -> "今天"
            date == now.minus(1, DateTimeUnit.DAY) -> "昨天"
            else -> "${date.year}年${date.monthNumber}月${date.dayOfMonth}日"
        }
    }

    companion object {
        fun formatTimestamp(epochMillis: Long): String {
            val instant = Instant.fromEpochMilliseconds(epochMillis)
            val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            return when {
                dt.date == now.date -> "今天 ${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
                dt.date == now.date.minus(1, DateTimeUnit.DAY) -> "昨天 ${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
                else -> "${dt.year}/${dt.monthNumber}/${dt.dayOfMonth} ${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
            }
        }
    }
}

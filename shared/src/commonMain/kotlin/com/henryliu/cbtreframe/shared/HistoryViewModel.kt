package com.henryliu.cbtreframe.shared

import com.henryliu.cbtreframe.shared.db.HistoryEntity
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
    val showFavoritesOnly: Boolean = false,
    val groupedEntries: List<DateGroup> = emptyList(),
    val weeklyCount: Int = 0,
    val weeklyFavoriteCount: Int = 0,
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
    private val _showFavoritesOnly = MutableStateFlow(false)

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
                _searchText,
                _showFavoritesOnly,
            ) { entries, search, favOnly ->
                computeState(entries, search, favOnly)
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

    fun setShowFavoritesOnly(show: Boolean) {
        _showFavoritesOnly.value = show
    }

    fun toggleFavorite(entry: HistoryEntity) {
        val newFav = if (entry.isFavorite == 0L) 1L else 0L
        repository.updateFavorite(entry.id, newFav)
    }

    fun deleteItem(id: String) {
        scope.launch {
            repository.deleteHistory(id)
        }
    }

    private fun computeState(
        entries: List<HistoryEntity>,
        searchText: String,
        showFavoritesOnly: Boolean,
    ): HistoryUiState {
        var filtered = entries
        if (showFavoritesOnly) {
            filtered = filtered.filter { it.isFavorite != 0L }
        }
        if (searchText.isNotBlank()) {
            val query = searchText.lowercase()
            filtered = filtered.filter { entry ->
                entry.inputThought.lowercase().contains(query) ||
                entry.distortion.lowercase().contains(query) ||
                entry.alternative.lowercase().contains(query) ||
                entry.action.lowercase().contains(query) ||
                entry.moodTag.lowercase().contains(query) ||
                entry.providerName.lowercase().contains(query) ||
                entry.modelName.lowercase().contains(query) ||
                entry.therapyTemplateRaw.lowercase().contains(query)
            }
        }
        val sorted = filtered.sortedByDescending { it.createdAt }
        val grouped = groupByDate(sorted)

        val weekStats = weeklyStats(entries)

        return HistoryUiState(
            searchText = searchText,
            showFavoritesOnly = showFavoritesOnly,
            groupedEntries = grouped,
            weeklyCount = weekStats.first,
            weeklyFavoriteCount = weekStats.second,
        )
    }

    private fun groupByDate(entries: List<HistoryEntity>): List<DateGroup> {
        val grouped = entries.groupBy { entry ->
            val instant = Instant.fromEpochMilliseconds(entry.createdAt)
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

    private fun weeklyStats(entries: List<HistoryEntity>): Pair<Int, Int> {
        val now = Clock.System.now()
        val weekAgo = now.minus(7, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
        val weekAgoEpoch = weekAgo.toEpochMilliseconds()
        val thisWeek = entries.filter { it.createdAt >= weekAgoEpoch }
        val favorites = thisWeek.filter { it.isFavorite != 0L }
        return Pair(thisWeek.size, favorites.size)
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

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

import com.henryliu.cbtreframe.shared.FollowUpMessage
import com.henryliu.cbtreframe.shared.ReframeOrchestrator
import com.henryliu.cbtreframe.shared.SettingsManager
import com.henryliu.cbtreframe.shared.KeychainProvider
import io.ktor.client.HttpClient
import kotlinx.serialization.encodeToString

data class HistoryUiState(
    val searchText: String = "",
    val showFavoritesOnly: Boolean = false,
    val groupedEntries: List<DateGroup> = emptyList(),
)

data class DateGroup(
    val dateLabel: String,
    val entries: List<HistoryEntity>,
)

class HistoryViewModel(
    private val repository: HistoryRepository,
    private val settingsManager: SettingsManager,
    private val httpClient: HttpClient,
    private val keychainProvider: KeychainProvider,
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
                _showFavoritesOnly
            ) { entries, search, favoritesOnly ->
                computeState(entries, search, favoritesOnly)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun clear() {
        scope.cancel()
    }

    suspend fun sendFollowUpMessage(
        entryId: String,
        originalThought: String,
        lastConclusion: String,
        messages: List<FollowUpMessage>,
        newText: String,
        templateRaw: String,
        providerRaw: String,
        modelRaw: String
    ): FollowUpMessage {
        val userMsg = FollowUpMessage(role = "user", text = newText)
        val contextMessages = messages + userMsg

        val promptContext = buildString {
            appendLine("Original Thought: $originalThought")
            appendLine("Last Conclusion: $lastConclusion")
            appendLine("Recent Conversation:")
            contextMessages.takeLast(6).forEach {
                appendLine("${it.role.uppercase()}: ${it.text}")
            }
        }

        val provider = com.henryliu.cbtreframe.shared.AIProvider.entries.firstOrNull { it.name == providerRaw }
            ?: com.henryliu.cbtreframe.shared.AIProvider.OPENAI
        val model = com.henryliu.cbtreframe.shared.FallbackModels.entries.firstOrNull { it.provider == provider && it.modelName == modelRaw }
            ?: com.henryliu.cbtreframe.shared.AIModel(provider, modelRaw, com.henryliu.cbtreframe.shared.prettyGenericName(modelRaw))

        val settings = settingsManager.loadSettings()
        
        val result = ReframeOrchestrator.runReframe(
            thought = promptContext,
            mood = "Follow-up",
            hasAkathisia = false,
            model = model,
            settings = settings,
            strategy = com.henryliu.cbtreframe.shared.ResponseStrategy.cbtNormal,
            httpClient = httpClient,
            apiKeyProvider = { p -> keychainProvider.load(p) }
        )

        val template = com.henryliu.cbtreframe.shared.ThinkingTemplate.entries.firstOrNull { it.name == templateRaw }
            ?: settings.thinkingTemplate
        val normalized = result.normalized(template)
        
        val responseText = buildString {
            if (normalized.distortion.isNotBlank()) appendLine(normalized.distortion)
            if (normalized.alternative.isNotBlank()) {
                if (isNotEmpty()) appendLine()
                appendLine(normalized.alternative)
            }
            if (normalized.action.isNotBlank()) {
                if (isNotEmpty()) appendLine()
                appendLine(normalized.action)
            }
        }.trim()

        val aiMsg = FollowUpMessage(role = "assistant", text = responseText)
        
        val updatedMessages = contextMessages + aiMsg
        persistFollowUpMessages(entryId, updatedMessages)
        
        return aiMsg
    }

    fun persistFollowUpMessages(entryId: String, messages: List<FollowUpMessage>) {
        val jsonString = kotlinx.serialization.json.Json.encodeToString(messages)
        scope.launch {
            repository.updateFollowUpMessages(entryId, jsonString)
        }
    }

    fun setSearchText(text: String) {
        _searchText.value = text
    }

    fun setShowFavoritesOnly(only: Boolean) {
        _showFavoritesOnly.value = only
    }

    fun toggleFavorite(entry: HistoryEntity) {
        scope.launch {
            val nextFav = if (entry.isFavorite == 0L) 1L else 0L
            repository.toggleFavorite(entry.id, nextFav)
        }
    }

    fun deleteItem(id: String) {
        scope.launch {
            repository.deleteHistory(id)
        }
    }

    fun weeklyStats(entries: List<HistoryEntity>): Pair<Int, Int> {
        val now = Clock.System.now().toEpochMilliseconds()
        val weekAgo = now - 7 * 24 * 60 * 60 * 1000L
        val thisWeek = entries.filter { it.timestamp >= weekAgo }
        val favorites = thisWeek.filter { it.isFavorite == 1L }
        return Pair(thisWeek.size, favorites.size)
    }


    private fun computeState(
        entries: List<HistoryEntity>,
        searchText: String,
        showFavoritesOnly: Boolean
    ): HistoryUiState {
        var filtered = entries
        if (showFavoritesOnly) {
            filtered = filtered.filter { it.isFavorite == 1L }
        }
        if (searchText.isNotBlank()) {
            val query = searchText.lowercase()
            filtered = filtered.filter { entry ->
                entry.inputText.lowercase().contains(query) ||
                entry.distortion.lowercase().contains(query) ||
                entry.alternative.lowercase().contains(query) ||
                entry.action.lowercase().contains(query) ||
                entry.moodTag.lowercase().contains(query) ||
                entry.providerName.lowercase().contains(query) ||
                entry.modelName.lowercase().contains(query) ||
                entry.therapyTemplateRaw.lowercase().contains(query) ||
                (entry.aiResponse?.lowercase()?.contains(query) ?: false)
            }
        }
        val sorted = filtered.sortedByDescending { it.timestamp }
        val grouped = groupByDate(sorted)

        return HistoryUiState(
            searchText = searchText,
            showFavoritesOnly = showFavoritesOnly,
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

package com.henryliu.cbtreframe.shared.viewmodels

import com.henryliu.cbtreframe.shared.db.AppDatabase
import com.henryliu.cbtreframe.shared.db.GetMoodInsightsGroupedByDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

data class MoodInsightsUiState(
    val timeFilter: TimeFilter = TimeFilter.DAYS_7,
    val dataPoints: List<InsightDataPoint> = emptyList(),
    val isLoading: Boolean = false
)

enum class TimeFilter(val days: Int) {
    DAYS_7(7),
    DAYS_30(30),
    DAYS_90(90)
}

data class InsightDataPoint(
    val dateLabel: String,
    val avgIntensity: Double,
    val reframeFrequency: Long
)

class MoodInsightsViewModel(
    private val database: AppDatabase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _timeFilter = MutableStateFlow(TimeFilter.DAYS_7)
    
    // According to matrix: stateIn(sharingStarted = SharingStarted.WhileSubscribed(5000))
    val uiState: StateFlow<MoodInsightsUiState> = _timeFilter
        .map { filter ->
            val now = Clock.System.now()
            val since = now.minus(filter.days, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            val offset = getSqliteOffsetModifier()
            
            val queryResult = database.appDatabaseQueries.getMoodInsightsGroupedByDate(
                offsetModifier = offset,
                sinceTimestamp = since.toEpochMilliseconds()
            ).executeAsList()
            
            MoodInsightsUiState(
                timeFilter = filter,
                dataPoints = queryResult.map { 
                    InsightDataPoint(
                        dateLabel = it.dateLabel ?: "",
                        avgIntensity = it.avgIntensity ?: 0.0,
                        reframeFrequency = it.reframeFrequency
                    )
                }
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MoodInsightsUiState(isLoading = true)
        )
        
    fun setTimeFilter(filter: TimeFilter) {
        _timeFilter.value = filter
    }
    
    private fun getSqliteOffsetModifier(): String {
        val offset = TimeZone.currentSystemDefault().offsetAt(Clock.System.now())
        val totalSeconds = offset.totalSeconds
        val hours = totalSeconds / 3600
        val minutes = (kotlin.math.abs(totalSeconds) % 3600) / 60
        val sign = if (hours >= 0) "+" else "-"
        val hStr = kotlin.math.abs(hours).toString().padStart(2, '0')
        val mStr = minutes.toString().padStart(2, '0')
        return "$sign$hStr:$mStr"
    }
}

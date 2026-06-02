package com.henryliu.cbtreframe.shared

import com.russhwolf.settings.Settings
import kotlinx.datetime.*

data class StreakData(
    val current: Int,
    val longest: Int,
)

class StreakService(private val settings: Settings) {
    companion object {
        private const val KEY_CURRENT = "streak.current"
        private const val KEY_LONGEST = "streak.longest"
        private const val KEY_LAST_DATE = "streak.lastDate"
        private const val KEY_TODAY_COUNT = "todayAnalysisCount"
        private const val KEY_TODAY_DATE = "todayAnalysisDate"
    }

    fun loadStreak(): StreakData {
        return StreakData(
            current = settings.getInt(KEY_CURRENT, 0),
            longest = settings.getInt(KEY_LONGEST, 0),
        )
    }

    fun markToday(): StreakData {
        val now = Clock.System.now()
        val today = todayDate(now)
        val lastEpoch = settings.getLong(KEY_LAST_DATE, 0L)
        val lastDate = if (lastEpoch > 0) Instant.fromEpochMilliseconds(lastEpoch) else null

        var current = settings.getInt(KEY_CURRENT, 0)
        var longest = settings.getInt(KEY_LONGEST, 0)

        if (lastDate != null) {
            val lastToday = todayDate(lastDate)
            if (lastToday == today) {
                return StreakData(current, longest)
            }
            val dayDiff = daysBetween(lastToday, today)
            current = if (dayDiff == 1) current + 1 else 1
        } else {
            current = 1
        }

        longest = maxOf(longest, current)
        settings.putInt(KEY_CURRENT, current)
        settings.putInt(KEY_LONGEST, longest)
        settings.putLong(KEY_LAST_DATE, now.toEpochMilliseconds())
        return StreakData(current, longest)
    }

    fun todayAnalysisCount(): Int {
        val now = Clock.System.now()
        val today = todayDate(now)
        val storedEpoch = settings.getLong(KEY_TODAY_DATE, 0L)
        if (storedEpoch > 0) {
            val stored = Instant.fromEpochMilliseconds(storedEpoch)
            if (todayDate(stored) == today) {
                return settings.getInt(KEY_TODAY_COUNT, 0)
            }
        }
        return 0
    }

    fun incrementTodayCount(): Int {
        val now = Clock.System.now()
        val today = todayDate(now)
        val storedEpoch = settings.getLong(KEY_TODAY_DATE, 0L)
        val nextCount: Int
        if (storedEpoch > 0) {
            val stored = Instant.fromEpochMilliseconds(storedEpoch)
            if (todayDate(stored) == today) {
                nextCount = settings.getInt(KEY_TODAY_COUNT, 0) + 1
            } else {
                settings.putLong(KEY_TODAY_DATE, now.toEpochMilliseconds())
                nextCount = 1
            }
        } else {
            settings.putLong(KEY_TODAY_DATE, now.toEpochMilliseconds())
            nextCount = 1
        }
        settings.putInt(KEY_TODAY_COUNT, nextCount)
        return nextCount
    }

    private fun todayDate(instant: Instant): LocalDate {
        return instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    private fun daysBetween(from: LocalDate, to: LocalDate): Int {
        return from.daysUntil(to)
    }
}

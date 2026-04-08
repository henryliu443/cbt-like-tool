import Foundation

@MainActor
final class StreakService {
    private let defaults = UserDefaults.standard
    private let currentKey = "streak.current"
    private let longestKey = "streak.longest"
    private let lastDateKey = "streak.lastDate"
    private let todayCountKey = "todayAnalysisCount"
    private let todayDateKey = "todayAnalysisDate"

    func loadStreak() -> (current: Int, longest: Int) {
        (
            current: defaults.integer(forKey: currentKey),
            longest: defaults.integer(forKey: longestKey)
        )
    }

    func markToday() -> (current: Int, longest: Int) {
        let cal = Calendar.current
        let now = Date()
        let last = defaults.object(forKey: lastDateKey) as? Date
        var current = defaults.integer(forKey: currentKey)
        var longest = defaults.integer(forKey: longestKey)

        if let last, cal.isDate(last, inSameDayAs: now) {
            return (current, longest)
        }
        if let last, let delta = cal.dateComponents([.day], from: cal.startOfDay(for: last), to: cal.startOfDay(for: now)).day, delta == 1 {
            current += 1
        } else {
            current = 1
        }
        longest = max(longest, current)
        defaults.set(current, forKey: currentKey)
        defaults.set(longest, forKey: longestKey)
        defaults.set(now, forKey: lastDateKey)
        return (current, longest)
    }

    func todayAnalysisCount() -> Int {
        let today = Calendar.current.startOfDay(for: Date())
        if let stored = defaults.object(forKey: todayDateKey) as? Date,
           Calendar.current.isDate(stored, inSameDayAs: today) {
            return defaults.integer(forKey: todayCountKey)
        }
        return 0
    }

    func incrementTodayCount() -> Int {
        let today = Calendar.current.startOfDay(for: Date())
        let nextCount: Int
        if let stored = defaults.object(forKey: todayDateKey) as? Date,
           Calendar.current.isDate(stored, inSameDayAs: today) {
            nextCount = defaults.integer(forKey: todayCountKey) + 1
        } else {
            defaults.set(today, forKey: todayDateKey)
            nextCount = 1
        }
        defaults.set(nextCount, forKey: todayCountKey)
        return nextCount
    }
}

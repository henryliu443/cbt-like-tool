import Foundation

@MainActor
final class StreakService: ObservableObject {
    @Published private(set) var currentStreak: Int = 0
    @Published private(set) var longestStreak: Int = 0

    private let defaults = UserDefaults.standard
    private let currentKey = "streak.current"
    private let longestKey = "streak.longest"
    private let lastDateKey = "streak.lastDate"

    init() {
        currentStreak = defaults.integer(forKey: currentKey)
        longestStreak = defaults.integer(forKey: longestKey)
    }

    func markToday() {
        let cal = Calendar.current
        let now = Date()
        let last = defaults.object(forKey: lastDateKey) as? Date

        if let last, cal.isDate(last, inSameDayAs: now) {
            return
        }
        if let last, let delta = cal.dateComponents([.day], from: cal.startOfDay(for: last), to: cal.startOfDay(for: now)).day, delta == 1 {
            currentStreak += 1
        } else {
            currentStreak = 1
        }
        longestStreak = max(longestStreak, currentStreak)
        defaults.set(currentStreak, forKey: currentKey)
        defaults.set(longestStreak, forKey: longestKey)
        defaults.set(now, forKey: lastDateKey)
    }
}

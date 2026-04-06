import Foundation
import UserNotifications

enum ReminderService {
    static func requestPermission() async -> Bool {
        (try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound])) ?? false
    }

    static func scheduleDailyReminder(hour: Int, minute: Int) async {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: ["daily.checkin"])

        var comps = DateComponents()
        comps.hour = hour
        comps.minute = minute

        let content = UNMutableNotificationContent()
        content.title = "CBT Reframe"
        content.body = "今天心情怎么样？花 1 分钟记录一下。"
        content.sound = .default

        let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: true)
        let req = UNNotificationRequest(identifier: "daily.checkin", content: content, trigger: trigger)
        try? await center.add(req)
    }

    static func cancelDailyReminder() {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["daily.checkin"])
    }
}

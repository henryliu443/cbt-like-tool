import Foundation
#if !SKIP
import UserNotifications
#else
import android.content.Context
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit
#endif

enum ReminderService {
    static func requestPermission() async -> Bool {
        #if !SKIP
        return (try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound])) ?? false
        #else
        // For Android 13+ (API 33+), POST_NOTIFICATIONS is required.
        // In a full implementation, this should trigger the Activity permission request.
        return true
        #endif
    }

    static func scheduleDailyReminder(hour: Int, minute: Int) async {
        #if !SKIP
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
        #else
        guard let context = _getAndroidContext() else { return }

        let calendar = Calendar.getInstance()
        let now = calendar.getTimeInMillis()

        calendar.set(Calendar.HOUR_OF_DAY, Int32(hour))
        calendar.set(Calendar.MINUTE, Int32(minute))
        calendar.set(Calendar.SECOND, 0)

        var targetTime = calendar.getTimeInMillis()
        if targetTime <= now {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            targetTime = calendar.getTimeInMillis()
        }

        let initialDelay = targetTime - now

        let workRequest = PeriodicWorkRequest.Builder(
            ReminderWorker.self,
            1, TimeUnit.DAYS
        )
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily.checkin",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
        #endif
    }

    static func cancelDailyReminder() {
        #if !SKIP
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["daily.checkin"])
        #else
        guard let context = _getAndroidContext() else { return }
        WorkManager.getInstance(context).cancelUniqueWork("daily.checkin")
        #endif
    }
    
    #if SKIP
    private static func _getAndroidContext() -> android.content.Context? {
        if let activity = AndroidContextTracker.sharedActivity as? android.content.Context {
            return activity
        }
        return nil
    }
    #endif
}

#if SKIP
open class ReminderWorker: Worker {
    public init(context: android.content.Context, workerParams: androidx.work.WorkerParameters) {
        super.init(context, workerParams)
    }

    public override func doWork() -> ListenableWorker.Result {
        let context = self.getApplicationContext()
        let notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as! NotificationManager

        let channelId = "cbt_reframe_reminders"
        if Build.VERSION.SDK_INT >= Build.VERSION_CODES.O {
            let channel = NotificationChannel(
                channelId,
                "Daily Check-in",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        let notification = NotificationCompat.Builder(context, channelId)
            // Note: Replace with an actual app drawable resource id
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle("CBT Reframe")
            .setContentText("今天心情怎么样？花 1 分钟记录一下。")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)

        return ListenableWorker.Result.success()
    }
}
#endif

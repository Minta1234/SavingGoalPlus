import Foundation
import UserNotifications

public final class NotificationManager: ObservableObject {
    public static let shared = NotificationManager()

    @Published public var isAuthorized: Bool = false
    @Published public var reminderHour: Int = 20 // 8:00 PM
    @Published public var reminderMinute: Int = 0

    private let reminderNotificationId = "savinggoal_daily_reminder"

    private init() {
        loadSettings()
        checkAuthorization()
    }

    public func checkAuthorization() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            DispatchQueue.main.async {
                self.isAuthorized = settings.authorizationStatus == .authorized
            }
        }
    }

    public func requestAuthorization(completion: @escaping (Bool) -> Void) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
            DispatchQueue.main.async {
                self.isAuthorized = granted
                completion(granted)
            }
        }
    }

    public func scheduleDailyReminder(hour: Int, minute: Int) {
        self.reminderHour = hour
        self.reminderMinute = minute
        saveSettings()

        let content = UNMutableNotificationContent()
        content.title = "🎯 Savings Goal Reminder"
        content.body = "Keep your financial momentum going! Record your daily savings progress now."
        content.sound = .default

        var dateComponents = DateComponents()
        dateComponents.hour = hour
        dateComponents.minute = minute

        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        let request = UNNotificationRequest(identifier: reminderNotificationId, content: content, trigger: trigger)

        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [reminderNotificationId])
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("Failed to schedule daily notification: \(error.localizedDescription)")
            }
        }
    }

    public func sendTestNotification() {
        let content = UNMutableNotificationContent()
        content.title = "🎯 Savings Goal: Daily Habit Reminder"
        content.body = "Every dollar saved brings you closer to your dream! Keep up the great work."
        content.sound = .default

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 2.0, repeats: false)
        let request = UNNotificationRequest(identifier: "test_notification_\(UUID().uuidString)", content: content, trigger: trigger)

        UNUserNotificationCenter.current().add(request)
    }

    public func cancelAllReminders() {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [reminderNotificationId])
    }

    private func loadSettings() {
        let defaults = UserDefaults.standard
        if defaults.object(forKey: "reminder_hour") != nil {
            self.reminderHour = defaults.integer(forKey: "reminder_hour")
            self.reminderMinute = defaults.integer(forKey: "reminder_minute")
        }
    }

    private func saveSettings() {
        let defaults = UserDefaults.standard
        defaults.set(reminderHour, forKey: "reminder_hour")
        defaults.set(reminderMinute, forKey: "reminder_minute")
    }
}


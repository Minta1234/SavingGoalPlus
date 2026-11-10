import Foundation
import Combine

public struct StreakDay: Identifiable {
    public let id: String
    public let label: String
    public let description: String
    public let saved: Bool
    public let isToday: Bool
}

public struct StreakInfo {
    public let currentStreak: Int
    public let bestStreak: Int
    public let lastSavedDate: String?
    public let totalDaysSaved: Int
    public let isStreakActiveToday: Bool
    public let recentDays: [StreakDay]

    public var levelTitle: String {
        if currentStreak >= 100 { return "👑 Savings Titan" }
        if currentStreak >= 30 { return "💎 Month Master" }
        if currentStreak >= 14 { return "🏆 Fortnight Legend" }
        if currentStreak >= 7 { return "⚡ Week Champion" }
        if currentStreak >= 3 { return "🔥 Warming Up" }
        if currentStreak >= 1 { return "🌱 Habit Spark" }
        return "✨ Start Your Streak"
    }

    public var motivationalText: String {
        if isStreakActiveToday {
            return "Streak burning bright today! 🔥"
        } else if currentStreak > 0 {
            return "Save today to keep your \(currentStreak)-day streak alive! 🔥"
        } else {
            return "Make your first deposit to ignite your streak!"
        }
    }
}

public class SavingsStreakManager: ObservableObject {
    public static let shared = SavingsStreakManager()

    @Published public var streakInfo: StreakInfo = StreakInfo(
        currentStreak: 0,
        bestStreak: 0,
        lastSavedDate: nil,
        totalDaysSaved: 0,
        isStreakActiveToday: false,
        recentDays: []
    )

    private let defaults = UserDefaults.standard
    private let keyCurrent = "STREAK_CURRENT"
    private let keyBest = "STREAK_BEST"
    private let keyLastDate = "STREAK_LAST_DATE"
    private let keyTotal = "STREAK_TOTAL_DAYS"
    private let keyRecentDates = "STREAK_RECENT_DATES"

    private let formatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    private init() {
        refresh()
    }

    public func refresh() {
        let current = defaults.integer(forKey: keyCurrent)
        let best = defaults.integer(forKey: keyBest)
        let lastDate = defaults.string(forKey: keyLastDate)
        let total = defaults.integer(forKey: keyTotal)

        let today = formatter.string(from: Date())
        let yesterday = formatter.string(from: Calendar.current.date(byAdding: .day, value: -1, to: Date()) ?? Date())

        let validStreak: Int
        if lastDate == nil {
            validStreak = 0
        } else if lastDate == today || lastDate == yesterday {
            validStreak = current
        } else {
            validStreak = 0
        }

        let days = lastSevenDays()
        let dates = recentDates(lastDate: lastDate, current: current, days: days)
        if defaults.stringArray(forKey: keyRecentDates) == nil {
            defaults.set(Array(dates), forKey: keyRecentDates)
        }
        let label = DateFormatter()
        label.setLocalizedDateFormatFromTemplate("EEE")
        let description = DateFormatter()
        description.setLocalizedDateFormatFromTemplate("EEEE MMM d")

        streakInfo = StreakInfo(
            currentStreak: validStreak,
            bestStreak: max(best, validStreak),
            lastSavedDate: lastDate,
            totalDaysSaved: total,
            isStreakActiveToday: (lastDate == today),
            recentDays: days.map { day in
                let id = formatter.string(from: day)
                return StreakDay(id: id, label: label.string(from: day),
                                 description: description.string(from: day),
                                 saved: dates.contains(id), isToday: id == today)
            }
        )
    }

    public func recordDeposit() {
        let today = formatter.string(from: Date())
        let yesterday = formatter.string(from: Calendar.current.date(byAdding: .day, value: -1, to: Date()) ?? Date())

        let lastDate = defaults.string(forKey: keyLastDate)
        var current = defaults.integer(forKey: keyCurrent)
        var best = defaults.integer(forKey: keyBest)
        var total = defaults.integer(forKey: keyTotal)
        var dates = recentDates(lastDate: lastDate, current: current, days: lastSevenDays())
        dates.insert(today)

        if lastDate == today {
            // Already counted today
        } else if lastDate == yesterday {
            current += 1
            total += 1
        } else {
            current = 1
            total += 1
        }

        if current > best {
            best = current
        }

        defaults.set(current, forKey: keyCurrent)
        defaults.set(best, forKey: keyBest)
        defaults.set(today, forKey: keyLastDate)
        defaults.set(total, forKey: keyTotal)
        defaults.set(Array(dates), forKey: keyRecentDates)

        refresh()
    }

    private func lastSevenDays() -> [Date] {
        let now = Date()
        return (-6...0).compactMap { Calendar.current.date(byAdding: .day, value: $0, to: now) }
    }

    private func recentDates(lastDate: String?, current: Int, days: [Date]) -> Set<String> {
        let dates: Set<String>
        if let stored = defaults.stringArray(forKey: keyRecentDates) {
            dates = Set(stored)
        } else if let lastDate, let lastSaved = formatter.date(from: lastDate) {
            // Recover only the most recent consecutive run from older installs.
            dates = Set((0..<min(max(current, 1), 7)).compactMap { offset in
                Calendar.current.date(byAdding: .day, value: -offset, to: lastSaved)
                    .map { formatter.string(from: $0) }
            })
        } else {
            dates = []
        }
        return dates.intersection(days.map { formatter.string(from: $0) })
    }

}


import Foundation
import Combine

public struct StreakInfo {
    public let currentStreak: Int
    public let bestStreak: Int
    public let lastSavedDate: String?
    public let totalDaysSaved: Int
    public let isStreakActiveToday: Bool

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
        isStreakActiveToday: false
    )

    private let defaults = UserDefaults.standard
    private let keyCurrent = "STREAK_CURRENT"
    private let keyBest = "STREAK_BEST"
    private let keyLastDate = "STREAK_LAST_DATE"
    private let keyTotal = "STREAK_TOTAL_DAYS"

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

        streakInfo = StreakInfo(
            currentStreak: validStreak,
            bestStreak: max(best, validStreak),
            lastSavedDate: lastDate,
            totalDaysSaved: total,
            isStreakActiveToday: (lastDate == today)
        )
    }

    public func recordDeposit() {
        let today = formatter.string(from: Date())
        let yesterday = formatter.string(from: Calendar.current.date(byAdding: .day, value: -1, to: Date()) ?? Date())

        let lastDate = defaults.string(forKey: keyLastDate)
        var current = defaults.integer(forKey: keyCurrent)
        var best = defaults.integer(forKey: keyBest)
        var total = defaults.integer(forKey: keyTotal)

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

        refresh()
    }
}


import Foundation

public enum SavingsCalculations {
    private static let dayMillis: Double = 86_400_000.0

    public static func parseDateToEpochMs(_ dateString: String) -> Int64 {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        if let date = formatter.date(from: dateString) {
            return Int64(date.timeIntervalSince1970 * 1000)
        }
        return Int64(Date().timeIntervalSince1970 * 1000)
    }

    public static func calculateRates(
        targetAmount: Double,
        currentSaved: Double,
        targetDateEpochMs: Int64,
        nowEpochMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) -> SavingsRatePlan {
        let remaining = max(0.0, targetAmount - currentSaved)
        if remaining <= 0.0 {
            return SavingsRatePlan(
                dailyRate: 0.0,
                weeklyRate: 0.0,
                biWeeklyRate: 0.0,
                monthlyRate: 0.0,
                daysRemaining: 0
            )
        }

        let diffMs = Double(targetDateEpochMs - nowEpochMs)
        let rawDays = Int(ceil(diffMs / dayMillis))
        let daysRemaining = max(1, rawDays)

        let daysDouble = Double(daysRemaining)
        let daily = remaining / daysDouble
        let weekly = remaining / max(1.0, daysDouble / 7.0)
        let biWeekly = remaining / max(1.0, daysDouble / 14.0)
        let monthly = remaining / max(1.0, daysDouble / 30.0)

        return SavingsRatePlan(
            dailyRate: daily,
            weeklyRate: weekly,
            biWeeklyRate: biWeekly,
            monthlyRate: monthly,
            daysRemaining: daysRemaining
        )
    }

    public static func formatCurrency(_ amount: Double, currencySymbol: String = "$") -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        let formattedNumber = formatter.string(from: NSNumber(value: amount)) ?? String(format: "%.2f", amount)
        return "\(currencySymbol)\(formattedNumber)"
    }

    public static func formatDisplayDate(_ dateString: String) -> String {
        let inputFormatter = DateFormatter()
        inputFormatter.dateFormat = "yyyy-MM-dd"
        inputFormatter.locale = Locale(identifier: "en_US_POSIX")

        if let date = inputFormatter.date(from: dateString) {
            let outputFormatter = DateFormatter()
            outputFormatter.dateStyle = .medium
            outputFormatter.timeStyle = .none
            return outputFormatter.string(from: date)
        }
        return dateString
    }
}


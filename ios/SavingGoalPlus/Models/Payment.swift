import Foundation

public struct Payment: Identifiable, Codable, Equatable, Hashable {
    public var id: Int64
    public var goalId: Int64
    public var amount: Double // Positive = Deposit, Negative = Withdrawal
    public var date: String   // YYYY-MM-DD
    public var notes: String
    public var createdAt: Int64

    public init(
        id: Int64 = 0,
        goalId: Int64,
        amount: Double,
        date: String = Payment.todayDateString(),
        notes: String = "",
        createdAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) {
        self.id = id
        self.goalId = goalId
        self.amount = amount
        self.date = date
        self.notes = notes
        self.createdAt = createdAt
    }

    public var isDeposit: Bool {
        amount >= 0.0
    }

    public static func todayDateString() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone.current
        return formatter.string(from: Date())
    }
}


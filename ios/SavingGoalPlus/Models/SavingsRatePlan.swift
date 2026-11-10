import Foundation

public struct SavingsRatePlan: Codable, Equatable {
    public let dailyRate: Double
    public let weeklyRate: Double
    public let biWeeklyRate: Double
    public let monthlyRate: Double
    public let daysRemaining: Int

    public init(
        dailyRate: Double,
        weeklyRate: Double,
        biWeeklyRate: Double,
        monthlyRate: Double,
        daysRemaining: Int
    ) {
        self.dailyRate = dailyRate
        self.weeklyRate = weeklyRate
        self.biWeeklyRate = biWeeklyRate
        self.monthlyRate = monthlyRate
        self.daysRemaining = daysRemaining
    }
}

public struct TotalSummary: Codable, Equatable {
    public let totalTargetAmount: Double
    public let totalSavedAmount: Double
    public let totalRemainingAmount: Double
    public let overallPercentage: Int
    public let activeGoalCount: Int
    public let completedGoalCount: Int

    public var totalGoalsCount: Int {
        activeGoalCount + completedGoalCount
    }

    public init(
        totalTargetAmount: Double = 0.0,
        totalSavedAmount: Double = 0.0,
        totalRemainingAmount: Double = 0.0,
        overallPercentage: Int = 0,
        activeGoalCount: Int = 0,
        completedGoalCount: Int = 0
    ) {
        self.totalTargetAmount = totalTargetAmount
        self.totalSavedAmount = totalSavedAmount
        self.totalRemainingAmount = totalRemainingAmount
        self.overallPercentage = overallPercentage
        self.activeGoalCount = activeGoalCount
        self.completedGoalCount = completedGoalCount
    }
}

public struct BackupPayload: Codable {
    public let version: Int
    public let exportedAt: Int64
    public let goals: [Goal]
    public let payments: [Payment]

    public init(
        version: Int = 1,
        exportedAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        goals: [Goal],
        payments: [Payment]
    ) {
        self.version = version
        self.exportedAt = exportedAt
        self.goals = goals
        self.payments = payments
    }
}


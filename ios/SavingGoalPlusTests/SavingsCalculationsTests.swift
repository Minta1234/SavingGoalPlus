import XCTest
@testable import SavingGoalPlus

final class SavingsCalculationsTests: XCTestCase {
    private let dayMillis: Int64 = 86_400_000

    func testStandardRateProjections() {
        let nowEpoch: Int64 = 1700000000000
        let targetEpoch: Int64 = nowEpoch + (30 * dayMillis) // 30 days ahead

        let plan = SavingsCalculations.calculateRates(
            targetAmount: 3000.0,
            currentSaved: 0.0,
            targetDateEpochMs: targetEpoch,
            nowEpochMs: nowEpoch
        )

        XCTAssertEqual(plan.daysRemaining, 30)
        XCTAssertEqual(plan.dailyRate, 100.0, accuracy: 0.01)
        XCTAssertEqual(plan.weeklyRate, 700.0, accuracy: 0.01)
        XCTAssertEqual(plan.biWeeklyRate, 1400.0, accuracy: 0.01)
        XCTAssertEqual(plan.monthlyRate, 3000.0, accuracy: 0.01)
    }

    func testCompletedGoalRatesAreZero() {
        let nowEpoch: Int64 = 1700000000000
        let targetEpoch: Int64 = nowEpoch + (15 * dayMillis)

        let plan = SavingsCalculations.calculateRates(
            targetAmount: 1000.0,
            currentSaved: 1000.0,
            targetDateEpochMs: targetEpoch,
            nowEpochMs: nowEpoch
        )

        XCTAssertEqual(plan.daysRemaining, 0)
        XCTAssertEqual(plan.dailyRate, 0.0)
        XCTAssertEqual(plan.weeklyRate, 0.0)
        XCTAssertEqual(plan.biWeeklyRate, 0.0)
        XCTAssertEqual(plan.monthlyRate, 0.0)
    }

    func testCurrencyFormatting() {
        let formatted = SavingsCalculations.formatCurrency(1234.56, currencySymbol: "$")
        XCTAssertEqual(formatted, "$1,234.56")
    }
}


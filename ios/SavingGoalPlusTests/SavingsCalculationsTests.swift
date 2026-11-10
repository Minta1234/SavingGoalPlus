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

    func testGoalSortingBySavedAmount() {
        let goalZero = Goal(id: 1, title: "A", targetAmount: 100, savedAmount: 0, targetDate: "2027-01-01")
        let goalHigh = Goal(id: 2, title: "Savings", targetAmount: 10000, savedAmount: 6702.41, targetDate: "2027-01-01")

        let goals = [goalZero, goalHigh]
        let sortedDesc = goals.sorted {
            if $0.savedAmount != $1.savedAmount { return $0.savedAmount > $1.savedAmount }
            return $0.targetAmount > $1.targetAmount
        }

        XCTAssertEqual(sortedDesc.first?.id, 2)
        XCTAssertEqual(sortedDesc.last?.id, 1)
    }
}


final class BackupImportTests: XCTestCase {
    func testFilePreviewAcceptsValidBackupAndRejectsOrphanedPayment() throws {
        let file = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString + ".json")
        defer { try? FileManager.default.removeItem(at: file) }
        let goal = Goal(id: 1, title: "Trip", targetAmount: 100, savedAmount: 20, targetDate: "2027-01-01")
        let payment = Payment(id: 1, goalId: 1, amount: 20)
        let encoder = JSONEncoder()

        try encoder.encode(BackupPayload(goals: [goal], payments: [payment])).write(to: file)
        let preview = try SavingsStorageService.readBackup(file)
        XCTAssertEqual(preview.goals, [goal])
        XCTAssertEqual(preview.payments, [payment])

        try encoder.encode(BackupPayload(goals: [], payments: [payment])).write(to: file)
        XCTAssertThrowsError(try SavingsStorageService.readBackup(file))
    }
}

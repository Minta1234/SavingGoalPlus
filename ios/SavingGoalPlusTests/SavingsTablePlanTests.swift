import XCTest
#if canImport(SavingGoalPlus)
@testable import SavingGoalPlus
#else
@testable import SavingGoalPlusCore
#endif

final class SavingsTablePlanTests: XCTestCase {
    func testFinalInstallmentAndCheckedTotal() throws {
        let plan = try XCTUnwrap(SavingsTablePlan(daily: 3000, target: 10000))
        XCTAssertEqual(plan.dayCount, 4)
        XCTAssertEqual(plan.amount(4), 1000)
        XCTAssertEqual(plan.cumulative(4), plan.target)
        XCTAssertEqual(plan.saved([1, 4]), 4000)
        XCTAssertEqual(SavingsTablePlan(daily: 100, target: 1)?.amount(1), 1)
    }
    func testValidationAndSatang() {
        XCTAssertEqual(SavingsTablePlan.parse("123.45"), 12345)
        XCTAssertEqual(SavingsTablePlan.format(1), "0.01")
        for value in ["0", "-1", "NaN", "1e3", "0.001", "1000000000", "99999999999999999999999"] {
            XCTAssertNil(SavingsTablePlan.parse(value))
        }
        XCTAssertNil(SavingsTablePlan(daily: 1, target: 10001))
        XCTAssertNil(SavingsTablePlan(daily: 1, target: SavingsTablePlan.maximumAmount))
        XCTAssertEqual(SavingsTablePlan(daily: 1, target: 10000)?.dayCount, 10000)
    }
    func testCSV() throws {
        let plan = try XCTUnwrap(SavingsTablePlan(daily: 30, target: 50))
        let csv = plan.csv(completed: [2], headings: ["วันที่", "ออม", "สะสม", "เหลือ", "สำเร็จ"])
        XCTAssertTrue(csv.hasPrefix("\u{FEFF}\"วันที่\""))
        XCTAssertTrue(csv.hasSuffix("1,0.30,0.30,0.20,0\r\n2,0.20,0.50,0.00,1\r\n"))
    }
    func testMultipleTablesAndRenamingPreserveIndependentHistory() throws {
        let plan = try XCTUnwrap(SavingsTablePlan(daily: 3000, target: 10000))
        let first = SavingsTableEntry(name: "Travel", plan: plan)
        let other = SavingsTableEntry(name: "Emergency", plan: plan)
        XCTAssertNotEqual(first.id, other.id)
        let checked = first.withDay(1, saved: true).withDay(4, saved: true)
        XCTAssertEqual(checked.plan.saved(checked.completed), 4000)
        XCTAssertTrue(other.completed.isEmpty)
        let renamed = checked.withDetails(name: "  Holiday  ", plan: plan)
        XCTAssertEqual(renamed.id, first.id)
        XCTAssertEqual(renamed.name, "Holiday")
        XCTAssertEqual(renamed.completed, checked.completed)
        let changed = renamed.withDetails(name: renamed.name, plan: try XCTUnwrap(SavingsTablePlan(daily: 500, target: 2000)))
        XCTAssertTrue(changed.completed.isEmpty)
        XCTAssertEqual(checked.completed, [1, 4])
    }

    func testMultipleTablesSurviveSerializationAndDeletion() throws {
        let plan = try XCTUnwrap(SavingsTablePlan(daily: 100, target: 500))
        let entries = [SavingsTableEntry(name: "First", plan: plan, completed: [1]),
                       SavingsTableEntry(name: "Second", plan: plan, completed: [2])]
        let restored = try JSONDecoder().decode([SavingsTableEntry].self, from: JSONEncoder().encode(entries))
        XCTAssertEqual(restored, entries)
        XCTAssertEqual(restored.filter { $0.id != entries[0].id }, [entries[1]])
        let empty = try JSONDecoder().decode([SavingsTableEntry].self, from: JSONEncoder().encode([SavingsTableEntry]()))
        XCTAssertTrue(empty.isEmpty)
    }
    func testProgressShowsHundredthsAndCapsAtOneHundred() throws {
        let plan = try XCTUnwrap(SavingsTablePlan(daily: 4000, target: 6000000))
        XCTAssertEqual(plan.progressLabel(4000), "0.07%")
        XCTAssertEqual(plan.progressFraction(4000), 40.0 / 60000, accuracy: 0.00000001)
        XCTAssertEqual(plan.progressLabel(-1), "0.00%")
        XCTAssertEqual(plan.progressLabel(0), "0.00%")
        XCTAssertEqual(plan.progressLabel(plan.target), "100.00%")
        XCTAssertEqual(plan.progressLabel(Int64.max), "100.00%")
        XCTAssertEqual(try XCTUnwrap(SavingsTablePlan(daily: 1, target: 800)).progressLabel(1), "0.13%")
        let maximum = try XCTUnwrap(SavingsTablePlan(daily: SavingsTablePlan.maximumAmount, target: SavingsTablePlan.maximumAmount))
        XCTAssertEqual(maximum.progressLabel(maximum.target), "100.00%")
    }
}

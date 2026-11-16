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
}

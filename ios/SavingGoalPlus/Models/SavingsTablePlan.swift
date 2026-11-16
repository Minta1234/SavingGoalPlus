import Foundation

/// All amounts are integer satang; the last installment never exceeds the target.
struct SavingsTablePlan: Codable, Equatable {
    static let maximumAmount: Int64 = 99_999_999_999
    static let maximumDays = 10_000
    let daily: Int64
    let target: Int64

    init?(daily: Int64, target: Int64) {
        guard daily > 0, target > 0, daily <= Self.maximumAmount,
              target <= Self.maximumAmount,
              (target - 1) / daily + 1 <= Self.maximumDays else { return nil }
        self.daily = daily
        self.target = target
    }

    var dayCount: Int { Int((target - 1) / daily + 1) }
    func amount(_ day: Int) -> Int64 { min(daily, target - Int64(day - 1) * daily) }
    func cumulative(_ day: Int) -> Int64 { min(target, Int64(day) * daily) }
    func saved(_ completed: Set<Int>) -> Int64 {
        completed.filter { (1...dayCount).contains($0) }.reduce(0) { $0 + amount($1) }
    }
    static func parse(_ text: String) -> Int64? {
        let input = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard input.range(of: "^[0-9]+(\\.[0-9]{1,2})?$", options: .regularExpression) != nil else { return nil }
        let parts = input.split(separator: ".")
        guard let whole = Int64(parts[0]), whole <= maximumAmount / 100 else { return nil }
        let fraction = parts.count > 1 ? String(parts[1]) : ""
        let cents = Int64(fraction + String(repeating: "0", count: 2 - fraction.count)) ?? 0
        let value = whole * 100 + cents
        return value > 0 && value <= maximumAmount ? value : nil
    }
    static func format(_ amount: Int64) -> String {
        "\(amount / 100).\(String(format: "%02lld", amount % 100))"
    }
    func csv(completed: Set<Int>, headings: [String]) -> String {
        let header = headings.map { "\"" + $0.replacingOccurrences(of: "\"", with: "\"\"") + "\"" }.joined(separator: ",")
        let rows = (1...dayCount).map { day in
            "\(day),\(Self.format(amount(day))),\(Self.format(cumulative(day))),\(Self.format(target - cumulative(day))),\(completed.contains(day) ? 1 : 0)"
        }
        return "\u{FEFF}" + ([header] + rows).joined(separator: "\r\n") + "\r\n"
    }
}

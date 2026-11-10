import SwiftUI

public struct TotalSummaryCardView: View {
    @AppStorage(SavingsCalculations.currencyPreferenceKey) private var currencySymbol = "$"
    public let summary: TotalSummary

    public init(summary: TotalSummary) {
        self.summary = summary
    }

    public var body: some View {
        VStack(spacing: 16) {
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("TOTAL SAVED")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.secondary)
                        .tracking(1.2)

                    Text(SavingsCalculations.formatCurrency(summary.totalSavedAmount, currencySymbol: currencySymbol))
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundColor(ColorTheme.primaryEmerald)
                }

                Spacer()

                // Mini Percentage Badge
                ZStack {
                    Circle()
                        .stroke(ColorTheme.trackBackground, lineWidth: 5)
                        .frame(width: 54, height: 54)

                    Circle()
                        .trim(from: 0.0, to: CGFloat(summary.overallPercentage) / 100.0)
                        .stroke(
                            ColorTheme.stageColor(for: summary.overallPercentage),
                            style: StrokeStyle(lineWidth: 5, lineCap: .round)
                        )
                        .rotationEffect(.degrees(-90))
                        .frame(width: 54, height: 54)

                    Text("\(summary.overallPercentage)%")
                        .font(.system(size: 12, weight: .bold, design: .rounded))
                        .foregroundColor(.primary)
                }
            }

            Divider()

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("REMAINING")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(.secondary)
                        .tracking(1.0)
                    Text(SavingsCalculations.formatCurrency(summary.totalRemainingAmount, currencySymbol: currencySymbol))
                        .font(.system(size: 16, weight: .semibold, design: .rounded))
                        .foregroundColor(.primary)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    Text("TOTAL TARGET")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(.secondary)
                        .tracking(1.0)
                    Text(SavingsCalculations.formatCurrency(summary.totalTargetAmount, currencySymbol: currencySymbol))
                        .font(.system(size: 16, weight: .semibold, design: .rounded))
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(18)
        .background(ColorTheme.cardBackground)
        .cornerRadius(18)
        .shadow(color: Color.black.opacity(0.04), radius: 8, x: 0, y: 3)
    }
}


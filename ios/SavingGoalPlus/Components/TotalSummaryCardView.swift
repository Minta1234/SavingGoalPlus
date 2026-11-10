import SwiftUI

public struct TotalSummaryCardView: View {
    @AppStorage(SavingsCalculations.currencyPreferenceKey) private var currencySymbol = "$"
    public let summary: TotalSummary

    public init(summary: TotalSummary) {
        self.summary = summary
    }

    public var body: some View {
        VStack(spacing: 14) {
            // Top Row: "You've already saved" & Piggy Bank Badge
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("You've already saved")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(Color(white: 0.7))
                        .tracking(0.5)

                    Text(SavingsCalculations.formatCurrency(summary.totalSavedAmount, currencySymbol: currencySymbol))
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                }

                Spacer()

                // Piggy Bank Icon in rounded container
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.white.opacity(0.12))
                        .frame(width: 44, height: 44)

                    Image(systemName: "banknote.fill")
                        .font(.system(size: 22))
                        .foregroundColor(ColorTheme.primaryEmerald)
                }
            }

            // Progress Bar
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.white.opacity(0.15))
                        .frame(height: 6)

                    Capsule()
                        .fill(ColorTheme.primaryEmerald)
                        .frame(width: geo.size.width * CGFloat(summary.overallPercentage) / 100.0, height: 6)
                }
            }
            .frame(height: 6)

            // Bottom Stats Row: Target, Remaining, Total Goals & Percent (All in 1 Card!)
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Target")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(Color(white: 0.65))
                    Text(SavingsCalculations.formatCurrency(summary.totalTargetAmount, currencySymbol: currencySymbol))
                        .font(.system(size: 14, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                }

                Spacer()

                VStack(alignment: .center, spacing: 2) {
                    Text("Remaining")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(Color(white: 0.65))
                    Text(SavingsCalculations.formatCurrency(summary.totalRemainingAmount, currencySymbol: currencySymbol))
                        .font(.system(size: 14, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    Text("Total Goals")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(Color(white: 0.65))
                    Text("\(summary.activeGoalCount) (\(summary.overallPercentage)%)")
                        .font(.system(size: 14, weight: .bold, design: .rounded))
                        .foregroundColor(ColorTheme.primaryEmerald)
                }
            }
        }
        .padding(18)
        .background(
            LinearGradient(
                colors: [Color(red: 0.13, green: 0.16, blue: 0.22), Color(red: 0.18, green: 0.22, blue: 0.29)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .cornerRadius(20)
        .shadow(color: Color.black.opacity(0.12), radius: 8, x: 0, y: 3)
    }
}

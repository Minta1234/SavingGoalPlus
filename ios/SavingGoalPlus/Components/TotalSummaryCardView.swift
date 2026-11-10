import SwiftUI

public struct TotalSummaryCardView: View {
    @AppStorage(SavingsCalculations.currencyPreferenceKey) private var currencySymbol = "$"
    public let goals: [Goal]
    public let summary: TotalSummary

    @State private var selectedIndex = 0

    public init(
        goals: [Goal] = [],
        summary: TotalSummary
    ) {
        self.goals = goals
        self.summary = summary
    }

    public var body: some View {
        if goals.isEmpty {
            singleCard(
                title: "You've already saved",
                saved: summary.totalSavedAmount,
                target: summary.totalTargetAmount,
                remaining: summary.totalRemainingAmount,
                percent: summary.overallPercentage,
                badge: "\(summary.activeGoalCount) Goals"
            )
        } else {
            VStack(spacing: 10) {
                TabView(selection: $selectedIndex) {
                    ForEach(Array(goals.enumerated()), id: \.element.id) { index, goal in
                        singleCard(
                            title: goal.title,
                            saved: goal.savedAmount,
                            target: goal.targetAmount,
                            remaining: goal.remainingAmount,
                            percent: goal.progressPercentage,
                            badge: "\(index + 1)/\(goals.count) (\(goal.progressPercentage)%)"
                        )
                        .tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .frame(height: 170)

                if goals.count > 1 {
                    HStack(spacing: 6) {
                        ForEach(0..<goals.count, id: \.self) { idx in
                            Capsule()
                                .fill(selectedIndex == idx ? ColorTheme.primaryEmerald : Color.secondary.opacity(0.3))
                                .frame(width: selectedIndex == idx ? 22 : 6, height: 6)
                                .animation(.spring(response: 0.35, dampingFraction: 0.7), value: selectedIndex)
                        }
                    }
                    .padding(.top, 2)
                }
            }
        }
    }

    private func singleCard(
        title: String,
        saved: Double,
        target: Double,
        remaining: Double,
        percent: Int,
        badge: String
    ) -> some View {
        VStack(spacing: 14) {
            // Top Row: Goal Title & Saved Amount + Badge
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(.system(size: 15, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                        .lineLimit(1)

                    Text("You've already saved")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(Color(white: 0.7))
                        .tracking(0.5)

                    Text(SavingsCalculations.formatCurrency(saved, currencySymbol: currencySymbol))
                        .font(.system(size: 26, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                }

                Spacer()

                // Piggy Bank Icon badge
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
                        .frame(width: geo.size.width * CGFloat(min(100, max(0, percent))) / 100.0, height: 6)
                }
            }
            .frame(height: 6)

            // Bottom Stats Row: Target, Remaining, Total Goals & Percent
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Target")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(Color(white: 0.65))
                    Text(SavingsCalculations.formatCurrency(target, currencySymbol: currencySymbol))
                        .font(.system(size: 13, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                }

                Spacer()

                VStack(alignment: .center, spacing: 2) {
                    Text("Remaining")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(Color(white: 0.65))
                    Text(SavingsCalculations.formatCurrency(remaining, currencySymbol: currencySymbol))
                        .font(.system(size: 13, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    Text("Progress")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(Color(white: 0.65))
                    Text(badge)
                        .font(.system(size: 13, weight: .bold, design: .rounded))
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

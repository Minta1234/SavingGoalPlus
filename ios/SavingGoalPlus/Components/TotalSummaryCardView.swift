import SwiftUI

public struct TotalSummaryCardView: View {
    @AppStorage(SavingsCalculations.currencyPreferenceKey) private var currencySymbol = "$"
    @ObservedObject private var streakManager = SavingsStreakManager.shared
    @State private var currentPage = 0

    public let summary: TotalSummary

    public init(summary: TotalSummary) {
        self.summary = summary
    }

    public var body: some View {
        VStack(spacing: 10) {
            TabView(selection: $currentPage) {
                balanceCard
                    .tag(0)

                targetCard
                    .tag(1)

                streakCard
                    .tag(2)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .frame(height: 148)

            // K PLUS Style Animated Indicator Dots
            HStack(spacing: 6) {
                ForEach(0..<3) { idx in
                    Capsule()
                        .fill(currentPage == idx ? ColorTheme.primaryEmerald : Color.secondary.opacity(0.25))
                        .frame(width: currentPage == idx ? 22 : 6, height: 6)
                        .animation(.easeInOut(duration: 0.25), value: currentPage)
                        .onTapGesture {
                            withAnimation(.easeInOut(duration: 0.25)) {
                                currentPage = idx
                            }
                        }
                }
            }
            .padding(.top, 2)
        }
    }

    // Card 0: Balance & Remaining
    private var balanceCard: some View {
        VStack(spacing: 14) {
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("TOTAL SAVED")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.secondary)
                        .tracking(1.2)

                    Text(SavingsCalculations.formatCurrency(summary.totalSavedAmount, currencySymbol: currencySymbol))
                        .font(.system(size: 26, weight: .bold, design: .rounded))
                        .foregroundColor(ColorTheme.primaryEmerald)
                }

                Spacer()

                // Mini Percentage Ring
                ZStack {
                    Circle()
                        .stroke(ColorTheme.trackBackground, lineWidth: 4)
                        .frame(width: 48, height: 48)

                    Circle()
                        .trim(from: 0.0, to: CGFloat(summary.overallPercentage) / 100.0)
                        .stroke(
                            ColorTheme.stageColor(for: summary.overallPercentage),
                            style: StrokeStyle(lineWidth: 4, lineCap: .round)
                        )
                        .rotationEffect(.degrees(-90))
                        .frame(width: 48, height: 48)

                    Text("\(summary.overallPercentage)%")
                        .font(.system(size: 11, weight: .bold, design: .rounded))
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
                        .font(.system(size: 15, weight: .semibold, design: .rounded))
                        .foregroundColor(.primary)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    Text("TOTAL GOALS")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(.secondary)
                        .tracking(1.0)
                    Text("\(summary.activeGoalCount) Active")
                        .font(.system(size: 15, weight: .semibold, design: .rounded))
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(16)
        .background(ColorTheme.cardBackground)
        .cornerRadius(18)
        .shadow(color: Color.black.opacity(0.04), radius: 6, x: 0, y: 2)
    }

    // Card 1: Target Overview & Portfolio
    private var targetCard: some View {
        VStack(spacing: 14) {
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("OVERALL TARGET")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.secondary)
                        .tracking(1.2)

                    Text(SavingsCalculations.formatCurrency(summary.totalTargetAmount, currencySymbol: currencySymbol))
                        .font(.system(size: 26, weight: .bold, design: .rounded))
                        .foregroundColor(.primary)
                }

                Spacer()

                Image(systemName: "flag.fill")
                    .font(.system(size: 22))
                    .foregroundColor(ColorTheme.primaryEmerald)
                    .frame(width: 46, height: 46)
                    .background(ColorTheme.primaryEmerald.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }

            Divider()

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("COMPLETION")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(.secondary)
                        .tracking(1.0)
                    Text("\(summary.overallPercentage)% Achieved")
                        .font(.system(size: 15, weight: .semibold, design: .rounded))
                        .foregroundColor(ColorTheme.primaryEmerald)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    Text("ACTIVE TARGETS")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(.secondary)
                        .tracking(1.0)
                    Text("\(summary.activeGoalCount) Goals")
                        .font(.system(size: 15, weight: .semibold, design: .rounded))
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(16)
        .background(ColorTheme.cardBackground)
        .cornerRadius(18)
        .shadow(color: Color.black.opacity(0.04), radius: 6, x: 0, y: 2)
    }

    // Card 2: Momentum & Streak
    private var streakCard: some View {
        VStack(spacing: 14) {
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("SAVINGS STREAK")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.secondary)
                        .tracking(1.2)

                    Text(streakManager.streakInfo.currentStreak > 0 ? "\(streakManager.streakInfo.currentStreak) Days 🔥" : "Start Streak ✨")
                        .font(.system(size: 26, weight: .bold, design: .rounded))
                        .foregroundColor(.orange)
                }

                Spacer()

                Image(systemName: "flame.fill")
                    .font(.system(size: 22))
                    .foregroundColor(.orange)
                    .frame(width: 46, height: 46)
                    .background(Color.orange.opacity(0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }

            Divider()

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("TIER")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(.secondary)
                        .tracking(1.0)
                    Text(streakManager.streakInfo.levelTitle)
                        .font(.system(size: 14, weight: .semibold, design: .rounded))
                        .foregroundColor(.primary)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    Text("BEST STREAK")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(.secondary)
                        .tracking(1.0)
                    Text("\(streakManager.streakInfo.bestStreak) Days 🏆")
                        .font(.system(size: 14, weight: .semibold, design: .rounded))
                        .foregroundColor(.orange)
                }
            }
        }
        .padding(16)
        .background(ColorTheme.cardBackground)
        .cornerRadius(18)
        .shadow(color: Color.black.opacity(0.04), radius: 6, x: 0, y: 2)
    }
}

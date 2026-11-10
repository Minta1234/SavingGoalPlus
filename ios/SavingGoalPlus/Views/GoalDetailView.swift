import SwiftUI

public struct GoalDetailView: View {
    @AppStorage(SavingsCalculations.currencyPreferenceKey) private var currencySymbol = "$"
    public let goalId: Int64
    @ObservedObject var storage = SavingsStorageService.shared

    @State private var showingEditSheet = false
    @State private var showingPaymentSheet = false
    @State private var showConfetti = false

    private var currentGoal: Goal? {
        storage.goals.first(where: { $0.id == goalId })
    }

    private var payments: [Payment] {
        storage.getPayments(forGoalId: goalId)
    }

    private var ratePlan: SavingsRatePlan {
        guard let goal = currentGoal else {
            return SavingsRatePlan(dailyRate: 0, weeklyRate: 0, biWeeklyRate: 0, monthlyRate: 0, daysRemaining: 0)
        }
        let epoch = SavingsCalculations.parseDateToEpochMs(goal.targetDate)
        return SavingsCalculations.calculateRates(
            targetAmount: goal.targetAmount,
            currentSaved: goal.savedAmount,
            targetDateEpochMs: epoch
        )
    }

    public var body: some View {
        ZStack {
            if let goal = currentGoal {
                ScrollView {
                    VStack(spacing: 20) {
                        // 260° Gauge
                        CircularGoalGaugeView(
                            progressFraction: goal.progressFraction,
                            savedAmount: goal.savedAmount,
                            targetAmount: goal.targetAmount,
                            size: 220
                        )
                        .padding(.top, 10)

                        // Action Buttons: + Deposit / - Withdraw
                        HStack(spacing: 16) {
                            Button(action: { showingPaymentSheet = true }) {
                                Label("Add Deposit", systemImage: "plus.circle.fill")
                                    .font(.system(size: 16, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 14)
                                    .background(ColorTheme.primaryEmerald)
                                    .foregroundColor(.white)
                                    .cornerRadius(14)
                            }
                        }
                        .padding(.horizontal)

                        // Rate Breakdown Cards
                        VStack(alignment: .leading, spacing: 12) {
                            Text("REQUIRED SAVINGS BREAKDOWN")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.secondary)
                                .tracking(1.2)
                                .padding(.horizontal)

                            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                                RateCard(title: "DAILY", amount: ratePlan.dailyRate)
                                RateCard(title: "WEEKLY", amount: ratePlan.weeklyRate)
                                RateCard(title: "BI-WEEKLY", amount: ratePlan.biWeeklyRate)
                                RateCard(title: "MONTHLY", amount: ratePlan.monthlyRate)
                            }
                            .padding(.horizontal)
                        }

                        // Payment History
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Text("TRANSACTION HISTORY (\(payments.count))")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(.secondary)
                                    .tracking(1.2)
                                Spacer()
                            }
                            .padding(.horizontal)

                            if payments.isEmpty {
                                Text("No transactions recorded yet.")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                                    .frame(maxWidth: .infinity, alignment: .center)
                                    .padding(.vertical, 24)
                            } else {
                                ForEach(payments) { p in
                                    HStack {
                                        Image(systemName: p.isDeposit ? "arrow.down.left.circle.fill" : "arrow.up.right.circle.fill")
                                            .foregroundColor(p.isDeposit ? ColorTheme.primaryEmerald : ColorTheme.withdrawalRed)
                                            .font(.system(size: 24))

                                        VStack(alignment: .leading, spacing: 2) {
                                            Group {
                                                if p.notes.isEmpty {
                                                    Text(p.isDeposit ? LocalizedStringKey("Deposit") : LocalizedStringKey("Withdrawal"))
                                                } else {
                                                    Text(p.notes)
                                                }
                                            }
                                                .font(.system(size: 15, weight: .semibold))
                                            Text(SavingsCalculations.formatDisplayDate(p.date))
                                                .font(.system(size: 12))
                                                .foregroundColor(.secondary)
                                        }

                                        Spacer()

                                        Text(SavingsCalculations.formatCurrency(abs(p.amount), currencySymbol: currencySymbol))
                                            .font(.system(size: 15, weight: .bold, design: .rounded))
                                            .foregroundColor(p.isDeposit ? ColorTheme.primaryEmerald : ColorTheme.withdrawalRed)

                                        Button(action: { storage.deletePayment(id: p.id) }) {
                                            Image(systemName: "trash")
                                                .font(.system(size: 13))
                                                .foregroundColor(.secondary)
                                                .padding(6)
                                        }
                                    }
                                    .padding(14)
                                    .background(ColorTheme.cardBackground)
                                    .cornerRadius(12)
                                    .padding(.horizontal)
                                }
                            }
                        }
                    }
                    .padding(.bottom, 30)
                }
                .navigationTitle(goal.title)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button("Edit") { showingEditSheet = true }
                    }
                }
                .sheet(isPresented: $showingEditSheet) {
                    GoalEditSheet(existingGoal: goal) { title, target, date, category, notes in
                        var updated = goal
                        updated.title = title
                        updated.targetAmount = target
                        updated.targetDate = date
                        updated.category = category
                        updated.notes = notes
                        storage.updateGoal(updated)
                    }
                }
                .sheet(isPresented: $showingPaymentSheet) {
                    PaymentSheet(goalTitle: goal.title) { amount, notes, date in
                        let hitGoal = storage.addPayment(goalId: goal.id, amount: amount, notes: notes, date: date)
                        if hitGoal {
                            showConfetti = true
                        }
                    }
                }
            } else {
                Text("Goal not found")
                    .foregroundColor(.secondary)
            }

            // Confetti Layer
            ConfettiView(trigger: $showConfetti)
        }
    }
}

private struct RateCard: View {
    @AppStorage(SavingsCalculations.currencyPreferenceKey) private var currencySymbol = "$"
    let title: String
    let amount: Double

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(LocalizedStringKey(title))
                .font(.system(size: 10, weight: .bold))
                .foregroundColor(.secondary)
                .tracking(1.0)
            Text(SavingsCalculations.formatCurrency(amount, currencySymbol: currencySymbol))
                .font(.system(size: 18, weight: .bold, design: .rounded))
                .foregroundColor(.primary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(ColorTheme.cardBackground)
        .cornerRadius(14)
    }
}


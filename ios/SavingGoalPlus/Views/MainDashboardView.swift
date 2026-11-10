import SwiftUI

public struct MainDashboardView: View {
    @ObservedObject var storage = SavingsStorageService.shared
    @Environment(\.scenePhase) private var scenePhase
    @ObservedObject var streakManager = SavingsStreakManager.shared
    @State private var showingAddGoalSheet = false
    @State private var showingStreakSheet = false
    @State private var selectedFilter: GoalFilter = .all
    @State private var goalToEdit: Goal?
    @State private var depositGoal: Goal?
    @State private var showDepositError = false
    @State private var showConfetti = false

    enum GoalFilter: String, CaseIterable {
        case all = "All"
        case inProgress = "In Progress"
        case completed = "Completed"
    }

    private var filteredGoals: [Goal] {
        let active = storage.goals.filter { !$0.isArchived }
        switch selectedFilter {
        case .all:
            return active
        case .inProgress:
            return active.filter { !$0.isCompleted }
        case .completed:
            return active.filter { $0.isCompleted }
        }
    }

    public var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    // Total Summary Carousel (Swipeable K-Plus Cards per Goal)
                    TotalSummaryCardView(
                        goals: filteredGoals,
                        summary: storage.computeTotalSummary()
                    )
                        .padding(.horizontal)
                        .padding(.top, 8)

                    streakBanner
                        .padding(.horizontal)

                    // Filter Segmented Control
                    Picker("Filter", selection: $selectedFilter) {
                        ForEach(GoalFilter.allCases, id: \.self) { filter in
                            Text(LocalizedStringKey(filter.rawValue)).tag(filter)
                        }
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal)

                    // Goals List
                    if filteredGoals.isEmpty {
                        VStack(spacing: 12) {
                            Image(systemName: "target")
                                .font(.system(size: 44))
                                .foregroundColor(ColorTheme.primaryEmerald.opacity(0.6))
                            Text("No Goals Found")
                                .font(.headline)
                            Text("Tap the '+' button below to start tracking your first financial goal!")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 32)
                        }
                        .padding(.vertical, 40)
                    } else {
                        LazyVStack(spacing: 14) {
                            ForEach(filteredGoals) { goal in
                                NavigationLink(destination: GoalDetailView(goalId: goal.id)) {
                                    GoalCardView(
                                        goal: goal,
                                        onEdit: { goalToEdit = goal },
                                        onArchive: { storage.toggleArchive(id: goal.id) },
                                        onDelete: { storage.deleteGoal(id: goal.id) }
                                    )
                                }
                                .buttonStyle(PlainButtonStyle())
                            }
                        }
                        .padding(.horizontal)
                    }
                }
                .padding(.bottom, 80)
            }
            .overlay { ConfettiView(trigger: $showConfetti).allowsHitTesting(false) }
            .navigationTitle("SavingGoalPlus")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showingAddGoalSheet = true }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 22))
                            .foregroundColor(ColorTheme.primaryEmerald)
                    }
                }
            }
            .onChange(of: scenePhase) { phase in
                if phase == .active { streakManager.refresh() }
            }
            .sheet(isPresented: $showingAddGoalSheet) {
                GoalEditSheet { title, target, date, category, notes in
                    storage.addGoal(title: title, targetAmount: target, targetDate: date, notes: notes, category: category)
                }
            }
            .sheet(isPresented: $showingStreakSheet) {
                StreakMilestonesSheet(streakInfo: streakManager.streakInfo)
            }
            .sheet(item: $depositGoal) { goal in
                PaymentSheet(goalTitle: goal.title, todayOnly: true) { amount, notes, date in
                    guard storage.goals.contains(where: { $0.id == goal.id && !$0.isArchived }) else {
                        showDepositError = true
                        return
                    }
                    showConfetti = storage.addPayment(goalId: goal.id, amount: amount, notes: notes, date: date)
                }
            }
            .alert("Could not save the deposit", isPresented: $showDepositError) {
                Button("OK", role: .cancel) { }
            } message: {
                Text("Check that the goal still exists and try again.")
            }
            .sheet(item: $goalToEdit) { goal in
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
        }
    }

    private var streakBanner: some View {
        let streak = streakManager.streakInfo
        let status: LocalizedStringKey = streak.isStreakActiveToday ? "Saved today" : (streak.currentStreak > 0 ? "Save today" : "Start saving")
        return Button {
            showingStreakSheet = true
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "flame.fill")
                    .font(.title3)
                    .foregroundColor(streak.currentStreak > 0 ? .orange : .secondary)
                    .accessibilityHidden(true)
                Text("\(streak.currentStreak) day streak")
                    .font(.subheadline.bold())
                Spacer(minLength: 0)
                Text(status)
                    .font(.caption)
                    .foregroundColor(.secondary)
                Image(systemName: "chevron.forward")
                    .font(.caption2.weight(.semibold))
                    .foregroundColor(.secondary)
                    .accessibilityHidden(true)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .frame(minHeight: 44)
            .contentShape(Rectangle())
            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Color.secondary.opacity(0.25)))
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityHint("Opens streak details")
    }

}

public struct StreakMilestonesSheet: View {
    @Environment(\.dismiss) private var dismiss
    public let streakInfo: StreakInfo

    private let tiers = [
        ("👑 Savings Titan", "100+ Days Streak"),
        ("💎 Month Master", "30 - 99 Days Streak"),
        ("🏆 Fortnight Legend", "14 - 29 Days Streak"),
        ("⚡ Week Champion", "7 - 13 Days Streak"),
        ("🔥 Warming Up", "3 - 6 Days Streak"),
        ("🌱 Habit Spark", "1 - 2 Days Streak")
    ]

    public var body: some View {
        NavigationView {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("\(streakInfo.currentStreak) day streak")
                            .font(.title2.bold())
                        Group {
                            if streakInfo.isStreakActiveToday {
                                Text("Streak burning bright today! 🔥")
                            } else if streakInfo.currentStreak > 0 {
                                Text("Save today to keep your \(streakInfo.currentStreak)-day streak alive! 🔥")
                            } else {
                                Text("Make your first deposit to ignite your streak!")
                            }
                        }
                        .font(.subheadline)
                        .foregroundColor(.secondary)

                        HStack {
                            Spacer()
                            VStack {
                                Text("\(streakInfo.currentStreak)d")
                                    .font(.title2)
                                    .fontWeight(.bold)
                                    .foregroundColor(.orange)
                                Text("Current")
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            VStack {
                                Text("\(streakInfo.bestStreak)d")
                                    .font(.title2)
                                    .fontWeight(.bold)
                                    .foregroundColor(.orange)
                                Text("Best")
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            VStack {
                                Text("\(streakInfo.totalDaysSaved)d")
                                    .font(.title2)
                                    .fontWeight(.bold)
                                    .foregroundColor(.orange)
                                Text("Total Days")
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                        }
                        .padding(.vertical, 8)
                        .background(Color(.tertiarySystemBackground))
                        .cornerRadius(12)
                    }
                    .padding(.vertical, 4)
                } header: {
                    Text("Streak Stats")
                }

                Section("Last 7 days") {
                    HStack(spacing: 4) {
                        ForEach(streakInfo.recentDays) { day in
                            VStack(spacing: 5) {
                                Text(day.label)
                                    .font(.caption2)
                                    .lineLimit(1)
                                Circle()
                                    .fill(day.saved ? Color.orange : Color.secondary.opacity(0.15))
                                    .overlay(Circle().strokeBorder(day.isToday ? Color.orange : .clear, lineWidth: 2))
                                    .overlay {
                                        if day.saved {
                                            Image(systemName: "checkmark")
                                                .font(.caption2.bold())
                                                .foregroundColor(.black)
                                        }
                                    }
                                    .frame(width: 22, height: 22)
                            }
                            .frame(maxWidth: .infinity)
                            .accessibilityElement(children: .ignore)
                            .accessibilityLabel(day.isToday ? Text("Today") : Text(verbatim: day.description))
                            .accessibilityValue(day.saved ? Text("Saved") : Text("No deposit"))
                        }
                    }
                    .padding(.vertical, 4)
                }

                Section {
                    ForEach(tiers, id: \.0) { tier, req in
                        HStack {
                            Text(LocalizedStringKey(tier))
                                .font(.body)
                                .fontWeight(streakInfo.levelTitle == tier ? .bold : .regular)
                                .foregroundColor(streakInfo.levelTitle == tier ? .orange : .primary)
                            Spacer()
                            Text(LocalizedStringKey(req))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding(.vertical, 2)
                    }
                } header: {
                    Text("Motivational Tiers")
                }
            }
            .navigationTitle("Savings Streak 🔥")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
        }
    }
}


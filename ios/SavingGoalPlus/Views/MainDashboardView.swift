import SwiftUI

public struct MainDashboardView: View {
    @ObservedObject var storage = SavingsStorageService.shared
    @ObservedObject var streakManager = SavingsStreakManager.shared
    @State private var showingAddGoalSheet = false
    @State private var showingStreakSheet = false
    @State private var selectedFilter: GoalFilter = .all
    @State private var goalToEdit: Goal?

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
                    // Total Summary Card (Unlocked)
                    TotalSummaryCardView(summary: storage.computeTotalSummary())
                        .padding(.horizontal)
                        .padding(.top, 8)

                    // Daily Savings Streak Card (Duolingo / TikTok style)
                    Button(action: { showingStreakSheet = true }) {
                        HStack(spacing: 12) {
                            ZStack {
                                Circle()
                                    .fill(
                                        LinearGradient(
                                            colors: [Color.orange, Color.red],
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        )
                                    )
                                    .frame(width: 44, height: 44)
                                Image(systemName: "flame.fill")
                                    .foregroundColor(.white)
                                    .font(.system(size: 22))
                            }

                            VStack(alignment: .leading, spacing: 3) {
                                HStack(spacing: 6) {
                                    Text(streakManager.streakInfo.currentStreak > 0 ? "\(streakManager.streakInfo.currentStreak) Day Streak!" : "Start Savings Streak")
                                        .font(.subheadline)
                                        .fontWeight(.bold)
                                        .foregroundColor(.primary)

                                    Text(streakManager.streakInfo.isStreakActiveToday ? "Active Today" : "Needs Deposit")
                                        .font(.system(size: 10, weight: .semibold))
                                        .padding(.horizontal, 6)
                                        .padding(.vertical, 2)
                                        .background(streakManager.streakInfo.isStreakActiveToday ? Color.green.opacity(0.15) : Color.orange.opacity(0.15))
                                        .foregroundColor(streakManager.streakInfo.isStreakActiveToday ? .green : .orange)
                                        .cornerRadius(6)
                                }

                                Text("\(streakManager.streakInfo.levelTitle) • \(streakManager.streakInfo.motivationalText)")
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                                    .lineLimit(1)
                            }

                            Spacer()

                            HStack(spacing: 3) {
                                Image(systemName: "star.fill")
                                    .font(.system(size: 10))
                                    .foregroundColor(.orange)
                                Text("\(streakManager.streakInfo.bestStreak)d")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(.orange)
                            }
                            .padding(.horizontal, 7)
                            .padding(.vertical, 3)
                            .background(Color.orange.opacity(0.12))
                            .cornerRadius(8)
                        }
                        .padding(12)
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(16)
                    }
                    .buttonStyle(PlainButtonStyle())
                    .padding(.horizontal)

                    // Filter Segmented Control
                    Picker("Filter", selection: $selectedFilter) {
                        ForEach(GoalFilter.allCases, id: \.self) { filter in
                            Text(filter.rawValue).tag(filter)
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
            .sheet(isPresented: $showingAddGoalSheet) {
                GoalEditSheet { title, target, date, category, notes in
                    storage.addGoal(title: title, targetAmount: target, targetDate: date, notes: notes, category: category)
                }
            }
            .sheet(isPresented: $showingStreakSheet) {
                StreakMilestonesSheet(streakInfo: streakManager.streakInfo)
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
                        Text("Build a daily habit of saving! Deposit into any of your goals every day to keep your streak burning bright.")
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

                Section {
                    ForEach(tiers, id: \.0) { tier, req in
                        HStack {
                            Text(tier)
                                .font(.body)
                                .fontWeight(streakInfo.levelTitle == tier ? .bold : .regular)
                                .foregroundColor(streakInfo.levelTitle == tier ? .orange : .primary)
                            Spacer()
                            Text(req)
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


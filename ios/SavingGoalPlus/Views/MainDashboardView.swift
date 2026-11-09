import SwiftUI

public struct MainDashboardView: View {
    @ObservedObject var storage = SavingsStorageService.shared
    @State private var showingAddGoalSheet = false
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


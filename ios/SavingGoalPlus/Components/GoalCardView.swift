import SwiftUI

public struct GoalCardView: View {
    public let goal: Goal
    public let onEdit: () -> Void
    public let onArchive: () -> Void
    public let onDelete: () -> Void

    public init(
        goal: Goal,
        onEdit: @escaping () -> Void,
        onArchive: @escaping () -> Void,
        onDelete: @escaping () -> Void
    ) {
        self.goal = goal
        self.onEdit = onEdit
        self.onArchive = onArchive
        self.onDelete = onDelete
    }

    private var plan: SavingsRatePlan {
        let epoch = SavingsCalculations.parseDateToEpochMs(goal.targetDate)
        return SavingsCalculations.calculateRates(
            targetAmount: goal.targetAmount,
            currentSaved: goal.savedAmount,
            targetDateEpochMs: epoch
        )
    }

    public var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 14) {
                // Category / Monogram Icon
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(ColorTheme.stageColor(for: goal.progressPercentage).opacity(0.15))
                        .frame(width: 48, height: 48)

                    Text(String(goal.title.prefix(2)).uppercased())
                        .font(.system(size: 16, weight: .bold, design: .rounded))
                        .foregroundColor(ColorTheme.stageColor(for: goal.progressPercentage))
                }

                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(goal.title)
                            .font(.system(size: 17, weight: .semibold, design: .rounded))
                            .foregroundColor(.primary)
                            .lineLimit(1)

                        if goal.isCompleted {
                            Image(systemName: "checkmark.seal.fill")
                                .foregroundColor(ColorTheme.primaryEmerald)
                                .font(.system(size: 14))
                        }
                    }

                    Text("Target: \(SavingsCalculations.formatDisplayDate(goal.targetDate)) • \(plan.daysRemaining) days left")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }

                Spacer()

                // Actions Menu
                Menu {
                    Button(action: onEdit) {
                        Label("Edit Goal", systemImage: "pencil")
                    }
                    Button(action: onArchive) {
                        Label(goal.isArchived ? "Unarchive" : "Archive", systemImage: "archivebox")
                    }
                    Button(role: .destructive, action: onDelete) {
                        Label("Delete Goal", systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.secondary)
                        .padding(8)
                }
            }

            // Progress Bar
            GeometryReader { proxy in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(ColorTheme.trackBackground)
                        .frame(height: 8)

                    Capsule()
                        .fill(ColorTheme.stageColor(for: goal.progressPercentage))
                        .frame(width: proxy.size.width * CGFloat(goal.progressFraction), height: 8)
                }
            }
            .frame(height: 8)

            // Amounts Row
            HStack {
                Text(SavingsCalculations.formatCurrency(goal.savedAmount))
                    .font(.system(size: 15, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)

                Text("of \(SavingsCalculations.formatCurrency(goal.targetAmount))")
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)

                Spacer()

                Text("\(goal.progressPercentage)%")
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundColor(ColorTheme.stageColor(for: goal.progressPercentage))
            }
        }
        .padding(16)
        .background(ColorTheme.cardBackground)
        .cornerRadius(16)
        .shadow(color: Color.black.opacity(0.03), radius: 6, x: 0, y: 2)
    }
}


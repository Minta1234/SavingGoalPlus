import SwiftUI

public struct GoalCardView: View {
    @AppStorage(SavingsCalculations.currencyPreferenceKey) private var currencySymbol = "$"
    @AppStorage private var cardColorHex: String
    @State private var showingCardColor = false
    public let goal: Goal
    public let onEdit: () -> Void
    public let onArchive: () -> Void
    public let onDelete: () -> Void
    public let onSaveToday: (() -> Void)?

    public init(
        goal: Goal,
        onEdit: @escaping () -> Void,
        onArchive: @escaping () -> Void,
        onDelete: @escaping () -> Void,
        onSaveToday: (() -> Void)? = nil
    ) {
        self.goal = goal
        self.onEdit = onEdit
        self.onArchive = onArchive
        self.onDelete = onDelete
        self.onSaveToday = onSaveToday
        _cardColorHex = AppStorage(wrappedValue: "", "goal_card_color_\(goal.id)_\(goal.createdAt)")
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
                    Button { showingCardColor = true } label: {
                        Label("Card color", systemImage: "paintpalette")
                    }
                    Button(action: onArchive) {
                        Label(goal.isArchived ? LocalizedStringKey("Unarchive") : LocalizedStringKey("Archive"), systemImage: "archivebox")
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
                Text(SavingsCalculations.formatCurrency(goal.savedAmount, currencySymbol: currencySymbol))
                    .font(.system(size: 15, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)

                Text("of \(SavingsCalculations.formatCurrency(goal.targetAmount, currencySymbol: currencySymbol))")
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)

                Spacer()

                Text("\(goal.progressPercentage)%")
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundColor(ColorTheme.stageColor(for: goal.progressPercentage))
            }
        }
        .padding(16)
        .background {
            ZStack {
                ColorTheme.cardBackground
                if let tint = ColorTheme.color(from: cardColorHex) {
                    tint.opacity(0.18)
                }
            }
        }
        .cornerRadius(16)
        .sheet(isPresented: $showingCardColor) {
            NavigationView {
                Form {
                    ColorPicker("Card color", selection: Binding(
                        get: { ColorTheme.color(from: cardColorHex) ?? ColorTheme.primaryEmerald },
                        set: { if let hex = ColorTheme.hex(from: $0) { cardColorHex = hex } }
                    ), supportsOpacity: false)
                    Button("Use default") { cardColorHex = "" }
                }
                .navigationTitle("Card color")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Done") { showingCardColor = false }
                    }
                }
            }
        }
        .shadow(color: Color.black.opacity(0.03), radius: 6, x: 0, y: 2)
    }
}


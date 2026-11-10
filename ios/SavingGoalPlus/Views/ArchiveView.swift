import SwiftUI

public struct ArchiveView: View {
    @AppStorage(SavingsCalculations.currencyPreferenceKey) private var currencySymbol = "$"
    @ObservedObject var storage = SavingsStorageService.shared

    private var archivedGoals: [Goal] {
        storage.goals.filter { $0.isArchived }
    }

    public var body: some View {
        NavigationView {
            List {
                if archivedGoals.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "archivebox")
                            .font(.system(size: 40))
                            .foregroundColor(.secondary)
                        Text("No Archived Goals")
                            .font(.headline)
                        Text("Completed or retired goals you archive will appear here safely.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 40)
                    .listRowBackground(Color.clear)
                } else {
                    ForEach(archivedGoals) { goal in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(goal.title)
                                    .font(.headline)
                                Text("\(SavingsCalculations.formatCurrency(goal.savedAmount, currencySymbol: currencySymbol)) of \(SavingsCalculations.formatCurrency(goal.targetAmount, currencySymbol: currencySymbol))")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            Button("Restore") {
                                storage.toggleArchive(id: goal.id)
                            }
                            .buttonStyle(.bordered)
                        }
                    }
                    .onDelete { indexSet in
                        for i in indexSet {
                            storage.deleteGoal(id: archivedGoals[i].id)
                        }
                    }
                }
            }
            .navigationTitle("Archived Goals")
        }
    }
}


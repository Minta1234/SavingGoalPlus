import SwiftUI

public struct GoalEditSheet: View {
    @Environment(\.dismiss) private var dismiss

    public var existingGoal: Goal?
    public var onSave: (String, Double, String, String, String) -> Void

    @State private var title: String = ""
    @State private var targetAmountString: String = ""
    @State private var targetDate: Date = Calendar.current.date(byAdding: .month, value: 3, to: Date()) ?? Date()
    @State private var category: String = "General"
    @State private var notes: String = ""

    private let categories = ["General", "Emergency", "Travel", "Electronics", "Vehicle", "Home", "Education", "Shopping"]

    public init(
        existingGoal: Goal? = nil,
        onSave: @escaping (String, Double, String, String, String) -> Void
    ) {
        self.existingGoal = existingGoal
        self.onSave = onSave
    }

    public var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Goal Details")) {
                    TextField("Goal Title (e.g. Dream House)", text: $title)

                    TextField("Target Amount ($)", text: $targetAmountString)
                        .keyboardType(.decimalPad)

                    Picker("Category", selection: $category) {
                        ForEach(categories, id: \.self) { cat in
                            Text(cat).tag(cat)
                        }
                    }

                    DatePicker(
                        "Target Date",
                        selection: $targetDate,
                        in: Date()...,
                        displayedComponents: .date
                    )
                }

                Section(header: Text("Notes (Optional)")) {
                    TextEditor(text: $notes)
                        .frame(minHeight: 80)
                }
            }
            .navigationTitle(existingGoal == nil ? "New Savings Goal" : "Edit Goal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        let amount = Double(targetAmountString.replacingOccurrences(of: ",", with: "")) ?? 0.0
                        let formatter = DateFormatter()
                        formatter.dateFormat = "yyyy-MM-dd"
                        let dateString = formatter.string(from: targetDate)

                        onSave(title, amount, dateString, category, notes)
                        dismiss()
                    }
                    .disabled(title.trimmingCharacters(in: .whitespaces).isEmpty || (Double(targetAmountString) ?? 0) <= 0)
                }
            }
            .onAppear {
                if let goal = existingGoal {
                    title = goal.title
                    targetAmountString = String(format: "%.2f", goal.targetAmount)
                    category = goal.category
                    notes = goal.notes

                    let formatter = DateFormatter()
                    formatter.dateFormat = "yyyy-MM-dd"
                    if let parsedDate = formatter.date(from: goal.targetDate) {
                        targetDate = parsedDate
                    }
                }
            }
        }
    }
}


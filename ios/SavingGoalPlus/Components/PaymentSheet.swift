import SwiftUI

public struct PaymentSheet: View {
    @Environment(\.dismiss) private var dismiss

    public let goalTitle: String
    public var onSave: (Double, String, String) -> Void

    @State private var isDeposit: Bool = true
    @State private var amountString: String = ""
    @State private var notes: String = ""
    @State private var transactionDate: Date = Date()

    public init(
        goalTitle: String,
        onSave: @escaping (Double, String, String) -> Void
    ) {
        self.goalTitle = goalTitle
        self.onSave = onSave
    }

    public var body: some View {
        NavigationView {
            Form {
                Section {
                    Picker("Transaction Type", selection: $isDeposit) {
                        Text("Deposit (+)").tag(true)
                        Text("Withdrawal (-)").tag(false)
                    }
                    .pickerStyle(.segmented)
                }

                Section(header: Text("Transaction Details for \(goalTitle)")) {
                    TextField("Amount ($)", text: $amountString)
                        .keyboardType(.decimalPad)
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                        .foregroundColor(isDeposit ? ColorTheme.primaryEmerald : ColorTheme.withdrawalRed)

                    DatePicker("Date", selection: $transactionDate, displayedComponents: .date)

                    TextField("Note (e.g. Monthly salary savings)", text: $notes)
                }
            }
            .navigationTitle(isDeposit ? "Add Deposit" : "Record Withdrawal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        guard let rawAmount = Double(amountString.replacingOccurrences(of: ",", with: "")), rawAmount > 0 else { return }
                        let finalAmount = isDeposit ? rawAmount : -rawAmount
                        let formatter = DateFormatter()
                        formatter.dateFormat = "yyyy-MM-dd"
                        let dateString = formatter.string(from: transactionDate)

                        onSave(finalAmount, notes, dateString)
                        dismiss()
                    }
                    .disabled((Double(amountString) ?? 0) <= 0)
                }
            }
        }
    }
}


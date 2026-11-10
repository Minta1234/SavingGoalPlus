import SwiftUI

public struct PaymentSheet: View {
    @Environment(\.dismiss) private var dismiss
    @AppStorage(SavingsCalculations.currencyPreferenceKey) private var currencySymbol = "$"

    public let goalTitle: String
    public let todayOnly: Bool
    public var onSave: (Double, String, String) -> Void

    @State private var isDeposit: Bool = true
    @State private var amountString: String = ""
    @State private var notes: String = ""
    @State private var transactionDate: Date = Date()

    public init(
        goalTitle: String,
        todayOnly: Bool = false,
        onSave: @escaping (Double, String, String) -> Void
    ) {
        self.goalTitle = goalTitle
        self.todayOnly = todayOnly
        self.onSave = onSave
    }

    private var validAmount: Double? {
        guard let amount = Double(amountString.replacingOccurrences(of: ",", with: "")),
              amount.isFinite, amount > 0 else { return nil }
        return amount
    }

    public var body: some View {
        NavigationView {
            Form {
                if !todayOnly {
                    Section {
                        Picker("Transaction Type", selection: $isDeposit) {
                            Text("Deposit (+)").tag(true)
                            Text("Withdrawal (-)").tag(false)
                        }
                        .pickerStyle(.segmented)
                    }
                }

                Section(header: Text("Transaction Details for \(goalTitle)")) {
                    TextField("Amount (\(currencySymbol))", text: $amountString)
                        .keyboardType(.decimalPad)
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                        .foregroundColor(isDeposit ? ColorTheme.primaryEmerald : ColorTheme.withdrawalRed)

                    if !todayOnly {
                        DatePicker("Date", selection: $transactionDate, displayedComponents: .date)
                    }

                    TextField("Note (e.g. Monthly salary savings)", text: $notes)
                }
            }
            .navigationTitle(LocalizedStringKey(todayOnly ? "Save today" : (isDeposit ? "Add Deposit" : "Record Withdrawal")))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        guard let rawAmount = validAmount else { return }
                        let finalAmount = isDeposit ? rawAmount : -rawAmount
                        let formatter = DateFormatter()
                        formatter.dateFormat = "yyyy-MM-dd"
                        let dateString = todayOnly ? Payment.todayDateString() : formatter.string(from: transactionDate)

                        onSave(finalAmount, notes, dateString)
                        dismiss()
                    }
                    .disabled(validAmount == nil)
                }
            }
        }
    }
}


import SwiftUI
import UniformTypeIdentifiers

private struct SavingsTableDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.commaSeparatedText] }
    var text: String
    init(text: String) { self.text = text }
    init(configuration: ReadConfiguration) throws {
        text = String(data: configuration.file.regularFileContents ?? Data(), encoding: .utf8) ?? ""
    }
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: Data(text.utf8))
    }
}

private struct SavedSavingsTable: Codable {
    let plan: SavingsTablePlan
    var completed: Set<Int>
}

struct SavingsTableView: View {
    @Environment(\.dismiss) private var dismiss
    @AppStorage("savings_spreadsheet") private var stored = Data()
    @State private var daily = ""
    @State private var target = ""
    @State private var error = false
    @State private var pending: SavingsTablePlan?
    @State private var exporting = false
    @State private var document = SavingsTableDocument(text: "")
    @State private var exportError = false

    private var table: SavedSavingsTable? {
        guard let value = try? JSONDecoder().decode(SavedSavingsTable.self, from: stored),
              SavingsTablePlan(daily: value.plan.daily, target: value.plan.target) != nil else { return nil }
        return value
    }
    private func save(_ plan: SavingsTablePlan, completed: Set<Int>) {
        if let data = try? JSONEncoder().encode(SavedSavingsTable(plan: plan, completed: completed)) {
            stored = data
        }
    }

    var body: some View {
        Form {
            Section {
                Text("Choose your daily savings and target in baht. The final day saves only the remaining amount.")
                TextField("Save / day (THB)", text: $daily).keyboardType(.decimalPad)
                TextField("Target (THB)", text: $target).keyboardType(.decimalPad)
                if error {
                    Text("Enter positive amounts with at most 2 decimal places (up to 999,999,999.99 baht). Maximum 10,000 days.")
                        .foregroundColor(.red)
                }
                Button("Create table") {
                    guard let d = SavingsTablePlan.parse(daily), let t = SavingsTablePlan.parse(target),
                          let next = SavingsTablePlan(daily: d, target: t) else { error = true; return }
                    error = false
                    guard next != table?.plan else { return }
                    if let existing = table, !existing.completed.isEmpty { pending = next }
                    else { save(next, completed: []) }
                }
            }
            if let table = table {
                Section {
                    Text(String.localizedStringWithFormat(NSLocalizedString("%lld days · Saved %@ / %@ THB", comment: "Savings table summary"),
                         Int64(table.plan.dayCount), SavingsTablePlan.format(table.plan.saved(table.completed)), SavingsTablePlan.format(table.plan.target)))
                    Text("Check off each day after saving. This table is stored on this device, separately from goal transactions and JSON backups.")
                        .font(.caption).foregroundColor(.secondary)
                    Button("Export CSV for Excel") {
                        let headings = ["Day", "Save / day (THB)", "Planned total (THB)", "Remaining (THB)", "Saved (1/0)"]
                            .map { NSLocalizedString($0, comment: "Savings table column") }
                        document = SavingsTableDocument(text: table.plan.csv(completed: table.completed, headings: headings))
                        exporting = true
                    }
                }
                Section {
                    ForEach(1...table.plan.dayCount, id: \.self) { day in
                        Toggle(isOn: Binding(get: { table.completed.contains(day) }, set: { checked in
                            var days = table.completed
                            if checked { days.insert(day) } else { days.remove(day) }
                            save(table.plan, completed: days)
                        })) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Day \(day)").font(.headline)
                                Text("฿\(SavingsTablePlan.format(table.plan.amount(day)))")
                                Text(String.localizedStringWithFormat(NSLocalizedString("Planned total: %@ THB", comment: "Savings table row"), SavingsTablePlan.format(table.plan.cumulative(day))))
                                    .font(.caption).foregroundColor(.secondary)
                            }
                        }
                    }
                } header: { Text("Savings spreadsheet") }
            }
        }
        .navigationTitle("Savings spreadsheet")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Menu {
                    Button("Savings goals") { dismiss() }
                    Button("Savings spreadsheet") { }
                } label: { Image(systemName: "line.3.horizontal").accessibilityLabel("Functions") }
            }
        }
        .onAppear {
            if let plan = table?.plan {
                daily = SavingsTablePlan.format(plan.daily)
                target = SavingsTablePlan.format(plan.target)
            }
        }
        .alert("Replace table?", isPresented: Binding(get: { pending != nil }, set: { if !$0 { pending = nil } })) {
            Button("Create table", role: .destructive) {
                if let plan = pending { save(plan, completed: []) }
                pending = nil
            }
            Button("Cancel", role: .cancel) { pending = nil }
        } message: { Text("Creating a new plan clears the saved-day checks in this table.") }
        .fileExporter(isPresented: $exporting, document: document, contentType: .commaSeparatedText, defaultFilename: "savings-table.csv") { result in
            if case .failure = result { exportError = true }
        }
        .alert("Could not export file", isPresented: $exportError) { Button("OK", role: .cancel) {} }
    }
}

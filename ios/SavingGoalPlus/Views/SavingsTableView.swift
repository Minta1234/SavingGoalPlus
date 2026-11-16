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
    @AppStorage("savings_spreadsheet") private var legacyStored = Data()
    @AppStorage("savings_spreadsheets_v2") private var stored = Data()
    @State private var selectedId: UUID?
    @State private var name = ""
    @State private var initialized = false
    @State private var daily = ""
    @State private var target = ""
    @State private var error = false
    @State private var pending: SavingsTableEntry?
    @State private var deleting: SavingsTableEntry?
    @State private var exporting = false
    @State private var document = SavingsTableDocument(text: "")
    @State private var exportError = false

    private var tables: [SavingsTableEntry] {
        (try? JSONDecoder().decode([SavingsTableEntry].self, from: stored)) ?? []
    }
    private var table: SavingsTableEntry? { tables.first { $0.id == selectedId } }
    private func persist(_ entries: [SavingsTableEntry]) {
        if let data = try? JSONEncoder().encode(entries) { stored = data }
    }
    private func select(_ entry: SavingsTableEntry?) {
        selectedId = entry?.id
        name = entry?.name ?? ""
        daily = entry.map { SavingsTablePlan.format($0.plan.daily) } ?? ""
        target = entry.map { SavingsTablePlan.format($0.plan.target) } ?? ""
        error = false
    }
    private func save(_ entry: SavingsTableEntry) {
        if tables.contains(where: { $0.id == entry.id }) {
            persist(tables.map { $0.id == entry.id ? entry : $0 })
        } else { persist(tables + [entry]) }
        select(entry)
    }
    private func initialize() {
        guard !initialized else { return }
        if stored.isEmpty {
            var entries: [SavingsTableEntry] = []
            if let legacy = try? JSONDecoder().decode(SavedSavingsTable.self, from: legacyStored),
               SavingsTablePlan(daily: legacy.plan.daily, target: legacy.plan.target) != nil {
                entries = [SavingsTableEntry(name: NSLocalizedString("Savings spreadsheet", comment: "Migrated table"),
                                             plan: legacy.plan, completed: legacy.completed)]
            }
            persist(entries)
        }
        select(tables.first)
        initialized = true
    }

    var body: some View {
        Form {
            Section {
                if !tables.isEmpty {
                    Menu {
                        ForEach(tables) { entry in
                            Button(entry.name) { select(entry) }
                        }
                    } label: {
                        HStack {
                            Text(table?.name ?? NSLocalizedString("Select table", comment: "Table chooser"))
                            Spacer()
                            Image(systemName: "chevron.down")
                        }
                    }
                }
                Button("+ New table") { select(nil) }
            }
            Section {
                TextField("Table name", text: $name)
                Text("Choose your daily savings and target in baht. The final day saves only the remaining amount.")
                TextField("Save / day (THB)", text: $daily).keyboardType(.decimalPad)
                TextField("Target (THB)", text: $target).keyboardType(.decimalPad)
                if error {
                    Text("Enter a table name and positive amounts with at most 2 decimal places (up to 999,999,999.99 baht). Maximum 10,000 days.")
                        .foregroundColor(.red)
                }
                Button(LocalizedStringKey(table == nil ? "Create table" : "Save table")) {
                    guard !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, let d = SavingsTablePlan.parse(daily), let t = SavingsTablePlan.parse(target),
                          let next = SavingsTablePlan(daily: d, target: t) else { error = true; return }
                    error = false
                    let entry = table?.withDetails(name: name, plan: next)
                        ?? SavingsTableEntry(name: name.trimmingCharacters(in: .whitespacesAndNewlines), plan: next)
                    if let existing = table, next != existing.plan, !existing.completed.isEmpty { pending = entry }
                    else { save(entry) }
                }
                if let table = table {
                    Button("Delete table", role: .destructive) { deleting = table }
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
                            guard let current = tables.first(where: { $0.id == table.id }) else { return }
                            let updated = current.withDay(day, saved: checked)
                            persist(tables.map { $0.id == updated.id ? updated : $0 })
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
        .onAppear { initialize() }
        .alert("Replace table?", isPresented: Binding(get: { pending != nil }, set: { if !$0 { pending = nil } })) {
            Button("Save table", role: .destructive) {
                if let entry = pending { save(entry) }
                pending = nil
            }
            Button("Cancel", role: .cancel) { pending = nil }
        } message: { Text("Creating a new plan clears the saved-day checks in this table.") }
        .alert("Delete table", isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } })) {
            Button("Delete table", role: .destructive) {
                let remaining = tables.filter { $0.id != deleting?.id }
                persist(remaining)
                select(remaining.first)
                deleting = nil
            }
            Button("Cancel", role: .cancel) { deleting = nil }
        } message: {
            Text(String.localizedStringWithFormat(NSLocalizedString("Delete “%@” and its saved-day history? Other tables will be kept.", comment: "Delete confirmation"), deleting?.name ?? ""))
        }
        .fileExporter(isPresented: $exporting, document: document, contentType: .commaSeparatedText, defaultFilename: "savings-table-\(selectedId?.uuidString ?? "new").csv") { result in
            if case .failure = result { exportError = true }
        }
        .alert("Could not export file", isPresented: $exportError) { Button("OK", role: .cancel) {} }
    }
}

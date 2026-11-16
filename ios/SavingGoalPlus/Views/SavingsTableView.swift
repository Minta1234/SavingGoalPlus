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
    @State private var editingId: UUID?
    @State private var showEditor = false
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
    private func edit(_ entry: SavingsTableEntry?) {
        editingId = entry?.id
        showEditor = true
        name = entry?.name ?? ""
        daily = entry.map { SavingsTablePlan.format($0.plan.daily) } ?? ""
        target = entry.map { SavingsTablePlan.format($0.plan.target) } ?? ""
        error = false
    }
    private func save(_ entry: SavingsTableEntry) {
        if tables.contains(where: { $0.id == entry.id }) {
            persist(tables.map { $0.id == entry.id ? entry : $0 })
        } else { persist(tables + [entry]) }
        if editingId == nil { selectedId = entry.id }
        showEditor = false
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
        initialized = true
    }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 14) {
                if let table = table { detail(table) }
                else { dashboard }
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .id(selectedId)
        .background(ColorTheme.screenBackground.ignoresSafeArea())
        .navigationTitle(table?.name ?? NSLocalizedString("Savings spreadsheet", comment: "Screen title"))
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                if table != nil {
                    Button { selectedId = nil } label: {
                        Image(systemName: "chevron.left").accessibilityLabel("All tables")
                    }
                } else {
                    Menu {
                        Button("Savings goals") { dismiss() }
                        Button("Savings spreadsheet") { selectedId = nil }
                    } label: { Image(systemName: "line.3.horizontal").accessibilityLabel("Functions") }
                }
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                if table == nil {
                    Button { edit(nil) } label: {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 22)).foregroundColor(ColorTheme.primaryEmerald)
                            .accessibilityLabel("+ New table")
                    }
                }
            }
        }
        .onAppear { initialize() }
        .sheet(isPresented: $showEditor) { editor }
        .alert("Delete table", isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } })) {
            Button("Delete table", role: .destructive) {
                let remaining = tables.filter { $0.id != deleting?.id }
                persist(remaining)
                if selectedId == deleting?.id { selectedId = nil }
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

    @ViewBuilder private var dashboard: some View {
        if tables.isEmpty {
            VStack(spacing: 12) {
                Image(systemName: "tablecells").font(.system(size: 64))
                    .foregroundColor(ColorTheme.primaryEmerald.opacity(0.4))
                Text("No savings tables yet").font(.title3.bold())
                Text("Tap + to create your first savings table.")
                    .font(.subheadline).foregroundColor(.secondary).multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity).padding(.vertical, 64)
        } else {
            let saved = tables.reduce(Int64(0)) { $0 + $1.plan.saved($1.completed) }
            let total = tables.reduce(Int64(0)) { $0 + $1.plan.target }
            VStack(alignment: .leading, spacing: 10) {
                Text("Savings overview").font(.headline)
                Text("฿\(SavingsTablePlan.format(saved))")
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                Text(String.localizedStringWithFormat(NSLocalizedString("Target: %@ THB", comment: "Savings target"), SavingsTablePlan.format(total)))
                    .font(.subheadline)
                Text(String.localizedStringWithFormat(NSLocalizedString("%lld tables", comment: "Table count"), Int64(tables.count)))
                    .font(.caption.bold())
            }
            .frame(maxWidth: .infinity, alignment: .leading).padding(20)
            .background(ColorTheme.primaryEmerald.opacity(0.15))
            .cornerRadius(20)
            ForEach(tables) { entry in
                SavingsTableCardView(entry: entry, onOpen: { selectedId = entry.id },
                                     onEdit: { edit(entry) }, onDelete: { deleting = entry })
            }
        }
    }

    @ViewBuilder private func detail(_ table: SavingsTableEntry) -> some View {
        let saved = table.plan.saved(table.completed)
        VStack(spacing: 12) {
            CircularGoalGaugeView(progressFraction: Double(saved) / Double(table.plan.target),
                                  savedAmount: Double(saved) / 100, targetAmount: Double(table.plan.target) / 100,
                                  size: 210, currencyOverride: "฿")
            Text(String.localizedStringWithFormat(NSLocalizedString("Remaining: %@ THB", comment: "Remaining savings"), SavingsTablePlan.format(table.plan.target - saved)))
                .font(.headline)
            Text(scheduleText(table.plan)).font(.subheadline).foregroundColor(.secondary)
            HStack(spacing: 16) {
                Button("Edit table") { edit(table) }.buttonStyle(.bordered)
                Button("Export CSV for Excel") {
                    let headings = ["Day", "Save / day (THB)", "Planned total (THB)", "Remaining (THB)", "Saved (1/0)"]
                        .map { NSLocalizedString($0, comment: "Savings table column") }
                    document = SavingsTableDocument(text: table.plan.csv(completed: table.completed, headings: headings))
                    exporting = true
                }.buttonStyle(.borderedProminent)
            }
            Button("Delete table", role: .destructive) { deleting = table }
        }
        .frame(maxWidth: .infinity).padding(20).background(ColorTheme.cardBackground).cornerRadius(20)
        Text("Check off each day after saving. This table is stored on this device, separately from goal transactions and JSON backups.")
            .font(.caption).foregroundColor(.secondary).frame(maxWidth: .infinity, alignment: .leading)
        Text("Daily savings").font(.headline).frame(maxWidth: .infinity, alignment: .leading).padding(.top, 8)
        ForEach(1...table.plan.dayCount, id: \.self) { day in
            Toggle(isOn: Binding(get: { table.completed.contains(day) }, set: { checked in
                guard let current = tables.first(where: { $0.id == table.id }) else { return }
                let updated = current.withDay(day, saved: checked)
                persist(tables.map { $0.id == updated.id ? updated : $0 })
            })) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Day \(day)").font(.headline)
                    Text("฿\(SavingsTablePlan.format(table.plan.amount(day)))").font(.title3.bold())
                    Text(String.localizedStringWithFormat(NSLocalizedString("Planned total: %@ THB", comment: "Savings table row"), SavingsTablePlan.format(table.plan.cumulative(day))))
                        .font(.caption).foregroundColor(.secondary)
                }
            }
            .tint(ColorTheme.primaryEmerald)
            .padding(16)
            .background(table.completed.contains(day) ? ColorTheme.primaryEmerald.opacity(0.12) : ColorTheme.cardBackground)
            .cornerRadius(16)
        }
    }

    private var editor: some View {
        NavigationView {
            Form {
                Section {
                    TextField("Table name", text: $name)
                    TextField("Save / day (THB)", text: $daily).keyboardType(.decimalPad)
                    TextField("Target (THB)", text: $target).keyboardType(.decimalPad)
                }
                Section {
                    Text("Choose your daily savings and target in baht. The final day saves only the remaining amount.")
                    if error {
                        Text("Enter a table name and positive amounts with at most 2 decimal places (up to 999,999,999.99 baht). Maximum 10,000 days.")
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle(LocalizedStringKey(editingId == nil ? "+ New table" : "Edit table"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { showEditor = false } }
                ToolbarItem(placement: .confirmationAction) { Button("Save table") { submitEditor() } }
            }
            .alert("Replace table?", isPresented: Binding(get: { pending != nil }, set: { if !$0 { pending = nil } })) {
                Button("Save table", role: .destructive) {
                    if let entry = pending { save(entry) }
                    pending = nil
                }
                Button("Cancel", role: .cancel) { pending = nil }
            } message: { Text("Creating a new plan clears the saved-day checks in this table.") }
        }
    }

    private func submitEditor() {
        guard !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              let d = SavingsTablePlan.parse(daily), let t = SavingsTablePlan.parse(target),
              let next = SavingsTablePlan(daily: d, target: t) else { error = true; return }
        error = false
        let existing = tables.first { $0.id == editingId }
        let entry = existing?.withDetails(name: name, plan: next)
            ?? SavingsTableEntry(name: name.trimmingCharacters(in: .whitespacesAndNewlines), plan: next)
        if let existing = existing, next != existing.plan, !existing.completed.isEmpty { pending = entry }
        else { save(entry) }
    }
}

private func scheduleText(_ plan: SavingsTablePlan) -> String {
    String.localizedStringWithFormat(NSLocalizedString("%@ THB / day · %lld days", comment: "Daily savings plan"), SavingsTablePlan.format(plan.daily), Int64(plan.dayCount))
}

private struct SavingsTableCardView: View {
    let entry: SavingsTableEntry
    let onOpen: () -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void
    private var saved: Int64 { entry.plan.saved(entry.completed) }
    private var percent: Int { Int(saved * 100 / entry.plan.target) }
    private var color: Color { ColorTheme.stageColor(for: percent) }

    var body: some View {
        VStack(spacing: 14) {
            HStack(spacing: 14) {
                Text(String(entry.name.prefix(2)).uppercased())
                    .font(.system(size: 16, weight: .bold, design: .rounded)).foregroundColor(color)
                    .frame(width: 48, height: 48).background(color.opacity(0.15)).cornerRadius(14)
                VStack(alignment: .leading, spacing: 4) {
                    Text(entry.name).font(.system(size: 17, weight: .semibold, design: .rounded)).lineLimit(1)
                    Text(scheduleText(entry.plan)).font(.system(size: 12)).foregroundColor(.secondary)
                }
                Spacer(minLength: 0)
                Menu {
                    Button(action: onEdit) { Label("Edit table", systemImage: "pencil") }
                    Button(role: .destructive, action: onDelete) { Label("Delete table", systemImage: "trash") }
                } label: {
                    Image(systemName: "ellipsis").font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.secondary).padding(8).accessibilityLabel("Table options")
                }
            }
            ProgressView(value: Double(saved), total: Double(entry.plan.target)).tint(color)
                .animation(.easeInOut(duration: 0.4), value: saved)
            HStack {
                Text("฿\(SavingsTablePlan.format(saved)) / ฿\(SavingsTablePlan.format(entry.plan.target))")
                    .font(.system(size: 15, weight: .bold, design: .rounded)).foregroundColor(color)
                Spacer()
                Text("\(percent)%").font(.system(size: 14, weight: .bold, design: .rounded)).foregroundColor(color)
            }
        }
        .padding(16).background(ColorTheme.cardBackground).cornerRadius(16)
        .shadow(color: .black.opacity(0.03), radius: 6, x: 0, y: 2)
        .contentShape(Rectangle()).onTapGesture(perform: onOpen)
        .accessibilityAction(named: Text("Open table"), onOpen)
    }
}

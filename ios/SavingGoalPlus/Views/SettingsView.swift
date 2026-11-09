import SwiftUI

public struct SettingsView: View {
    @ObservedObject var notifications = NotificationManager.shared
    @ObservedObject var storage = SavingsStorageService.shared

    @State private var reminderDate: Date = {
        var comp = DateComponents()
        comp.hour = 20
        comp.minute = 0
        return Calendar.current.date(from: comp) ?? Date()
    }()

    @State private var showingExportAlert = false
    @State private var exportText = ""
    @State private var showingImportSheet = false
    @State private var importText = ""
    @State private var importErrorMessage = ""
    @State private var showingErrorAlert = false
    @State private var showingSuccessAlert = false

    public var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Daily Savings Reminders")) {
                    DatePicker("Reminder Time", selection: $reminderDate, displayedComponents: .hourAndMinute)
                        .onChange(of: reminderDate) { newDate in
                            let comp = Calendar.current.dateComponents([.hour, .minute], from: newDate)
                            let h = comp.hour ?? 20
                            let m = comp.minute ?? 0
                            notifications.scheduleDailyReminder(hour: h, minute: m)
                        }

                    Button("Send Test Notification Now") {
                        if !notifications.isAuthorized {
                            notifications.requestAuthorization { granted in
                                if granted {
                                    notifications.sendTestNotification()
                                }
                            }
                        } else {
                            notifications.sendTestNotification()
                        }
                    }
                }

                Section(header: Text("Data Backup & Restore")) {
                    Button(action: exportJson) {
                        Label("Export Backup (JSON)", systemImage: "square.and.arrow.up")
                    }

                    Button(action: exportCsv) {
                        Label("Export Data as CSV", systemImage: "tablecells")
                    }

                    Button(action: { showingImportSheet = true }) {
                        Label("Restore Backup (JSON)", systemImage: "square.and.arrow.down")
                    }
                }

                Section(header: Text("About SavingGoalPlus")) {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text("1.0.0 (Native iOS)")
                            .foregroundColor(.secondary)
                    }

                    HStack {
                        Text("License")
                        Spacer()
                        Text("MIT")
                            .foregroundColor(.secondary)
                    }

                    HStack {
                        Text("Maintainer")
                        Spacer()
                        Text("Minta1234")
                            .foregroundColor(.secondary)
                    }

                    HStack {
                        Text("AI Architecture")
                        Spacer()
                        Text("Antigravity (DeepMind)")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .navigationTitle("Settings")
            .sheet(isPresented: $showingExportAlert) {
                ShareSheet(activityItems: [exportText])
            }
            .sheet(isPresented: $showingImportSheet) {
                NavigationView {
                    VStack(spacing: 12) {
                        Text("Paste your JSON backup payload below:")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal)

                        TextEditor(text: $importText)
                            .padding(8)
                            .background(Color(UIColor.secondarySystemBackground))
                            .cornerRadius(10)
                            .padding(.horizontal)

                        Spacer()
                    }
                    .padding(.top)
                    .navigationTitle("Import Backup")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { showingImportSheet = false }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Import") {
                                do {
                                    try storage.importBackupJson(importText)
                                    showingImportSheet = false
                                    showingSuccessAlert = true
                                } catch {
                                    importErrorMessage = error.localizedDescription
                                    showingErrorAlert = true
                                }
                            }
                            .disabled(importText.trimmingCharacters(in: .whitespaces).isEmpty)
                        }
                    }
                }
            }
            .alert("Import Successful", isPresented: $showingSuccessAlert) {
                Button("OK", role: .cancel) {}
            } message: {
                Text("All goals and payment history have been restored.")
            }
            .alert("Import Failed", isPresented: $showingErrorAlert) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(importErrorMessage)
            }
        }
    }

    private func exportJson() {
        self.exportText = storage.exportBackupJson()
        self.showingExportAlert = true
    }

    private func exportCsv() {
        self.exportText = storage.exportCsv()
        self.showingExportAlert = true
    }
}

private struct ShareSheet: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

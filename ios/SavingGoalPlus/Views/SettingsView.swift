import SwiftUI

#if canImport(UIKit)
import UIKit
#endif

public struct SettingsView: View {
    @ObservedObject var notifications = NotificationManager.shared
    @ObservedObject var storage = SavingsStorageService.shared
    @AppStorage("app_accent_theme") private var selectedAccent: String = "emerald"

    @State private var reminderDate: Date = {
        var comp = DateComponents()
        comp.hour = 20
        comp.minute = 0
        return Calendar.current.date(from: comp) ?? Date()
    }()

    @State private var showingExportAlert = false
    @State private var exportItems: [Any] = []
    @State private var showingImportSheet = false
    @State private var importText = ""
    @State private var importErrorMessage = ""
    @State private var showingErrorAlert = false
    @State private var showingSuccessAlert = false
    @State private var showingChangelogSheet = false

    public init() {}

    public var body: some View {
        NavigationView {
            Form {
                // Theme Accent Section (iOS UI Swatches)
                Section(header: Text("Theme Accent (iOS Style)")) {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Active: \(AppAccentTheme(rawValue: selectedAccent)?.displayName ?? "Apple Mint")")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 14), count: 4), spacing: 14) {
                            ForEach(AppAccentTheme.allCases) { theme in
                                Button(action: {
                                    selectedAccent = theme.rawValue
                                    UserDefaults.standard.set(theme.rawValue, forKey: "app_accent_theme")
                                }) {
                                    VStack(spacing: 4) {
                                        ZStack {
                                            Circle()
                                                .fill(theme.color)
                                                .frame(width: 44, height: 44)
                                                .overlay(
                                                    Circle()
                                                        .stroke(Color.primary, lineWidth: selectedAccent == theme.rawValue ? 3 : 0)
                                                )

                                            if selectedAccent == theme.rawValue {
                                                Image(systemName: "checkmark")
                                                    .font(.system(size: 16, weight: .bold))
                                                    .foregroundColor(.white)
                                            }
                                        }

                                        Text(theme.displayName.components(separatedBy: " ").first ?? "")
                                            .font(.system(size: 10, weight: selectedAccent == theme.rawValue ? .bold : .regular))
                                            .foregroundColor(selectedAccent == theme.rawValue ? .primary : .secondary)
                                    }
                                }
                                .buttonStyle(PlainButtonStyle())
                            }
                        }
                        .padding(.vertical, 6)
                    }
                }

                // Daily Reminder Section
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

                // Data Backup Section
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

                // Updates & Changelog Section
                Section(header: Text("Updates & Change Log")) {
                    Button(action: { showingChangelogSheet = true }) {
                        HStack {
                            Label("What's New / Change Log", systemImage: "clock.arrow.circlepath")
                            Spacer()
                            Text("v1.0.1")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }

                    if let url = URL(string: "https://github.com/Minta1234/SavingGoalPlus/releases/latest") {
                        Link(destination: url) {
                            Label("Check for Updates (GitHub)", systemImage: "arrow.down.circle")
                        }
                    }
                }

                // About Section
                Section(header: Text("About SavingGoalPlus")) {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text("1.0.1 (Native iOS & Android)")
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
            #if canImport(UIKit)
            .sheet(isPresented: $showingExportAlert) {
                ShareSheet(activityItems: exportItems)
            }
            #endif
            .sheet(isPresented: $showingChangelogSheet) {
                ChangelogSheetView()
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
                            .font(.system(.body, design: .monospaced))
                            .padding(8)
                            .background(ColorTheme.cardBackground)
                            .cornerRadius(12)
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.gray.opacity(0.2)))
                            .padding(.horizontal)

                        Spacer()
                    }
                    .padding(.top)
                    .navigationTitle("Restore Backup")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { showingImportSheet = false }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Restore") {
                                do {
                                    try storage.importBackup(jsonString: importText)
                                    showingImportSheet = false
                                    showingSuccessAlert = true
                                } catch {
                                    importErrorMessage = error.localizedDescription
                                    showingErrorAlert = true
                                }
                            }
                            .disabled(importText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                        }
                    }
                }
            }
            .alert("Import Error", isPresented: $showingErrorAlert) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(importErrorMessage)
            }
            .alert("Success", isPresented: $showingSuccessAlert) {
                Button("OK", role: .cancel) { }
            } message: {
                Text("Backup imported successfully!")
            }
        }
    }

    private func exportJson() {
        let json = storage.exportBackupJson()
        let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent("SavingGoalPlus_Backup.json")
        try? json.write(to: tempURL, atomically: true, encoding: .utf8)
        exportItems = [tempURL]
        showingExportAlert = true
    }

    private func exportCsv() {
        let csv = storage.exportCsv()
        let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent("SavingGoalPlus_Transactions.csv")
        try? csv.write(to: tempURL, atomically: true, encoding: .utf8)
        exportItems = [tempURL]
        showingExportAlert = true
    }
}

// Full iOS Change Log Sheet
public struct ChangelogSheetView: View {
    @Environment(\.dismiss) var dismiss

    public init() {}

    public var body: some View {
        NavigationView {
            List {
                Section(header: Text("v1.0.2 • Latest")) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("🎨 8 iOS-Style Theme Accent Palettes")
                            .font(.headline)
                        Text("Choose from Apple Mint, Cupertino Blue, Indigo Night, Rose Pink, Sunset Orange, Pacific Teal, Royal Purple, and Midnight Slate.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)

                        Divider().padding(.vertical, 4)

                        Text("🔄 Seamless In-Place Updates")
                            .font(.headline)
                        Text("Unified permanent release signing keystore eliminates the need to uninstall before updating Android APKs.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)

                        Divider().padding(.vertical, 4)

                        Text("📋 In-App Change Log & Update Checker")
                            .font(.headline)
                        Text("Easily view release history and check for GitHub Releases in 1 tap.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 4)
                }

                Section(header: Text("v1.0.1 • Multi-Platform iOS Release")) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("• 🍏 100% Native Swift 5.9 + SwiftUI (iOS 16.0+)")
                        Text("• 📦 Sideloadable unsigned .ipa and .xcarchive packages")
                        Text("• ⚡ Smart path-filtered GitHub Actions CI/CD")
                    }
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                }

                Section(header: Text("v1.0.0 • Initial Release")) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("• 🚀 100% Native Kotlin 2.0 + Jetpack Compose rewrite")
                        Text("• 📊 260° Dynamic Arc Gauge with 3-tier colors")
                        Text("• 🧮 Daily, Weekly, Bi-Weekly, Monthly projection math")
                        Text("• 🎉 2D Confetti Particle Physics milestone celebration")
                        Text("• ⏰ Scheduled daily reminder notifications via AlarmManager")
                        Text("• 🔒 Offline SQLite storage with JSON/CSV export")
                    }
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                }
            }
            .navigationTitle("Change Log")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

#if canImport(UIKit)
struct ShareSheet: UIViewControllerRepresentable {
    var activityItems: [Any]
    var applicationActivities: [UIActivity]? = nil

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: applicationActivities)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
#endif

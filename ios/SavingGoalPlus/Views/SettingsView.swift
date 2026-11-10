import SwiftUI
import UniformTypeIdentifiers
import WebKit

private var installedVersion: String? {
    Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String
}

public struct SettingsView: View {
    @ObservedObject var notifications = NotificationManager.shared
    @ObservedObject var storage = SavingsStorageService.shared
    @AppStorage(AppLanguage.preferenceKey) private var language = AppLanguage.system
    @AppStorage(SavingsCalculations.currencyPreferenceKey) private var currencySymbol = "$"
    @State private var showingCurrencySheet = false
    @State private var showingUpdateSheet = false
    @AppStorage(ColorTheme.accentPresetKey) private var selectedAccent = "emerald"
    @AppStorage(ColorTheme.customAccentKey) private var customAccent = ""

    @State private var reminderDate: Date = {
        var comp = DateComponents()
        comp.hour = 20
        comp.minute = 0
        return Calendar.current.date(from: comp) ?? Date()
    }()

    @State private var showingExportAlert = false
    @State private var exportDocument = ExportDocument(data: Data())
    @State private var exportType: UTType = .json
    @State private var exportName = ""
    @State private var exportError: String?
    @State private var showingFilePicker = false
    @State private var readingBackup = false
    @State private var pendingBackup: BackupPayload?
    @State private var importErrorMessage = ""
    @State private var showingErrorAlert = false
    @State private var showingSuccessAlert = false
    @State private var showingChangelogSheet = false

    public init() {}

    public var body: some View {
        NavigationView {
            Form {
                Section("App language") {
                    Picker("App language", selection: $language) {
                        ForEach(AppLanguage.allCases) { option in
                            Text(option.label).tag(option)
                        }
                    }
                }

                // Theme Accent Section (iOS UI Swatches)
                Section(header: Text("Theme Accent (iOS Style)")) {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Active: \(customAccent.isEmpty ? (AppAccentTheme(rawValue: selectedAccent)?.displayName ?? "Apple Mint") : "Custom")")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 14), count: 4), spacing: 14) {
                            ForEach(AppAccentTheme.allCases) { theme in
                                Button(action: {
                                    selectedAccent = theme.rawValue
                                    customAccent = ""
                                }) {
                                    VStack(spacing: 4) {
                                        ZStack {
                                            Circle()
                                                .fill(theme.color)
                                                .frame(width: 44, height: 44)
                                                .overlay(
                                                    Circle()
                                                        .stroke(Color.primary, lineWidth: customAccent.isEmpty && selectedAccent == theme.rawValue ? 3 : 0)
                                                )

                                            if customAccent.isEmpty && selectedAccent == theme.rawValue {
                                                Image(systemName: "checkmark")
                                                    .font(.system(size: 16, weight: .bold))
                                                    .foregroundColor(.white)
                                            }
                                        }

                                        Text(theme.displayName.components(separatedBy: " ").first ?? "")
                                            .font(.system(size: 10, weight: customAccent.isEmpty && selectedAccent == theme.rawValue ? .bold : .regular))
                                            .foregroundColor(customAccent.isEmpty && selectedAccent == theme.rawValue ? .primary : .secondary)
                                    }
                                }
                                .buttonStyle(PlainButtonStyle())
                            }
                        }
                        .padding(.vertical, 6)
                        ColorPicker("Custom accent color", selection: Binding(
                            get: { ColorTheme.accent(preset: selectedAccent, custom: customAccent) },
                            set: { if let hex = ColorTheme.hex(from: $0) { customAccent = hex } }
                        ), supportsOpacity: false)
                        if !customAccent.isEmpty {
                            Button("Use preset accent") { customAccent = "" }
                        }
                    }
                }

                Section("Currency") {
                    Button { showingCurrencySheet = true } label: {
                        HStack {
                            Text("Change Currency")
                            Spacer()
                            Text(currencySymbol).foregroundColor(.secondary)
                        }
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

                    Button { showingFilePicker = true } label: {
                        Label("Restore Backup from Files", systemImage: "square.and.arrow.down")
                    }
                    .disabled(readingBackup)
                    if readingBackup { ProgressView("Reading backup…") }
                }

                // Updates & Changelog Section
                Section(header: Text("Updates & Change Log")) {
                    Button(action: { showingChangelogSheet = true }) {
                        Label("What's New / Change Log", systemImage: "clock.arrow.circlepath")
                    }

                    Button { showingUpdateSheet = true } label: {
                        Label("Check for Updates", systemImage: "arrow.down.circle")
                    }
                }

                // About Section
                Section(header: Text("About SavingGoalPlus")) {
                    HStack {
                        Text("Version")
                        Spacer()
                        Group {
                            if let installedVersion { Text(installedVersion) } else { Text("Unavailable") }
                        }
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
            .fileExporter(
                isPresented: $showingExportAlert,
                document: exportDocument,
                contentType: exportType,
                defaultFilename: exportName
            ) { result in
                if case .failure(let error) = result {
                    exportError = error.localizedDescription
                }
            }
            .alert("Export Error", isPresented: Binding(
                get: { exportError != nil },
                set: { if !$0 { exportError = nil } }
            )) {
                Button("OK", role: .cancel) { exportError = nil }
            } message: {
                Text(exportError ?? "")
            }
            .sheet(isPresented: $showingChangelogSheet) {
                ChangelogSheetView()
            }
            .sheet(isPresented: $showingCurrencySheet) {
                CurrencySettingsSheet(symbol: $currencySymbol)
            }
            .sheet(isPresented: $showingUpdateSheet) {
                UpdateCheckSheet()
            }
            .fileImporter(isPresented: $showingFilePicker, allowedContentTypes: [.data]) { result in
                previewBackup(result)
            }
            .alert("Replace savings data?", isPresented: Binding(
                get: { pendingBackup != nil },
                set: { if !$0 { pendingBackup = nil } }
            ), presenting: pendingBackup) { backup in
                Button("Cancel", role: .cancel) { pendingBackup = nil }
                Button("Restore", role: .destructive) {
                    pendingBackup = nil
                    do {
                        try storage.restoreBackup(backup)
                        showingSuccessAlert = true
                    } catch {
                        reportImportError(error)
                    }
                }
            } message: { backup in
                Text("This backup contains \(backup.goals.count) goals and \(backup.payments.count) transactions. Restoring replaces the current goals and transactions.")
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

    private func previewBackup(_ result: Result<URL, Error>) {
        do {
            let url = try result.get()
            readingBackup = true
            Task {
                defer { readingBackup = false }
                do {
                    pendingBackup = try await Task.detached(priority: .userInitiated) {
                        try SavingsStorageService.readBackup(url)
                    }.value
                } catch {
                    reportImportError(error)
                }
            }
        } catch {
            reportImportError(error)
        }
    }

    private func reportImportError(_ error: Error) {
        importErrorMessage = error.localizedDescription
        showingErrorAlert = true
    }

    private func exportJson() {
        do {
            exportDocument = ExportDocument(data: try storage.exportBackupData())
            exportType = .json
            exportName = "SavingGoalPlus_Backup"
            showingExportAlert = true
        } catch {
            exportError = error.localizedDescription
        }
    }

    private func exportCsv() {
        prepareExport(storage.exportCsv(), type: .commaSeparatedText, name: "SavingGoalPlus_Transactions")
    }

    private func prepareExport(_ content: String, type: UTType, name: String) {
        exportDocument = ExportDocument(data: Data(content.utf8))
        exportType = type
        exportName = name
        showingExportAlert = true
    }
}

private struct CurrencySettingsSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var symbol: String
    @State private var draft = ""
    private let presets = ["$", "€", "£", "¥", "฿", "₹", "₩", "₱", "R$", "zł", "CHF", "kr"]

    var body: some View {
        NavigationView {
            Form {
                Section("Popular symbols") {
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 58))]) {
                        ForEach(presets, id: \.self) { preset in
                            Button { draft = preset } label: {
                                Text(preset)
                                    .frame(maxWidth: .infinity, minHeight: 44)
                                    .background(draft == preset ? Color.accentColor.opacity(0.16) : Color.clear,
                                                in: RoundedRectangle(cornerRadius: 8))
                            }
                            .buttonStyle(.borderless)
                            .accessibilityAddTraits(draft == preset ? .isSelected : [])
                        }
                    }
                }
                Section {
                    TextField("Custom Symbol / Code", text: $draft)
                        .autocorrectionDisabled()
                } footer: {
                    Text("This changes how amounts are labeled. Amounts are not converted.")
                }
            }
            .navigationTitle("Change Currency")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        symbol = draft.trimmingCharacters(in: .whitespacesAndNewlines)
                        dismiss()
                    }
                    .disabled(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || draft.count > 12)
                }
            }
            .onAppear { draft = symbol }
        }
    }
}

private struct ExportDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.json, .commaSeparatedText] }
    let data: Data

    init(data: Data) {
        self.data = data
    }

    init(configuration: ReadConfiguration) throws {
        guard let data = configuration.file.regularFileContents else {
            throw CocoaError(.fileReadCorruptFile)
        }
        self.data = data
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}

private struct GitHubReleaseNote: Decodable, Identifiable {
    let tag: String
    let name: String?
    let bodyHTML: String?
    let htmlURL: URL

    var id: String { tag }
    var title: String { name.flatMap { $0.isEmpty ? nil : $0 } ?? tag }
    var isGitHubReleaseLink: Bool {
        htmlURL.absoluteString.hasPrefix("https://github.com/Minta1234/SavingGoalPlus/releases/tag/")
    }

    enum CodingKeys: String, CodingKey {
        case tag = "tag_name"
        case name
        case bodyHTML = "body_html"
        case htmlURL = "html_url"
    }

    static func load() async throws -> [Self] {
        try await request("?per_page=10")
    }

    static func latest() async throws -> Self {
        try await request("/latest")
    }

    private static func request<T: Decodable>(_ suffix: String) async throws -> T {
        let endpoint = URL(string: "https://api.github.com/repos/Minta1234/SavingGoalPlus/releases" + suffix)!
        var request = URLRequest(url: endpoint, timeoutInterval: 15)
        request.setValue("application/vnd.github.html+json", forHTTPHeaderField: "Accept")
        request.setValue("SavingGoalPlus", forHTTPHeaderField: "User-Agent")
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }
        return try JSONDecoder().decode(T.self, from: data)
    }
}

private struct UpdateCheckSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var latest: GitHubReleaseNote?
    @State private var failed = false
    @State private var attempt = 0

    var body: some View {
        NavigationView {
            List {
                Section("Installed version") {
                    if let installedVersion { Text(installedVersion) } else { Text("Unavailable") }
                }
                if failed {
                    Text("Could not check GitHub for updates.")
                    Button("Try again") { attempt += 1 }
                } else if let latest {
                    Section("Latest release") {
                        Text(latest.tag)
                        Text(updateStatus(latest.tag))
                        if latest.isGitHubReleaseLink {
                            Link("Open GitHub Release", destination: latest.htmlURL)
                        }
                    }
                } else {
                    ProgressView("Checking GitHub…")
                }
            }
            .navigationTitle("Check for Updates")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } }
            }
            .task(id: attempt) {
                failed = false
                do {
                    latest = try await GitHubReleaseNote.latest()
                } catch {
                    if !Task.isCancelled { failed = true }
                }
            }
        }
    }

    private func updateStatus(_ published: String) -> LocalizedStringKey {
        func components(_ version: String) -> [Int]? {
            let value = version.hasPrefix("v") ? version.dropFirst() : version[...]
            let pieces = value.split(separator: ".", omittingEmptySubsequences: false)
            guard pieces.count == 3, pieces.allSatisfy({ part in
                !part.isEmpty && part.utf8.allSatisfy { (48...57).contains($0) }
            }) else { return nil }
            let numbers = pieces.compactMap { Int($0) }
            return numbers.count == 3 ? numbers : nil
        }

        guard let installedVersion, let current = components(installedVersion), let latest = components(published)
        else { return "Could not compare the version numbers." }
        if current == latest { return "Already up to date." }
        return current.lexicographicallyPrecedes(latest)
            ? "An update is available."
            : "The installed version is newer than the latest release."
    }
}

public struct ChangelogSheetView: View {
    @Environment(\.dismiss) var dismiss
    @Environment(\.locale) private var locale
    @State private var releases: [GitHubReleaseNote]?
    @State private var failed = false
    @State private var attempt = 0

    public init() {}

    public var body: some View {
        NavigationView {
            Group {
                if failed {
                    VStack(spacing: 12) {
                        Text("Could not load release notes from GitHub.")
                        Button("Try again") { attempt += 1 }
                    }
                    .padding()
                } else if let releases {
                    if releases.isEmpty {
                        Text("No published releases yet.")
                    } else {
                        ReleaseNotesWebView(document: releaseDocument(releases, locale: locale))
                    }
                } else {
                    ProgressView("Loading release notes from GitHub…")
                }
            }
            .navigationTitle("Change Log")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
            .task(id: attempt) {
                failed = false
                do {
                    releases = try await GitHubReleaseNote.load()
                } catch {
                    if !Task.isCancelled { failed = true }
                }
            }
        }
    }
}

private struct ReleaseNotesWebView: UIViewRepresentable {
    @Environment(\.openURL) private var openURL
    let document: String

    func makeCoordinator() -> Coordinator { Coordinator(openURL: openURL) }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.defaultWebpagePreferences.allowsContentJavaScript = false
        configuration.websiteDataStore = .nonPersistent()
        let view = WKWebView(frame: .zero, configuration: configuration)
        view.navigationDelegate = context.coordinator
        return view
    }

    func updateUIView(_ view: WKWebView, context: Context) {
        guard context.coordinator.loadedDocument != document else { return }
        context.coordinator.loadedDocument = document
        view.loadHTMLString(document, baseURL: nil)
    }

    final class Coordinator: NSObject, WKNavigationDelegate {
        let openURL: OpenURLAction
        var loadedDocument: String?

        init(openURL: OpenURLAction) {
            self.openURL = openURL
        }

        func webView(
            _ webView: WKWebView,
            decidePolicyFor action: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            if action.navigationType == .linkActivated {
                if let url = action.request.url, url.scheme == "https" { openURL(url) }
                decisionHandler(.cancel)
            } else {
                let isLocalDocument = action.request.url?.absoluteString == "about:blank"
                decisionHandler(isLocalDocument ? .allow : .cancel)
            }
        }
    }
}

private func releaseDocument(_ releases: [GitHubReleaseNote], locale: Locale) -> String {
    let emptyNote = htmlEscape(String(localized: "No notes were published for this release.", locale: locale))
    let viewOnGitHub = htmlEscape(String(localized: "View on GitHub", locale: locale))
    let articles = releases.map { release in
        let body = release.bodyHTML.flatMap { $0.isEmpty ? nil : $0 }
            ?? "<p>\(emptyNote)</p>"
        let url = release.htmlURL.absoluteString
        let link = release.isGitHubReleaseLink
            ? "<p><a href=\"\(htmlEscape(url))\">\(viewOnGitHub)</a></p>" : ""
        return "<article><h1>\(htmlEscape(release.title))</h1>\(body)\(link)</article>"
    }.joined()
    return """
        <!doctype html><html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'">
        <style>
          :root { color-scheme: light dark; --line: #d1d9e0; --code: #f6f8fa; --link: #0969da; }
          @media (prefers-color-scheme: dark) { :root { --line: #3d444d; --code: #212830; --link: #58a6ff; } }
          body { margin: 16px; font: 16px/1.55 system-ui, sans-serif; overflow-wrap: anywhere; }
          article + article { border-top: 1px solid var(--line); margin-top: 24px; padding-top: 16px; }
          h1, h2 { border-bottom: 1px solid var(--line); padding-bottom: .3em; }
          h1 { font-size: 1.4em; } h2 { font-size: 1.2em; } h3 { font-size: 1.1em; }
          a { color: var(--link); }
          pre, :not(pre) > code { background: var(--code); border-radius: 6px; }
          pre { padding: 12px; overflow-x: auto; } :not(pre) > code { padding: .15em .3em; }
          pre, code, tt { font-family: monospace; font-size: .875em; }
          blockquote { margin-left: 0; padding-left: 12px; border-left: 3px solid var(--line); }
          table { display: block; overflow-x: auto; border-collapse: collapse; }
          td, th { border: 1px solid var(--line); padding: 6px 10px; }
        </style></head><body>\(articles)</body></html>
        """
}

private func htmlEscape(_ value: String) -> String {
    value.replacingOccurrences(of: "&", with: "&amp;")
        .replacingOccurrences(of: "<", with: "&lt;")
        .replacingOccurrences(of: ">", with: "&gt;")
        .replacingOccurrences(of: "\"", with: "&quot;")
}

import SwiftUI

@main
struct SavingGoalPlusApp: App {
    @StateObject private var storage = SavingsStorageService.shared
    @StateObject private var notifications = NotificationManager.shared

    init() {
        // Request notification authorization on first run
        NotificationManager.shared.checkAuthorization()
    }

    var body: some Scene {
        WindowGroup {
            TabView {
                MainDashboardView()
                    .tabItem {
                        Label("Goals", systemImage: "target")
                    }

                ArchiveView()
                    .tabItem {
                        Label("Archive", systemImage: "archivebox")
                    }

                SettingsView()
                    .tabItem {
                        Label("Settings", systemImage: "gearshape")
                    }
            }
            .accentColor(ColorTheme.primaryEmerald)
        }
    }
}


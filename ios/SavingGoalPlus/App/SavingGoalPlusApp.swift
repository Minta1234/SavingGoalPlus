import SwiftUI

enum AppLanguage: String, CaseIterable, Identifiable {
    case system
    case english = "en"
    case thai = "th"
    case japanese = "ja"
    case russian = "ru"
    case chinese = "zh"
    case spanish = "es"
    case french = "fr"

    static let preferenceKey = "app_language"
    var id: String { rawValue }

    var locale: Locale {
        switch self {
        case .system: return .autoupdatingCurrent
        case .english, .thai, .japanese, .russian, .chinese, .spanish, .french: return Locale(identifier: rawValue)
        }
    }

    var label: LocalizedStringKey {
        switch self {
        case .system: return "System default"
        case .english: return "English"
        case .thai: return "ไทย"
        case .japanese: return "日本語"
        case .russian: return "Русский"
        case .chinese: return "中文"
        case .spanish: return "Español"
        case .french: return "Français"
        }
    }
}

@main
struct SavingGoalPlusApp: App {
    @AppStorage(AppLanguage.preferenceKey) private var language = AppLanguage.system
    @AppStorage(AppThemeMode.preferenceKey) private var appearance = AppThemeMode.system
    @AppStorage(ColorTheme.accentPresetKey) private var accentPreset = "emerald"
    @AppStorage(ColorTheme.customAccentKey) private var customAccent = ""
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
            .tint(ColorTheme.accent(preset: accentPreset, custom: customAccent))
            .environment(\.locale, language.locale)
            .preferredColorScheme(appearance.colorScheme)
        }
    }
}


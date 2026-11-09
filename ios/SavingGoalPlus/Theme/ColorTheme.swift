import SwiftUI

#if canImport(UIKit)
import UIKit
#endif

public enum AppAccentTheme: String, CaseIterable, Identifiable {
    case emerald = "emerald"
    case cupertinoBlue = "cupertino_blue"
    case indigo = "indigo"
    case rosePink = "rose_pink"
    case sunsetOrange = "sunset_orange"
    case pacificTeal = "pacific_teal"
    case royalPurple = "royal_purple"
    case midnightSlate = "midnight"

    public var id: String { rawValue }

    public var displayName: String {
        switch self {
        case .emerald: return "Apple Mint"
        case .cupertinoBlue: return "Cupertino Blue"
        case .indigo: return "Indigo Night"
        case .rosePink: return "Rose Pink"
        case .sunsetOrange: return "Sunset Orange"
        case .pacificTeal: return "Pacific Teal"
        case .royalPurple: return "Royal Purple"
        case .midnightSlate: return "Midnight Slate"
        }
    }

    public var color: Color {
        switch self {
        case .emerald: return Color(red: 16/255, green: 185/255, blue: 129/255)
        case .cupertinoBlue: return Color(red: 0/255, green: 122/255, blue: 255/255)
        case .indigo: return Color(red: 88/255, green: 86/255, blue: 214/255)
        case .rosePink: return Color(red: 255/255, green: 45/255, blue: 85/255)
        case .sunsetOrange: return Color(red: 255/255, green: 149/255, blue: 0/255)
        case .pacificTeal: return Color(red: 48/255, green: 176/255, blue: 199/255)
        case .royalPurple: return Color(red: 175/255, green: 82/255, blue: 222/255)
        case .midnightSlate: return Color(red: 100/255, green: 116/255, blue: 139/255)
        }
    }
}

public enum ColorTheme {
    public static var primaryEmerald: Color {
        let saved = UserDefaults.standard.string(forKey: "app_accent_theme") ?? "emerald"
        return (AppAccentTheme(rawValue: saved) ?? .emerald).color
    }

    public static let stageCyan = Color(red: 6/255, green: 182/255, blue: 212/255)
    public static let stageAmber = Color(red: 245/255, green: 158/255, blue: 11/255)
    public static let withdrawalRed = Color(red: 239/255, green: 68/255, blue: 68/255)

    #if canImport(UIKit)
    public static let trackBackground = Color(UIColor.systemGray5)
    public static let cardBackground = Color(UIColor.secondarySystemGroupedBackground)
    public static let screenBackground = Color(UIColor.systemGroupedBackground)
    #else
    public static let trackBackground = Color.gray.opacity(0.2)
    public static let cardBackground = Color.gray.opacity(0.1)
    public static let screenBackground = Color.clear
    #endif

    public static func stageColor(for percentage: Int) -> Color {
        if percentage <= 30 {
            return stageAmber
        } else if percentage <= 60 {
            return stageCyan
        } else {
            return primaryEmerald
        }
    }
}

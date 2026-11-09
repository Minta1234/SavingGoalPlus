import SwiftUI

public enum ColorTheme {
    public static let primaryEmerald = Color(red: 16/255, green: 185/255, blue: 129/255)
    public static let stageCyan = Color(red: 6/255, green: 182/255, blue: 212/255)
    public static let stageAmber = Color(red: 245/255, green: 158/255, blue: 11/255)
    public static let withdrawalRed = Color(red: 239/255, green: 68/255, blue: 68/255)
    public static let trackBackground = Color(UIColor.systemGray5)
    public static let cardBackground = Color(UIColor.secondarySystemGroupedBackground)
    public static let screenBackground = Color(UIColor.systemGroupedBackground)

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

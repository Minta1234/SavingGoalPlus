package com.minta1234.savingsgoal.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * iOS-inspired vibrant accent color palettes for SavingGoalPlus.
 */
enum class AppThemePalette(
    val id: String,
    val displayName: String,
    val primary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val previewHex: String
) {
    EMERALD_MINT(
        id = "emerald",
        displayName = "Apple Mint",
        primary = Color(0xFF10B981),
        primaryContainer = Color(0xFF064E3B),
        onPrimaryContainer = Color(0xFFA7F3D0),
        gradientStart = Color(0xFF10B981),
        gradientEnd = Color(0xFF06B6D4),
        previewHex = "#10B981"
    ),
    CUPERTINO_BLUE(
        id = "cupertino_blue",
        displayName = "Cupertino Blue",
        primary = Color(0xFF007AFF),
        primaryContainer = Color(0xFF003E8A),
        onPrimaryContainer = Color(0xFFBCE0FD),
        gradientStart = Color(0xFF007AFF),
        gradientEnd = Color(0xFF5856D6),
        previewHex = "#007AFF"
    ),
    INDIGO_NIGHT(
        id = "indigo",
        displayName = "Indigo Night",
        primary = Color(0xFF5856D6),
        primaryContainer = Color(0xFF2C2B6B),
        onPrimaryContainer = Color(0xFFD8D7F9),
        gradientStart = Color(0xFF5856D6),
        gradientEnd = Color(0xFFAF52DE),
        previewHex = "#5856D6"
    ),
    ROSE_PINK(
        id = "rose_pink",
        displayName = "Rose Pink",
        primary = Color(0xFFFF2D55),
        primaryContainer = Color(0xFF800E26),
        onPrimaryContainer = Color(0xFFFFD1DC),
        gradientStart = Color(0xFFFF2D55),
        gradientEnd = Color(0xFFFF9500),
        previewHex = "#FF2D55"
    ),
    SUNSET_ORANGE(
        id = "sunset_orange",
        displayName = "Sunset Orange",
        primary = Color(0xFFFF9500),
        primaryContainer = Color(0xFF7A4500),
        onPrimaryContainer = Color(0xFFFFE5B4),
        gradientStart = Color(0xFFFF9500),
        gradientEnd = Color(0xFFFF2D55),
        previewHex = "#FF9500"
    ),
    PACIFIC_TEAL(
        id = "pacific_teal",
        displayName = "Pacific Teal",
        primary = Color(0xFF30B0C7),
        primaryContainer = Color(0xFF125562),
        onPrimaryContainer = Color(0xFFC7F3FB),
        gradientStart = Color(0xFF30B0C7),
        gradientEnd = Color(0xFF007AFF),
        previewHex = "#30B0C7"
    ),
    ROYAL_PURPLE(
        id = "royal_purple",
        displayName = "Royal Purple",
        primary = Color(0xFFAF52DE),
        primaryContainer = Color(0xFF562570),
        onPrimaryContainer = Color(0xFFF1D5FD),
        gradientStart = Color(0xFFAF52DE),
        gradientEnd = Color(0xFF5856D6),
        previewHex = "#AF52DE"
    ),
    MIDNIGHT_CHARCOAL(
        id = "midnight",
        displayName = "Midnight Slate",
        primary = Color(0xFF64748B),
        primaryContainer = Color(0xFF334155),
        onPrimaryContainer = Color(0xFFE2E8F0),
        gradientStart = Color(0xFF64748B),
        gradientEnd = Color(0xFF1E293B),
        previewHex = "#64748B"
    );

    companion object {
        fun fromId(id: String?): AppThemePalette {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: EMERALD_MINT
        }
    }
}


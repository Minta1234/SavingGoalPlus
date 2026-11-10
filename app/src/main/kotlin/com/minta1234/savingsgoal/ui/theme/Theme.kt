package com.minta1234.savingsgoal.ui.theme

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.minta1234.savingsgoal.R

internal enum class AppThemeMode(val id: String, @param:StringRes val label: Int) {
    SYSTEM("system", R.string.ui_system_default),
    LIGHT("light", R.string.theme_light),
    DARK("dark", R.string.theme_dark)
}

val LocalAppThemePalette = staticCompositionLocalOf { AppThemePalette.EMERALD_MINT }

@Composable
fun SavingsGoalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: AppThemePalette = AppThemePalette.EMERALD_MINT,
    dynamicColor: Boolean = false,
    customAccent: Color? = null,
    content: @Composable () -> Unit
) {
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = palette.primary,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.onPrimaryContainer,
            secondary = palette.primary,
            tertiary = tertiaryDark
        )
        else -> lightColorScheme(
            primary = palette.primary,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.onPrimaryContainer,
            secondary = palette.primary,
            tertiary = tertiaryLight
        )
    }

    val colorScheme = if (customAccent == null) baseScheme else {
        val foreground = if (customAccent.luminance() >= 0.179f) Color.Black else Color.White
        val container = customAccent.copy(alpha = 0.20f).compositeOver(baseScheme.surface)
        val readableAccent = if (darkTheme && customAccent.luminance() < 0.35f) {
            Color.White.copy(alpha = 0.60f).compositeOver(customAccent)
        } else if (!darkTheme && customAccent.luminance() > 0.18f) {
            Color.Black.copy(alpha = 0.55f).compositeOver(customAccent)
        } else customAccent
        baseScheme.copy(
            primary = readableAccent,
            onPrimary = if (readableAccent.luminance() >= 0.179f) Color.Black else Color.White,
            primaryContainer = container,
            onPrimaryContainer = baseScheme.onSurface,
            secondary = customAccent,
            onSecondary = foreground
        )
    }

    CompositionLocalProvider(LocalAppThemePalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

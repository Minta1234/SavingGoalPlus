package com.minta1234.savingsgoal

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.minta1234.savingsgoal.data.SavingsStreakManager
import com.minta1234.savingsgoal.ui.theme.AppLanguage
import com.minta1234.savingsgoal.ui.theme.AppThemePalette
import com.minta1234.savingsgoal.ui.theme.AppThemeMode
import com.minta1234.savingsgoal.ui.theme.SavingsGoalTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeManager {
    private val _currentPalette = MutableStateFlow(AppThemePalette.EMERALD_MINT)
    val currentPalette: StateFlow<AppThemePalette> = _currentPalette.asStateFlow()
    private val _customAccent = MutableStateFlow<Int?>(null)
    val customAccent: StateFlow<Int?> = _customAccent.asStateFlow()
    private val _appearance = MutableStateFlow(AppThemeMode.SYSTEM)
    internal val appearance = _appearance.asStateFlow()

    fun init(context: Context) {
        try {
            val prefs = context.getSharedPreferences("com.rmystudio.savingsgoal.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE)
            val mode = try { prefs.getString("THEME_MODE", AppThemeMode.SYSTEM.id) } catch (_: Exception) { AppThemeMode.SYSTEM.id }
            _appearance.value = AppThemeMode.entries.firstOrNull { it.id == mode } ?: AppThemeMode.SYSTEM
            val savedId = try { prefs.getString("THEME_PALETTE_ID", "emerald") } catch (_: Exception) { "emerald" }
            _currentPalette.value = AppThemePalette.fromId(savedId)
            _customAccent.value = try {
                if (prefs.contains("THEME_CUSTOM_ACCENT")) prefs.getInt("THEME_CUSTOM_ACCENT", 0) else null
            } catch (_: Exception) {
                null
            }
        } catch (_: Exception) {
            _appearance.value = AppThemeMode.SYSTEM
            _currentPalette.value = AppThemePalette.EMERALD_MINT
            _customAccent.value = null
        }
    }

    internal fun setAppearance(context: Context, mode: AppThemeMode) {
        try {
            context.getSharedPreferences("com.rmystudio.savingsgoal.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE)
                .edit().putString("THEME_MODE", mode.id).apply()
        } catch (_: Exception) {
        }
        _appearance.value = mode
    }

    fun setPalette(context: Context, palette: AppThemePalette) {
        _currentPalette.value = palette
        try {
            val prefs = context.getSharedPreferences("com.rmystudio.savingsgoal.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE)
            prefs.edit().putString("THEME_PALETTE_ID", palette.id).remove("THEME_CUSTOM_ACCENT").apply()
        } catch (_: Exception) {
        }
        _customAccent.value = null
    }

    fun setCustomAccent(context: Context, value: Int?) {
        try {
            val prefs = context.getSharedPreferences("com.rmystudio.savingsgoal.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            if (value == null) editor.remove("THEME_CUSTOM_ACCENT") else editor.putInt("THEME_CUSTOM_ACCENT", value)
            editor.apply()
        } catch (_: Exception) {
        }
        _customAccent.value = value
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        AppLanguage.load(this)

        try {
            enableEdgeToEdge()
        } catch (_: Exception) {
        }

        setContent {
            val appearance by ThemeManager.appearance.collectAsState()
            val darkTheme = when (appearance) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            LaunchedEffect(darkTheme) {
                try {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        ) { darkTheme },
                        navigationBarStyle = SystemBarStyle.auto(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        ) { darkTheme }
                    )
                } catch (_: Exception) {
                }
            }
            val palette by ThemeManager.currentPalette.collectAsState()
            val customAccent by ThemeManager.customAccent.collectAsState()
            val language by AppLanguage.selected.collectAsState()
            val configuration = LocalConfiguration.current
            val localizedContext = remember(language, configuration) {
                language.localizedContext(this, configuration)
            }
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration
            ) {
                SavingsGoalTheme(darkTheme = darkTheme, palette = palette, customAccent = customAccent?.let { Color(it) }) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainNavigation()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            SavingsStreakManager.init(this)
        } catch (_: Exception) {
        }
    }
}

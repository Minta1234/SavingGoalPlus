package com.example.savingsgoal

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import com.example.savingsgoal.data.SavingsStreakManager
import com.example.savingsgoal.ui.theme.AppLanguage
import com.example.savingsgoal.ui.theme.AppThemePalette
import com.example.savingsgoal.ui.theme.SavingsGoalTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeManager {
    private val _currentPalette = MutableStateFlow(AppThemePalette.EMERALD_MINT)
    val currentPalette: StateFlow<AppThemePalette> = _currentPalette.asStateFlow()
    private val _customAccent = MutableStateFlow<Int?>(null)
    val customAccent: StateFlow<Int?> = _customAccent.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("com.rmystudio.savingsgoal.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE)
        val savedId = prefs.getString("THEME_PALETTE_ID", "emerald")
        _currentPalette.value = AppThemePalette.fromId(savedId)
        _customAccent.value = if (prefs.contains("THEME_CUSTOM_ACCENT")) prefs.getInt("THEME_CUSTOM_ACCENT", 0) else null
    }

    fun setPalette(context: Context, palette: AppThemePalette) {
        _currentPalette.value = palette
        val prefs = context.getSharedPreferences("com.rmystudio.savingsgoal.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE)
        prefs.edit().putString("THEME_PALETTE_ID", palette.id).remove("THEME_CUSTOM_ACCENT").apply()
        _customAccent.value = null
    }

    fun setCustomAccent(context: Context, value: Int?) {
        val prefs = context.getSharedPreferences("com.rmystudio.savingsgoal.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        if (value == null) editor.remove("THEME_CUSTOM_ACCENT") else editor.putInt("THEME_CUSTOM_ACCENT", value)
        editor.apply()
        _customAccent.value = value
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        AppLanguage.load(this)

        enableEdgeToEdge()
        setContent {
            val palette by ThemeManager.currentPalette.collectAsState()
            val customAccent by ThemeManager.customAccent.collectAsState()
            val language by AppLanguage.selected.collectAsState()
            val configuration = LocalConfiguration.current
            val resources = remember(language, configuration) {
                language.localizedContext(this, configuration).resources
            }
            CompositionLocalProvider(LocalResources provides resources) {
                SavingsGoalTheme(palette = palette, customAccent = customAccent?.let { Color(it) }) {
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
        SavingsStreakManager.init(this)
    }
}

package com.example.savingsgoal

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.savingsgoal.data.SavingsStreakManager
import com.example.savingsgoal.ui.theme.AppThemePalette
import com.example.savingsgoal.ui.theme.SavingsGoalTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeManager {
    private val _currentPalette = MutableStateFlow(AppThemePalette.EMERALD_MINT)
    val currentPalette: StateFlow<AppThemePalette> = _currentPalette.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("com.rmystudio.savingsgoal.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE)
        val savedId = prefs.getString("THEME_PALETTE_ID", "emerald")
        _currentPalette.value = AppThemePalette.fromId(savedId)
    }

    fun setPalette(context: Context, palette: AppThemePalette) {
        _currentPalette.value = palette
        val prefs = context.getSharedPreferences("com.rmystudio.savingsgoal.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE)
        prefs.edit().putString("THEME_PALETTE_ID", palette.id).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)

        enableEdgeToEdge()
        setContent {
            val palette by ThemeManager.currentPalette.collectAsState()
            SavingsGoalTheme(palette = palette) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SavingsStreakManager.init(this)
    }
}

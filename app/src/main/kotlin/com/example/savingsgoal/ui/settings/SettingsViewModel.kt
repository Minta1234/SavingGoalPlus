package com.example.savingsgoal.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.savingsgoal.data.SavingsRepository
import com.example.savingsgoal.notification.SavingsNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.example.savingsgoal.ThemeManager
import com.example.savingsgoal.ui.theme.AppThemePalette

data class SettingsUiState(
    val defaultCurrency: String = "$",
    val toSaveOption: Int = 1,
    val showTotalGoalsCard: Boolean = true,
    val isReminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val selectedPalette: AppThemePalette = AppThemePalette.EMERALD_MINT,
    val exportMessage: String? = null
) {
    val formattedReminderTime: String
        get() {
            val h = if (reminderHour == 0) 12 else if (reminderHour > 12) reminderHour - 12 else reminderHour
            val amPm = if (reminderHour >= 12) "PM" else "AM"
            return String.format("%02d:%02d %s", h, reminderMinute, amPm)
        }
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SavingsRepository(application)
    private val prefs = application.getSharedPreferences(
        "com.rmystudio.savingsgoal.PREFERENCE_FILE_KEY",
        Context.MODE_PRIVATE
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        val currency = repository.getDefaultCurrency()
        val toSaveOption = repository.getToSaveOption()
        val showTotal = repository.getShowTotalGoalsCard()
        val isReminder = prefs.getBoolean("REMINDER_ENABLED", false)
        val hour = prefs.getInt("REMINDER_HOUR", 20)
        val minute = prefs.getInt("REMINDER_MINUTE", 0)
        val paletteId = prefs.getString("THEME_PALETTE_ID", "emerald")
        val palette = AppThemePalette.fromId(paletteId)

        _uiState.value = SettingsUiState(
            defaultCurrency = currency,
            toSaveOption = toSaveOption,
            showTotalGoalsCard = showTotal,
            isReminderEnabled = isReminder,
            reminderHour = hour,
            reminderMinute = minute,
            selectedPalette = palette
        )
    }

    fun updateThemePalette(palette: AppThemePalette) {
        ThemeManager.setPalette(getApplication(), palette)
        loadSettings()
    }

    fun updateDefaultCurrency(currency: String) {
        repository.setDefaultCurrency(currency)
        loadSettings()
    }

    fun updateToSaveOption(option: Int) {
        repository.setToSaveOption(option)
        loadSettings()
    }

    fun updateShowTotalGoals(show: Boolean) {
        repository.setShowTotalGoalsCard(show)
        loadSettings()
    }

    fun updateReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("REMINDER_ENABLED", enabled).apply()
        val app = getApplication<Application>()
        if (enabled) {
            SavingsNotificationManager.scheduleDailyReminder(
                app,
                _uiState.value.reminderHour,
                _uiState.value.reminderMinute
            )
        } else {
            SavingsNotificationManager.cancelDailyReminder(app)
        }
        loadSettings()
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt("REMINDER_HOUR", hour)
            .putInt("REMINDER_MINUTE", minute)
            .apply()

        if (_uiState.value.isReminderEnabled) {
            SavingsNotificationManager.scheduleDailyReminder(getApplication(), hour, minute)
        }
        loadSettings()
    }

    fun sendTestReminder() {
        SavingsNotificationManager.showReminderNotification(getApplication())
    }
}

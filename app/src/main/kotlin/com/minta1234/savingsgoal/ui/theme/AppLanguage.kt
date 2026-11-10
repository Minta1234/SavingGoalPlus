package com.minta1234.savingsgoal.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.StringRes
import com.minta1234.savingsgoal.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class AppLanguage(val tag: String, @param:StringRes val label: Int) {
    SYSTEM("", R.string.ui_system_default),
    ENGLISH("en", R.string.ui_english),
    THAI("th", R.string.ui_thai);

    companion object {
        private const val PREFERENCES = "app_language"
        private const val KEY = "language_tag"
        private val current = MutableStateFlow(SYSTEM)
        val selected = current.asStateFlow()

        fun load(context: Context) {
            current.value = read(context)
        }

        fun set(context: Context, language: AppLanguage) {
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit().putString(KEY, language.tag).apply()
            current.value = language
        }

        private fun read(context: Context): AppLanguage {
            val saved = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(KEY, "")
            return entries.firstOrNull { it.tag == saved } ?: SYSTEM
        }
    }

    fun localizedContext(context: Context, configuration: Configuration): Context {
        if (this == SYSTEM) return context
        val localized = Configuration(configuration)
        localized.setLocales(LocaleList.forLanguageTags(tag))
        return context.createConfigurationContext(localized)
    }
}

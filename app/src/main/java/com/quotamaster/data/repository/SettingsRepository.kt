package com.quotamaster.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Theme mode for the app.
 */
enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark")
}

/**
 * App-wide settings persisted via DataStore.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingComplete: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE          = stringPreferencesKey("theme_mode")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val REMINDER_ENABLED    = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR       = intPreferencesKey("reminder_hour")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .map { prefs ->
            AppSettings(
                themeMode = ThemeMode.entries
                    .firstOrNull { it.key == prefs[Keys.THEME_MODE] }
                    ?: ThemeMode.SYSTEM,
                onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false,
                reminderEnabled    = prefs[Keys.REMINDER_ENABLED] ?: false,
                reminderHour       = prefs[Keys.REMINDER_HOUR] ?: 20
            )
        }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.key
        }
    }

    suspend fun setOnboardingComplete() {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETE] = true
        }
    }

    suspend fun updateReminder(enabled: Boolean, hour: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.REMINDER_ENABLED] = enabled
            prefs[Keys.REMINDER_HOUR]    = hour
        }
    }
}
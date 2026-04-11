package com.example.mcpodcasts.data.settings

import android.content.Context
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    System,
    Light,
    Dark,
}

enum class AppLanguage(val languageTag: String) {
    System(""),
    Portuguese("pt"),
    English("en"),
    French("fr"),
    Spanish("es"),
    Italian("it"),
    German("de");

    fun toLocaleListCompat(): LocaleListCompat {
        return if (languageTag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val appLanguage: AppLanguage = AppLanguage.System,
    val refreshIntervalHours: Int = 1,
    val syncSummaryNotificationsEnabled: Boolean = true,
    /** When true, enables Android loudness enhancement on the playback audio session. */
    val volumeNormalizationEnabled: Boolean = false,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        val themeMode = preferences[THEME_MODE_KEY]
            ?.let { storedValue -> ThemeMode.entries.find { entry -> entry.name == storedValue } }
            ?: ThemeMode.System
        val appLanguage = preferences[APP_LANGUAGE_KEY]
            ?.let { storedValue -> AppLanguage.entries.find { entry -> entry.name == storedValue } }
            ?: AppLanguage.System

        AppSettings(
            themeMode = themeMode,
            appLanguage = appLanguage,
            refreshIntervalHours = preferences[REFRESH_INTERVAL_HOURS_KEY] ?: 1,
            syncSummaryNotificationsEnabled = preferences[SYNC_SUMMARY_NOTIFICATIONS_KEY] ?: true,
            volumeNormalizationEnabled = preferences[VOLUME_NORMALIZATION_KEY] ?: false,
        )
    }

    suspend fun getCurrentSettings(): AppSettings {
        return settings.first()
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = themeMode.name
        }
    }

    suspend fun setAppLanguage(appLanguage: AppLanguage) {
        context.settingsDataStore.edit { preferences ->
            preferences[APP_LANGUAGE_KEY] = appLanguage.name
        }
    }

    suspend fun setRefreshIntervalHours(hours: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[REFRESH_INTERVAL_HOURS_KEY] = hours.coerceAtLeast(1)
        }
    }

    suspend fun setSyncSummaryNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[SYNC_SUMMARY_NOTIFICATIONS_KEY] = enabled
        }
    }

    suspend fun setVolumeNormalizationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[VOLUME_NORMALIZATION_KEY] = enabled
        }
    }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE_KEY = stringPreferencesKey("app_language")
        val REFRESH_INTERVAL_HOURS_KEY = intPreferencesKey("refresh_interval_hours")
        val SYNC_SUMMARY_NOTIFICATIONS_KEY = booleanPreferencesKey("sync_summary_notifications")
        val VOLUME_NORMALIZATION_KEY = booleanPreferencesKey("volume_normalization")
    }
}

package com.example.mcpodcasts.data.settings

import android.content.Context
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

    suspend fun getLastPlayedEpisodeId(): String? {
        return context.settingsDataStore.data.first()[LAST_PLAYED_EPISODE_ID_KEY]
    }

    suspend fun setLastPlayedEpisodeId(episodeId: String?) {
        context.settingsDataStore.edit { preferences ->
            if (episodeId.isNullOrBlank()) {
                preferences.remove(LAST_PLAYED_EPISODE_ID_KEY)
            } else {
                preferences[LAST_PLAYED_EPISODE_ID_KEY] = episodeId
            }
        }
    }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE_KEY = stringPreferencesKey("app_language")
        val SYNC_SUMMARY_NOTIFICATIONS_KEY = booleanPreferencesKey("sync_summary_notifications")
        val VOLUME_NORMALIZATION_KEY = booleanPreferencesKey("volume_normalization")
        val LAST_PLAYED_EPISODE_ID_KEY = stringPreferencesKey("last_played_episode_id")
    }
}

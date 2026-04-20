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
    val queueSortOrder: QueueSortOrder = QueueSortOrder.NewestFirst,
    val queueReadFilter: QueueReadFilter = QueueReadFilter.Unread,
    val queuePodcastFilterFeedUrl: String? = null,
    val calendarReadFilter: QueueReadFilter = QueueReadFilter.All,
    val calendarPodcastFilterFeedUrl: String? = null,
    val subscriptionFiltersEncoded: String = "",
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
        val queueSortOrder = preferences[QUEUE_SORT_ORDER_KEY]
            ?.let { storedValue -> QueueSortOrder.entries.find { entry -> entry.name == storedValue } }
            ?: QueueSortOrder.NewestFirst
        val queueReadFilter = preferences[QUEUE_READ_FILTER_KEY]
            ?.let { storedValue -> QueueReadFilter.entries.find { entry -> entry.name == storedValue } }
            ?: QueueReadFilter.Unread
        val calendarReadFilter = preferences[CALENDAR_READ_FILTER_KEY]
            ?.let { storedValue -> QueueReadFilter.entries.find { entry -> entry.name == storedValue } }
            ?: QueueReadFilter.All

        AppSettings(
            themeMode = themeMode,
            appLanguage = appLanguage,
            syncSummaryNotificationsEnabled = preferences[SYNC_SUMMARY_NOTIFICATIONS_KEY] ?: true,
            volumeNormalizationEnabled = preferences[VOLUME_NORMALIZATION_KEY] ?: false,
            queueSortOrder = queueSortOrder,
            queueReadFilter = queueReadFilter,
            queuePodcastFilterFeedUrl = preferences[QUEUE_PODCAST_FILTER_KEY],
            calendarReadFilter = calendarReadFilter,
            calendarPodcastFilterFeedUrl = preferences[CALENDAR_PODCAST_FILTER_KEY],
            subscriptionFiltersEncoded = preferences[SUBSCRIPTION_FILTERS_KEY].orEmpty(),
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

    suspend fun setQueueSortOrder(order: QueueSortOrder) {
        context.settingsDataStore.edit { preferences ->
            preferences[QUEUE_SORT_ORDER_KEY] = order.name
        }
    }

    suspend fun setQueueReadFilter(filter: QueueReadFilter) {
        context.settingsDataStore.edit { preferences ->
            preferences[QUEUE_READ_FILTER_KEY] = filter.name
        }
    }

    suspend fun setQueuePodcastFilterFeedUrl(feedUrl: String?) {
        context.settingsDataStore.edit { preferences ->
            if (feedUrl.isNullOrBlank()) {
                preferences.remove(QUEUE_PODCAST_FILTER_KEY)
            } else {
                preferences[QUEUE_PODCAST_FILTER_KEY] = feedUrl
            }
        }
    }

    suspend fun setCalendarReadFilter(filter: QueueReadFilter) {
        context.settingsDataStore.edit { preferences ->
            preferences[CALENDAR_READ_FILTER_KEY] = filter.name
        }
    }

    suspend fun setCalendarPodcastFilterFeedUrl(feedUrl: String?) {
        context.settingsDataStore.edit { preferences ->
            if (feedUrl.isNullOrBlank()) {
                preferences.remove(CALENDAR_PODCAST_FILTER_KEY)
            } else {
                preferences[CALENDAR_PODCAST_FILTER_KEY] = feedUrl
            }
        }
    }

    suspend fun setSubscriptionFiltersEncoded(value: String) {
        context.settingsDataStore.edit { preferences ->
            if (value.isBlank()) {
                preferences.remove(SUBSCRIPTION_FILTERS_KEY)
            } else {
                preferences[SUBSCRIPTION_FILTERS_KEY] = value
            }
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
        val QUEUE_SORT_ORDER_KEY = stringPreferencesKey("queue_sort_order")
        val QUEUE_READ_FILTER_KEY = stringPreferencesKey("queue_read_filter")
        val QUEUE_PODCAST_FILTER_KEY = stringPreferencesKey("queue_podcast_filter_feed_url")
        val CALENDAR_READ_FILTER_KEY = stringPreferencesKey("calendar_read_filter")
        val CALENDAR_PODCAST_FILTER_KEY = stringPreferencesKey("calendar_podcast_filter_feed_url")
        val SUBSCRIPTION_FILTERS_KEY = stringPreferencesKey("subscription_filters_encoded")
        val LAST_PLAYED_EPISODE_ID_KEY = stringPreferencesKey("last_played_episode_id")
    }
}

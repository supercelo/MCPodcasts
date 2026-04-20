package com.example.mcpodcasts.data.backup

import androidx.room.withTransaction
import com.example.mcpodcasts.data.local.EpisodeDao
import com.example.mcpodcasts.data.local.EpisodeEntity
import com.example.mcpodcasts.data.local.PodcastDao
import com.example.mcpodcasts.data.local.PodcastDatabase
import com.example.mcpodcasts.data.local.PodcastEntity
import com.example.mcpodcasts.data.settings.AppLanguage
import com.example.mcpodcasts.data.settings.QueueReadFilter
import com.example.mcpodcasts.data.settings.QueueSortOrder
import com.example.mcpodcasts.data.settings.SettingsRepository
import com.example.mcpodcasts.data.settings.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupManager(
    private val database: PodcastDatabase,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getCurrentSettings()
        val root = JSONObject()
        root.put(KEY_VERSION, BACKUP_VERSION)
        root.put(KEY_EXPORTED_AT, System.currentTimeMillis())
        root.put(
            KEY_SETTINGS,
            JSONObject()
                .put(KEY_THEME_MODE, settings.themeMode.name)
                .put(KEY_APP_LANGUAGE, settings.appLanguage.name)
                .put(KEY_SYNC_SUMMARY_NOTIFICATIONS, settings.syncSummaryNotificationsEnabled)
                .put(KEY_VOLUME_NORMALIZATION, settings.volumeNormalizationEnabled)
                .put(KEY_QUEUE_SORT_ORDER, settings.queueSortOrder.name)
                .put(KEY_QUEUE_READ_FILTER, settings.queueReadFilter.name)
                .put(KEY_QUEUE_PODCAST_FILTER_FEED_URL, settings.queuePodcastFilterFeedUrl)
                .put(KEY_CALENDAR_READ_FILTER, settings.calendarReadFilter.name)
                .put(KEY_CALENDAR_PODCAST_FILTER_FEED_URL, settings.calendarPodcastFilterFeedUrl)
                .put(KEY_SUBSCRIPTION_FILTERS_ENCODED, settings.subscriptionFiltersEncoded)
                .put(KEY_LAST_PLAYED_EPISODE_ID, settingsRepository.getLastPlayedEpisodeId()),
        )

        val podcastsJson = JSONArray()
        val podcasts = podcastDao.getAllPodcasts()
        podcasts.forEach { podcast ->
            val episodes = episodeDao.getEpisodesForPodcast(podcast.feedUrl)
            podcastsJson.put(
                JSONObject()
                    .put(KEY_FEED_URL, podcast.feedUrl)
                    .put(KEY_TITLE, podcast.title)
                    .put(KEY_AUTHOR, podcast.author)
                    .put(KEY_DESCRIPTION, podcast.description)
                    .put(KEY_IMAGE_URL, podcast.imageUrl)
                    .put(KEY_SITE_URL, podcast.siteUrl)
                    .put(KEY_SUBSCRIBED_AT, podcast.subscribedAt)
                    .put(KEY_LAST_SYNCED_AT, podcast.lastSyncedAt)
                    .put(KEY_NOTIFY_NEW_EPISODES, podcast.notifyNewEpisodes)
                    .put(KEY_INCLUDE_IN_QUEUE, podcast.includeInQueue)
                    .put(KEY_INTRO_SKIP_SECONDS, podcast.introSkipSeconds)
                    .put(KEY_OUTRO_SKIP_SECONDS, podcast.outroSkipSeconds)
                    .put(KEY_EPISODES, episodesToJsonArray(episodes)),
            )
        }
        root.put(KEY_PODCASTS, podcastsJson)
        root.toString(2)
    }

    suspend fun importFromJson(json: String) = withContext(Dispatchers.IO) {
        val root = JSONObject(json)
        val settingsJson = root.optJSONObject(KEY_SETTINGS)
        val podcastsJson = root.optJSONArray(KEY_PODCASTS) ?: JSONArray()

        database.withTransaction {
            restoreSettings(settingsJson)
            importPodcasts(podcastsJson)
        }
    }

    private suspend fun importPodcasts(podcastsJson: JSONArray) {
        for (index in 0 until podcastsJson.length()) {
            val podcastJson = podcastsJson.optJSONObject(index) ?: continue
            val feedUrl = podcastJson.optString(KEY_FEED_URL).trim()
            if (feedUrl.isBlank()) continue
            if (podcastDao.getPodcast(feedUrl) != null) continue

            val podcast = PodcastEntity(
                feedUrl = feedUrl,
                title = podcastJson.optString(KEY_TITLE).ifBlank { feedUrl },
                author = podcastJson.nullableString(KEY_AUTHOR),
                description = podcastJson.nullableString(KEY_DESCRIPTION),
                imageUrl = podcastJson.nullableString(KEY_IMAGE_URL),
                siteUrl = podcastJson.nullableString(KEY_SITE_URL),
                subscribedAt = podcastJson.optLong(KEY_SUBSCRIBED_AT, System.currentTimeMillis()),
                lastSyncedAt = podcastJson.optLong(KEY_LAST_SYNCED_AT, 0L),
                notifyNewEpisodes = podcastJson.optBoolean(KEY_NOTIFY_NEW_EPISODES, true),
                includeInQueue = podcastJson.optBoolean(KEY_INCLUDE_IN_QUEUE, true),
                introSkipSeconds = podcastJson.optInt(KEY_INTRO_SKIP_SECONDS, 0),
                outroSkipSeconds = podcastJson.optInt(KEY_OUTRO_SKIP_SECONDS, 0),
            )
            podcastDao.upsertPodcast(podcast)

            val episodesJson = podcastJson.optJSONArray(KEY_EPISODES) ?: JSONArray()
            val episodes = mutableListOf<EpisodeEntity>()
            for (episodeIndex in 0 until episodesJson.length()) {
                val episodeJson = episodesJson.optJSONObject(episodeIndex) ?: continue
                val episodeId = episodeJson.optString(KEY_EPISODE_ID).trim()
                val audioUrl = episodeJson.optString(KEY_AUDIO_URL).trim()
                val title = episodeJson.optString(KEY_TITLE).trim()
                if (episodeId.isBlank() || audioUrl.isBlank() || title.isBlank()) continue

                episodes += EpisodeEntity(
                    episodeId = episodeId,
                    podcastId = feedUrl,
                    guid = episodeJson.nullableString(KEY_GUID),
                    title = title,
                    summary = episodeJson.nullableString(KEY_SUMMARY),
                    audioUrl = audioUrl,
                    artworkUrl = episodeJson.nullableString(KEY_ARTWORK_URL),
                    episodeUrl = episodeJson.nullableString(KEY_EPISODE_URL),
                    publishedAt = episodeJson.optLong(KEY_PUBLISHED_AT, 0L),
                    durationLabel = episodeJson.nullableString(KEY_DURATION_LABEL),
                    durationMs = episodeJson.optLong(KEY_DURATION_MS, 0L).coerceAtLeast(0L),
                    playbackPositionMs = episodeJson.optLong(KEY_PLAYBACK_POSITION_MS, 0L).coerceAtLeast(0L),
                    isRead = episodeJson.optBoolean(KEY_IS_READ, false),
                    isCompleted = episodeJson.optBoolean(KEY_IS_COMPLETED, false),
                )
            }

            if (episodes.isNotEmpty()) {
                episodeDao.upsertEpisodes(episodes)
            }
        }
    }

    private suspend fun restoreSettings(settingsJson: JSONObject?) {
        val themeMode = ThemeMode.entries.find { entry ->
            entry.name == settingsJson?.optString(KEY_THEME_MODE)
        } ?: ThemeMode.System
        val appLanguage = AppLanguage.entries.find { entry ->
            entry.name == settingsJson?.optString(KEY_APP_LANGUAGE)
        } ?: AppLanguage.System
        val queueSortOrder = QueueSortOrder.entries.find { entry ->
            entry.name == settingsJson?.optString(KEY_QUEUE_SORT_ORDER)
        } ?: QueueSortOrder.NewestFirst
        val queueReadFilter = QueueReadFilter.entries.find { entry ->
            entry.name == settingsJson?.optString(KEY_QUEUE_READ_FILTER)
        } ?: QueueReadFilter.Unread
        val calendarReadFilter = QueueReadFilter.entries.find { entry ->
            entry.name == settingsJson?.optString(KEY_CALENDAR_READ_FILTER)
        } ?: QueueReadFilter.All

        settingsRepository.setThemeMode(themeMode)
        settingsRepository.setAppLanguage(appLanguage)
        settingsRepository.setSyncSummaryNotificationsEnabled(
            settingsJson?.optBoolean(KEY_SYNC_SUMMARY_NOTIFICATIONS, true) ?: true,
        )
        settingsRepository.setVolumeNormalizationEnabled(
            settingsJson?.optBoolean(KEY_VOLUME_NORMALIZATION, false) ?: false,
        )
        settingsRepository.setQueueSortOrder(queueSortOrder)
        settingsRepository.setQueueReadFilter(queueReadFilter)
        settingsRepository.setQueuePodcastFilterFeedUrl(
            settingsJson?.nullableString(KEY_QUEUE_PODCAST_FILTER_FEED_URL),
        )
        settingsRepository.setCalendarReadFilter(calendarReadFilter)
        settingsRepository.setCalendarPodcastFilterFeedUrl(
            settingsJson?.nullableString(KEY_CALENDAR_PODCAST_FILTER_FEED_URL),
        )
        settingsRepository.setSubscriptionFiltersEncoded(
            settingsJson?.optString(KEY_SUBSCRIPTION_FILTERS_ENCODED).orEmpty(),
        )
        settingsRepository.setLastPlayedEpisodeId(
            settingsJson?.nullableString(KEY_LAST_PLAYED_EPISODE_ID),
        )
    }

    private fun episodesToJsonArray(episodes: List<EpisodeEntity>): JSONArray {
        val array = JSONArray()
        episodes.forEach { episode ->
            array.put(
                JSONObject()
                    .put(KEY_EPISODE_ID, episode.episodeId)
                    .put(KEY_GUID, episode.guid)
                    .put(KEY_TITLE, episode.title)
                    .put(KEY_SUMMARY, episode.summary)
                    .put(KEY_AUDIO_URL, episode.audioUrl)
                    .put(KEY_ARTWORK_URL, episode.artworkUrl)
                    .put(KEY_EPISODE_URL, episode.episodeUrl)
                    .put(KEY_PUBLISHED_AT, episode.publishedAt)
                    .put(KEY_DURATION_LABEL, episode.durationLabel)
                    .put(KEY_DURATION_MS, episode.durationMs)
                    .put(KEY_PLAYBACK_POSITION_MS, episode.playbackPositionMs)
                    .put(KEY_IS_READ, episode.isRead)
                    .put(KEY_IS_COMPLETED, episode.isCompleted),
            )
        }
        return array
    }

    private fun JSONObject.nullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf(String::isNotBlank)
    }

    private companion object {
        const val BACKUP_VERSION = 1

        const val KEY_VERSION = "version"
        const val KEY_EXPORTED_AT = "exportedAt"
        const val KEY_SETTINGS = "settings"
        const val KEY_PODCASTS = "podcasts"

        const val KEY_THEME_MODE = "themeMode"
        const val KEY_APP_LANGUAGE = "appLanguage"
        const val KEY_SYNC_SUMMARY_NOTIFICATIONS = "syncSummaryNotificationsEnabled"
        const val KEY_VOLUME_NORMALIZATION = "volumeNormalizationEnabled"
        const val KEY_QUEUE_SORT_ORDER = "queueSortOrder"
        const val KEY_QUEUE_READ_FILTER = "queueReadFilter"
        const val KEY_QUEUE_PODCAST_FILTER_FEED_URL = "queuePodcastFilterFeedUrl"
        const val KEY_CALENDAR_READ_FILTER = "calendarReadFilter"
        const val KEY_CALENDAR_PODCAST_FILTER_FEED_URL = "calendarPodcastFilterFeedUrl"
        const val KEY_SUBSCRIPTION_FILTERS_ENCODED = "subscriptionFiltersEncoded"
        const val KEY_LAST_PLAYED_EPISODE_ID = "lastPlayedEpisodeId"

        const val KEY_FEED_URL = "feedUrl"
        const val KEY_TITLE = "title"
        const val KEY_AUTHOR = "author"
        const val KEY_DESCRIPTION = "description"
        const val KEY_IMAGE_URL = "imageUrl"
        const val KEY_SITE_URL = "siteUrl"
        const val KEY_SUBSCRIBED_AT = "subscribedAt"
        const val KEY_LAST_SYNCED_AT = "lastSyncedAt"
        const val KEY_NOTIFY_NEW_EPISODES = "notifyNewEpisodes"
        const val KEY_INCLUDE_IN_QUEUE = "includeInQueue"
        const val KEY_INTRO_SKIP_SECONDS = "introSkipSeconds"
        const val KEY_OUTRO_SKIP_SECONDS = "outroSkipSeconds"
        const val KEY_EPISODES = "episodes"

        const val KEY_EPISODE_ID = "episodeId"
        const val KEY_GUID = "guid"
        const val KEY_SUMMARY = "summary"
        const val KEY_AUDIO_URL = "audioUrl"
        const val KEY_ARTWORK_URL = "artworkUrl"
        const val KEY_EPISODE_URL = "episodeUrl"
        const val KEY_PUBLISHED_AT = "publishedAt"
        const val KEY_DURATION_LABEL = "durationLabel"
        const val KEY_DURATION_MS = "durationMs"
        const val KEY_PLAYBACK_POSITION_MS = "playbackPositionMs"
        const val KEY_IS_READ = "isRead"
        const val KEY_IS_COMPLETED = "isCompleted"
    }
}

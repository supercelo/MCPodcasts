package com.example.mcpodcasts

import android.content.Context
import com.example.mcpodcasts.data.backup.BackupManager
import com.example.mcpodcasts.data.discovery.PodcastDiscoveryRepository
import com.example.mcpodcasts.data.local.PodcastDatabase
import com.example.mcpodcasts.data.repository.PodcastRepository
import com.example.mcpodcasts.data.rss.RssFeedParser
import com.example.mcpodcasts.data.settings.SettingsRepository
import com.example.mcpodcasts.notifications.EpisodeNotificationManager
import com.example.mcpodcasts.playback.PlayerConnection
import okhttp3.OkHttpClient

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = PodcastDatabase.getInstance(appContext)
    private val httpClient = OkHttpClient.Builder().build()
    private val rssFeedParser = RssFeedParser()

    val podcastRepository: PodcastRepository by lazy {
        PodcastRepository(
            context = appContext,
            database = database,
            podcastDao = database.podcastDao(),
            episodeDao = database.episodeDao(),
            httpClient = httpClient,
            rssFeedParser = rssFeedParser,
        )
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(
            database = database,
            podcastDao = database.podcastDao(),
            episodeDao = database.episodeDao(),
            settingsRepository = settingsRepository,
        )
    }

    val podcastDiscoveryRepository: PodcastDiscoveryRepository by lazy {
        PodcastDiscoveryRepository(
            context = appContext,
            httpClient = httpClient,
        )
    }

    val episodeNotificationManager: EpisodeNotificationManager by lazy {
        EpisodeNotificationManager(appContext)
    }

    val playerConnection: PlayerConnection by lazy {
        PlayerConnection(
            context = appContext,
            repository = podcastRepository,
            settingsRepository = settingsRepository,
        )
    }
}

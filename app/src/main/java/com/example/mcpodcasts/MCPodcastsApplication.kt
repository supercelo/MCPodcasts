package com.example.mcpodcasts

import android.app.Application
import com.example.mcpodcasts.work.PodcastSyncScheduler
import kotlinx.coroutines.runBlocking

class MCPodcastsApplication : Application() {
    val container: AppContainer by lazy {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        container.episodeNotificationManager.ensureChannels()
        val settings = runBlocking {
            container.settingsRepository.getCurrentSettings()
        }
        PodcastSyncScheduler.ensureScheduled(
            context = this,
            refreshIntervalHours = settings.refreshIntervalHours,
        )
    }
}

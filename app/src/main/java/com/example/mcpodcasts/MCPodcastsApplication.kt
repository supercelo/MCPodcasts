package com.example.mcpodcasts

import android.app.Application
import com.example.mcpodcasts.work.PodcastSyncScheduler

class MCPodcastsApplication : Application() {
    val container: AppContainer by lazy {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        container.episodeNotificationManager.ensureChannels()
        PodcastSyncScheduler.ensureScheduled(context = this)
    }
}

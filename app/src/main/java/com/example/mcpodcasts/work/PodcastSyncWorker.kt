package com.example.mcpodcasts.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.mcpodcasts.MCPodcastsApplication
import com.example.mcpodcasts.domain.normalizeRefreshIntervalHours
import java.util.concurrent.TimeUnit

class PodcastSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val application = applicationContext as? MCPodcastsApplication
            ?: return Result.failure()
        val notificationManager = application.container.episodeNotificationManager

        notificationManager.ensureChannels()
        setForeground(notificationManager.createSyncForegroundInfo())

        return runCatching {
            val newEpisodes = application.container.podcastRepository.refreshAllFeeds()
            val settings = application.container.settingsRepository.getCurrentSettings()
            if (settings.syncSummaryNotificationsEnabled) {
                application.container.episodeNotificationManager.notifySyncSummary(newEpisodes)
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }
}

object PodcastSyncScheduler {
    private const val PERIODIC_SYNC_NAME = "podcast-periodic-sync"
    private const val ONE_TIME_SYNC_NAME = "podcast-manual-sync"

    fun ensureScheduled(
        context: Context,
        refreshIntervalHours: Int,
    ) {
        val request = PeriodicWorkRequestBuilder<PodcastSyncWorker>(
            repeatInterval = normalizeRefreshIntervalHours(refreshIntervalHours),
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            request,
        )
    }

    fun requestImmediateSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<PodcastSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_SYNC_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}

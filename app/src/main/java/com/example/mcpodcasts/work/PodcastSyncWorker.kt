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
import java.util.Calendar
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
        runCatching {
            setForeground(notificationManager.createSyncForegroundInfo())
        }.onFailure { error ->
            if (!isForegroundPromotionBlocked(error)) {
                throw error
            }
        }

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

    private companion object {
        private const val FOREGROUND_START_NOT_ALLOWED =
            "android.app.ForegroundServiceStartNotAllowedException"

        private fun isForegroundPromotionBlocked(error: Throwable): Boolean {
            if (error is SecurityException) {
                return true
            }
            return error.javaClass.name == FOREGROUND_START_NOT_ALLOWED
        }
    }
}

object PodcastSyncScheduler {
    private const val PERIODIC_SYNC_NAME = "podcast-periodic-sync"
    private const val ONE_TIME_SYNC_NAME = "podcast-manual-sync"

    fun ensureScheduled(context: Context) {
        val now = System.currentTimeMillis()
        val midnight = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val initialDelayMs = (midnight.timeInMillis - now).coerceAtLeast(0L)

        val request = PeriodicWorkRequestBuilder<PodcastSyncWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
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

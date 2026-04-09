package com.example.mcpodcasts.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import com.example.mcpodcasts.MainActivity
import com.example.mcpodcasts.R
import com.example.mcpodcasts.data.repository.NewEpisodesSummary

class EpisodeNotificationManager(private val context: Context) {
    fun ensureChannels() {
        ensureChannel(
            channelId = CHANNEL_ID,
            nameRes = R.string.notification_channel_name,
            descriptionRes = R.string.notification_channel_description,
            importance = NotificationManager.IMPORTANCE_DEFAULT,
        )
        ensureChannel(
            channelId = SYNC_CHANNEL_ID,
            nameRes = R.string.sync_notification_channel_name,
            descriptionRes = R.string.sync_notification_channel_description,
            importance = NotificationManager.IMPORTANCE_LOW,
        )
    }

    fun notifySyncSummary(summaries: List<NewEpisodesSummary>) {
        if (summaries.isEmpty() || !canPostNotifications()) {
            return
        }

        val notificationManager = NotificationManagerCompat.from(context)
        val totalNewEpisodes = summaries.sumOf { it.episodeTitles.size }
        val title = context.resources.getQuantityString(
            R.plurals.notification_title_new_episodes,
            totalNewEpisodes,
            totalNewEpisodes,
        )
        val message = if (summaries.size == 1) {
            context.getString(
                R.string.notification_message_single_podcast,
                summaries.first().podcastTitle,
            )
        } else {
            context.getString(R.string.notification_message_multiple_podcasts, summaries.size)
        }

        val details = summaries.joinToString(separator = "\n") { summary ->
            "${summary.podcastTitle}: ${summary.episodeTitles.size}"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setContentIntent(createContentIntent())
            .setAutoCancel(true)
            .build()

        notificationManager.notify(SYNC_SUMMARY_NOTIFICATION_ID, notification)
    }

    fun createSyncForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.sync_in_progress_title))
            .setContentText(context.getString(R.string.sync_in_progress_message))
            .setContentIntent(createContentIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                SYNC_FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(SYNC_FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel(
        channelId: String,
        nameRes: Int,
        descriptionRes: Int,
        importance: Int,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val existingChannel = notificationManager.getNotificationChannel(channelId)
        if (existingChannel != null) {
            return
        }

        notificationManager.createNotificationChannel(
            NotificationChannel(
                channelId,
                context.getString(nameRes),
                importance,
            ).apply {
                description = context.getString(descriptionRes)
            }
        )
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            context,
            2026,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private companion object {
        const val CHANNEL_ID = "new-episodes"
        const val SYNC_CHANNEL_ID = "podcast-sync"
        const val SYNC_FOREGROUND_NOTIFICATION_ID = 99_000
        const val SYNC_SUMMARY_NOTIFICATION_ID = 99_001
    }
}

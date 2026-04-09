package com.example.mcpodcasts.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.mcpodcasts.MainActivity
import com.example.mcpodcasts.R
import com.example.mcpodcasts.data.repository.NewEpisodesSummary

class EpisodeNotificationManager(private val context: Context) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existingChannel != null) {
            return
        }

        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
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
        const val SYNC_SUMMARY_NOTIFICATION_ID = 99_001
    }
}

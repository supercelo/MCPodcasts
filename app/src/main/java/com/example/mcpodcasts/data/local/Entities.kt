package com.example.mcpodcasts.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val feedUrl: String,
    val title: String,
    val author: String?,
    val description: String?,
    val imageUrl: String?,
    val siteUrl: String?,
    val subscribedAt: Long,
    val lastSyncedAt: Long,
    val notifyNewEpisodes: Boolean,
    val includeInQueue: Boolean,
    val introSkipSeconds: Int,
    val outroSkipSeconds: Int,
)

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = PodcastEntity::class,
            parentColumns = ["feedUrl"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("podcastId"),
        Index("publishedAt"),
    ],
)
data class EpisodeEntity(
    @PrimaryKey val episodeId: String,
    val podcastId: String,
    val guid: String?,
    val title: String,
    val summary: String?,
    val audioUrl: String,
    val artworkUrl: String?,
    val episodeUrl: String?,
    val publishedAt: Long,
    val durationLabel: String?,
    val durationMs: Long,
    val playbackPositionMs: Long,
    val isRead: Boolean,
    val isCompleted: Boolean,
)

data class QueueEpisode(
    val episodeId: String,
    val podcastId: String,
    val podcastTitle: String,
    val title: String,
    val summary: String?,
    val audioUrl: String,
    val artworkUrl: String?,
    val episodeUrl: String?,
    val publishedAt: Long,
    val durationLabel: String?,
    val durationMs: Long,
    val playbackPositionMs: Long,
    val introSkipSeconds: Int,
    val outroSkipSeconds: Int,
    val isRead: Boolean,
    val isCompleted: Boolean,
)

data class SubscriptionSummary(
    val feedUrl: String,
    val title: String,
    val author: String?,
    val description: String?,
    val imageUrl: String?,
    val episodeCount: Int,
    val lastSyncedAt: Long,
    val notifyNewEpisodes: Boolean,
    val includeInQueue: Boolean,
    val introSkipSeconds: Int,
    val outroSkipSeconds: Int,
)

data class CalendarEpisode(
    val episodeId: String,
    val podcastId: String,
    val podcastTitle: String,
    val title: String,
    val summary: String?,
    val artworkUrl: String?,
    val publishedAt: Long,
    val durationLabel: String?,
    val audioUrl: String,
    val durationMs: Long,
    val playbackPositionMs: Long,
    val introSkipSeconds: Int,
    val outroSkipSeconds: Int,
    val isRead: Boolean,
)

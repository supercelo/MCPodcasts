package com.example.mcpodcasts.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Upsert
    suspend fun upsertPodcast(podcast: PodcastEntity)

    @Query("SELECT subscribedAt FROM podcasts WHERE feedUrl = :feedUrl")
    suspend fun getSubscribedAt(feedUrl: String): Long?

    @Query("SELECT * FROM podcasts WHERE feedUrl = :feedUrl")
    suspend fun getPodcast(feedUrl: String): PodcastEntity?

    @Query("SELECT feedUrl FROM podcasts ORDER BY title ASC")
    suspend fun getSubscribedFeedUrls(): List<String>

    @Query("SELECT * FROM podcasts ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllPodcasts(): List<PodcastEntity>

    @Query(
        """
        UPDATE podcasts
        SET notifyNewEpisodes = :notifyNewEpisodes,
            includeInQueue = :includeInQueue,
            introSkipSeconds = :introSkipSeconds,
            outroSkipSeconds = :outroSkipSeconds
        WHERE feedUrl = :feedUrl
        """
    )
    suspend fun updateSubscriptionSettings(
        feedUrl: String,
        notifyNewEpisodes: Boolean,
        includeInQueue: Boolean,
        introSkipSeconds: Int,
        outroSkipSeconds: Int,
    )

    @Query("DELETE FROM podcasts WHERE feedUrl = :feedUrl")
    suspend fun deletePodcast(feedUrl: String)

    @Query(
        """
        SELECT
            podcasts.feedUrl AS feedUrl,
            podcasts.title AS title,
            podcasts.author AS author,
            podcasts.description AS description,
            podcasts.imageUrl AS imageUrl,
            COUNT(episodes.episodeId) AS episodeCount,
            podcasts.lastSyncedAt AS lastSyncedAt,
            podcasts.notifyNewEpisodes AS notifyNewEpisodes,
            podcasts.includeInQueue AS includeInQueue,
            podcasts.introSkipSeconds AS introSkipSeconds,
            podcasts.outroSkipSeconds AS outroSkipSeconds
        FROM podcasts
        LEFT JOIN episodes ON episodes.podcastId = podcasts.feedUrl
        GROUP BY podcasts.feedUrl
        ORDER BY podcasts.title COLLATE NOCASE ASC
        """
    )
    fun observeSubscriptions(): Flow<List<SubscriptionSummary>>
}

@Dao
interface EpisodeDao {
    @Upsert
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId")
    suspend fun getEpisodesForPodcast(podcastId: String): List<EpisodeEntity>

    @Query("DELETE FROM episodes WHERE podcastId = :podcastId")
    suspend fun deleteEpisodesForPodcast(podcastId: String)

    @Query("DELETE FROM episodes WHERE podcastId = :podcastId AND episodeId NOT IN (:episodeIds)")
    suspend fun deleteEpisodesNotIn(podcastId: String, episodeIds: List<String>)

    @Query(
        """
        SELECT
            episodes.episodeId AS episodeId,
            episodes.podcastId AS podcastId,
            podcasts.title AS podcastTitle,
            episodes.title AS title,
            episodes.summary AS summary,
            episodes.audioUrl AS audioUrl,
            episodes.artworkUrl AS artworkUrl,
            episodes.episodeUrl AS episodeUrl,
            episodes.publishedAt AS publishedAt,
            episodes.durationLabel AS durationLabel,
            episodes.durationMs AS durationMs,
            episodes.playbackPositionMs AS playbackPositionMs,
            podcasts.introSkipSeconds AS introSkipSeconds,
            podcasts.outroSkipSeconds AS outroSkipSeconds,
            episodes.isRead AS isRead,
            episodes.isCompleted AS isCompleted
        FROM episodes
        INNER JOIN podcasts ON podcasts.feedUrl = episodes.podcastId
        WHERE podcasts.includeInQueue = 1
        ORDER BY episodes.publishedAt DESC, episodes.title COLLATE NOCASE ASC
        """
    )
    fun observeQueue(): Flow<List<QueueEpisode>>

    @Query(
        """
        SELECT
            episodes.episodeId AS episodeId,
            episodes.podcastId AS podcastId,
            podcasts.title AS podcastTitle,
            episodes.title AS title,
            episodes.summary AS summary,
            episodes.audioUrl AS audioUrl,
            episodes.artworkUrl AS artworkUrl,
            episodes.episodeUrl AS episodeUrl,
            episodes.publishedAt AS publishedAt,
            episodes.durationLabel AS durationLabel,
            episodes.durationMs AS durationMs,
            episodes.playbackPositionMs AS playbackPositionMs,
            podcasts.introSkipSeconds AS introSkipSeconds,
            podcasts.outroSkipSeconds AS outroSkipSeconds,
            episodes.isRead AS isRead,
            episodes.isCompleted AS isCompleted
        FROM episodes
        INNER JOIN podcasts ON podcasts.feedUrl = episodes.podcastId
        WHERE podcasts.includeInQueue = 1
        ORDER BY episodes.publishedAt DESC, episodes.title COLLATE NOCASE ASC
        """
    )
    suspend fun getQueueSnapshot(): List<QueueEpisode>

    @Query(
        """
        SELECT
            episodes.episodeId AS episodeId,
            episodes.podcastId AS podcastId,
            podcasts.title AS podcastTitle,
            episodes.title AS title,
            episodes.summary AS summary,
            episodes.artworkUrl AS artworkUrl,
            episodes.publishedAt AS publishedAt,
            episodes.durationLabel AS durationLabel,
            episodes.audioUrl AS audioUrl,
            episodes.durationMs AS durationMs,
            episodes.playbackPositionMs AS playbackPositionMs,
            podcasts.introSkipSeconds AS introSkipSeconds,
            podcasts.outroSkipSeconds AS outroSkipSeconds,
            episodes.isRead AS isRead
        FROM episodes
        INNER JOIN podcasts ON podcasts.feedUrl = episodes.podcastId
        ORDER BY episodes.publishedAt DESC, episodes.title COLLATE NOCASE ASC
        """
    )
    fun observeCalendarEpisodes(): Flow<List<CalendarEpisode>>

    @Query(
        """
        UPDATE episodes
        SET playbackPositionMs = :positionMs,
            durationMs = CASE
                WHEN :durationMs > 0 THEN :durationMs
                ELSE durationMs
            END,
            isRead = :isRead,
            isCompleted = :isCompleted
        WHERE episodeId = :episodeId
        """
    )
    suspend fun updatePlaybackState(
        episodeId: String,
        positionMs: Long,
        durationMs: Long,
        isRead: Boolean,
        isCompleted: Boolean,
    )

    @Query(
        """
        UPDATE episodes
        SET isRead = :isRead,
            playbackPositionMs = CASE
                WHEN :isRead THEN 0
                ELSE playbackPositionMs
            END
        WHERE episodeId = :episodeId
        """
    )
    suspend fun markEpisodeRead(
        episodeId: String,
        isRead: Boolean,
    )

    @Query(
        """
        UPDATE episodes
        SET isRead = :isRead,
            playbackPositionMs = CASE
                WHEN :isRead THEN 0
                ELSE playbackPositionMs
            END
        WHERE podcastId = :podcastId
        """
    )
    suspend fun markAllEpisodesReadForPodcast(
        podcastId: String,
        isRead: Boolean,
    )
}

package com.example.mcpodcasts.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.mcpodcasts.R
import com.example.mcpodcasts.data.local.CalendarEpisode
import com.example.mcpodcasts.data.local.EpisodeDao
import com.example.mcpodcasts.data.local.EpisodeEntity
import com.example.mcpodcasts.data.local.PodcastDao
import com.example.mcpodcasts.data.local.PodcastDatabase
import com.example.mcpodcasts.data.local.PodcastEntity
import com.example.mcpodcasts.data.local.QueueEpisode
import com.example.mcpodcasts.data.local.SubscriptionSummary
import com.example.mcpodcasts.data.rss.ParsedEpisode
import com.example.mcpodcasts.data.rss.RssFeedParser
import com.example.mcpodcasts.domain.mergeWithExistingEpisode
import com.example.mcpodcasts.domain.stableId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class NewEpisodesSummary(
    val feedUrl: String,
    val podcastTitle: String,
    val imageUrl: String?,
    val episodeTitles: List<String>,
)

class PodcastRepository(
    context: Context,
    private val database: PodcastDatabase,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val httpClient: OkHttpClient,
    private val rssFeedParser: RssFeedParser,
) {
    private val appContext = context.applicationContext

    fun observeQueue(): Flow<List<QueueEpisode>> = episodeDao.observeQueue()

    fun observeSubscriptions(): Flow<List<SubscriptionSummary>> = podcastDao.observeSubscriptions()

    fun observeCalendarEpisodes(): Flow<List<CalendarEpisode>> = episodeDao.observeCalendarEpisodes()

    suspend fun getQueueSnapshot(): List<QueueEpisode> = withContext(Dispatchers.IO) {
        episodeDao.getQueueSnapshot()
    }

    suspend fun addSubscription(feedUrl: String): Result<Unit> = runCatching {
        val normalizedUrl = normalizeFeedUrl(feedUrl)
        syncFeed(
            feedUrl = normalizedUrl,
            notifyForNewEpisodes = false,
        )
    }

    suspend fun refreshAllFeeds(): List<NewEpisodesSummary> = withContext(Dispatchers.IO) {
        buildList {
            podcastDao.getSubscribedFeedUrls().forEach { feedUrl ->
                val syncResult = runCatching {
                    syncFeed(
                        feedUrl = feedUrl,
                        notifyForNewEpisodes = true,
                    )
                }.getOrNull() ?: return@forEach

                if (syncResult.shouldNotify && syncResult.newEpisodeTitles.isNotEmpty()) {
                    add(
                        NewEpisodesSummary(
                            feedUrl = syncResult.feedUrl,
                            podcastTitle = syncResult.podcastTitle,
                            imageUrl = syncResult.imageUrl,
                            episodeTitles = syncResult.newEpisodeTitles,
                        )
                    )
                }
            }
        }
    }

    suspend fun updatePlaybackState(
        episodeId: String,
        positionMs: Long,
        durationMs: Long,
        isRead: Boolean,
        isCompleted: Boolean,
    ) = withContext(Dispatchers.IO) {
        episodeDao.updatePlaybackState(
            episodeId = episodeId,
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = durationMs.coerceAtLeast(0L),
            isRead = isRead,
            isCompleted = isCompleted,
        )
    }

    suspend fun markEpisodeRead(
        episodeId: String,
        isRead: Boolean,
    ) = withContext(Dispatchers.IO) {
        episodeDao.markEpisodeRead(
            episodeId = episodeId,
            isRead = isRead,
        )
    }

    suspend fun markAllEpisodesReadForPodcast(
        feedUrl: String,
        isRead: Boolean,
    ) = withContext(Dispatchers.IO) {
        episodeDao.markAllEpisodesReadForPodcast(
            podcastId = feedUrl,
            isRead = isRead,
        )
    }

    suspend fun updateSubscriptionSettings(
        feedUrl: String,
        notifyNewEpisodes: Boolean,
        includeInQueue: Boolean,
        introSkipSeconds: Int,
        outroSkipSeconds: Int,
    ) = withContext(Dispatchers.IO) {
        podcastDao.updateSubscriptionSettings(
            feedUrl = feedUrl,
            notifyNewEpisodes = notifyNewEpisodes,
            includeInQueue = includeInQueue,
            introSkipSeconds = introSkipSeconds.coerceAtLeast(0),
            outroSkipSeconds = outroSkipSeconds.coerceAtLeast(0),
        )
    }

    suspend fun removeSubscription(feedUrl: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            episodeDao.deleteEpisodesForPodcast(feedUrl)
            podcastDao.deletePodcast(feedUrl)
        }
    }

    private suspend fun syncFeed(
        feedUrl: String,
        notifyForNewEpisodes: Boolean,
    ): FeedSyncResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(feedUrl)
            .header("User-Agent", "Podcast MC/1.0")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            error(appContext.getString(R.string.error_fetch_feed, response.code))
        }

        val parsedFeed = response.use { networkResponse ->
            val body = networkResponse.body ?: error(appContext.getString(R.string.error_feed_no_content))
            body.byteStream().use(rssFeedParser::parse)
        }

        val syncedAt = System.currentTimeMillis()
        val existingPodcast = podcastDao.getPodcast(feedUrl)
        val subscribedAt = existingPodcast?.subscribedAt ?: syncedAt
        val existingEpisodes = episodeDao.getEpisodesForPodcast(feedUrl)
            .associateBy(EpisodeEntity::episodeId)
        val podcast = PodcastEntity(
            feedUrl = feedUrl,
            title = parsedFeed.title,
            author = parsedFeed.author,
            description = parsedFeed.description,
            imageUrl = parsedFeed.imageUrl,
            siteUrl = parsedFeed.siteUrl,
            subscribedAt = subscribedAt,
            lastSyncedAt = syncedAt,
            notifyNewEpisodes = existingPodcast?.notifyNewEpisodes ?: true,
            includeInQueue = existingPodcast?.includeInQueue ?: true,
            introSkipSeconds = existingPodcast?.introSkipSeconds ?: 0,
            outroSkipSeconds = existingPodcast?.outroSkipSeconds ?: 0,
        )

        val episodes = parsedFeed.episodes
            .map { parsedEpisode ->
                parsedEpisode.mergeWithExistingEpisode(
                    podcastId = feedUrl,
                    fallbackArtworkUrl = parsedFeed.imageUrl,
                    existingEpisode = existingEpisodes[parsedEpisode.stableId()],
                )
            }
        val newEpisodeTitles = episodes
            .filter { episode -> existingEpisodes[episode.episodeId] == null }
            .map(EpisodeEntity::title)

        database.withTransaction {
            podcastDao.upsertPodcast(podcast)

            if (episodes.isEmpty()) {
                episodeDao.deleteEpisodesForPodcast(feedUrl)
            } else {
                episodeDao.upsertEpisodes(episodes)
                episodeDao.deleteEpisodesNotIn(feedUrl, episodes.map(EpisodeEntity::episodeId))
            }
        }

        FeedSyncResult(
            feedUrl = feedUrl,
            podcastTitle = podcast.title,
            imageUrl = podcast.imageUrl,
            newEpisodeTitles = if (existingPodcast == null || !notifyForNewEpisodes) {
                emptyList()
            } else {
                newEpisodeTitles
            },
            shouldNotify = notifyForNewEpisodes && podcast.notifyNewEpisodes,
        )
    }

    private fun normalizeFeedUrl(feedUrl: String): String {
        val trimmed = feedUrl.trim()
        if (trimmed.isBlank()) {
            error(appContext.getString(R.string.error_invalid_rss_url))
        }

        return trimmed
            .replace("feed://", "https://")
            .replace("http://", "http://")
    }

}

private data class FeedSyncResult(
    val feedUrl: String,
    val podcastTitle: String,
    val imageUrl: String?,
    val newEpisodeTitles: List<String>,
    val shouldNotify: Boolean,
)


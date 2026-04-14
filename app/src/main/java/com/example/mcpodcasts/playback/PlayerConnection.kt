package com.example.mcpodcasts.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.mcpodcasts.data.local.CalendarEpisode
import com.example.mcpodcasts.data.local.QueueEpisode
import com.example.mcpodcasts.data.repository.PodcastRepository
import com.example.mcpodcasts.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val connected: Boolean = false,
    val currentEpisodeId: String? = null,
    val title: String = "",
    val podcastTitle: String = "",
    val artworkUrl: String? = null,
    /** Episode publication time in epoch millis; null if unknown. */
    val publishedAtMs: Long? = null,
    /** Episode summary/description; null if unavailable. */
    val summary: String? = null,
    /** Episode webpage URL (from RSS `<link>` element); null if unavailable. */
    val episodeUrl: String? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasMedia: Boolean = false,
)

class PlayerConnection(
    context: Context,
    private val repository: PodcastRepository,
    private val settingsRepository: SettingsRepository,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessionToken = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
    private val _uiState = MutableStateFlow(PlayerUiState())
    private val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()

    private var controller: MediaController? = null
    private var queuePlaybackEpisodes: Map<String, QueueEpisode> = emptyMap()
    private var singlePlaybackEpisode: CalendarEpisode? = null
    private var hasAttemptedRestore = false

    /** True after user scrubbed into [0, intro) for current episode. */
    private var userAllowsIntroPlayback: Boolean = false

    /** True after user scrubbed into [outroStart, duration] for current episode. */
    private var userAllowsOutroPlayback: Boolean = false

    /** Prevents repeated seekToNext / seek-to-end when already handling outro for this item. */
    private var outroSkipTriggeredForMediaId: String? = null

    /** When this differs from [Player.currentMediaItem] id, clear [outroSkipTriggeredForMediaId]. */
    private var outroMonitorLastMediaId: String? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publishState(player)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                controller?.let { persistPlayback(it) }
            }
        }
    }

    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        controllerFuture.addListener(
            {
                runCatching {
                    controllerFuture.get()
                }.onSuccess { mediaController ->
                    controller = mediaController.also {
                        it.addListener(playerListener)
                        publishState(it)
                    }
                    scope.launch {
                        restoreSavedPlayback(mediaController)
                    }
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )

        scope.launch {
            while (isActive) {
                controller?.let { mediaController ->
                    publishState(mediaController)
                    persistPlayback(mediaController)
                }
                delay(2_000)
            }
        }

        scope.launch {
            while (isActive) {
                controller?.let { maybeAutoSkipOutro(it) }
                delay(350)
            }
        }
    }

    fun playQueue(queue: List<QueueEpisode>, selectedEpisodeId: String) {
        val mediaController = controller ?: return
        val selectedIndex = queue.indexOfFirst { episode -> episode.episodeId == selectedEpisodeId }
        if (selectedIndex < 0) {
            return
        }

        val selectedEpisode = queue[selectedIndex]
        resetScrubSkipState()
        singlePlaybackEpisode = null
        queuePlaybackEpisodes = queue.associateBy(QueueEpisode::episodeId)
        val startMs = computeStartPositionWithIntro(
            savedMs = selectedEpisode.playbackPositionMs,
            introSkipSeconds = selectedEpisode.introSkipSeconds,
        )
        mediaController.setMediaItems(
            queue.map(QueueEpisode::toMediaItem),
            selectedIndex,
            startMs,
        )
        mediaController.repeatMode = Player.REPEAT_MODE_OFF
        mediaController.prepare()
        mediaController.play()
    }

    fun playCalendarEpisode(episode: CalendarEpisode) {
        val mediaController = controller ?: return
        resetScrubSkipState()
        queuePlaybackEpisodes = emptyMap()
        singlePlaybackEpisode = episode
        val startMs = computeStartPositionWithIntro(
            savedMs = episode.playbackPositionMs,
            introSkipSeconds = episode.introSkipSeconds,
        )
        mediaController.setMediaItem(
            episode.toMediaItem(),
            startMs,
        )
        mediaController.repeatMode = Player.REPEAT_MODE_OFF
        mediaController.prepare()
        mediaController.play()
    }

    fun togglePlayback() {
        controller?.let { mediaController ->
            if (mediaController.isPlaying) {
                mediaController.pause()
            } else {
                mediaController.play()
            }
        }
    }

    fun seekBack() {
        controller?.seekBack()
    }

    fun seekForward() {
        controller?.seekForward()
    }

    fun seekToPosition(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    /**
     * Seek from the in-app progress bar: updates whether intro/outro zones may play
     * (scrub into intro or outro) vs default skip behaviour.
     */
    fun seekToPositionFromUser(positionMs: Long) {
        val mediaController = controller ?: return
        val safe = positionMs.coerceAtLeast(0L)
        mediaController.seekTo(safe)
        val episodeId = mediaController.currentMediaItem?.mediaId ?: return
        val skips = resolveSkipsForMedia(episodeId)
        if (skips == null) {
            userAllowsIntroPlayback = false
            userAllowsOutroPlayback = false
            return
        }
        val introMs = skips.first * 1_000L
        val duration = mediaController.duration.takeIf { it > 0 } ?: 0L

        userAllowsIntroPlayback = skips.first > 0 && safe < introMs

        val outroMs = skips.second * 1_000L
        userAllowsOutroPlayback = skips.second > 0 &&
            duration > outroMs &&
            safe >= duration - outroMs

        if (skips.second > 0 && duration > outroMs) {
            val outroStart = duration - outroMs
            if (safe < outroStart) {
                outroSkipTriggeredForMediaId = null
            }
        }
    }

    fun skipToNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun release() {
        controller?.removeListener(playerListener)
        MediaController.releaseFuture(controllerFuture)
        scope.cancel()
    }

    private fun publishState(player: Player) {
        val previousState = _uiState.value
        val mediaMetadata = player.mediaMetadata
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        val hasKnownMedia = player.currentMediaItem != null || player.mediaItemCount > 0
        val publishedAtMs = when {
            player.currentMediaItem != null -> resolvePublishedAtMs(player)
            hasKnownMedia -> previousState.publishedAtMs
            else -> null
        }
        val episodeUrl = when {
            player.currentMediaItem != null -> resolveEpisodeUrl(player)
            hasKnownMedia -> previousState.episodeUrl
            else -> null
        }
        val summary = when {
            player.currentMediaItem != null -> resolveSummary(player)
            hasKnownMedia -> previousState.summary
            else -> null
        }
        _uiState.value = PlayerUiState(
            connected = true,
            currentEpisodeId = player.currentMediaItem?.mediaId ?: previousState.currentEpisodeId.takeIf { hasKnownMedia },
            title = mediaMetadata.title?.toString()?.takeUnless { it.isBlank() }
                ?: previousState.title.takeIf { hasKnownMedia }
                .orEmpty(),
            podcastTitle = mediaMetadata.artist?.toString()?.takeUnless { it.isBlank() }
                ?: previousState.podcastTitle.takeIf { hasKnownMedia }
                .orEmpty(),
            artworkUrl = mediaMetadata.artworkUri?.toString()
                ?: previousState.artworkUrl.takeIf { hasKnownMedia },
            publishedAtMs = publishedAtMs,
            summary = summary,
            episodeUrl = episodeUrl,
            durationMs = duration,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            hasMedia = hasKnownMedia,
        )
    }

    private fun resolvePublishedAtMs(player: Player): Long? {
        val item = player.currentMediaItem ?: return null
        val fromTag = item.localConfiguration?.tag
        if (fromTag is Long && fromTag > 0L) {
            return fromTag
        }
        return queuePlaybackEpisodes[item.mediaId]?.publishedAt?.takeIf { it > 0L }
            ?: singlePlaybackEpisode
                ?.takeIf { episode -> episode.episodeId == item.mediaId }
                ?.publishedAt
                ?.takeIf { it > 0L }
    }

    private fun resolveEpisodeUrl(player: Player): String? {
        val mediaId = player.currentMediaItem?.mediaId ?: return null
        return queuePlaybackEpisodes[mediaId]?.episodeUrl
            ?: singlePlaybackEpisode
                ?.takeIf { it.episodeId == mediaId }
                ?.episodeUrl
    }

    private fun resolveSummary(player: Player): String? {
        val mediaId = player.currentMediaItem?.mediaId ?: return null
        return queuePlaybackEpisodes[mediaId]?.summary
            ?: singlePlaybackEpisode
                ?.takeIf { it.episodeId == mediaId }
                ?.summary
    }

    private fun persistPlayback(player: Player) {
        val episodeId = player.currentMediaItem?.mediaId ?: return
        val durationMs = player.duration.takeIf { it > 0 } ?: 0L
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val isEnded = player.playbackState == Player.STATE_ENDED
        val outroSkipSec = resolveSkipsForMedia(episodeId)?.second ?: 0
        val outroSkipMs = outroSkipSec * 1_000L
        val completionThresholdMs = if (durationMs > 0L && outroSkipMs in 1 until durationMs) {
            durationMs - outroSkipMs
        } else {
            durationMs
        }
        val isCompleted = isEnded || (completionThresholdMs > 0 && positionMs >= completionThresholdMs - 1_000)

        scope.launch(Dispatchers.IO) {
            settingsRepository.setLastPlayedEpisodeId(episodeId)
            repository.updatePlaybackState(
                episodeId = episodeId,
                positionMs = if (isCompleted) 0L else positionMs,
                durationMs = durationMs,
                isRead = isCompleted,
                isCompleted = isCompleted,
            )
        }
    }

    private fun resetScrubSkipState() {
        userAllowsIntroPlayback = false
        userAllowsOutroPlayback = false
        outroSkipTriggeredForMediaId = null
        outroMonitorLastMediaId = null
    }

    private suspend fun restoreSavedPlayback(mediaController: MediaController) {
        if (hasAttemptedRestore) return
        hasAttemptedRestore = true
        if (mediaController.mediaItemCount > 0) return

        val lastPlayedEpisodeId = settingsRepository.getLastPlayedEpisodeId() ?: return
        val queue = repository.getQueueSnapshot()
        val selectedIndex = queue.indexOfFirst { episode ->
            episode.episodeId == lastPlayedEpisodeId && !episode.isCompleted
        }
        if (selectedIndex < 0) return

        val selectedEpisode = queue[selectedIndex]
        resetScrubSkipState()
        singlePlaybackEpisode = null
        queuePlaybackEpisodes = queue.associateBy(QueueEpisode::episodeId)
        val startMs = computeStartPositionWithIntro(
            savedMs = selectedEpisode.playbackPositionMs,
            introSkipSeconds = selectedEpisode.introSkipSeconds,
        )
        mediaController.setMediaItems(
            queue.map(QueueEpisode::toMediaItem),
            selectedIndex,
            startMs,
        )
        mediaController.repeatMode = Player.REPEAT_MODE_OFF
        mediaController.prepare()
        mediaController.pause()
        publishState(mediaController)
    }

    /**
     * Initial seek when loading: skip intro unless the user previously resumed inside it (saved in (0, intro)).
     */
    private fun computeStartPositionWithIntro(savedMs: Long, introSkipSeconds: Int): Long {
        val saved = savedMs.coerceAtLeast(0L)
        if (introSkipSeconds <= 0) return saved
        val introMs = introSkipSeconds * 1_000L
        return when {
            saved == 0L -> introMs
            saved < introMs -> saved
            else -> saved
        }
    }

    private fun resolveSkipsForMedia(episodeId: String): Pair<Int, Int>? {
        queuePlaybackEpisodes[episodeId]?.let { episode ->
            return episode.introSkipSeconds to episode.outroSkipSeconds
        }
        singlePlaybackEpisode?.takeIf { it.episodeId == episodeId }?.let { episode ->
            return episode.introSkipSeconds to episode.outroSkipSeconds
        }
        return null
    }

    /**
     * Poll (~350 ms): if playback enters the outro zone without an explicit user scrub into it,
     * mark the episode complete and skip to the next queue item or end-of-content for a single item.
     */
    private fun maybeAutoSkipOutro(player: Player) {
        if (!player.isPlaying) return
        val episodeId = player.currentMediaItem?.mediaId ?: return
        if (episodeId != outroMonitorLastMediaId) {
            outroMonitorLastMediaId = episodeId
            outroSkipTriggeredForMediaId = null
        }
        val skips = resolveSkipsForMedia(episodeId) ?: return
        val outroSec = skips.second
        if (outroSec <= 0) return
        val duration = player.duration.takeIf { it > 0 } ?: return
        val outroMs = outroSec * 1_000L
        if (outroMs >= duration) return
        val outroStart = duration - outroMs
        val position = player.currentPosition
        if (userAllowsOutroPlayback) return
        if (position < outroStart) return
        if (outroSkipTriggeredForMediaId == episodeId) return

        outroSkipTriggeredForMediaId = episodeId
        scope.launch(Dispatchers.IO) {
            repository.updatePlaybackState(
                episodeId = episodeId,
                positionMs = 0L,
                durationMs = duration,
                isRead = true,
                isCompleted = true,
            )
        }
        if (player.mediaItemCount > 1) {
            userAllowsIntroPlayback = false
            userAllowsOutroPlayback = false
            player.seekToNextMediaItem()
        } else {
            userAllowsIntroPlayback = false
            userAllowsOutroPlayback = false
            player.seekTo(duration)
            player.pause()
        }
    }
}

private fun QueueEpisode.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(episodeId)
        .setUri(audioUrl)
        .setTag(publishedAt)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(podcastTitle)
                .setArtworkUri(artworkUrl?.let(Uri::parse))
                .build()
        )
        .build()
}

private fun CalendarEpisode.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(episodeId)
        .setUri(audioUrl)
        .setTag(publishedAt)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(podcastTitle)
                .setArtworkUri(artworkUrl?.let(Uri::parse))
                .build()
        )
        .build()
}

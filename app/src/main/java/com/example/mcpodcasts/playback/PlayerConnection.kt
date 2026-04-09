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
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasMedia: Boolean = false,
)

class PlayerConnection(
    context: Context,
    private val repository: PodcastRepository,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessionToken = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
    private val _uiState = MutableStateFlow(PlayerUiState())
    private val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()

    private var controller: MediaController? = null
    private var queuePlaybackEpisodes: Map<String, QueueEpisode> = emptyMap()

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publishState(player)
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
    }

    fun playQueue(queue: List<QueueEpisode>, selectedEpisodeId: String) {
        val mediaController = controller ?: return
        val selectedIndex = queue.indexOfFirst { episode -> episode.episodeId == selectedEpisodeId }
        if (selectedIndex < 0) {
            return
        }

        val selectedEpisode = queue[selectedIndex]
        queuePlaybackEpisodes = queue.associateBy(QueueEpisode::episodeId)
        mediaController.setMediaItems(
            queue.map(QueueEpisode::toMediaItem),
            selectedIndex,
            selectedEpisode.playbackPositionMs
                .coerceAtLeast(selectedEpisode.introSkipSeconds * 1_000L),
        )
        mediaController.repeatMode = Player.REPEAT_MODE_OFF
        mediaController.prepare()
        mediaController.play()
    }

    fun playCalendarEpisode(episode: CalendarEpisode) {
        val mediaController = controller ?: return
        queuePlaybackEpisodes = emptyMap()
        mediaController.setMediaItem(
            episode.toMediaItem(),
            episode.playbackPositionMs.coerceAtLeast(0L),
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
            durationMs = duration,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            hasMedia = hasKnownMedia,
        )
    }

    private fun persistPlayback(player: Player) {
        val episodeId = player.currentMediaItem?.mediaId ?: return
        val durationMs = player.duration.takeIf { it > 0 } ?: 0L
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val outroSkipMs = queuePlaybackEpisodes[episodeId]?.outroSkipSeconds?.times(1_000L) ?: 0L
        val completionThresholdMs = if (durationMs > 0L && outroSkipMs in 1 until durationMs) {
            durationMs - outroSkipMs
        } else {
            durationMs
        }
        val isCompleted = completionThresholdMs > 0 && positionMs >= completionThresholdMs - 1_000

        scope.launch(Dispatchers.IO) {
            repository.updatePlaybackState(
                episodeId = episodeId,
                positionMs = if (isCompleted) 0L else positionMs,
                durationMs = durationMs,
                isRead = isCompleted,
                isCompleted = isCompleted,
            )
        }
    }
}

private fun QueueEpisode.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(episodeId)
        .setUri(audioUrl)
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
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(podcastTitle)
                .setArtworkUri(artworkUrl?.let(Uri::parse))
                .build()
        )
        .build()
}

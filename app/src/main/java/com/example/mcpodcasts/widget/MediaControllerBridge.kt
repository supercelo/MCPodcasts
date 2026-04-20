package com.example.mcpodcasts.widget

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.mcpodcasts.playback.PlaybackService
import com.example.mcpodcasts.playback.PlayerUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Briefly binds a [MediaController] to [PlaybackService] and executes [block] on the main thread,
 * releasing the controller afterwards. Used by widget action callbacks so each button press
 * issues a command through the media session.
 */
internal suspend fun <T> withMediaController(
    context: Context,
    block: suspend (MediaController) -> T,
): T = withContext(Dispatchers.Main) {
    val token = SessionToken(
        context.applicationContext,
        ComponentName(context.applicationContext, PlaybackService::class.java),
    )
    val future = MediaController.Builder(context.applicationContext, token).buildAsync()
    val controller = suspendCancellableCoroutine<MediaController> { continuation ->
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess(continuation::resume)
                    .onFailure(continuation::resumeWithException)
            },
            ContextCompat.getMainExecutor(context.applicationContext),
        )
        continuation.invokeOnCancellation { MediaController.releaseFuture(future) }
    }
    try {
        block(controller)
    } finally {
        controller.release()
    }
}

/**
 * Mirrors the [controller]'s current playback state into the widget DataStore so the home screen
 * widget reflects changes triggered from widget actions, even when the app process isn't running
 * and the in-app [com.example.mcpodcasts.playback.PlayerConnection] listener isn't observing.
 *
 * [overrideIsPlaying] paints the optimistic end state immediately because the [MediaController]
 * needs a few ms to propagate `isPlaying` after a pause/play. Leave it `null` for actions that
 * don't toggle play/pause (seek, skip).
 */
internal suspend fun refreshWidgetFromController(
    context: Context,
    controller: MediaController,
    overrideIsPlaying: Boolean? = null,
) = withContext(Dispatchers.Main) {
    val metadata = controller.mediaMetadata
    val durationMs = controller.duration.takeIf { it > 0 } ?: 0L
    val positionMs = controller.currentPosition.coerceAtLeast(0L)
    val hasMedia = controller.currentMediaItem != null || controller.mediaItemCount > 0
    val state = PlayerUiState(
        connected = true,
        currentEpisodeId = controller.currentMediaItem?.mediaId,
        title = metadata.title?.toString().orEmpty(),
        podcastTitle = metadata.artist?.toString().orEmpty(),
        artworkUrl = metadata.artworkUri?.toString(),
        publishedAtMs = null,
        summary = null,
        episodeUrl = null,
        durationMs = durationMs,
        positionMs = positionMs,
        isPlaying = overrideIsPlaying ?: controller.isPlaying,
        isBuffering = controller.playbackState == Player.STATE_BUFFERING,
        hasMedia = hasMedia,
    )
    PodcastPlayerWidgetUpdater.refresh(context, state)
}

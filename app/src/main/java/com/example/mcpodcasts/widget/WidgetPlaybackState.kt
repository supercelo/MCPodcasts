package com.example.mcpodcasts.widget

import android.graphics.Bitmap

/** Snapshot of playback state rendered by [PodcastPlayerWidget]. */
internal data class WidgetPlaybackState(
    val hasEpisode: Boolean,
    val episodeId: String?,
    val title: String,
    val podcastTitle: String,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val artwork: Bitmap?,
) {
    companion object {
        val EMPTY = WidgetPlaybackState(
            hasEpisode = false,
            episodeId = null,
            title = "",
            podcastTitle = "",
            isPlaying = false,
            positionMs = 0L,
            durationMs = 0L,
            artwork = null,
        )
    }
}

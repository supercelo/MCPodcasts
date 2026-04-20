package com.example.mcpodcasts.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File

internal object WidgetPreferencesKeys {
    val HasEpisode = booleanPreferencesKey("widget_has_episode")
    val EpisodeId = stringPreferencesKey("widget_episode_id")
    val Title = stringPreferencesKey("widget_title")
    val Podcast = stringPreferencesKey("widget_podcast")
    val IsPlaying = booleanPreferencesKey("widget_is_playing")
    val PositionMs = longPreferencesKey("widget_position_ms")
    val DurationMs = longPreferencesKey("widget_duration_ms")
    val ArtworkPath = stringPreferencesKey("widget_artwork_path")
}

internal fun Preferences.toWidgetState(context: Context): WidgetPlaybackState {
    val hasEpisode = this[WidgetPreferencesKeys.HasEpisode] ?: false
    if (!hasEpisode) return WidgetPlaybackState.EMPTY
    val artwork = this[WidgetPreferencesKeys.ArtworkPath]?.let { path ->
        loadArtwork(context, path)
    }
    return WidgetPlaybackState(
        hasEpisode = true,
        episodeId = this[WidgetPreferencesKeys.EpisodeId],
        title = this[WidgetPreferencesKeys.Title].orEmpty(),
        podcastTitle = this[WidgetPreferencesKeys.Podcast].orEmpty(),
        isPlaying = this[WidgetPreferencesKeys.IsPlaying] ?: false,
        positionMs = this[WidgetPreferencesKeys.PositionMs] ?: 0L,
        durationMs = this[WidgetPreferencesKeys.DurationMs] ?: 0L,
        artwork = artwork,
    )
}

private fun loadArtwork(context: Context, path: String): Bitmap? {
    val file = File(path)
    if (!file.exists()) return null
    return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
}

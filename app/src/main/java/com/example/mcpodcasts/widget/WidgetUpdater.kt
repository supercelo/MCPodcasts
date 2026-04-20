package com.example.mcpodcasts.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.example.mcpodcasts.playback.PlayerUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Mirrors the latest [PlayerUiState] into the widget's DataStore-backed state so the home
 * screen widget always reflects whatever the in-app player shows.
 */
internal object PodcastPlayerWidgetUpdater {
    private val mutex = Mutex()
    private var lastArtworkUrl: String? = null
    private var lastArtworkFile: File? = null

    suspend fun refresh(context: Context, state: PlayerUiState) = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val manager = GlanceAppWidgetManager(app)
        val ids = runCatching {
            manager.getGlanceIds(PodcastPlayerWidget::class.java)
        }.getOrNull().orEmpty()
        if (ids.isEmpty()) return@withContext
        val artworkFile = resolveArtwork(app, state.artworkUrl)
        mutex.withLock {
            ids.forEach { id ->
                updateAppWidgetState(app, id) { prefs ->
                    prefs.writeWidgetState(state, artworkFile)
                }
                PodcastPlayerWidget().update(app, id)
            }
        }
    }

    private fun MutablePreferences.writeWidgetState(state: PlayerUiState, artworkFile: File?) {
        this[WidgetPreferencesKeys.HasEpisode] = state.hasMedia
        this[WidgetPreferencesKeys.EpisodeId] = state.currentEpisodeId.orEmpty()
        this[WidgetPreferencesKeys.Title] = state.title
        this[WidgetPreferencesKeys.Podcast] = state.podcastTitle
        this[WidgetPreferencesKeys.IsPlaying] = state.isPlaying
        this[WidgetPreferencesKeys.PositionMs] = state.positionMs
        this[WidgetPreferencesKeys.DurationMs] = state.durationMs
        this[WidgetPreferencesKeys.ArtworkPath] = artworkFile?.absolutePath.orEmpty()
    }

    private suspend fun resolveArtwork(context: Context, url: String?): File? {
        if (url.isNullOrBlank()) return null
        if (url == lastArtworkUrl) {
            val cached = lastArtworkFile
            if (cached != null && cached.exists()) return cached
        }
        val bitmap = loadBitmap(context, url) ?: return null
        val scaled = scaleForWidget(bitmap)
        val file = File(context.cacheDir, "widget_artwork.png")
        val saved = runCatching {
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        }.isSuccess
        if (!saved) return null
        lastArtworkUrl = url
        lastArtworkFile = file
        return file
    }

    private suspend fun loadBitmap(context: Context, url: String): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        val loader = ImageLoader(context)
        val result = runCatching { loader.execute(request) }.getOrNull()
        if (result !is SuccessResult) return null
        return runCatching { result.image.toBitmap() }.getOrNull()
    }

    private fun scaleForWidget(bitmap: Bitmap): Bitmap {
        val max = 256
        val width = bitmap.width
        val height = bitmap.height
        if (width <= max && height <= max) return bitmap
        val ratio = maxOf(width, height).toFloat() / max
        val targetWidth = (width / ratio).toInt().coerceAtLeast(1)
        val targetHeight = (height / ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}

package com.example.mcpodcasts.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.mcpodcasts.MCPodcastsApplication
import com.example.mcpodcasts.data.local.CalendarEpisode
import com.example.mcpodcasts.data.local.QueueEpisode
import com.example.mcpodcasts.playback.PlayerConnection
import com.example.mcpodcasts.playback.PlayerUiState
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel(
    application: Application,
    private val playerConnection: PlayerConnection,
) : AndroidViewModel(application) {
    val playerState: StateFlow<PlayerUiState> = playerConnection.uiState

    fun playQueue(queue: List<QueueEpisode>, selectedEpisodeId: String) {
        playerConnection.playQueue(queue, selectedEpisodeId)
    }

    fun playCalendarEpisode(episode: CalendarEpisode) {
        playerConnection.playCalendarEpisode(episode)
    }

    fun togglePlayback() {
        playerConnection.togglePlayback()
    }

    fun seekBack() {
        playerConnection.seekBack()
    }

    fun seekForward() {
        playerConnection.seekForward()
    }

    fun seekToPosition(positionMs: Long) {
        playerConnection.seekToPosition(positionMs)
    }

    fun seekToPositionFromUser(positionMs: Long) {
        playerConnection.seekToPositionFromUser(positionMs)
    }

    fun skipToPrevious() {
        playerConnection.skipToPrevious()
    }

    fun skipToNext() {
        playerConnection.skipToNext()
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras,
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val app = application as MCPodcastsApplication

                @Suppress("UNCHECKED_CAST")
                return PlayerViewModel(
                    application = application,
                    playerConnection = app.container.playerConnection,
                ) as T
            }
        }
    }
}

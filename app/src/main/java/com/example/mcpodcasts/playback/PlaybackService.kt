package com.example.mcpodcasts.playback

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.mcpodcasts.MainActivity
import com.example.mcpodcasts.MCPodcastsApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var volumeNormalizationEnabled: Boolean = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(SEEK_BACK_MS)
            .setSeekForwardIncrementMs(SEEK_FORWARD_MS)
            .build()

        exoPlayer.addListener(
            object : Player.Listener {
                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    applyLoudnessEnhancer(audioSessionId)
                }
            },
        )

        val app = application as MCPodcastsApplication
        serviceScope.launch {
            app.container.settingsRepository.settings
                .map { it.volumeNormalizationEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    volumeNormalizationEnabled = enabled
                    applyLoudnessEnhancer(exoPlayer.audioSessionId)
                }
        }

        player = exoPlayer

        val seekBackButton = CommandButton.Builder(CommandButton.ICON_SKIP_BACK_10)
            .setPlayerCommand(Player.COMMAND_SEEK_BACK)
            .setDisplayName("Seek back 10s")
            .build()
        val seekForwardButton = CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_30)
            .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
            .setDisplayName("Seek forward 30s")
            .build()
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setId("mcpodcasts-session")
            .setSessionActivity(createSessionActivity())
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                ): MediaSession.ConnectionResult {
                    if (session.isMediaNotificationController(controller)) {
                        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                            .setMediaButtonPreferences(listOf(seekBackButton, seekForwardButton))
                            .build()
                    }
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
                }
            })
            .build()
            .also { session ->
                session.setSessionExtras(
                    Bundle().apply {
                        putBoolean(MediaConstants.EXTRAS_KEY_SLOT_RESERVATION_SEEK_TO_PREV, true)
                        putBoolean(MediaConstants.EXTRAS_KEY_SLOT_RESERVATION_SEEK_TO_NEXT, true)
                    },
                )
            }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val exoPlayer = player
        if (exoPlayer == null || !exoPlayer.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        releaseLoudnessEnhancer()
        mediaSession?.release()
        player?.release()
        mediaSession = null
        player = null
        super.onDestroy()
    }

    private fun releaseLoudnessEnhancer() {
        loudnessEnhancer?.release()
        loudnessEnhancer = null
    }

    private fun applyLoudnessEnhancer(audioSessionId: Int) {
        releaseLoudnessEnhancer()
        if (!volumeNormalizationEnabled) return
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        runCatching {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                enabled = true
            }
        }
    }

    private fun createSessionActivity(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        return PendingIntent.getActivity(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val SEEK_BACK_MS = 10_000L
        const val SEEK_FORWARD_MS = 30_000L
    }
}

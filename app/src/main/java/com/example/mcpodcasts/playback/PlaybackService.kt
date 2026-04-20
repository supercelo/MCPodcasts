package com.example.mcpodcasts.playback

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.LoudnessEnhancer
import android.view.KeyEvent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
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

@OptIn(UnstableApi::class)
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
            .setSlots(CommandButton.SLOT_BACK)
            .setDisplayName("Seek back 10s")
            .build()
        val seekForwardButton = CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_30)
            .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
            .setSlots(CommandButton.SLOT_FORWARD)
            .setDisplayName("Seek forward 30s")
            .build()
        val previousEpisodeButton = CommandButton.Builder(CommandButton.ICON_PREVIOUS)
            .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .setSlots(CommandButton.SLOT_BACK_SECONDARY)
            .setDisplayName("Previous episode")
            .build()
        val nextEpisodeButton = CommandButton.Builder(CommandButton.ICON_NEXT)
            .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .setSlots(CommandButton.SLOT_FORWARD_SECONDARY)
            .setDisplayName("Next episode")
            .build()
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setId("mcpodcasts-session")
            .setSessionActivity(createSessionActivity())
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                ): MediaSession.ConnectionResult {
                    val isNotificationController = session.isMediaNotificationController(controller)
                    val isAppController = controller.packageName == packageName
                    if (isAppController && !isNotificationController) {
                        return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
                    }
                    if (isNotificationController) {
                        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                            .setMediaButtonPreferences(
                                listOf(
                                    previousEpisodeButton,
                                    seekBackButton,
                                    seekForwardButton,
                                    nextEpisodeButton,
                                ),
                            )
                            .build()
                    }
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setMediaButtonPreferences(listOf(seekBackButton, seekForwardButton))
                        .setAvailablePlayerCommands(
                            withoutQueueNavigationCommands(exoPlayer.availableCommands),
                        )
                        .build()
                }

                override fun onMediaButtonEvent(
                    session: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo,
                    intent: Intent,
                ): Boolean {
                    val isAppController = controllerInfo.packageName == packageName
                    val keyEvent = intent.extras?.getParcelable(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)

                    if (!isAppController && keyEvent?.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_NEXT,
                            KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> {
                                session.player.seekForward()
                                return true
                            }

                            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                            KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> {
                                session.player.seekBack()
                                return true
                            }
                        }
                    }

                    return super.onMediaButtonEvent(session, controllerInfo, intent)
                }
            })
            .build()
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

    /**
     * Keeps 10s / 30s seek for steering-wheel and Android Auto surfaces that would otherwise map
     * skip keys to the previous / next queue episode.
     */
    private fun withoutQueueNavigationCommands(available: Player.Commands): Player.Commands {
        return Player.Commands.Builder()
            .addAll(available)
            .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
            .remove(Player.COMMAND_SEEK_TO_NEXT)
            .build()
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

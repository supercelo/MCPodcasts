package com.example.mcpodcasts.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

internal class TogglePlaybackAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        withMediaController(context) { controller ->
            val wasPlaying = controller.isPlaying
            if (wasPlaying) controller.pause() else controller.play()
            refreshWidgetFromController(context, controller, overrideIsPlaying = !wasPlaying)
        }
    }
}

internal class SeekBackAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        withMediaController(context) { controller ->
            controller.seekBack()
            refreshWidgetFromController(context, controller)
        }
    }
}

internal class SeekForwardAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        withMediaController(context) { controller ->
            controller.seekForward()
            refreshWidgetFromController(context, controller)
        }
    }
}

internal class SkipPreviousEpisodeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        withMediaController(context) { controller ->
            controller.seekToPreviousMediaItem()
            refreshWidgetFromController(context, controller)
        }
    }
}

internal class SkipNextEpisodeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        withMediaController(context) { controller ->
            controller.seekToNextMediaItem()
            refreshWidgetFromController(context, controller)
        }
    }
}

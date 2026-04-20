package com.example.mcpodcasts.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Receiver for [PodcastPlayerWidget].
 *
 * We intentionally do NOT override onUpdate/onReceive to write state into the widget DataStore,
 * because Android frequently re-broadcasts APPWIDGET_UPDATE (e.g. after a launcher redraw or
 * after our own `.update()` call inside an action callback), and the in-app
 * [com.example.mcpodcasts.playback.PlayerConnection] `uiState` takes a few hundred ms to
 * reflect commands issued from the widget (pause/play are async). Letting the receiver push
 * `uiState.value` back into the DataStore right after a widget click would race against the
 * optimistic update performed by the action callback and roll the widget back to the pre-click
 * state. DataStore is kept in sync by:
 *   - [com.example.mcpodcasts.playback.PlayerConnection]'s widget-refresh collector whenever
 *     the in-app player state genuinely changes.
 *   - The widget action callbacks (see [WidgetActionCallbacks]) which call
 *     [refreshWidgetFromController] after issuing a command, covering the case where the app
 *     process isn't observing its own player.
 */
class PodcastPlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PodcastPlayerWidget()
}

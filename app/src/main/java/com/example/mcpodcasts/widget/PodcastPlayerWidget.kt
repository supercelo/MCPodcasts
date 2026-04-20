package com.example.mcpodcasts.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.mcpodcasts.MainActivity
import com.example.mcpodcasts.R

class PodcastPlayerWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val state = prefs.toWidgetState(context)
        provideContent {
            GlanceTheme {
                WidgetContent(state = state)
            }
        }
    }
}

@Composable
private fun WidgetContent(state: WidgetPlaybackState) {
    val context = LocalContext.current
    val openAppAction = actionStartActivity<MainActivity>()
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(openAppAction),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetArtwork(state = state)
            Spacer(modifier = GlanceModifier.width(10.dp))
            WidgetTexts(
                state = state,
                context = context,
                modifier = GlanceModifier.defaultWeight(),
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        ControlsRow(
            state = state,
            modifier = GlanceModifier.fillMaxWidth(),
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        ProgressBar(state)
    }
}

@Composable
private fun WidgetTexts(
    state: WidgetPlaybackState,
    context: Context,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(modifier = modifier) {
        Text(
            text = state.title.ifBlank { context.getString(R.string.widget_no_episode_title) },
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = state.podcastTitle
                .ifBlank { context.getString(R.string.widget_no_episode_subtitle) },
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp,
            ),
        )
    }
}

@Composable
private fun WidgetArtwork(state: WidgetPlaybackState) {
    val artwork = state.artwork
    val imageProvider = if (artwork != null) {
        ImageProvider(artwork)
    } else {
        ImageProvider(R.drawable.widget_artwork_placeholder)
    }
    Image(
        provider = imageProvider,
        contentDescription = state.title,
        contentScale = ContentScale.Crop,
        modifier = GlanceModifier
            .size(44.dp)
            .background(ImageProvider(R.drawable.widget_artwork_placeholder)),
    )
}

@Composable
private fun ProgressBar(state: WidgetPlaybackState) {
    val duration = state.durationMs.coerceAtLeast(0L)
    val position = state.positionMs.coerceIn(0L, duration)
    val progress = if (duration > 0L) position.toFloat() / duration.toFloat() else 0f
    LinearProgressIndicator(
        progress = progress.coerceIn(0f, 1f),
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(3.dp),
        color = GlanceTheme.colors.primary,
        backgroundColor = GlanceTheme.colors.surfaceVariant,
    )
}

@Composable
private fun ControlsRow(
    state: WidgetPlaybackState,
    modifier: GlanceModifier = GlanceModifier,
) {
    val iconTint = ColorFilter.tint(GlanceTheme.colors.onSurface)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ControlIcon(
                resId = R.drawable.ic_widget_skip_previous,
                contentDescription = R.string.cd_previous,
                tint = iconTint,
                onClickAction = actionRunCallback<SkipPreviousEpisodeAction>(),
            )
            ControlIcon(
                resId = R.drawable.ic_widget_replay_10,
                contentDescription = R.string.label_rewind_10,
                tint = iconTint,
                onClickAction = actionRunCallback<SeekBackAction>(),
            )
            ControlIcon(
                resId = R.drawable.ic_widget_play_pause,
                contentDescription = R.string.cd_play_pause,
                tint = iconTint,
                onClickAction = actionRunCallback<TogglePlaybackAction>(),
                size = 40.dp,
            )
            ControlIcon(
                resId = R.drawable.ic_widget_forward_30,
                contentDescription = R.string.label_forward_30,
                tint = iconTint,
                onClickAction = actionRunCallback<SeekForwardAction>(),
            )
            ControlIcon(
                resId = R.drawable.ic_widget_skip_next,
                contentDescription = R.string.cd_next,
                tint = iconTint,
                onClickAction = actionRunCallback<SkipNextEpisodeAction>(),
            )
        }
    }
}

@Composable
private fun ControlIcon(
    resId: Int,
    contentDescription: Int,
    tint: ColorFilter,
    onClickAction: androidx.glance.action.Action,
    size: androidx.compose.ui.unit.Dp = 34.dp,
) {
    val context = LocalContext.current
    Image(
        provider = ImageProvider(resId),
        contentDescription = context.getString(contentDescription),
        colorFilter = tint,
        modifier = GlanceModifier
            .size(size)
            .padding(4.dp)
            .clickable(onClickAction),
    )
}

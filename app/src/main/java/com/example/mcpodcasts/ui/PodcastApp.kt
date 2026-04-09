package com.example.mcpodcasts.ui

import androidx.compose.runtime.Composable
import com.example.mcpodcasts.ui.viewmodel.PlayerViewModel
import com.example.mcpodcasts.ui.viewmodel.PodcastsViewModel
import com.example.mcpodcasts.ui.viewmodel.SettingsViewModel

@Composable
fun PodcastApp(
    podcastsViewModel: PodcastsViewModel,
    playerViewModel: PlayerViewModel,
    settingsViewModel: SettingsViewModel,
) {
    PodcastAppContent(
        podcastsViewModel = podcastsViewModel,
        playerViewModel = playerViewModel,
        settingsViewModel = settingsViewModel,
    )
}

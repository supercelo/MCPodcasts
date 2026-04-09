package com.example.mcpodcasts

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mcpodcasts.ui.PodcastApp
import com.example.mcpodcasts.ui.theme.MCPodcastsTheme
import com.example.mcpodcasts.ui.viewmodel.PlayerViewModel
import com.example.mcpodcasts.ui.viewmodel.PodcastsViewModel
import com.example.mcpodcasts.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val settings = runBlocking {
            (application as MCPodcastsApplication).container.settingsRepository.getCurrentSettings()
        }
        AppCompatDelegate.setApplicationLocales(settings.appLanguage.toLocaleListCompat())
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory())
            val podcastsViewModel: PodcastsViewModel = viewModel(factory = PodcastsViewModel.factory())
            val playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory())
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            MCPodcastsTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
            ) {
                PodcastApp(
                    podcastsViewModel = podcastsViewModel,
                    playerViewModel = playerViewModel,
                    settingsViewModel = settingsViewModel,
                )
            }
        }
    }
}
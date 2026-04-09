package com.example.mcpodcasts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.ContextCompat
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
        requestNotificationPermissionIfNeeded(settings.syncSummaryNotificationsEnabled)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory())
            val podcastsViewModel: PodcastsViewModel = viewModel(factory = PodcastsViewModel.factory())
            val playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory())
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            MCPodcastsTheme(
                themeMode = settings.themeMode,
            ) {
                PodcastApp(
                    podcastsViewModel = podcastsViewModel,
                    playerViewModel = playerViewModel,
                    settingsViewModel = settingsViewModel,
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded(syncNotificationsEnabled: Boolean) {
        if (!syncNotificationsEnabled) {
            return
        }
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS_CODE)
    }

    private companion object {
        const val REQUEST_NOTIFICATIONS_CODE = 100
    }
}
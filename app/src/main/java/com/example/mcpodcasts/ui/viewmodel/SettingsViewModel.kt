package com.example.mcpodcasts.ui.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.mcpodcasts.data.settings.AppLanguage
import androidx.lifecycle.viewModelScope
import com.example.mcpodcasts.MCPodcastsApplication
import com.example.mcpodcasts.data.settings.AppSettings
import com.example.mcpodcasts.data.settings.SettingsRepository
import com.example.mcpodcasts.data.settings.ThemeMode
import com.example.mcpodcasts.work.PodcastSyncScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val repository: SettingsRepository,
) : AndroidViewModel(application) {
    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings(),
        )

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(themeMode)
        }
    }

    fun setAppLanguage(appLanguage: AppLanguage) {
        viewModelScope.launch {
            repository.setAppLanguage(appLanguage)
            AppCompatDelegate.setApplicationLocales(appLanguage.toLocaleListCompat())
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDynamicColor(enabled)
        }
    }

    fun setRefreshIntervalHours(hours: Int) {
        viewModelScope.launch {
            repository.setRefreshIntervalHours(hours)
            PodcastSyncScheduler.ensureScheduled(
                context = getApplication(),
                refreshIntervalHours = hours,
            )
        }
    }

    fun setSyncSummaryNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSyncSummaryNotificationsEnabled(enabled)
        }
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
                return SettingsViewModel(
                    application = application,
                    repository = app.container.settingsRepository,
                ) as T
            }
        }
    }
}

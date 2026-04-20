package com.example.mcpodcasts.ui.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.example.mcpodcasts.MCPodcastsApplication
import com.example.mcpodcasts.R
import com.example.mcpodcasts.data.backup.BackupManager
import com.example.mcpodcasts.data.settings.AppLanguage
import com.example.mcpodcasts.data.settings.QueueReadFilter
import com.example.mcpodcasts.data.settings.QueueSortOrder
import com.example.mcpodcasts.data.settings.AppSettings
import com.example.mcpodcasts.data.settings.SettingsRepository
import com.example.mcpodcasts.data.settings.ThemeMode
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val repository: SettingsRepository,
    private val backupManager: BackupManager,
) : AndroidViewModel(application) {
    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings(),
        )
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

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

    fun setSyncSummaryNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSyncSummaryNotificationsEnabled(enabled)
        }
    }

    fun setVolumeNormalizationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setVolumeNormalizationEnabled(enabled)
        }
    }

    fun setQueueSortOrder(order: QueueSortOrder) {
        viewModelScope.launch {
            repository.setQueueSortOrder(order)
        }
    }

    fun setQueueReadFilter(filter: QueueReadFilter) {
        viewModelScope.launch {
            repository.setQueueReadFilter(filter)
        }
    }

    fun setQueuePodcastFilterFeedUrl(feedUrl: String?) {
        viewModelScope.launch {
            repository.setQueuePodcastFilterFeedUrl(feedUrl)
        }
    }

    fun setCalendarReadFilter(filter: QueueReadFilter) {
        viewModelScope.launch {
            repository.setCalendarReadFilter(filter)
        }
    }

    fun setCalendarPodcastFilterFeedUrl(feedUrl: String?) {
        viewModelScope.launch {
            repository.setCalendarPodcastFilterFeedUrl(feedUrl)
        }
    }

    fun setSubscriptionFiltersEncoded(value: String) {
        viewModelScope.launch {
            repository.setSubscriptionFiltersEncoded(value)
        }
    }

    fun exportBackup(outputStream: OutputStream) {
        viewModelScope.launch {
            _isExporting.value = true
            runCatching {
                val json = backupManager.exportToJson()
                outputStream.use { stream ->
                    stream.writer(Charsets.UTF_8).use { writer ->
                        writer.write(json)
                    }
                }
            }.onSuccess {
                _message.value = getApplication<Application>().getString(R.string.msg_export_success)
            }.onFailure { error ->
                _message.value = error.message
                    ?: getApplication<Application>().getString(R.string.msg_export_failed)
            }
            _isExporting.value = false
        }
    }

    fun importBackup(inputStream: InputStream) {
        viewModelScope.launch {
            _isImporting.value = true
            runCatching {
                val json = inputStream.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).use { reader ->
                        reader.readText()
                    }
                }
                backupManager.importFromJson(json)
                val restoredSettings = repository.getCurrentSettings()
                AppCompatDelegate.setApplicationLocales(restoredSettings.appLanguage.toLocaleListCompat())
            }.onSuccess {
                _message.value = getApplication<Application>().getString(R.string.msg_import_success)
            }.onFailure { error ->
                _message.value = error.message
                    ?: getApplication<Application>().getString(R.string.msg_import_failed)
            }
            _isImporting.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun showErrorMessage(messageResId: Int) {
        _message.value = getApplication<Application>().getString(messageResId)
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
                    backupManager = app.container.backupManager,
                ) as T
            }
        }
    }
}

package com.example.mcpodcasts.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.example.mcpodcasts.MCPodcastsApplication
import com.example.mcpodcasts.R
import com.example.mcpodcasts.data.discovery.DiscoveredPodcast
import com.example.mcpodcasts.data.discovery.PodcastDiscoveryRepository
import com.example.mcpodcasts.data.local.CalendarEpisode
import com.example.mcpodcasts.data.local.QueueEpisode
import com.example.mcpodcasts.data.local.SubscriptionSummary
import com.example.mcpodcasts.data.repository.PodcastRepository
import com.example.mcpodcasts.data.settings.SettingsRepository
import com.example.mcpodcasts.work.PodcastSyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PodcastsViewModel(
    application: Application,
    private val repository: PodcastRepository,
    private val discoveryRepository: PodcastDiscoveryRepository,
    private val settingsRepository: SettingsRepository,
) : AndroidViewModel(application) {
    val queue: StateFlow<List<QueueEpisode>> = repository.observeQueue()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val subscriptions: StateFlow<List<SubscriptionSummary>> = repository.observeSubscriptions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val calendarEpisodes: StateFlow<List<CalendarEpisode>> = repository.observeCalendarEpisodes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _searchResults = MutableStateFlow<List<DiscoveredPodcast>>(emptyList())
    val searchResults: StateFlow<List<DiscoveredPodcast>> = _searchResults.asStateFlow()

    fun addSubscription(feedUrl: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.addSubscription(feedUrl)
                .onSuccess {
                    _message.value = getApplication<Application>().getString(R.string.msg_podcast_added_success)
                    val settings = settingsRepository.getCurrentSettings()
                    PodcastSyncScheduler.ensureScheduled(
                        context = getApplication(),
                        refreshIntervalHours = settings.refreshIntervalHours,
                    )
                }
                .onFailure { error ->
                    _message.value = error.message
                        ?: getApplication<Application>().getString(R.string.msg_add_feed_failed)
                }
            _isRefreshing.value = false
        }
    }

    fun refreshSubscriptions() {
        viewModelScope.launch {
            _isRefreshing.value = true
            runCatching {
                repository.refreshAllFeeds()
            }.onFailure { error ->
                _message.value = error.message
                    ?: getApplication<Application>().getString(R.string.msg_refresh_failed)
            }
            _isRefreshing.value = false
        }
    }

    fun searchPodcasts(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            discoveryRepository.search(query)
                .onSuccess { results ->
                    _searchResults.value = results
                    if (query.isNotBlank() && results.isEmpty()) {
                        _message.value = getApplication<Application>().getString(R.string.msg_no_podcasts_found)
                    }
                }
                .onFailure { error ->
                    _message.value = error.message
                        ?: getApplication<Application>().getString(R.string.msg_search_failed)
                }
            _isSearching.value = false
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    fun updateSubscriptionSettings(
        feedUrl: String,
        notifyNewEpisodes: Boolean,
        includeInQueue: Boolean,
        introSkipSeconds: Int,
        outroSkipSeconds: Int,
    ) {
        viewModelScope.launch {
            repository.updateSubscriptionSettings(
                feedUrl = feedUrl,
                notifyNewEpisodes = notifyNewEpisodes,
                includeInQueue = includeInQueue,
                introSkipSeconds = introSkipSeconds,
                outroSkipSeconds = outroSkipSeconds,
            )
            _message.value = getApplication<Application>()
                .getString(R.string.msg_subscription_preferences_updated)
        }
    }

    fun markEpisodeRead(
        episodeId: String,
        isRead: Boolean = true,
    ) {
        viewModelScope.launch {
            repository.markEpisodeRead(
                episodeId = episodeId,
                isRead = isRead,
            )
        }
    }

    fun clearMessage() {
        _message.value = null
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
                return PodcastsViewModel(
                    application = application,
                    repository = app.container.podcastRepository,
                    discoveryRepository = app.container.podcastDiscoveryRepository,
                    settingsRepository = app.container.settingsRepository,
                ) as T
            }
        }
    }
}

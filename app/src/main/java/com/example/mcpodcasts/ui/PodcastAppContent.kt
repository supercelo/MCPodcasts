package com.example.mcpodcasts.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Forward30
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.mcpodcasts.R
import com.example.mcpodcasts.data.discovery.DiscoveredPodcast
import com.example.mcpodcasts.data.local.CalendarEpisode
import com.example.mcpodcasts.data.local.QueueEpisode
import com.example.mcpodcasts.data.local.SubscriptionSummary
import com.example.mcpodcasts.data.settings.AppLanguage
import com.example.mcpodcasts.data.settings.AppSettings
import com.example.mcpodcasts.data.settings.ThemeMode
import com.example.mcpodcasts.domain.buildMonthRangeForEpisodes
import com.example.mcpodcasts.domain.formatDurationMsForLabel
import com.example.mcpodcasts.domain.toDurationMs
import com.example.mcpodcasts.playback.PlayerUiState
import com.example.mcpodcasts.ui.viewmodel.AppMainTab
import com.example.mcpodcasts.ui.viewmodel.PlayerViewModel
import com.example.mcpodcasts.ui.viewmodel.PodcastsViewModel
import com.example.mcpodcasts.ui.viewmodel.SettingsViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToLong
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.annotation.StringRes

private val IconScale = 1.1f
private val IconButtonShape = RoundedCornerShape(4.dp)
private val ReadStateStrokeWidth = 3.dp

private fun scaledDp(base: Dp): Dp = base * IconScale

private sealed class EpisodeDetailTarget {
    data class FromQueue(val episode: QueueEpisode) : EpisodeDetailTarget()
    data class FromCalendar(val episode: CalendarEpisode) : EpisodeDetailTarget()
}

private const val SubFilterRecordSep = '\u001f'
private const val SubFilterFieldSep = '\u001e'

private fun encodeSubscriptionFilters(map: Map<String, Pair<QueueReadFilter, Boolean>>): String =
    map.entries.joinToString(SubFilterRecordSep.toString()) { (k, v) ->
        "$k$SubFilterFieldSep${v.first.name}$SubFilterFieldSep${v.second}"
    }

private fun decodeSubscriptionFilters(encoded: String): Map<String, Pair<QueueReadFilter, Boolean>> =
    if (encoded.isEmpty()) {
        emptyMap()
    } else {
        encoded.split(SubFilterRecordSep).mapNotNull { part ->
            val bits = part.split(SubFilterFieldSep)
            if (bits.size == 3) {
                val filter = runCatching { QueueReadFilter.valueOf(bits[1]) }.getOrElse {
                    if (bits[1].toBoolean()) QueueReadFilter.Unread else QueueReadFilter.All
                }
                bits[0] to (filter to bits[2].toBoolean())
            } else {
                null
            }
        }.toMap()
    }

@StringRes
private fun AppMainTab.titleRes(): Int = when (this) {
    AppMainTab.Queue -> R.string.tab_queue
    AppMainTab.Calendar -> R.string.tab_calendar
    AppMainTab.Subscriptions -> R.string.tab_subscriptions
    AppMainTab.Settings -> R.string.tab_settings
}

private enum class QueueSortOrder {
    NewestFirst,
    OldestFirst,
}

private enum class QueueReadFilter {
    All,
    Unread,
    Read,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PodcastAppContent(
    podcastsViewModel: PodcastsViewModel,
    playerViewModel: PlayerViewModel,
    settingsViewModel: SettingsViewModel,
) {
    val queue by podcastsViewModel.queue.collectAsStateWithLifecycle()
    val calendarEpisodes by podcastsViewModel.calendarEpisodes.collectAsStateWithLifecycle()
    val subscriptions by podcastsViewModel.subscriptions.collectAsStateWithLifecycle()
    val searchResults by podcastsViewModel.searchResults.collectAsStateWithLifecycle()
    val isRefreshing by podcastsViewModel.isRefreshing.collectAsStateWithLifecycle()
    val isSearching by podcastsViewModel.isSearching.collectAsStateWithLifecycle()
    val message by podcastsViewModel.message.collectAsStateWithLifecycle()
    val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val selectedTab by podcastsViewModel.mainTab.collectAsStateWithLifecycle()

    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showPlayerSheet by rememberSaveable { mutableStateOf(false) }
    var selectedSubscriptionFeedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSubscriptionSettingsFeedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var queueSortOrder by rememberSaveable { mutableStateOf(QueueSortOrder.NewestFirst) }
    var queueReadFilter by rememberSaveable { mutableStateOf(QueueReadFilter.Unread) }
    var queuePodcastFilterFeedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var showQueuePodcastMenu by remember { mutableStateOf(false) }
    var calendarReadFilter by rememberSaveable { mutableStateOf(QueueReadFilter.All) }
    var calendarPodcastFilterFeedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var showCalendarPodcastMenu by remember { mutableStateOf(false) }
    var calendarSelectedDateIso by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var subscriptionFiltersEncoded by rememberSaveable { mutableStateOf("") }
    val subscriptionFilters = remember(subscriptionFiltersEncoded) {
        decodeSubscriptionFilters(subscriptionFiltersEncoded)
    }
    fun updateSubscriptionFilters(feedUrl: String, readFilter: QueueReadFilter, ascendingOrder: Boolean) {
        val next = decodeSubscriptionFilters(subscriptionFiltersEncoded).toMutableMap()
        next[feedUrl] = readFilter to ascendingOrder
        subscriptionFiltersEncoded = encodeSubscriptionFilters(next)
    }
    var addSearchQuery by rememberSaveable { mutableStateOf("") }
    var addManualUrl by rememberSaveable { mutableStateOf("") }
    var episodeDetailTarget by remember { mutableStateOf<EpisodeDetailTarget?>(null) }
    val selectedSubscription = subscriptions.firstOrNull { it.feedUrl == selectedSubscriptionSettingsFeedUrl }
    val selectedSubscriptionEpisodes = calendarEpisodes.filter { episode ->
        episode.podcastId == selectedSubscriptionFeedUrl
    }
    val queuePodcastOptions = remember(subscriptions) {
        subscriptions
            .filter(SubscriptionSummary::includeInQueue)
            .sortedBy { it.title.lowercase(Locale.getDefault()) }
    }
    val filteredCalendarEpisodes = remember(calendarEpisodes, calendarReadFilter, calendarPodcastFilterFeedUrl) {
        calendarEpisodes
            .asSequence()
            .filter { episode ->
                when (calendarReadFilter) {
                    QueueReadFilter.All -> true
                    QueueReadFilter.Unread -> !episode.isRead
                    QueueReadFilter.Read -> episode.isRead
                }
            }
            .filter { episode ->
                calendarPodcastFilterFeedUrl == null || episode.podcastId == calendarPodcastFilterFeedUrl
            }
            .toList()
    }
    val filteredQueue = remember(queue, queueSortOrder, queueReadFilter, queuePodcastFilterFeedUrl) {
        queue
            .asSequence()
            .filter { episode ->
                when (queueReadFilter) {
                    QueueReadFilter.All -> true
                    QueueReadFilter.Unread -> !episode.isRead
                    QueueReadFilter.Read -> episode.isRead
                }
            }
            .filter { episode ->
                queuePodcastFilterFeedUrl == null || episode.podcastId == queuePodcastFilterFeedUrl
            }
            .sortedWith(
                compareBy<QueueEpisode> { episode ->
                    when (queueSortOrder) {
                        QueueSortOrder.NewestFirst -> -episode.publishedAt
                        QueueSortOrder.OldestFirst -> episode.publishedAt
                    }
                }.thenBy { episode -> episode.title.lowercase(Locale.getDefault()) }
            )
            .toList()
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val playerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val subscriptionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val subscriptionSettingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val episodeDetailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val shouldShowMiniPlayer = playerState.hasMedia ||
        playerState.currentEpisodeId != null ||
        playerState.title.isNotBlank()

    LaunchedEffect(message) {
        message?.let { text ->
            snackbarHostState.showSnackbar(text)
            podcastsViewModel.clearMessage()
        }
    }

    LaunchedEffect(selectedTab) {
        episodeDetailTarget = null
    }

    LaunchedEffect(queuePodcastOptions, queuePodcastFilterFeedUrl) {
        if (queuePodcastFilterFeedUrl != null &&
            queuePodcastOptions.none { it.feedUrl == queuePodcastFilterFeedUrl }
        ) {
            queuePodcastFilterFeedUrl = null
        }
    }

    LaunchedEffect(subscriptions, calendarPodcastFilterFeedUrl) {
        if (calendarPodcastFilterFeedUrl != null &&
            subscriptions.none { it.feedUrl == calendarPodcastFilterFeedUrl }
        ) {
            calendarPodcastFilterFeedUrl = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(selectedTab.titleRes()))
                        Text(
                            text = when (selectedTab) {
                                AppMainTab.Queue -> pluralStringResource(
                                    R.plurals.topbar_queue_summary,
                                    filteredQueue.size,
                                    filteredQueue.size,
                                )
                                AppMainTab.Calendar -> stringResource(R.string.topbar_calendar_summary)
                                AppMainTab.Subscriptions -> pluralStringResource(
                                    R.plurals.topbar_subscriptions_summary,
                                    subscriptions.size,
                                    subscriptions.size,
                                )
                                AppMainTab.Settings -> stringResource(R.string.settings_subtitle)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    if (selectedTab == AppMainTab.Queue) {
                        IconButton(
                            onClick = {
                                queueSortOrder = when (queueSortOrder) {
                                    QueueSortOrder.NewestFirst -> QueueSortOrder.OldestFirst
                                    QueueSortOrder.OldestFirst -> QueueSortOrder.NewestFirst
                                }
                            },
                            shape = IconButtonShape,
                        ) {
                            Icon(
                                imageVector = when (queueSortOrder) {
                                    QueueSortOrder.NewestFirst -> Icons.Outlined.ArrowDownward
                                    QueueSortOrder.OldestFirst -> Icons.Outlined.ArrowUpward
                                },
                                contentDescription = stringResource(R.string.cd_sort_order),
                                modifier = Modifier.size(scaledDp(24.dp)),
                            )
                        }
                        IconButton(
                            onClick = { queueReadFilter = queueReadFilter.next() },
                            shape = IconButtonShape,
                        ) {
                            Icon(
                                imageVector = queueReadFilter.icon(),
                                contentDescription = stringResource(R.string.cd_filter_read_state),
                                modifier = Modifier.size(scaledDp(24.dp)),
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { showQueuePodcastMenu = true },
                                shape = IconButtonShape,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Podcasts,
                                    contentDescription = stringResource(R.string.cd_queue_podcast_filter),
                                    modifier = Modifier.size(scaledDp(24.dp)),
                                    tint = if (queuePodcastFilterFeedUrl != null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            DropdownMenu(
                                expanded = showQueuePodcastMenu,
                                onDismissRequest = { showQueuePodcastMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.filter_all_podcasts)) },
                                    onClick = {
                                        queuePodcastFilterFeedUrl = null
                                        showQueuePodcastMenu = false
                                    },
                                    trailingIcon = {
                                        if (queuePodcastFilterFeedUrl == null) {
                                            Icon(imageVector = Icons.Outlined.Done, contentDescription = null)
                                        }
                                    },
                                )
                                queuePodcastOptions.forEach { subscription ->
                                    DropdownMenuItem(
                                        text = { Text(subscription.title) },
                                        onClick = {
                                            queuePodcastFilterFeedUrl = subscription.feedUrl
                                            showQueuePodcastMenu = false
                                        },
                                        trailingIcon = {
                                            if (queuePodcastFilterFeedUrl == subscription.feedUrl) {
                                                Icon(imageVector = Icons.Outlined.Done, contentDescription = null)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                    if (selectedTab == AppMainTab.Calendar) {
                        IconButton(
                            onClick = { calendarReadFilter = calendarReadFilter.next() },
                            shape = IconButtonShape,
                        ) {
                            Icon(
                                imageVector = calendarReadFilter.icon(),
                                contentDescription = stringResource(R.string.cd_calendar_read_filter),
                                modifier = Modifier.size(scaledDp(24.dp)),
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { showCalendarPodcastMenu = true },
                                shape = IconButtonShape,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Podcasts,
                                    contentDescription = stringResource(R.string.cd_calendar_podcast_filter),
                                    modifier = Modifier.size(scaledDp(24.dp)),
                                    tint = if (calendarPodcastFilterFeedUrl != null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            DropdownMenu(
                                expanded = showCalendarPodcastMenu,
                                onDismissRequest = { showCalendarPodcastMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.filter_all_podcasts)) },
                                    onClick = {
                                        calendarPodcastFilterFeedUrl = null
                                        showCalendarPodcastMenu = false
                                    },
                                    trailingIcon = {
                                        if (calendarPodcastFilterFeedUrl == null) {
                                            Icon(imageVector = Icons.Outlined.Done, contentDescription = null)
                                        }
                                    },
                                )
                                subscriptions.forEach { subscription ->
                                    DropdownMenuItem(
                                        text = { Text(subscription.title) },
                                        onClick = {
                                            calendarPodcastFilterFeedUrl = subscription.feedUrl
                                            showCalendarPodcastMenu = false
                                        },
                                        trailingIcon = {
                                            if (calendarPodcastFilterFeedUrl == subscription.feedUrl) {
                                                Icon(imageVector = Icons.Outlined.Done, contentDescription = null)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                    if (selectedTab == AppMainTab.Subscriptions) {
                        val lastSyncedAt = remember(subscriptions) {
                            subscriptions.maxOfOrNull { it.lastSyncedAt } ?: 0L
                        }
                        if (lastSyncedAt > 0L) {
                            Text(
                                text = lastSyncedAt.asFriendlyDateTime(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 4.dp),
                            )
                        }
                        IconButton(
                            onClick = podcastsViewModel::refreshSubscriptions,
                            enabled = !isRefreshing,
                            shape = IconButtonShape,
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(scaledDp(20.dp)),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = stringResource(R.string.cd_refresh_feeds),
                                    modifier = Modifier.size(scaledDp(26.dp)),
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (selectedTab == AppMainTab.Subscriptions) {
                ExtendedFloatingActionButton(
                    onClick = { showAddSheet = true },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            modifier = Modifier.size(scaledDp(24.dp)),
                        )
                    },
                    text = { Text(stringResource(R.string.action_search_or_add)) },
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Column {
                if (shouldShowMiniPlayer) {
                    CompactMiniPlayer(
                        playerState = playerState,
                        onTogglePlayback = playerViewModel::togglePlayback,
                        onOpenPlayer = { showPlayerSheet = true },
                    )
                }

                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == AppMainTab.Queue,
                        onClick = { podcastsViewModel.setMainTab(AppMainTab.Queue) },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.QueueMusic,
                                contentDescription = null,
                                modifier = Modifier.size(scaledDp(26.dp)),
                            )
                        },
                        label = { Text(stringResource(R.string.tab_queue)) },
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppMainTab.Calendar,
                        onClick = { podcastsViewModel.setMainTab(AppMainTab.Calendar) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(scaledDp(26.dp)),
                            )
                        },
                        label = { Text(stringResource(R.string.tab_calendar)) },
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppMainTab.Subscriptions,
                        onClick = { podcastsViewModel.setMainTab(AppMainTab.Subscriptions) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Podcasts,
                                contentDescription = null,
                                modifier = Modifier.size(scaledDp(26.dp)),
                            )
                        },
                        label = { Text(stringResource(R.string.tab_subscriptions)) },
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppMainTab.Settings,
                        onClick = { podcastsViewModel.setMainTab(AppMainTab.Settings) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(scaledDp(26.dp)),
                            )
                        },
                        label = { Text(stringResource(R.string.tab_settings)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            AppMainTab.Queue -> CompactQueueScreen(
                queue = filteredQueue,
                hasEpisodes = queue.isNotEmpty(),
                isRefreshing = isRefreshing,
                onRefresh = podcastsViewModel::refreshSubscriptions,
                onPlayEpisode = { episodeId ->
                    playerViewModel.playQueue(filteredQueue, episodeId)
                },
                onOpenEpisodeDetail = { episode -> episodeDetailTarget = EpisodeDetailTarget.FromQueue(episode) },
                onMarkEpisodeRead = podcastsViewModel::markEpisodeRead,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            AppMainTab.Calendar -> CalendarScreen(
                episodes = filteredCalendarEpisodes,
                selectedDateIso = calendarSelectedDateIso,
                onSelectedDateIsoChange = { calendarSelectedDateIso = it },
                onPlayEpisode = playerViewModel::playCalendarEpisode,
                onOpenEpisodeDetail = { episode -> episodeDetailTarget = EpisodeDetailTarget.FromCalendar(episode) },
                onMarkEpisodeRead = podcastsViewModel::markEpisodeRead,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            AppMainTab.Subscriptions -> SubscriptionsScreenContent(
                subscriptions = subscriptions,
                onOpenSubscription = { feedUrl ->
                    selectedSubscriptionFeedUrl = feedUrl
                },
                onOpenSettings = { feedUrl ->
                    selectedSubscriptionSettingsFeedUrl = feedUrl
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            AppMainTab.Settings -> AppSettingsContent(
                settings = settings,
                onAppLanguageSelected = settingsViewModel::setAppLanguage,
                onThemeSelected = settingsViewModel::setThemeMode,
                onSyncSummaryNotificationsChanged = settingsViewModel::setSyncSummaryNotificationsEnabled,
                onVolumeNormalizationChanged = settingsViewModel::setVolumeNormalizationEnabled,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddSheet = false
                podcastsViewModel.clearSearchResults()
            },
            sheetState = addSheetState,
        ) {
            AddPodcastSheet(
                isSearching = isSearching,
                searchResults = searchResults,
                searchQuery = addSearchQuery,
                manualUrl = addManualUrl,
                onSearchQueryChange = { addSearchQuery = it },
                onManualUrlChange = { addManualUrl = it },
                onSearch = podcastsViewModel::searchPodcasts,
                onAddByUrl = { feedUrl ->
                    podcastsViewModel.addSubscription(feedUrl)
                    showAddSheet = false
                    podcastsViewModel.setMainTab(AppMainTab.Queue)
                    podcastsViewModel.clearSearchResults()
                },
                onSubscribeFromResult = { feedUrl ->
                    podcastsViewModel.addSubscription(feedUrl)
                    showAddSheet = false
                    podcastsViewModel.setMainTab(AppMainTab.Queue)
                    podcastsViewModel.clearSearchResults()
                },
            )
        }
    }

    if (selectedSubscriptionFeedUrl != null) {
        val subscription = subscriptions.firstOrNull { it.feedUrl == selectedSubscriptionFeedUrl }
        if (subscription != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedSubscriptionFeedUrl = null },
                sheetState = subscriptionSheetState,
            ) {
                SubscriptionEpisodesSheet(
                    subscription = subscription,
                    episodes = selectedSubscriptionEpisodes,
                    readFilter = subscriptionFilters[subscription.feedUrl]?.first ?: QueueReadFilter.All,
                    ascendingOrder = subscriptionFilters[subscription.feedUrl]?.second ?: false,
                    onReadFilterChange = { filter ->
                        val asc = subscriptionFilters[subscription.feedUrl]?.second ?: false
                        updateSubscriptionFilters(subscription.feedUrl, filter, asc)
                    },
                    onToggleSortOrder = {
                        val currentAsc = subscriptionFilters[subscription.feedUrl]?.second ?: false
                        val filter = subscriptionFilters[subscription.feedUrl]?.first ?: QueueReadFilter.All
                        updateSubscriptionFilters(subscription.feedUrl, filter, !currentAsc)
                    },
                    onBulkMarkEpisodes = { isRead ->
                        podcastsViewModel.markAllEpisodesReadForSubscription(
                            feedUrl = subscription.feedUrl,
                            isRead = isRead,
                        )
                    },
                    onPlayEpisode = playerViewModel::playCalendarEpisode,
                    onOpenEpisodeDetail = { episode ->
                        episodeDetailTarget = EpisodeDetailTarget.FromCalendar(episode)
                    },
                    onToggleRead = { episodeId, isRead ->
                        podcastsViewModel.markEpisodeRead(episodeId, isRead)
                    },
                    onOpenSettings = {
                        val feedUrl = subscription.feedUrl
                        selectedSubscriptionFeedUrl = null
                        selectedSubscriptionSettingsFeedUrl = feedUrl
                    },
                )
            }
        }
    }

    if (selectedSubscription != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedSubscriptionSettingsFeedUrl = null },
            sheetState = subscriptionSettingsSheetState,
        ) {
            SubscriptionPreferencesSheet(
                subscription = selectedSubscription,
                onSave = { notifyNewEpisodes, includeInQueue, introSkipSeconds, outroSkipSeconds ->
                    podcastsViewModel.updateSubscriptionSettings(
                        feedUrl = selectedSubscription.feedUrl,
                        notifyNewEpisodes = notifyNewEpisodes,
                        includeInQueue = includeInQueue,
                        introSkipSeconds = introSkipSeconds,
                        outroSkipSeconds = outroSkipSeconds,
                    )
                    selectedSubscriptionSettingsFeedUrl = null
                },
                onRemovePodcast = {
                    val feedUrl = selectedSubscription.feedUrl
                    podcastsViewModel.removeSubscription(feedUrl)
                    selectedSubscriptionSettingsFeedUrl = null
                    if (selectedSubscriptionFeedUrl == feedUrl) {
                        selectedSubscriptionFeedUrl = null
                    }
                    if (queuePodcastFilterFeedUrl == feedUrl) {
                        queuePodcastFilterFeedUrl = null
                    }
                    if (calendarPodcastFilterFeedUrl == feedUrl) {
                        calendarPodcastFilterFeedUrl = null
                    }
                },
            )
        }
    }

    if (showPlayerSheet && shouldShowMiniPlayer) {
        ModalBottomSheet(
            onDismissRequest = { showPlayerSheet = false },
            sheetState = playerSheetState,
        ) {
            ScrubbableNowPlayingSheet(
                playerState = playerState,
                onTogglePlayback = playerViewModel::togglePlayback,
                onSeekBack = playerViewModel::seekBack,
                onSeekForward = playerViewModel::seekForward,
                onSeekToPosition = playerViewModel::seekToPositionFromUser,
                onPrevious = playerViewModel::skipToPrevious,
                onNext = playerViewModel::skipToNext,
            )
        }
    }

    if (episodeDetailTarget != null) {
        ModalBottomSheet(
            onDismissRequest = { episodeDetailTarget = null },
            sheetState = episodeDetailSheetState,
        ) {
            when (val target = episodeDetailTarget!!) {
                is EpisodeDetailTarget.FromQueue -> {
                    val ep = target.episode
                    EpisodeDetailSheetContent(
                        title = ep.title,
                        podcastTitle = ep.podcastTitle,
                        artworkUrl = ep.artworkUrl,
                        summary = ep.summary,
                        publishedAt = ep.publishedAt,
                        durationLabel = episodeDurationForMetaLine(
                            durationLabel = ep.durationLabel,
                            durationMs = ep.durationMs,
                        ),
                        onPlay = {
                            playerViewModel.playQueue(filteredQueue, ep.episodeId)
                        },
                    )
                }
                is EpisodeDetailTarget.FromCalendar -> {
                    val ep = target.episode
                    EpisodeDetailSheetContent(
                        title = ep.title,
                        podcastTitle = ep.podcastTitle,
                        artworkUrl = ep.artworkUrl,
                        summary = ep.summary,
                        publishedAt = ep.publishedAt,
                        durationLabel = episodeDurationForMetaLine(
                            durationLabel = ep.durationLabel,
                            durationMs = ep.durationMs,
                        ),
                        onPlay = {
                            playerViewModel.playCalendarEpisode(ep)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactQueueScreen(
    queue: List<QueueEpisode>,
    hasEpisodes: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onPlayEpisode: (String) -> Unit,
    onOpenEpisodeDetail: (QueueEpisode) -> Unit,
    onMarkEpisodeRead: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier,
    ) {
        if (queue.isEmpty()) {
            AppEmptyState(
                title = stringResource(
                    if (hasEpisodes) R.string.queue_filtered_empty_title else R.string.queue_empty_title,
                ),
                subtitle = stringResource(
                    if (hasEpisodes) R.string.queue_filtered_empty_subtitle else R.string.queue_empty_subtitle,
                ),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = queue,
                    key = QueueEpisode::episodeId,
                ) { episode ->
                    CompactQueueCard(
                        episode = episode,
                        onPlayEpisode = { onPlayEpisode(episode.episodeId) },
                        onOpenDetail = { onOpenEpisodeDetail(episode) },
                        onToggleRead = { onMarkEpisodeRead(episode.episodeId, !episode.isRead) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactQueueCard(
    episode: QueueEpisode,
    onPlayEpisode: () -> Unit,
    onOpenDetail: () -> Unit,
    onToggleRead: () -> Unit,
) {
    val progress = if (episode.durationMs > 0L) {
        (episode.playbackPositionMs.toFloat() / episode.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val readAlpha = if (episode.isRead) 0.45f else 1f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenDetail),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = episode.artworkUrl,
                    contentDescription = episode.title,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .alpha(readAlpha),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = readAlpha),
                    )
                    Text(
                        text = episode.podcastTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = readAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildEpisodeMetaLine(
                            publishedAt = episode.publishedAt,
                            durationLabel = episodeDurationForMetaLine(
                                durationLabel = episode.durationLabel,
                                durationMs = episode.durationMs,
                            ),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = readAlpha),
                    )
                    if (progress > 0f) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            IconButton(
                onClick = onPlayEpisode,
                shape = IconButtonShape,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircle,
                    contentDescription = stringResource(R.string.cd_play_episode),
                    modifier = Modifier.size(scaledDp(30.dp)),
                )
            }
            ReadStateIconButton(
                isRead = episode.isRead,
                onClick = onToggleRead,
            )
        }
    }
}

@Composable
private fun EpisodeDetailSheetContent(
    title: String,
    podcastTitle: String,
    artworkUrl: String?,
    summary: String?,
    publishedAt: Long,
    durationLabel: String,
    onPlay: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.episode_detail_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            AsyncImage(
                model = artworkUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
        }
        item {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Text(
                text = podcastTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Text(
                text = buildEpisodeMetaLine(
                    publishedAt = publishedAt,
                    durationLabel = durationLabel,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Text(
                text = summary?.trim()?.takeIf { it.isNotEmpty() }
                    ?: stringResource(R.string.episode_detail_no_description),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item {
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
                shape = IconButtonShape,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.size(scaledDp(24.dp)),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.episode_detail_play))
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarScreen(
    episodes: List<CalendarEpisode>,
    selectedDateIso: String,
    onSelectedDateIsoChange: (String) -> Unit,
    onPlayEpisode: (CalendarEpisode) -> Unit,
    onOpenEpisodeDetail: (CalendarEpisode) -> Unit,
    onMarkEpisodeRead: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (episodes.isEmpty()) {
        AppEmptyState(
            title = stringResource(R.string.calendar_empty_title),
            subtitle = stringResource(R.string.calendar_empty_subtitle),
            modifier = modifier,
        )
        return
    }

    val months = remember(episodes) { episodes.buildMonthRange() }
    val selectedDate = remember(selectedDateIso) {
        runCatching { LocalDate.parse(selectedDateIso) }.getOrElse { LocalDate.now() }
    }
    val initialPage = remember(months, selectedDate) {
        val idx = months.indexOf(YearMonth.from(selectedDate))
        when {
            idx >= 0 -> idx
            else -> months.indexOf(YearMonth.now()).takeIf { it >= 0 }
                ?: (months.lastIndex / 2).coerceAtLeast(0)
        }
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { months.size },
    )

    val episodesByDate = remember(episodes) {
        episodes.groupBy { episode ->
            Instant.ofEpochMilli(episode.publishedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }

    LaunchedEffect(months, selectedDateIso) {
        if (months.isEmpty()) return@LaunchedEffect
        val parsed = runCatching { LocalDate.parse(selectedDateIso) }.getOrNull() ?: return@LaunchedEffect
        val target = months.indexOf(YearMonth.from(parsed))
        if (target >= 0 && target != pagerState.currentPage) {
            pagerState.scrollToPage(target)
        }
    }

    LaunchedEffect(pagerState.currentPage, months, episodesByDate, selectedDateIso) {
        if (months.isEmpty()) return@LaunchedEffect
        val currentPageMonth = months[pagerState.currentPage]
        val parsed = LocalDate.parse(selectedDateIso)
        if (parsed.year != currentPageMonth.year || parsed.month != currentPageMonth.month) {
            val fallbackDate = episodesByDate.keys
                .filter { date -> YearMonth.from(date) == currentPageMonth }
                .sorted()
                .firstOrNull()
                ?: currentPageMonth.atDay(1)
            onSelectedDateIsoChange(fallbackDate.toString())
        }
    }

    val selectedDayEpisodes = episodesByDate[selectedDate].orEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                val month = months[page]
                CalendarMonthPage(
                    month = month,
                    episodesByDate = episodesByDate,
                    selectedDate = selectedDate,
                    onSelectDate = { date -> onSelectedDateIsoChange(date.toString()) },
                )
            }
        }
        item {
            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (selectedDayEpisodes.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Text(
                        text = stringResource(R.string.calendar_empty_day),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(
                items = selectedDayEpisodes.sortedByDescending(CalendarEpisode::publishedAt),
                key = CalendarEpisode::episodeId,
            ) { episode ->
                CalendarEpisodeCard(
                    episode = episode,
                    onPlayEpisode = { onPlayEpisode(episode) },
                    onOpenDetail = { onOpenEpisodeDetail(episode) },
                    onToggleReadState = {
                        onMarkEpisodeRead(episode.episodeId, !episode.isRead)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun CalendarMonthPage(
    month: YearMonth,
    episodesByDate: Map<LocalDate, List<CalendarEpisode>>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
) {
    val firstDayOfMonth = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val leadingEmptySlots = firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value
    val slots = buildList<LocalDate?> {
        repeat(leadingEmptySlots.coerceAtLeast(0)) {
            add(null)
        }
        repeat(daysInMonth) { dayOffset ->
            add(month.atDay(dayOffset + 1))
        }
        while (size % 7 != 0) {
            add(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.values().forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        slots.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                week.forEach { date ->
                    val count = date?.let { episodesByDate[it]?.size } ?: 0
                    val isSelected = date == selectedDate
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            count > 0 -> MaterialTheme.colorScheme.surfaceContainerHigh
                            else -> MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        onClick = {
                            date?.let(onSelectDate)
                        },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (date == null) {
                                Text(text = "")
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (count > 0) {
                                        Badge(
                                            modifier = Modifier.border(1.5.dp, Color.White, CircleShape),
                                            containerColor = Color(0xFF1D3091),
                                            contentColor = Color.White,
                                        ) {
                                            Text(
                                                text = count.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 3.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarEpisodeCard(
    episode: CalendarEpisode,
    onPlayEpisode: () -> Unit,
    onOpenDetail: () -> Unit,
    onToggleReadState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val readAlpha = if (episode.isRead) 0.45f else 1f
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenDetail),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = episode.artworkUrl,
                    contentDescription = episode.title,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .alpha(readAlpha),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = readAlpha),
                    )
                    Text(
                        text = episode.podcastTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = readAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildEpisodeMetaLine(
                            publishedAt = episode.publishedAt,
                            durationLabel = episodeDurationForMetaLine(
                                durationLabel = episode.durationLabel,
                                durationMs = episode.durationMs,
                            ),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = readAlpha),
                    )
                }
            }
            IconButton(
                onClick = onPlayEpisode,
                shape = IconButtonShape,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircle,
                    contentDescription = stringResource(R.string.cd_play_episode),
                    modifier = Modifier.size(scaledDp(28.dp)),
                )
            }
            ReadStateIconButton(
                isRead = episode.isRead,
                onClick = onToggleReadState,
            )
        }
    }
}

@Composable
private fun SubscriptionsScreenContent(
    subscriptions: List<SubscriptionSummary>,
    onOpenSubscription: (String) -> Unit,
    onOpenSettings: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (subscriptions.isEmpty()) {
        AppEmptyState(
            title = stringResource(R.string.subscriptions_empty_title),
            subtitle = stringResource(R.string.subscriptions_empty_subtitle),
            modifier = modifier,
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = subscriptions,
            key = SubscriptionSummary::feedUrl,
        ) { subscription ->
            SubscriptionGridItem(
                subscription = subscription,
                onClick = { onOpenSubscription(subscription.feedUrl) },
            )
        }
    }
}

@Composable
private fun SubscriptionGridItem(
    subscription: SubscriptionSummary,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = subscription.imageUrl,
            contentDescription = subscription.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (subscription.unreadCount > 0) {
            Badge(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .border(1.5.dp, Color.White, CircleShape),
                containerColor = Color(0xFF1D3091),
                contentColor = Color.White,
            ) {
                Text(
                    text = subscription.unreadCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun CompactMiniPlayer(
    playerState: PlayerUiState,
    onTogglePlayback: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPlayer)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = playerState.artworkUrl,
                    contentDescription = playerState.title,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playerState.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = playerState.podcastTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onTogglePlayback,
                    shape = IconButtonShape,
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) {
                            Icons.Outlined.PauseCircle
                        } else {
                            Icons.Outlined.PlayCircle
                        },
                        contentDescription = stringResource(R.string.cd_play_pause),
                        modifier = Modifier.size(scaledDp(34.dp)),
                    )
                }
            }
            if (playerState.durationMs > 0L) {
                LinearProgressIndicator(
                    progress = {
                        (playerState.positionMs.toFloat() / playerState.durationMs.toFloat())
                            .coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AddPodcastSheet(
    isSearching: Boolean,
    searchResults: List<DiscoveredPodcast>,
    searchQuery: String,
    manualUrl: String,
    onSearchQueryChange: (String) -> Unit,
    onManualUrlChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onAddByUrl: (String) -> Unit,
    onSubscribeFromResult: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.discover_podcasts_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.discover_podcasts_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.search_podcasts_label)) },
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    TextButton(
                        onClick = { onSearch(searchQuery) },
                        enabled = searchQuery.isNotBlank() && !isSearching,
                    ) {
                        Text(stringResource(R.string.action_search))
                    }
                }
                if (isSearching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        items(searchResults, key = DiscoveredPodcast::feedUrl) { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = result.imageUrl,
                        contentDescription = result.title,
                        modifier = Modifier
                            .size(62.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = result.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = result.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(onClick = { onSubscribeFromResult(result.feedUrl) }) {
                        Text(stringResource(R.string.action_subscribe))
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.add_by_url_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = manualUrl,
                    onValueChange = onManualUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.rss_url_label)) },
                    singleLine = true,
                )
                TextButton(
                    onClick = { onAddByUrl(manualUrl) },
                    enabled = manualUrl.isNotBlank(),
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.action_add))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppSettingsContent(
    settings: AppSettings,
    onAppLanguageSelected: (AppLanguage) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onSyncSummaryNotificationsChanged: (Boolean) -> Unit,
    onVolumeNormalizationChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_theme_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { onThemeSelected(mode) },
                            label = { Text(stringResource(mode.themeLabelRes())) },
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
                Text(
                    text = stringResource(R.string.settings_language_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = settings.appLanguage == AppLanguage.System,
                        onClick = { onAppLanguageSelected(AppLanguage.System) },
                        label = { Text(stringResource(AppLanguage.System.labelRes())) },
                    )
                }
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppLanguage.entries.filter { it != AppLanguage.System }.forEach { language ->
                        FilterChip(
                            selected = settings.appLanguage == language,
                            onClick = { onAppLanguageSelected(language) },
                            label = { Text(stringResource(language.labelRes())) },
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_sync_notification_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.settings_sync_notification_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Switch(
                        checked = settings.syncSummaryNotificationsEnabled,
                        onCheckedChange = onSyncSummaryNotificationsChanged,
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_volume_normalization_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.settings_volume_normalization_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Switch(
                        checked = settings.volumeNormalizationEnabled,
                        onCheckedChange = onVolumeNormalizationChanged,
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun SubscriptionEpisodesSheet(
    subscription: SubscriptionSummary,
    episodes: List<CalendarEpisode>,
    readFilter: QueueReadFilter,
    ascendingOrder: Boolean,
    onReadFilterChange: (QueueReadFilter) -> Unit,
    onToggleSortOrder: () -> Unit,
    onBulkMarkEpisodes: (Boolean) -> Unit,
    onPlayEpisode: (CalendarEpisode) -> Unit,
    onOpenEpisodeDetail: (CalendarEpisode) -> Unit,
    onToggleRead: (String, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var showBulkReadDialog by remember { mutableStateOf(false) }
    val visibleEpisodes = remember(episodes, readFilter, ascendingOrder) {
        episodes
            .filter { episode ->
                when (readFilter) {
                    QueueReadFilter.All -> true
                    QueueReadFilter.Unread -> !episode.isRead
                    QueueReadFilter.Read -> episode.isRead
                }
            }
            .sortedBy(CalendarEpisode::publishedAt)
            .let { list -> if (ascendingOrder) list else list.reversed() }
    }
    val hasEpisodes = episodes.isNotEmpty()

    if (showBulkReadDialog) {
        AlertDialog(
            onDismissRequest = { showBulkReadDialog = false },
            title = {
                Text(text = stringResource(R.string.subscription_bulk_read_title))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = stringResource(R.string.subscription_bulk_read_message))
                    TextButton(
                        onClick = {
                            onBulkMarkEpisodes(true)
                            showBulkReadDialog = false
                        },
                    ) {
                        Text(text = stringResource(R.string.subscription_bulk_mark_all_read))
                    }
                    TextButton(
                        onClick = {
                            onBulkMarkEpisodes(false)
                            showBulkReadDialog = false
                        },
                    ) {
                        Text(text = stringResource(R.string.subscription_bulk_mark_all_unread))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBulkReadDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = subscription.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenSettings, shape = IconButtonShape) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = stringResource(R.string.cd_subscription_preferences),
                    modifier = Modifier.size(scaledDp(24.dp)),
                )
            }
        }
        subscription.author?.takeIf(String::isNotBlank)?.let { author ->
            Text(
                text = author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        subscription.description?.trim()?.takeIf(String::isNotEmpty)?.let { desc ->
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onReadFilterChange(readFilter.next()) },
                shape = IconButtonShape,
            ) {
                Icon(
                    imageVector = readFilter.icon(),
                    contentDescription = stringResource(R.string.cd_filter_read_state),
                    modifier = Modifier.size(scaledDp(22.dp)),
                )
            }
            IconButton(
                onClick = onToggleSortOrder,
                shape = IconButtonShape,
            ) {
                Icon(
                    imageVector = if (ascendingOrder) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                    contentDescription = stringResource(R.string.cd_sort_order),
                    modifier = Modifier.size(scaledDp(22.dp)),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            FilterChip(
                selected = false,
                onClick = {
                    if (hasEpisodes) {
                        showBulkReadDialog = true
                    }
                },
                enabled = hasEpisodes,
                label = { Text(stringResource(R.string.subscription_bulk_read_action)) },
            )
        }
        if (visibleEpisodes.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    text = stringResource(R.string.no_filtered_episodes),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = visibleEpisodes,
                    key = CalendarEpisode::episodeId,
                ) { episode ->
                    CalendarEpisodeCard(
                        episode = episode,
                        onPlayEpisode = { onPlayEpisode(episode) },
                        onOpenDetail = { onOpenEpisodeDetail(episode) },
                        onToggleReadState = {
                            onToggleRead(episode.episodeId, !episode.isRead)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ReadStateCheckmark(
    color: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = strokeWidth.toPx()
        val leg1Start = Offset(w * 0.22f, h * 0.52f)
        val leg1End = Offset(w * 0.44f, h * 0.72f)
        val leg2End = Offset(w * 0.78f, h * 0.30f)
        drawLine(
            color = color,
            start = leg1Start,
            end = leg1End,
            strokeWidth = sw,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = leg1End,
            end = leg2End,
            strokeWidth = sw,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun ReadStateIconButton(
    isRead: Boolean,
    onClick: () -> Unit,
) {
    val readStateDescription = stringResource(
        if (isRead) R.string.cd_mark_as_unread else R.string.cd_mark_as_read,
    )
    val strokeColor = MaterialTheme.colorScheme.outline
    IconButton(
        onClick = onClick,
        shape = IconButtonShape,
    ) {
        Surface(
            modifier = Modifier
                .size(scaledDp(24.dp))
                .semantics {
                    contentDescription = readStateDescription
                },
            shape = IconButtonShape,
            color = Color.Transparent,
            border = BorderStroke(ReadStateStrokeWidth, strokeColor),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isRead) {
                    ReadStateCheckmark(
                        color = strokeColor,
                        strokeWidth = ReadStateStrokeWidth,
                        modifier = Modifier.size(scaledDp(17.6.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionPreferencesSheet(
    subscription: SubscriptionSummary,
    onSave: (Boolean, Boolean, Int, Int) -> Unit,
    onRemovePodcast: () -> Unit,
) {
    var showRemoveDialog by remember { mutableStateOf(false) }
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(stringResource(R.string.subscription_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.subscription_delete_message,
                        subscription.title,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemovePodcast()
                        showRemoveDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.subscription_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
    var notifyNewEpisodes by rememberSaveable(subscription.feedUrl) {
        mutableStateOf(subscription.notifyNewEpisodes)
    }
    var includeInQueue by rememberSaveable(subscription.feedUrl) {
        mutableStateOf(subscription.includeInQueue)
    }
    var introSkipSeconds by rememberSaveable(subscription.feedUrl) {
        mutableFloatStateOf(subscription.introSkipSeconds.toFloat())
    }
    var outroSkipSeconds by rememberSaveable(subscription.feedUrl) {
        mutableFloatStateOf(subscription.outroSkipSeconds.toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = subscription.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.subscription_preferences_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.subscription_notify_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.subscription_notify_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = notifyNewEpisodes,
                    onCheckedChange = { notifyNewEpisodes = it },
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.subscription_queue_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.subscription_queue_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = includeInQueue,
                    onCheckedChange = { includeInQueue = it },
                )
            }
        }
        Text(
            text = stringResource(R.string.subscription_skip_intro, introSkipSeconds.roundToInt()),
            style = MaterialTheme.typography.titleMedium,
        )
        Slider(
            value = introSkipSeconds,
            onValueChange = { introSkipSeconds = it },
            valueRange = 0f..120f,
            steps = 23,
        )
        Text(
            text = stringResource(R.string.subscription_skip_outro, outroSkipSeconds.roundToInt()),
            style = MaterialTheme.typography.titleMedium,
        )
        Slider(
            value = outroSkipSeconds,
            onValueChange = { outroSkipSeconds = it },
            valueRange = 0f..120f,
            steps = 23,
        )
        TextButton(
            onClick = {
                onSave(
                    notifyNewEpisodes,
                    includeInQueue,
                    introSkipSeconds.roundToInt(),
                    outroSkipSeconds.roundToInt(),
                )
            },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(stringResource(R.string.action_save))
        }
        TextButton(
            onClick = { showRemoveDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text(stringResource(R.string.subscription_delete_podcast))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ScrubbableNowPlayingSheet(
    playerState: PlayerUiState,
    onTogglePlayback: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekToPosition: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var sliderFraction by remember(playerState.currentEpisodeId) { mutableFloatStateOf(0f) }
    val descriptionScrollState = rememberScrollState()

    LaunchedEffect(playerState.positionMs, playerState.durationMs, isScrubbing) {
        if (!isScrubbing && playerState.durationMs > 0L) {
            sliderFraction = (playerState.positionMs.toFloat() / playerState.durationMs.toFloat())
                .coerceIn(0f, 1f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.95f)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AsyncImage(
                model = playerState.artworkUrl,
                contentDescription = playerState.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = playerState.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = playerState.podcastTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                playerState.publishedAtMs?.takeIf { it > 0L }?.let { publishedAt ->
                    Text(
                        text = publishedAt.asFriendlyDate(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            playerState.summary?.trim()?.takeIf { it.isNotEmpty() }?.let { summary ->
                Text(
                    text = summary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .verticalScroll(descriptionScrollState),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Slider(
                value = sliderFraction,
                onValueChange = {
                    isScrubbing = true
                    sliderFraction = it
                },
                onValueChangeFinished = {
                    val position = (sliderFraction * playerState.durationMs).roundToLong()
                    onSeekToPosition(position)
                    isScrubbing = false
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = (sliderFraction * playerState.durationMs).roundToLong().asDurationLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = playerState.durationMs.asDurationLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPrevious,
                    shape = IconButtonShape,
                    modifier = Modifier.size(scaledDp(76.dp)),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SkipPrevious,
                        contentDescription = stringResource(R.string.cd_previous),
                        modifier = Modifier.size(scaledDp(48.dp)),
                    )
                }
                IconButton(
                    onClick = onSeekBack,
                    shape = IconButtonShape,
                    modifier = Modifier.size(scaledDp(76.dp)),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Replay10,
                        contentDescription = stringResource(R.string.label_rewind_10),
                        modifier = Modifier.size(scaledDp(46.dp)),
                    )
                }
                IconButton(
                    onClick = onTogglePlayback,
                    shape = IconButtonShape,
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) {
                            Icons.Outlined.PauseCircle
                        } else {
                            Icons.Outlined.PlayCircle
                        },
                        contentDescription = stringResource(R.string.cd_play_pause),
                        modifier = Modifier.size(scaledDp(72.dp)),
                    )
                }
                IconButton(
                    onClick = onSeekForward,
                    shape = IconButtonShape,
                    modifier = Modifier.size(scaledDp(76.dp)),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Forward30,
                        contentDescription = stringResource(R.string.label_forward_30),
                        modifier = Modifier.size(scaledDp(46.dp)),
                    )
                }
                IconButton(
                    onClick = onNext,
                    shape = IconButtonShape,
                    modifier = Modifier.size(scaledDp(76.dp)),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SkipNext,
                        contentDescription = stringResource(R.string.cd_next),
                        modifier = Modifier.size(scaledDp(48.dp)),
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AppEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AssistChip(
                onClick = {},
                label = { Text(stringResource(R.string.label_brand)) },
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun List<CalendarEpisode>.buildMonthRange(): List<YearMonth> {
    val publishedDates = map { episode ->
        Instant.ofEpochMilli(episode.publishedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    return buildMonthRangeForEpisodes(publishedDates)
}

@Composable
private fun episodeDurationForMetaLine(durationLabel: String?, durationMs: Long): String {
    val none = stringResource(R.string.no_duration)
    if (durationMs > 0L) {
        return formatDurationMsForLabel(durationMs).ifEmpty { none }
    }
    val trimmed = durationLabel?.trim().orEmpty()
    if (trimmed.isEmpty()) {
        return none
    }
    val parsedMs = trimmed.toDurationMs()
    return if (parsedMs > 0L) {
        formatDurationMsForLabel(parsedMs)
    } else {
        trimmed
    }
}

@Composable
private fun buildEpisodeMetaLine(
    publishedAt: Long,
    durationLabel: String,
): String {
    return "${publishedAt.asFriendlyDate()} • $durationLabel"
}

@StringRes
private fun QueueReadFilter.labelRes(): Int {
    return when (this) {
        QueueReadFilter.All -> R.string.filter_read_state_all
        QueueReadFilter.Unread -> R.string.filter_unread_only
        QueueReadFilter.Read -> R.string.filter_read_only
    }
}

private fun QueueReadFilter.next(): QueueReadFilter = when (this) {
    QueueReadFilter.All -> QueueReadFilter.Unread
    QueueReadFilter.Unread -> QueueReadFilter.Read
    QueueReadFilter.Read -> QueueReadFilter.All
}

private fun QueueReadFilter.icon() = when (this) {
    QueueReadFilter.All -> Icons.Outlined.FilterAlt
    QueueReadFilter.Unread -> Icons.Outlined.Hearing
    QueueReadFilter.Read -> Icons.Outlined.DoneAll
}

@StringRes
private fun AppLanguage.labelRes(): Int {
    return when (this) {
        AppLanguage.System -> R.string.language_system
        AppLanguage.Portuguese -> R.string.language_portuguese
        AppLanguage.English -> R.string.language_english
        AppLanguage.French -> R.string.language_french
        AppLanguage.Spanish -> R.string.language_spanish
        AppLanguage.Italian -> R.string.language_italian
        AppLanguage.German -> R.string.language_german
    }
}

private fun AppLanguage.settingsCode(): String =
    if (this == AppLanguage.System) {
        "SYS"
    } else {
        languageTag.uppercase(Locale.ROOT)
    }

@StringRes
private fun ThemeMode.themeCodeRes(): Int =
    when (this) {
        ThemeMode.System -> R.string.theme_code_system
        ThemeMode.Light -> R.string.theme_code_light
        ThemeMode.Dark -> R.string.theme_code_dark
    }

@StringRes
private fun ThemeMode.themeLabelRes(): Int =
    when (this) {
        ThemeMode.System -> R.string.theme_system
        ThemeMode.Light -> R.string.theme_light
        ThemeMode.Dark -> R.string.theme_dark
    }

@Composable
private fun Long.asFriendlyDate(): String {
    if (this <= 0L) {
        return stringResource(R.string.no_date)
    }

    return DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
        .format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
}

@Composable
private fun Long.asFriendlyDateTime(): String {
    if (this <= 0L) {
        return stringResource(R.string.now)
    }

    return DateTimeFormatter.ofPattern("d MMM yyyy • HH:mm", Locale.getDefault())
        .format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
}

private fun Long.asDurationLabel(): String {
    return formatDurationMsForLabel(this).ifEmpty { "--" }
}

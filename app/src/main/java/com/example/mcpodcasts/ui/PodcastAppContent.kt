package com.example.mcpodcasts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Forward30
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.example.mcpodcasts.playback.PlayerUiState
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

private enum class MainTab(@param:StringRes val titleRes: Int) {
    Queue(R.string.tab_queue),
    Calendar(R.string.tab_calendar),
    Subscriptions(R.string.tab_subscriptions),
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

    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Queue) }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showPlayerSheet by rememberSaveable { mutableStateOf(false) }
    var selectedSubscriptionFeedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSubscriptionSettingsFeedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedSubscription = subscriptions.firstOrNull { it.feedUrl == selectedSubscriptionSettingsFeedUrl }
    val selectedSubscriptionEpisodes = calendarEpisodes.filter { episode ->
        episode.podcastId == selectedSubscriptionFeedUrl
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val playerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val subscriptionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(selectedTab.titleRes))
                        Text(
                            text = when (selectedTab) {
                                MainTab.Queue -> pluralStringResource(
                                    R.plurals.topbar_queue_summary,
                                    queue.size,
                                    queue.size,
                                )
                                MainTab.Calendar -> stringResource(R.string.topbar_calendar_summary)
                                MainTab.Subscriptions -> pluralStringResource(
                                    R.plurals.topbar_subscriptions_summary,
                                    subscriptions.size,
                                    subscriptions.size,
                                )
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
                    if (selectedTab == MainTab.Subscriptions) {
                        IconButton(
                            onClick = podcastsViewModel::refreshSubscriptions,
                            enabled = !isRefreshing,
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = stringResource(R.string.cd_refresh_feeds),
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (selectedTab == MainTab.Subscriptions) {
                ExtendedFloatingActionButton(
                    onClick = { showAddSheet = true },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
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
                        selected = selectedTab == MainTab.Queue,
                        onClick = { selectedTab = MainTab.Queue },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.QueueMusic,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                            )
                        },
                        label = { Text(stringResource(R.string.tab_queue)) },
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainTab.Calendar,
                        onClick = { selectedTab = MainTab.Calendar },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                            )
                        },
                        label = { Text(stringResource(R.string.tab_calendar)) },
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainTab.Subscriptions,
                        onClick = { selectedTab = MainTab.Subscriptions },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Podcasts,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                            )
                        },
                        label = { Text(stringResource(R.string.tab_subscriptions)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            MainTab.Queue -> CompactQueueScreen(
                queue = queue,
                onPlayEpisode = { episodeId ->
                    playerViewModel.playQueue(queue, episodeId)
                },
                onMarkEpisodeRead = podcastsViewModel::markEpisodeRead,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            MainTab.Calendar -> CalendarScreen(
                episodes = calendarEpisodes,
                onPlayEpisode = playerViewModel::playCalendarEpisode,
                onMarkEpisodeRead = podcastsViewModel::markEpisodeRead,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            MainTab.Subscriptions -> SubscriptionsScreenContent(
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
                onSearch = podcastsViewModel::searchPodcasts,
                onAddByUrl = { feedUrl ->
                    podcastsViewModel.addSubscription(feedUrl)
                    showAddSheet = false
                    selectedTab = MainTab.Queue
                    podcastsViewModel.clearSearchResults()
                },
                onSubscribeFromResult = { feedUrl ->
                    podcastsViewModel.addSubscription(feedUrl)
                    showAddSheet = false
                    selectedTab = MainTab.Queue
                    podcastsViewModel.clearSearchResults()
                },
            )
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = settingsSheetState,
        ) {
            AppSettingsSheet(
                settings = settings,
                onAppLanguageSelected = settingsViewModel::setAppLanguage,
                onThemeSelected = settingsViewModel::setThemeMode,
                onRefreshIntervalSelected = settingsViewModel::setRefreshIntervalHours,
                onSyncSummaryNotificationsChanged = settingsViewModel::setSyncSummaryNotificationsEnabled,
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
                    onPlayEpisode = playerViewModel::playCalendarEpisode,
                    onToggleRead = { episodeId, isRead ->
                        podcastsViewModel.markEpisodeRead(episodeId, isRead)
                    },
                )
            }
        }
    }

    if (selectedSubscription != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedSubscriptionSettingsFeedUrl = null },
            sheetState = subscriptionSheetState,
        ) {
            SubscriptionPreferencesSheet(
                subscription = selectedSubscription,
                onSave = { notifyNewEpisodes, introSkipSeconds, outroSkipSeconds ->
                    podcastsViewModel.updateSubscriptionSettings(
                        feedUrl = selectedSubscription.feedUrl,
                        notifyNewEpisodes = notifyNewEpisodes,
                        introSkipSeconds = introSkipSeconds,
                        outroSkipSeconds = outroSkipSeconds,
                    )
                    selectedSubscriptionSettingsFeedUrl = null
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
                onSeekToPosition = playerViewModel::seekToPosition,
                onPrevious = playerViewModel::skipToPrevious,
                onNext = playerViewModel::skipToNext,
            )
        }
    }
}

@Composable
private fun CompactQueueScreen(
    queue: List<QueueEpisode>,
    onPlayEpisode: (String) -> Unit,
    onMarkEpisodeRead: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (queue.isEmpty()) {
        AppEmptyState(
            title = stringResource(R.string.queue_empty_title),
            subtitle = stringResource(R.string.queue_empty_subtitle),
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = queue,
            key = QueueEpisode::episodeId,
        ) { episode ->
            CompactQueueCard(
                episode = episode,
                onPlayEpisode = { onPlayEpisode(episode.episodeId) },
                onMarkRead = { onMarkEpisodeRead(episode.episodeId) },
            )
        }
    }
}

@Composable
private fun CompactQueueCard(
    episode: QueueEpisode,
    onPlayEpisode: () -> Unit,
    onMarkRead: () -> Unit,
) {
    val progress = if (episode.durationMs > 0L) {
        (episode.playbackPositionMs.toFloat() / episode.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

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
                .clickable(onClick = onPlayEpisode)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = episode.artworkUrl,
                contentDescription = episode.title,
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
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
                )
                Text(
                    text = episode.podcastTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildEpisodeMetaLine(
                        publishedAt = episode.publishedAt,
                        durationLabel = episode.durationLabel.orEmpty().ifBlank { episode.durationMs.asDurationLabel() },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            IconButton(onClick = onPlayEpisode) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircle,
                    contentDescription = stringResource(R.string.cd_play_episode),
                    modifier = Modifier.size(30.dp),
                )
            }
            ReadStateIconButton(
                isRead = false,
                onClick = onMarkRead,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarScreen(
    episodes: List<CalendarEpisode>,
    onPlayEpisode: (CalendarEpisode) -> Unit,
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
    val currentMonth = remember { YearMonth.now() }
    val initialPage = remember(months) {
        months.indexOf(currentMonth).takeIf { it >= 0 } ?: (months.lastIndex / 2).coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { months.size },
    )
    var selectedDateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val selectedDate = remember(selectedDateText) { LocalDate.parse(selectedDateText) }

    val episodesByDate = remember(episodes) {
        episodes.groupBy { episode ->
            Instant.ofEpochMilli(episode.publishedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }

    LaunchedEffect(pagerState.currentPage, months, episodesByDate) {
        val currentPageMonth = months[pagerState.currentPage]
        if (selectedDate.year != currentPageMonth.year || selectedDate.month != currentPageMonth.month) {
            val fallbackDate = episodesByDate.keys
                .filter { date -> YearMonth.from(date) == currentPageMonth }
                .sorted()
                .firstOrNull()
                ?: currentPageMonth.atDay(1)
            selectedDateText = fallbackDate.toString()
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
                    onSelectDate = { date -> selectedDateText = date.toString() },
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
                                        Text(
                                            text = count.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
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

@Composable
private fun CalendarEpisodeCard(
    episode: CalendarEpisode,
    onPlayEpisode: () -> Unit,
    onToggleReadState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onPlayEpisode),
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
            AsyncImage(
                model = episode.artworkUrl,
                contentDescription = episode.title,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = episode.podcastTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildEpisodeMetaLine(
                        publishedAt = episode.publishedAt,
                        durationLabel = episode.durationLabel.orEmpty().ifBlank { stringResource(R.string.no_duration) },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onPlayEpisode) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircle,
                    contentDescription = stringResource(R.string.cd_play_episode),
                    modifier = Modifier.size(28.dp),
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

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = subscriptions,
            key = SubscriptionSummary::feedUrl,
        ) { subscription ->
            SubscriptionCardExpanded(
                subscription = subscription,
                onOpenSubscription = { onOpenSubscription(subscription.feedUrl) },
                onOpenSettings = { onOpenSettings(subscription.feedUrl) },
            )
        }
    }
}

@Composable
private fun SubscriptionCardExpanded(
    subscription: SubscriptionSummary,
    onOpenSubscription: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onOpenSubscription),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = subscription.imageUrl,
                contentDescription = subscription.title,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subscription.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                subscription.author?.takeIf(String::isNotBlank)?.let { author ->
                    Text(
                        text = author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.subscription_episode_summary,
                        subscription.episodeCount,
                        subscription.lastSyncedAt.asFriendlyDateTime(),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (subscription.notifyNewEpisodes) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = stringResource(R.string.cd_notifications_enabled),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = stringResource(R.string.cd_subscription_preferences),
                    modifier = Modifier.size(26.dp),
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
                IconButton(onClick = onTogglePlayback) {
                    Icon(
                        imageVector = if (playerState.isPlaying) {
                            Icons.Outlined.PauseCircle
                        } else {
                            Icons.Outlined.PlayCircle
                        },
                        contentDescription = stringResource(R.string.cd_play_pause),
                        modifier = Modifier.size(34.dp),
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
    onSearch: (String) -> Unit,
    onAddByUrl: (String) -> Unit,
    onSubscribeFromResult: (String) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var manualUrl by rememberSaveable { mutableStateOf("") }

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
                        onValueChange = { searchQuery = it },
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
                    onValueChange = { manualUrl = it },
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
private fun AppSettingsSheet(
    settings: AppSettings,
    onAppLanguageSelected: (AppLanguage) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onRefreshIntervalSelected: (Int) -> Unit,
    onSyncSummaryNotificationsChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.settings_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.settings_language_title),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppLanguage.entries.forEach { appLanguage ->
                FilterChip(
                    selected = settings.appLanguage == appLanguage,
                    onClick = { onAppLanguageSelected(appLanguage) },
                    label = { Text(stringResource(appLanguage.labelRes())) },
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_theme_title),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.themeMode == mode,
                    onClick = { onThemeSelected(mode) },
                    label = {
                        Text(
                            text = when (mode) {
                                ThemeMode.System -> stringResource(R.string.theme_system)
                                ThemeMode.Light -> stringResource(R.string.theme_light)
                                ThemeMode.Dark -> stringResource(R.string.theme_dark)
                            }
                        )
                    },
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
                        text = stringResource(R.string.settings_sync_notification_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.settings_sync_notification_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.syncSummaryNotificationsEnabled,
                    onCheckedChange = onSyncSummaryNotificationsChanged,
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_refresh_title),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf(1, 3, 6, 12, 24).forEach { hours ->
                FilterChip(
                    selected = settings.refreshIntervalHours == hours,
                    onClick = { onRefreshIntervalSelected(hours) },
                    label = {
                        Text(
                            pluralStringResource(
                                R.plurals.hours_label,
                                hours,
                                hours,
                            )
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SubscriptionEpisodesSheet(
    subscription: SubscriptionSummary,
    episodes: List<CalendarEpisode>,
    onPlayEpisode: (CalendarEpisode) -> Unit,
    onToggleRead: (String, Boolean) -> Unit,
) {
    var unreadOnly by rememberSaveable(subscription.feedUrl) { mutableStateOf(false) }
    var ascendingOrder by rememberSaveable(subscription.feedUrl) { mutableStateOf(false) }

    val visibleEpisodes = remember(episodes, unreadOnly, ascendingOrder) {
        episodes
            .filter { episode -> !unreadOnly || !episode.isRead }
            .sortedBy(CalendarEpisode::publishedAt)
            .let { list -> if (ascendingOrder) list else list.reversed() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = subscription.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilterChip(
                selected = unreadOnly,
                onClick = { unreadOnly = !unreadOnly },
                label = { Text(stringResource(R.string.filter_unread_only)) },
            )
            FilterChip(
                selected = ascendingOrder,
                onClick = { ascendingOrder = !ascendingOrder },
                label = {
                    Text(
                        stringResource(
                            if (ascendingOrder) R.string.sort_date_asc else R.string.sort_date_desc,
                        )
                    )
                },
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
private fun ReadStateIconButton(
    isRead: Boolean,
    onClick: () -> Unit,
) {
    val readStateDescription = stringResource(
        if (isRead) R.string.cd_mark_as_unread else R.string.cd_mark_as_read,
    )
    val strokeColor = if (isRead) Color(0xFF87F35C) else Color(0xFFFF5B5B)
    IconButton(onClick = onClick) {
        Surface(
            modifier = Modifier
                .size(24.dp)
                .semantics {
                    contentDescription = readStateDescription
                },
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(2.5.dp, strokeColor),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isRead) {
                    Icon(
                        imageVector = Icons.Outlined.Done,
                        contentDescription = readStateDescription,
                        tint = strokeColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionPreferencesSheet(
    subscription: SubscriptionSummary,
    onSave: (Boolean, Int, Int) -> Unit,
) {
    var notifyNewEpisodes by rememberSaveable(subscription.feedUrl) {
        mutableStateOf(subscription.notifyNewEpisodes)
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
                    introSkipSeconds.roundToInt(),
                    outroSkipSeconds.roundToInt(),
                )
            },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(stringResource(R.string.action_save))
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

    LaunchedEffect(playerState.positionMs, playerState.durationMs, isScrubbing) {
        if (!isScrubbing && playerState.durationMs > 0L) {
            sliderFraction = (playerState.positionMs.toFloat() / playerState.durationMs.toFloat())
                .coerceIn(0f, 1f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
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
        }
        Column(modifier = Modifier.fillMaxWidth()) {
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
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Outlined.SkipPrevious,
                    contentDescription = stringResource(R.string.cd_previous),
                    modifier = Modifier.size(34.dp),
                )
            }
            PlayerTransportButton(
                icon = Icons.Outlined.Replay10,
                label = stringResource(R.string.label_rewind_10),
                onClick = onSeekBack,
            )
            IconButton(onClick = onTogglePlayback) {
                Icon(
                    imageVector = if (playerState.isPlaying) {
                        Icons.Outlined.PauseCircle
                    } else {
                        Icons.Outlined.PlayCircle
                    },
                    contentDescription = stringResource(R.string.cd_play_pause),
                    modifier = Modifier.size(72.dp),
                )
            }
            PlayerTransportButton(
                icon = Icons.Outlined.Forward30,
                label = stringResource(R.string.label_forward_30),
                onClick = onSeekForward,
            )
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Outlined.SkipNext,
                    contentDescription = stringResource(R.string.cd_next),
                    modifier = Modifier.size(34.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PlayerTransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(60.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(30.dp),
            )
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
private fun buildEpisodeMetaLine(
    publishedAt: Long,
    durationLabel: String,
): String {
    return "${publishedAt.asFriendlyDate()} • $durationLabel"
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

@Composable
private fun Long.asFriendlyDate(): String {
    if (this <= 0L) {
        return stringResource(R.string.no_date)
    }

    return DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        .format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
}

@Composable
private fun Long.asFriendlyDateTime(): String {
    if (this <= 0L) {
        return stringResource(R.string.now)
    }

    return DateTimeFormatter.ofPattern("d MMM • HH:mm", Locale.getDefault())
        .format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
}

private fun Long.asDurationLabel(): String {
    if (this <= 0L) {
        return "--:--"
    }

    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

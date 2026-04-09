package com.example.mcpodcasts.domain

import com.example.mcpodcasts.data.local.EpisodeEntity
import com.example.mcpodcasts.data.rss.ParsedEpisode
import java.security.MessageDigest
import java.time.LocalDate
import java.time.YearMonth

internal fun normalizeRefreshIntervalHours(hours: Int): Long {
    return hours.coerceAtLeast(1).toLong()
}

internal fun buildMonthRangeForEpisodes(
    publishedDates: List<LocalDate>,
    currentMonth: YearMonth = YearMonth.now(),
): List<YearMonth> {
    val minMonth = publishedDates.minOrNull()?.let(YearMonth::from) ?: currentMonth
    val maxMonth = maxOf(publishedDates.maxOrNull()?.let(YearMonth::from) ?: currentMonth, currentMonth)

    return buildList {
        var cursor = minMonth
        while (!cursor.isAfter(maxMonth)) {
            add(cursor)
            cursor = cursor.plusMonths(1)
        }
    }
}

internal fun ParsedEpisode.mergeWithExistingEpisode(
    podcastId: String,
    fallbackArtworkUrl: String?,
    existingEpisode: EpisodeEntity?,
): EpisodeEntity {
    val parsedDurationMs = durationLabel.toDurationMs()

    return EpisodeEntity(
        episodeId = stableId(),
        podcastId = podcastId,
        guid = guid,
        title = title,
        summary = summary,
        audioUrl = audioUrl,
        artworkUrl = artworkUrl ?: fallbackArtworkUrl,
        episodeUrl = episodeUrl,
        publishedAt = publishedAt,
        durationLabel = durationLabel,
        durationMs = parsedDurationMs.takeIf { it > 0L } ?: existingEpisode?.durationMs ?: 0L,
        playbackPositionMs = existingEpisode?.playbackPositionMs ?: 0L,
        isRead = existingEpisode?.isRead ?: false,
        isCompleted = existingEpisode?.isCompleted ?: false,
    )
}

internal fun ParsedEpisode.stableId(): String {
    val stableIdSource = guid?.takeIf(String::isNotBlank) ?: audioUrl
    return stableIdSource.sha256()
}

internal fun String?.toDurationMs(): Long {
    if (this.isNullOrBlank()) {
        return 0L
    }

    val parts = trim().split(":").mapNotNull { it.toLongOrNull() }
    if (parts.isEmpty()) {
        return 0L
    }

    val seconds = when (parts.size) {
        1 -> parts[0]
        2 -> (parts[0] * 60) + parts[1]
        else -> (parts[0] * 3600) + (parts[1] * 60) + parts[2]
    }
    return seconds * 1000
}

private fun String.sha256(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(toByteArray())
    return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

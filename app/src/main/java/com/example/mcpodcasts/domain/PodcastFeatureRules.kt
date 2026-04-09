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
    val mergedDurationMs = parsedDurationMs.takeIf { it > 0L } ?: existingEpisode?.durationMs ?: 0L
    val mergedDurationLabel = resolveDurationLabelForStorage(
        rawFromFeed = durationLabel,
        parsedDurationMs = parsedDurationMs,
        mergedDurationMs = mergedDurationMs,
        existingLabel = existingEpisode?.durationLabel,
    )

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
        durationLabel = mergedDurationLabel,
        durationMs = mergedDurationMs,
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

/**
 * RSS often sends `<itunes:duration>` as total seconds only (e.g. "4978"). Store a clock label for UI;
 * keep feed strings that already look like H:MM:SS or MM:SS.
 */
internal fun formatDurationMsForLabel(durationMs: Long): String {
    if (durationMs <= 0L) {
        return ""
    }

    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun resolveDurationLabelForStorage(
    rawFromFeed: String?,
    parsedDurationMs: Long,
    mergedDurationMs: Long,
    existingLabel: String?,
): String? {
    val rawTrimmed = rawFromFeed?.trim().orEmpty()

    if (parsedDurationMs > 0L) {
        return when {
            rawTrimmed.isEmpty() -> formatDurationMsForLabel(parsedDurationMs)
            rawTrimmed.all { it.isDigit() } -> formatDurationMsForLabel(parsedDurationMs)
            else -> rawTrimmed
        }
    }

    if (mergedDurationMs > 0L) {
        val existingTrimmed = existingLabel?.trim().orEmpty()
        return when {
            existingTrimmed.isNotEmpty() && !existingTrimmed.all { it.isDigit() } -> existingTrimmed
            else -> formatDurationMsForLabel(mergedDurationMs)
        }
    }

    return rawTrimmed.takeIf { it.isNotEmpty() }
}

private fun String.sha256(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(toByteArray())
    return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

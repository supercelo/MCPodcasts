package com.example.mcpodcasts

import com.example.mcpodcasts.data.local.EpisodeEntity
import com.example.mcpodcasts.data.rss.ParsedEpisode
import com.example.mcpodcasts.domain.buildMonthRangeForEpisodes
import com.example.mcpodcasts.domain.formatDurationMsForLabel
import com.example.mcpodcasts.domain.mergeWithExistingEpisode
import com.example.mcpodcasts.domain.normalizeRefreshIntervalHours
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastFeatureRulesTest {
    @Test
    fun normalizeRefreshIntervalHours_enforcesMinimumOneHour() {
        assertEquals(1L, normalizeRefreshIntervalHours(0))
        assertEquals(1L, normalizeRefreshIntervalHours(-5))
        assertEquals(6L, normalizeRefreshIntervalHours(6))
    }

    @Test
    fun buildMonthRangeForEpisodes_spansFromFirstEpisodeToCurrentMonth() {
        val currentMonth = YearMonth.of(2026, 4)
        val range = buildMonthRangeForEpisodes(
            publishedDates = listOf(
                LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 3, 22),
            ),
            currentMonth = currentMonth,
        )

        assertEquals(
            listOf(
                YearMonth.of(2026, 1),
                YearMonth.of(2026, 2),
                YearMonth.of(2026, 3),
                YearMonth.of(2026, 4),
            ),
            range,
        )
    }

    @Test
    fun mergeWithExistingEpisode_preservesUserStateAndDuration() {
        val existingEpisode = EpisodeEntity(
            episodeId = "episode-1",
            podcastId = "feed",
            guid = "guid-1",
            title = "Existing",
            summary = "summary",
            audioUrl = "https://example.com/audio.mp3",
            artworkUrl = "https://example.com/image.jpg",
            episodeUrl = "https://example.com",
            publishedAt = 10L,
            durationLabel = "01:00:00",
            durationMs = 3_600_000L,
            playbackPositionMs = 120_000L,
            isRead = true,
            isCompleted = true,
        )
        val parsedEpisode = ParsedEpisode(
            guid = "guid-1",
            title = "Updated title",
            summary = "updated summary",
            audioUrl = "https://example.com/audio.mp3",
            artworkUrl = null,
            episodeUrl = "https://example.com/new",
            publishedAt = 20L,
            durationLabel = null,
        )

        val merged = parsedEpisode.mergeWithExistingEpisode(
            podcastId = "feed",
            fallbackArtworkUrl = "https://example.com/fallback.jpg",
            existingEpisode = existingEpisode,
        )

        assertEquals(existingEpisode.playbackPositionMs, merged.playbackPositionMs)
        assertEquals(existingEpisode.durationMs, merged.durationMs)
        assertTrue(merged.isRead)
        assertTrue(merged.isCompleted)
        assertEquals("Updated title", merged.title)
        assertEquals("https://example.com/fallback.jpg", merged.artworkUrl)
    }

    @Test
    fun mergeWithExistingEpisode_formatsItunesDurationAsTotalSeconds() {
        val parsedEpisode = ParsedEpisode(
            guid = "g",
            title = "Episode",
            summary = null,
            audioUrl = "https://example.com/a.mp3",
            artworkUrl = null,
            episodeUrl = null,
            publishedAt = 1L,
            durationLabel = "4978",
        )

        val merged = parsedEpisode.mergeWithExistingEpisode(
            podcastId = "feed",
            fallbackArtworkUrl = null,
            existingEpisode = null,
        )

        assertEquals(4_978_000L, merged.durationMs)
        assertEquals("01:22:58", merged.durationLabel)
    }

    @Test
    fun formatDurationMsForLabel_alwaysUsesHhMmSsWithColons() {
        assertEquals("00:01:30", formatDurationMsForLabel(90_000L))
        assertEquals("00:00:05", formatDurationMsForLabel(5_000L))
        assertEquals("01:00:00", formatDurationMsForLabel(3_600_000L))
        assertEquals("02:03:04", formatDurationMsForLabel(7_384_000L))
    }
}

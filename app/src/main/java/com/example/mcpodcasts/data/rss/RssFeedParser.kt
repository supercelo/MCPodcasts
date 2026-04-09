package com.example.mcpodcasts.data.rss

import android.text.Html
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class ParsedFeed(
    val title: String,
    val author: String?,
    val description: String?,
    val imageUrl: String?,
    val siteUrl: String?,
    val episodes: List<ParsedEpisode>,
)

data class ParsedEpisode(
    val guid: String?,
    val title: String,
    val summary: String?,
    val audioUrl: String,
    val artworkUrl: String?,
    val episodeUrl: String?,
    val publishedAt: Long,
    val durationLabel: String?,
)

class RssFeedParser {
    fun parse(inputStream: InputStream): ParsedFeed {
        val parser = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }.newPullParser()

        parser.setInput(inputStream, null)

        var channelTitle = ""
        var channelAuthor: String? = null
        var channelDescription: String? = null
        var channelImageUrl: String? = null
        var channelSiteUrl: String? = null
        var insideItem = false
        var insideChannelImage = false
        var episodes = mutableListOf<ParsedEpisode>()

        var episodeGuid: String? = null
        var episodeTitle = ""
        var episodeSummary: String? = null
        var episodeAudioUrl: String? = null
        var episodeImageUrl: String? = null
        var episodeLink: String? = null
        var episodePublishedAt = 0L
        var episodeDuration: String? = null

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name
                    val prefix = parser.prefix.orEmpty()

                    when {
                        tagName == "item" -> {
                            insideItem = true
                            episodeGuid = null
                            episodeTitle = ""
                            episodeSummary = null
                            episodeAudioUrl = null
                            episodeImageUrl = null
                            episodeLink = null
                            episodePublishedAt = 0L
                            episodeDuration = null
                        }

                        !insideItem && tagName == "image" && prefix != "itunes" -> {
                            insideChannelImage = true
                        }

                        !insideItem && tagName == "title" && channelTitle.isBlank() -> {
                            channelTitle = parser.readText()
                        }

                        !insideItem && tagName == "description" && channelDescription.isNullOrBlank() -> {
                            channelDescription = parser.readCleanText()
                        }

                        !insideItem && tagName == "link" && channelSiteUrl.isNullOrBlank() -> {
                            channelSiteUrl = parser.readText()
                        }

                        !insideItem && tagName == "author" && channelAuthor.isNullOrBlank() -> {
                            channelAuthor = parser.readText()
                        }

                        !insideItem && prefix == "itunes" && tagName == "author" && channelAuthor.isNullOrBlank() -> {
                            channelAuthor = parser.readText()
                        }

                        !insideItem && insideChannelImage && tagName == "url" && channelImageUrl.isNullOrBlank() -> {
                            channelImageUrl = parser.readText()
                        }

                        !insideItem && prefix == "itunes" && tagName == "image" && channelImageUrl.isNullOrBlank() -> {
                            channelImageUrl = parser.getAttributeValue(null, "href")
                        }

                        insideItem && tagName == "guid" -> {
                            episodeGuid = parser.readText()
                        }

                        insideItem && tagName == "title" -> {
                            episodeTitle = parser.readText()
                        }

                        insideItem && tagName == "description" -> {
                            episodeSummary = parser.readCleanText()
                        }

                        insideItem && tagName == "summary" -> {
                            episodeSummary = parser.readCleanText()
                        }

                        insideItem && tagName == "link" -> {
                            episodeLink = parser.readText()
                        }

                        insideItem && tagName == "pubDate" -> {
                            episodePublishedAt = parsePublishedAt(parser.readText())
                        }

                        insideItem && tagName == "published" -> {
                            episodePublishedAt = parsePublishedAt(parser.readText())
                        }

                        insideItem && tagName == "updated" && episodePublishedAt == 0L -> {
                            episodePublishedAt = parsePublishedAt(parser.readText())
                        }

                        insideItem && tagName == "enclosure" -> {
                            episodeAudioUrl = parser.getAttributeValue(null, "url")
                        }

                        insideItem && prefix == "itunes" && tagName == "duration" -> {
                            episodeDuration = parser.readText()
                        }

                        insideItem && prefix == "itunes" && tagName == "image" -> {
                            episodeImageUrl = parser.getAttributeValue(null, "href")
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "image" -> insideChannelImage = false
                        "item" -> {
                            insideItem = false

                            val audioUrl = episodeAudioUrl
                            if (!audioUrl.isNullOrBlank() && episodeTitle.isNotBlank()) {
                                episodes.add(
                                    ParsedEpisode(
                                        guid = episodeGuid,
                                        title = episodeTitle,
                                        summary = episodeSummary,
                                        audioUrl = audioUrl,
                                        artworkUrl = episodeImageUrl ?: channelImageUrl,
                                        episodeUrl = episodeLink,
                                        publishedAt = episodePublishedAt,
                                        durationLabel = episodeDuration,
                                    )
                                )
                            }
                        }
                    }
                }
            }

            parser.next()
        }

        return ParsedFeed(
            title = channelTitle.ifBlank { "Podcast" },
            author = channelAuthor,
            description = channelDescription,
            imageUrl = channelImageUrl,
            siteUrl = channelSiteUrl,
            episodes = episodes,
        )
    }

    private fun parsePublishedAt(value: String): Long {
        if (value.isBlank()) {
            return 0L
        }

        val trimmed = value.trim()

        return runCatching {
            ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME)
                .toInstant()
                .toEpochMilli()
        }.recoverCatching {
            OffsetDateTime.parse(trimmed).toInstant().toEpochMilli()
        }.recoverCatching {
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
                .parse(trimmed)
                ?.time
                ?: 0L
        }.getOrDefault(0L)
    }

    private fun XmlPullParser.readText(): String {
        if (next() != XmlPullParser.TEXT) {
            return ""
        }

        val value = text.orEmpty()
        nextTag()
        return value.trim()
    }

    private fun XmlPullParser.readCleanText(): String {
        val rawText = readText()
        if (rawText.isBlank()) {
            return rawText
        }

        return Html.fromHtml(rawText, Html.FROM_HTML_MODE_COMPACT)
            .toString()
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

package com.example.mcpodcasts.data.discovery

import android.content.Context
import com.example.mcpodcasts.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class DiscoveredPodcast(
    val feedUrl: String,
    val title: String,
    val author: String,
    val imageUrl: String?,
)

class PodcastDiscoveryRepository(
    private val context: Context,
    private val httpClient: OkHttpClient,
) {
    suspend fun search(query: String): Result<List<DiscoveredPodcast>> = runCatching {
        withContext(Dispatchers.IO) {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isBlank()) {
                return@withContext emptyList()
            }

            val url = ITUNES_SEARCH_URL.toHttpUrl().newBuilder()
                .addQueryParameter("term", normalizedQuery)
                .addQueryParameter("media", "podcast")
                .addQueryParameter("entity", "podcast")
                .addQueryParameter("limit", "25")
                .build()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Podcast MC/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                error(context.getString(R.string.error_search_podcasts, response.code))
            }

            val body = response.use { networkResponse ->
                networkResponse.body?.string()
            }.orEmpty()

            val results = JSONObject(body).optJSONArray("results")
            buildList {
                if (results == null) {
                    return@buildList
                }

                for (index in 0 until results.length()) {
                    val item = results.optJSONObject(index) ?: continue
                    val feedUrl = item.optString("feedUrl")
                    val title = item.optString("collectionName")
                    if (feedUrl.isNullOrBlank() || title.isNullOrBlank()) {
                        continue
                    }

                    add(
                        DiscoveredPodcast(
                            feedUrl = feedUrl,
                            title = title,
                            author = item.optString("artistName"),
                            imageUrl = item.optString("artworkUrl600")
                                .ifBlank { item.optString("artworkUrl100").ifBlank { null } },
                        )
                    )
                }
            }
        }
    }

    private companion object {
        const val ITUNES_SEARCH_URL = "https://itunes.apple.com/search"
    }
}

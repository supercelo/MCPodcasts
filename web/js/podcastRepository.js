function normalizeFeedUrl(feedUrl) {
  const trimmed = String(feedUrl || "").trim();
  if (!trimmed) {
    throw new Error("Enter a valid RSS feed URL.");
  }
  return trimmed.replace(/^feed:\/\//i, "https://");
}

function compareByPublishedAndTitleDesc(a, b) {
  if (b.publishedAt !== a.publishedAt) {
    return b.publishedAt - a.publishedAt;
  }
  return String(a.title || "").localeCompare(String(b.title || ""), undefined, { sensitivity: "base" });
}

function compareByPublishedAndTitleAsc(a, b) {
  if (a.publishedAt !== b.publishedAt) {
    return a.publishedAt - b.publishedAt;
  }
  return String(a.title || "").localeCompare(String(b.title || ""), undefined, { sensitivity: "base" });
}

function buildNewEpisodesSummary(feedUrl, podcast, newEpisodeTitles, shouldNotify) {
  return {
    feedUrl,
    podcastTitle: podcast.title,
    imageUrl: podcast.imageUrl,
    episodeTitles: shouldNotify ? newEpisodeTitles : [],
  };
}

export function createPodcastRepository({ db, rules, rssParser }) {
  const listeners = new Set();

  function subscribe(listener) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  }

  function emit() {
    listeners.forEach((listener) => listener());
  }

  async function getPodcast(feedUrl) {
    return db.get("podcasts", feedUrl);
  }

  async function getEpisodesForPodcast(podcastId) {
    return db.getManyByIndex("episodes", "by_podcast_id", podcastId);
  }

  async function fetchFeed(feedUrl) {
    const proxiedUrl = `/proxy?url=${encodeURIComponent(feedUrl)}`;
    const response = await fetch(proxiedUrl, {
      headers: {
        "User-Agent": "Podcast MC/1.0",
      },
    });
    if (!response.ok) {
      throw new Error(`Couldn't fetch the feed (${response.status}).`);
    }
    return response.text();
  }

  async function syncFeed(feedUrl, notifyForNewEpisodes) {
    const normalizedUrl = normalizeFeedUrl(feedUrl);
    const xml = await fetchFeed(normalizedUrl);
    const parsedFeed = rssParser.parse(xml);
    const syncedAt = Date.now();

    const existingPodcast = await getPodcast(normalizedUrl);
    const subscribedAt = existingPodcast?.subscribedAt || syncedAt;
    const existingEpisodes = await getEpisodesForPodcast(normalizedUrl);
    const existingMap = new Map(existingEpisodes.map((episode) => [episode.episodeId, episode]));

    const podcast = {
      feedUrl: normalizedUrl,
      title: parsedFeed.title,
      author: parsedFeed.author,
      description: parsedFeed.description,
      imageUrl: parsedFeed.imageUrl,
      siteUrl: parsedFeed.siteUrl,
      subscribedAt,
      lastSyncedAt: syncedAt,
      notifyNewEpisodes: existingPodcast?.notifyNewEpisodes ?? true,
      includeInQueue: existingPodcast?.includeInQueue ?? true,
      introSkipSeconds: existingPodcast?.introSkipSeconds ?? 0,
      outroSkipSeconds: existingPodcast?.outroSkipSeconds ?? 0,
    };

    const episodes = [];
    const newEpisodeTitles = [];
    for (const parsedEpisode of parsedFeed.episodes) {
      const stableId = await rules.stableId(parsedEpisode);
      const merged = await rules.mergeWithExistingEpisode({
        parsedEpisode,
        podcastId: normalizedUrl,
        fallbackArtworkUrl: parsedFeed.imageUrl,
        existingEpisode: existingMap.get(stableId) || null,
      });
      episodes.push(merged);
      if (!existingMap.has(merged.episodeId)) {
        newEpisodeTitles.push(merged.title);
      }
    }

    await db.put("podcasts", podcast);
    await db.replacePodcastEpisodes(normalizedUrl, episodes);

    const shouldNotify = Boolean(notifyForNewEpisodes && podcast.notifyNewEpisodes && existingPodcast);
    return {
      feedUrl: normalizedUrl,
      podcast,
      newEpisodeTitles: shouldNotify ? newEpisodeTitles : [],
      shouldNotify,
    };
  }

  async function addSubscription(feedUrl) {
    const result = await syncFeed(feedUrl, false);
    emit();
    return result;
  }

  async function refreshAllFeeds() {
    const podcasts = await db.getAllFrom("podcasts");
    const summaries = [];
    for (const podcast of podcasts) {
      try {
        const result = await syncFeed(podcast.feedUrl, true);
        if (result.shouldNotify && result.newEpisodeTitles.length) {
          summaries.push(
            buildNewEpisodesSummary(
              result.feedUrl,
              result.podcast,
              result.newEpisodeTitles,
              result.shouldNotify
            )
          );
        }
      } catch (error) {
        console.warn("Skipping feed refresh after error", podcast.feedUrl, error);
      }
    }
    emit();
    return summaries;
  }

  async function getSubscriptionsSnapshot() {
    const podcasts = await db.getAllFrom("podcasts");
    const episodes = await db.getAllFrom("episodes");
    const countsByPodcast = new Map();
    for (const episode of episodes) {
      const current = countsByPodcast.get(episode.podcastId) || { episodeCount: 0, unreadCount: 0 };
      current.episodeCount += 1;
      if (!episode.isRead) {
        current.unreadCount += 1;
      }
      countsByPodcast.set(episode.podcastId, current);
    }
    return podcasts
      .map((podcast) => {
        const counts = countsByPodcast.get(podcast.feedUrl) || { episodeCount: 0, unreadCount: 0 };
        return {
          feedUrl: podcast.feedUrl,
          title: podcast.title,
          author: podcast.author,
          description: podcast.description,
          imageUrl: podcast.imageUrl,
          episodeCount: counts.episodeCount,
          unreadCount: counts.unreadCount,
          lastSyncedAt: podcast.lastSyncedAt,
          notifyNewEpisodes: podcast.notifyNewEpisodes,
          includeInQueue: podcast.includeInQueue,
          introSkipSeconds: podcast.introSkipSeconds,
          outroSkipSeconds: podcast.outroSkipSeconds,
        };
      })
      .sort((left, right) =>
        left.title.localeCompare(right.title, undefined, { sensitivity: "base" })
      );
  }

  async function getQueueSnapshot() {
    const [podcasts, episodes] = await Promise.all([db.getAllFrom("podcasts"), db.getAllFrom("episodes")]);
    const podcastsMap = new Map(podcasts.map((podcast) => [podcast.feedUrl, podcast]));
    return episodes
      .filter((episode) => podcastsMap.get(episode.podcastId)?.includeInQueue)
      .map((episode) => {
        const podcast = podcastsMap.get(episode.podcastId);
        return {
          ...episode,
          podcastTitle: podcast?.title || "",
          introSkipSeconds: podcast?.introSkipSeconds || 0,
          outroSkipSeconds: podcast?.outroSkipSeconds || 0,
        };
      })
      .sort(compareByPublishedAndTitleDesc);
  }

  async function getCalendarEpisodesSnapshot() {
    const [podcasts, episodes] = await Promise.all([db.getAllFrom("podcasts"), db.getAllFrom("episodes")]);
    const podcastsMap = new Map(podcasts.map((podcast) => [podcast.feedUrl, podcast]));
    return episodes
      .map((episode) => {
        const podcast = podcastsMap.get(episode.podcastId);
        return {
          episodeId: episode.episodeId,
          podcastId: episode.podcastId,
          podcastTitle: podcast?.title || "",
          title: episode.title,
          summary: episode.summary,
          artworkUrl: episode.artworkUrl,
          episodeUrl: episode.episodeUrl,
          publishedAt: episode.publishedAt,
          durationLabel: episode.durationLabel,
          audioUrl: episode.audioUrl,
          durationMs: episode.durationMs,
          playbackPositionMs: episode.playbackPositionMs,
          introSkipSeconds: podcast?.introSkipSeconds || 0,
          outroSkipSeconds: podcast?.outroSkipSeconds || 0,
          isRead: episode.isRead,
        };
      })
      .sort(compareByPublishedAndTitleDesc);
  }

  async function updatePlaybackState(episodeId, positionMs, durationMs, isRead, isCompleted) {
    const episode = await db.get("episodes", episodeId);
    if (!episode) {
      return;
    }
    await db.put("episodes", {
      ...episode,
      playbackPositionMs: Math.max(0, Number(positionMs) || 0),
      durationMs: durationMs > 0 ? durationMs : episode.durationMs,
      isRead: Boolean(isRead),
      isCompleted: Boolean(isCompleted),
    });
    emit();
  }

  async function markEpisodeRead(episodeId, isRead = true) {
    const episode = await db.get("episodes", episodeId);
    if (!episode) {
      return;
    }
    await db.put("episodes", {
      ...episode,
      isRead: Boolean(isRead),
      playbackPositionMs: isRead ? 0 : episode.playbackPositionMs,
    });
    emit();
  }

  async function markAllEpisodesReadForPodcast(feedUrl, isRead) {
    const episodes = await db.getManyByIndex("episodes", "by_podcast_id", feedUrl);
    const updated = episodes.map((episode) => ({
      ...episode,
      isRead: Boolean(isRead),
      playbackPositionMs: isRead ? 0 : episode.playbackPositionMs,
    }));
    await db.putMany("episodes", updated);
    emit();
  }

  async function updateSubscriptionSettings(
    feedUrl,
    notifyNewEpisodes,
    includeInQueue,
    introSkipSeconds,
    outroSkipSeconds
  ) {
    const podcast = await db.get("podcasts", feedUrl);
    if (!podcast) {
      return;
    }
    await db.put("podcasts", {
      ...podcast,
      notifyNewEpisodes: Boolean(notifyNewEpisodes),
      includeInQueue: Boolean(includeInQueue),
      introSkipSeconds: Math.max(0, Number(introSkipSeconds) || 0),
      outroSkipSeconds: Math.max(0, Number(outroSkipSeconds) || 0),
    });
    emit();
  }

  async function removeSubscription(feedUrl) {
    await db.deleteWhere("episodes", (episode) => episode.podcastId === feedUrl);
    await db.deleteKey("podcasts", feedUrl);
    emit();
  }

  async function getEpisodesForSubscription(feedUrl) {
    const episodes = await db.getManyByIndex("episodes", "by_podcast_id", feedUrl);
    return episodes.sort(compareByPublishedAndTitleDesc);
  }

  async function getEpisodeById(episodeId) {
    return db.get("episodes", episodeId);
  }

  return {
    subscribe,
    addSubscription,
    refreshAllFeeds,
    getSubscriptionsSnapshot,
    getQueueSnapshot,
    getCalendarEpisodesSnapshot,
    getEpisodesForSubscription,
    getEpisodeById,
    getPodcast,
    updatePlaybackState,
    markEpisodeRead,
    markAllEpisodesReadForPodcast,
    updateSubscriptionSettings,
    removeSubscription,
    compareByPublishedAndTitleDesc,
    compareByPublishedAndTitleAsc,
  };
}

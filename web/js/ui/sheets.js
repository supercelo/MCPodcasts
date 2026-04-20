import { escapeHtml, formatDate, payloadAttr } from "./html.js";

function renderSheetContainer(content, sheetKey) {
  if (!content) {
    return "";
  }
  return `
    <div class="sheet-backdrop open">
      <div class="sheet">
        <div class="row-between">
          <span></span>
          <button class="icon-btn" data-action="closeSheet" data-payload="${payloadAttr({ sheet: sheetKey })}">✕</button>
        </div>
        ${content}
      </div>
    </div>
  `;
}

function findEpisode(state, episodeId) {
  return state.data.calendarEpisodes.find((episode) => episode.episodeId === episodeId) || null;
}

function renderAddPodcastSheet({ state, i18n }) {
  if (!state.ui.sheets.addPodcast) {
    return "";
  }
  const searchResults = state.data.searchResults || [];
  return renderSheetContainer(
    `
      <h2>${escapeHtml(i18n.t("discover_podcasts_title"))}</h2>
      <p class="muted">${escapeHtml(i18n.t("discover_podcasts_subtitle"))}</p>

      <form class="form-row" data-action="search">
        <label>${escapeHtml(i18n.t("search_podcasts_label"))}</label>
        <input class="input" name="query" placeholder="${escapeHtml(i18n.t("search_podcasts_label"))}" />
        <button class="btn primary" type="submit">${state.meta.isSearching ? "…" : escapeHtml(i18n.t("action_search"))}</button>
      </form>

      <div class="list">
        ${searchResults
          .map(
            (item) => `
              <div class="card row-between">
                <div>
                  <p class="episode-title">${escapeHtml(item.title)}</p>
                  <p class="episode-meta">${escapeHtml(item.author || "")}</p>
                </div>
                <button class="btn" data-action="subscribeFeed" data-payload="${payloadAttr({ feedUrl: item.feedUrl })}">
                  ${escapeHtml(i18n.t("action_subscribe"))}
                </button>
              </div>
            `
          )
          .join("")}
      </div>

      <form class="form-row" data-action="subscribeFeed">
        <label>${escapeHtml(i18n.t("rss_url_label"))}</label>
        <input class="input" name="feedUrl" placeholder="https://..." />
        <button class="btn primary" type="submit">${escapeHtml(i18n.t("action_add"))}</button>
      </form>
    `,
    "addPodcast"
  );
}

function renderSubscriptionEpisodesSheet({ state, i18n }) {
  const feedUrl = state.ui.sheets.subscriptionEpisodes;
  if (!feedUrl) {
    return "";
  }
  const subscription = state.data.subscriptions.find((item) => item.feedUrl === feedUrl);
  const episodes = state.data.calendarEpisodes
    .filter((episode) => episode.podcastId === feedUrl)
    .sort((left, right) => right.publishedAt - left.publishedAt);
  if (!subscription) {
    return "";
  }

  return renderSheetContainer(
    `
      <h2>${escapeHtml(subscription.title)}</h2>
      <p class="muted">${escapeHtml(subscription.author || "")}</p>
      <div class="chip-row">
        <button class="chip" data-action="openPreferences" data-payload="${payloadAttr({ feedUrl })}">${escapeHtml(i18n.t("cd_subscription_preferences"))}</button>
        <button class="chip" data-action="markAllReadForPodcast" data-payload="${payloadAttr({ feedUrl, isRead: true })}">${escapeHtml(i18n.t("subscription_bulk_mark_all_read"))}</button>
        <button class="chip" data-action="markAllReadForPodcast" data-payload="${payloadAttr({ feedUrl, isRead: false })}">${escapeHtml(i18n.t("subscription_bulk_mark_all_unread"))}</button>
      </div>
      <div class="list">
        ${episodes
          .map(
            (episode) => `
              <div class="card episode-row">
                <img class="episode-art" src="${escapeHtml(episode.artworkUrl || "./assets/podcast_mc_blue_icon.png")}" alt="" />
                <div>
                  <p class="episode-title">${escapeHtml(episode.title)}</p>
                  <p class="episode-meta">${escapeHtml(formatDate(episode.publishedAt))}</p>
                </div>
                <div class="episode-actions">
                  <button class="icon-btn" data-action="playCalendarEpisode" data-payload="${payloadAttr({ episodeId: episode.episodeId })}">▶</button>
                  <button class="icon-btn" data-action="markEpisodeRead" data-payload="${payloadAttr({ episodeId: episode.episodeId, isRead: !episode.isRead })}">${episode.isRead ? "✓" : "○"}</button>
                  <button class="icon-btn" data-action="openEpisodeDetail" data-payload="${payloadAttr({ episodeId: episode.episodeId })}">i</button>
                </div>
              </div>
            `
          )
          .join("")}
      </div>
    `,
    "subscriptionEpisodes"
  );
}

function renderSubscriptionPreferencesSheet({ state, i18n }) {
  const feedUrl = state.ui.sheets.subscriptionPreferences;
  if (!feedUrl) {
    return "";
  }
  const subscription = state.data.subscriptions.find((item) => item.feedUrl === feedUrl);
  if (!subscription) {
    return "";
  }
  return renderSheetContainer(
    `
      <h2>${escapeHtml(i18n.t("cd_subscription_preferences"))}</h2>
      <p class="muted">${escapeHtml(i18n.t("subscription_preferences_subtitle"))}</p>

      <form class="form-row" data-action="updateSubscriptionSettings">
        <input type="hidden" name="feedUrl" value="${escapeHtml(feedUrl)}" />
        <label class="row-between">
          <span>${escapeHtml(i18n.t("subscription_notify_title"))}</span>
          <input type="checkbox" name="notifyNewEpisodes" ${subscription.notifyNewEpisodes ? "checked" : ""} />
        </label>
        <label class="row-between">
          <span>${escapeHtml(i18n.t("subscription_queue_title"))}</span>
          <input type="checkbox" name="includeInQueue" ${subscription.includeInQueue ? "checked" : ""} />
        </label>
        <div class="range-row">
          <label>${escapeHtml(i18n.format("subscription_skip_intro", subscription.introSkipSeconds))}</label>
          <input type="range" name="introSkipSeconds" min="0" max="120" value="${subscription.introSkipSeconds}" />
        </div>
        <div class="range-row">
          <label>${escapeHtml(i18n.format("subscription_skip_outro", subscription.outroSkipSeconds))}</label>
          <input type="range" name="outroSkipSeconds" min="0" max="120" value="${subscription.outroSkipSeconds}" />
        </div>
        <button class="btn primary" type="submit">${escapeHtml(i18n.t("action_save"))}</button>
      </form>
      <button class="btn danger" data-action="removeSubscription" data-payload="${payloadAttr({ feedUrl })}">
        ${escapeHtml(i18n.t("subscription_delete_podcast"))}
      </button>
    `,
    "subscriptionPreferences"
  );
}

function renderEpisodeDetailSheet({ state, i18n }) {
  const episodeId = state.ui.sheets.episodeDetail;
  if (!episodeId) {
    return "";
  }
  const episode = findEpisode(state, episodeId);
  if (!episode) {
    return "";
  }
  return renderSheetContainer(
    `
      <h2>${escapeHtml(i18n.t("episode_detail_title"))}</h2>
      <img class="episode-art" src="${escapeHtml(episode.artworkUrl || "./assets/podcast_mc_blue_icon.png")}" alt="" />
      <p class="episode-title">${escapeHtml(episode.title)}</p>
      <p class="episode-meta">${escapeHtml(episode.podcastTitle)} • ${escapeHtml(formatDate(episode.publishedAt))}</p>
      <p>${escapeHtml(episode.summary || i18n.t("episode_detail_no_description"))}</p>
      <button class="btn primary" data-action="playCalendarEpisode" data-payload="${payloadAttr({ episodeId })}">
        ${escapeHtml(i18n.t("episode_detail_play"))}
      </button>
    `,
    "episodeDetail"
  );
}

function renderNowPlayingSheet({ state, i18n, coverUrl }) {
  if (!state.ui.sheets.nowPlaying) {
    return "";
  }
  const player = state.player || {};
  const durationMs = Number(player.durationMs || 0);
  const positionMs = Number(player.positionMs || 0);
  const safeDuration = Math.max(0, durationMs);
  const safePosition = Math.min(Math.max(0, positionMs), safeDuration || positionMs);

  return renderSheetContainer(
    `
      <h2>${escapeHtml(player.title || i18n.t("app_name"))}</h2>
      <p class="muted">${escapeHtml(player.podcastTitle || "")}</p>
      <img class="episode-art" src="${escapeHtml(coverUrl(player.artworkUrl))}" alt="" style="width:100%;height:auto;max-height:320px;border-radius:32px;" />
      <p>${escapeHtml(player.summary || "")}</p>
      <input class="input" type="range" min="0" max="${safeDuration}" value="${safePosition}" data-change-action="seekTo" data-change-key="positionMs" />
      <div class="row-between muted">
        <span>${Math.floor(safePosition / 1000)}s</span>
        <span>${Math.floor(safeDuration / 1000)}s</span>
      </div>
      <div class="chip-row">
        <button class="icon-btn" data-action="skipPrev">⏮</button>
        <button class="icon-btn" data-action="seekBack">⏪10</button>
        <button class="icon-btn" data-action="togglePlayback">${player.isPlaying ? "⏸" : "▶"}</button>
        <button class="icon-btn" data-action="seekForward">30⏩</button>
        <button class="icon-btn" data-action="skipNext">⏭</button>
      </div>
    `,
    "nowPlaying"
  );
}

export function renderSheets({ state, i18n, coverUrl }) {
  return [
    renderAddPodcastSheet({ state, i18n }),
    renderSubscriptionEpisodesSheet({ state, i18n }),
    renderSubscriptionPreferencesSheet({ state, i18n }),
    renderEpisodeDetailSheet({ state, i18n }),
    renderNowPlayingSheet({ state, i18n, coverUrl }),
  ]
    .filter(Boolean)
    .join("");
}

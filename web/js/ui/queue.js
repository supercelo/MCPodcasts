import { escapeHtml, formatDate, payloadAttr } from "./html.js";

function filterQueue(queue, ui) {
  let filtered = [...queue];
  if (ui.queueFilterRead === "unread") {
    filtered = filtered.filter((item) => !item.isRead);
  } else if (ui.queueFilterRead === "read") {
    filtered = filtered.filter((item) => item.isRead);
  }
  if (ui.queuePodcastFilter && ui.queuePodcastFilter !== "all") {
    filtered = filtered.filter((item) => item.podcastId === ui.queuePodcastFilter);
  }
  if (ui.queueSort === "date_asc") {
    filtered.sort((left, right) => left.publishedAt - right.publishedAt);
  } else {
    filtered.sort((left, right) => right.publishedAt - left.publishedAt);
  }
  return filtered;
}

function renderEpisodeRow(episode) {
  const progress =
    episode.durationMs > 0
      ? Math.min(100, Math.floor((episode.playbackPositionMs / episode.durationMs) * 100))
      : 0;
  const readLabel = episode.isRead ? "✓" : "○";
  return `
    <div class="card episode-row">
      <img class="episode-art" src="${escapeHtml(episode.artworkUrl || "./assets/podcast_mc_blue_icon.png")}" alt="" />
      <div>
        <p class="episode-title">${escapeHtml(episode.title)}</p>
        <p class="episode-meta">${escapeHtml(episode.podcastTitle)} • ${escapeHtml(formatDate(episode.publishedAt))}</p>
        ${episode.durationMs > 0 ? `<div class="progress"><span style="width:${progress}%"></span></div>` : ""}
      </div>
      <div class="episode-actions">
        <button class="icon-btn" data-action="playQueue" data-payload="${payloadAttr({ episodeId: episode.episodeId })}">▶</button>
        <button class="icon-btn" data-action="markEpisodeRead" data-payload="${payloadAttr({ episodeId: episode.episodeId, isRead: !episode.isRead })}">${readLabel}</button>
        <button class="icon-btn" data-action="openEpisodeDetail" data-payload="${payloadAttr({ episodeId: episode.episodeId })}">i</button>
      </div>
    </div>
  `;
}

export function renderQueueTab({ state, i18n }) {
  const queue = filterQueue(state.data.queue, state.ui);
  const podcastOptions = [
    `<option value="all">${escapeHtml(i18n.t("filter_all_podcasts"))}</option>`,
    ...state.data.subscriptions.map((subscription) => {
      const selected = state.ui.queuePodcastFilter === subscription.feedUrl ? "selected" : "";
      return `<option value="${escapeHtml(subscription.feedUrl)}" ${selected}>${escapeHtml(subscription.title)}</option>`;
    }),
  ].join("");

  const controls = `
    <div class="card">
      <div class="form-row">
        <label>${escapeHtml(i18n.t("cd_filter_read_state"))}</label>
        <div class="chip-row">
          <button class="chip ${state.ui.queueFilterRead === "all" ? "selected" : ""}" data-action="setQueueFilters" data-payload="${payloadAttr({ read: "all" })}">${escapeHtml(i18n.t("filter_read_state_all"))}</button>
          <button class="chip ${state.ui.queueFilterRead === "unread" ? "selected" : ""}" data-action="setQueueFilters" data-payload="${payloadAttr({ read: "unread" })}">${escapeHtml(i18n.t("filter_unread_only"))}</button>
          <button class="chip ${state.ui.queueFilterRead === "read" ? "selected" : ""}" data-action="setQueueFilters" data-payload="${payloadAttr({ read: "read" })}">${escapeHtml(i18n.t("filter_read_only"))}</button>
        </div>
      </div>
      <div class="form-row">
        <label>${escapeHtml(i18n.t("cd_queue_podcast_filter"))}</label>
        <select class="select" data-change-action="setQueueFilters" data-change-key="podcastId">
          ${podcastOptions}
        </select>
      </div>
      <div class="chip-row">
        <button class="chip ${state.ui.queueSort === "date_desc" ? "selected" : ""}" data-action="setQueueFilters" data-payload="${payloadAttr({ sort: "date_desc" })}">${escapeHtml(i18n.t("sort_date_desc"))}</button>
        <button class="chip ${state.ui.queueSort === "date_asc" ? "selected" : ""}" data-action="setQueueFilters" data-payload="${payloadAttr({ sort: "date_asc" })}">${escapeHtml(i18n.t("sort_date_asc"))}</button>
      </div>
    </div>
  `;

  if (!state.data.queue.length) {
    return `
      <div class="empty-state">
        <h2>${escapeHtml(i18n.t("queue_empty_title"))}</h2>
        <p>${escapeHtml(i18n.t("queue_empty_subtitle"))}</p>
      </div>
    `;
  }

  if (!queue.length) {
    return `
      ${controls}
      <div class="empty-state">
        <h2>${escapeHtml(i18n.t("queue_filtered_empty_title"))}</h2>
        <p>${escapeHtml(i18n.t("queue_filtered_empty_subtitle"))}</p>
      </div>
    `;
  }

  return `
    ${controls}
    <div class="list">
      ${queue.map((episode) => renderEpisodeRow(episode)).join("")}
    </div>
  `;
}

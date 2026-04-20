import { escapeHtml, formatDate, payloadAttr } from "./html.js";

function toIsoDate(date) {
  return date.toISOString().slice(0, 10);
}

function buildMonthDays(year, month) {
  const first = new Date(year, month, 1);
  const offset = (first.getDay() + 6) % 7;
  const days = [];
  for (let index = 0; index < offset; index += 1) {
    days.push(null);
  }
  const lastDay = new Date(year, month + 1, 0).getDate();
  for (let day = 1; day <= lastDay; day += 1) {
    days.push(new Date(year, month, day));
  }
  return days;
}

function filterCalendarEpisodes(episodes, ui) {
  let filtered = [...episodes];
  if (ui.calendarFilterRead === "unread") {
    filtered = filtered.filter((item) => !item.isRead);
  } else if (ui.calendarFilterRead === "read") {
    filtered = filtered.filter((item) => item.isRead);
  }
  if (ui.calendarPodcastFilter && ui.calendarPodcastFilter !== "all") {
    filtered = filtered.filter((item) => item.podcastId === ui.calendarPodcastFilter);
  }
  filtered.sort((left, right) => right.publishedAt - left.publishedAt);
  return filtered;
}

export function renderCalendarTab({ state, i18n }) {
  if (!state.data.calendarEpisodes.length) {
    return `
      <div class="empty-state">
        <h2>${escapeHtml(i18n.t("calendar_empty_title"))}</h2>
        <p>${escapeHtml(i18n.t("calendar_empty_subtitle"))}</p>
      </div>
    `;
  }

  const filtered = filterCalendarEpisodes(state.data.calendarEpisodes, state.ui);
  const baseDate = state.ui.selectedDate ? new Date(state.ui.selectedDate) : new Date();
  const selectedDateIso = state.ui.selectedDate || toIsoDate(baseDate);
  const monthDate = new Date(baseDate.getFullYear(), baseDate.getMonth(), 1);
  const monthDays = buildMonthDays(monthDate.getFullYear(), monthDate.getMonth());
  const dayCounts = new Map();
  for (const episode of filtered) {
    const key = toIsoDate(new Date(episode.publishedAt));
    dayCounts.set(key, (dayCounts.get(key) || 0) + 1);
  }
  const dayEpisodes = filtered.filter((episode) => toIsoDate(new Date(episode.publishedAt)) === selectedDateIso);

  const podcastOptions = [
    `<option value="all">${escapeHtml(i18n.t("filter_all_podcasts"))}</option>`,
    ...state.data.subscriptions.map((subscription) => {
      const selected = state.ui.calendarPodcastFilter === subscription.feedUrl ? "selected" : "";
      return `<option value="${escapeHtml(subscription.feedUrl)}" ${selected}>${escapeHtml(subscription.title)}</option>`;
    }),
  ].join("");

  const calendarCells = monthDays
    .map((date) => {
      if (!date) {
        return `<div></div>`;
      }
      const iso = toIsoDate(date);
      const selected = iso === selectedDateIso ? "selected" : "";
      const hasItems = dayCounts.get(iso) ? "has-items" : "";
      return `<button class="calendar-cell ${selected} ${hasItems}" data-action="setCalendarDate" data-payload="${payloadAttr({ dateIso: iso })}">${date.getDate()}</button>`;
    })
    .join("");

  return `
    <div class="card">
      <div class="form-row">
        <label>${escapeHtml(i18n.t("cd_filter_read_state"))}</label>
        <div class="chip-row">
          <button class="chip ${state.ui.calendarFilterRead === "all" ? "selected" : ""}" data-action="setCalendarFilters" data-payload="${payloadAttr({ read: "all" })}">${escapeHtml(i18n.t("filter_read_state_all"))}</button>
          <button class="chip ${state.ui.calendarFilterRead === "unread" ? "selected" : ""}" data-action="setCalendarFilters" data-payload="${payloadAttr({ read: "unread" })}">${escapeHtml(i18n.t("filter_unread_only"))}</button>
          <button class="chip ${state.ui.calendarFilterRead === "read" ? "selected" : ""}" data-action="setCalendarFilters" data-payload="${payloadAttr({ read: "read" })}">${escapeHtml(i18n.t("filter_read_only"))}</button>
        </div>
      </div>
      <div class="form-row">
        <label>${escapeHtml(i18n.t("cd_calendar_podcast_filter"))}</label>
        <select class="select" data-change-action="setCalendarFilters" data-change-key="podcastId">
          ${podcastOptions}
        </select>
      </div>
      <div class="calendar-header">
        <strong>${escapeHtml(monthDate.toLocaleDateString(undefined, { month: "long", year: "numeric" }))}</strong>
        <span class="muted">${escapeHtml(selectedDateIso)}</span>
      </div>
      <div class="calendar-grid">${calendarCells}</div>
    </div>
    <div class="list">
      ${
        dayEpisodes.length
          ? dayEpisodes
              .map(
                (episode) => `
                <div class="card episode-row">
                  <img class="episode-art" src="${escapeHtml(episode.artworkUrl || "./assets/podcast_mc_blue_icon.png")}" alt="" />
                  <div>
                    <p class="episode-title">${escapeHtml(episode.title)}</p>
                    <p class="episode-meta">${escapeHtml(episode.podcastTitle)} • ${escapeHtml(formatDate(episode.publishedAt))}</p>
                  </div>
                  <div class="episode-actions">
                    <button class="icon-btn" data-action="playCalendarEpisode" data-payload="${payloadAttr({ episodeId: episode.episodeId })}">▶</button>
                    <button class="icon-btn" data-action="markEpisodeRead" data-payload="${payloadAttr({ episodeId: episode.episodeId, isRead: !episode.isRead })}">${episode.isRead ? "✓" : "○"}</button>
                    <button class="icon-btn" data-action="openEpisodeDetail" data-payload="${payloadAttr({ episodeId: episode.episodeId })}">i</button>
                  </div>
                </div>
              `
              )
              .join("")
          : `<div class="card muted">${escapeHtml(i18n.t("calendar_empty_day"))}</div>`
      }
    </div>
  `;
}

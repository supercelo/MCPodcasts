import { escapeHtml, formatDate, payloadAttr } from "./html.js";

export function renderSubscriptionsTab({ state, i18n }) {
  if (!state.data.subscriptions.length) {
    return `
      <div class="empty-state">
        <img src="./assets/podcast_mc_blue_icon.png" alt="" />
        <h2>${escapeHtml(i18n.t("subscriptions_empty_title"))}</h2>
        <p>${escapeHtml(i18n.t("subscriptions_empty_subtitle"))}</p>
      </div>
    `;
  }

  const cards = state.data.subscriptions
    .map((subscription) => {
      const synced = formatDate(subscription.lastSyncedAt) || i18n.t("now");
      return `
        <div class="card">
          <button class="tile" data-action="openSubscriptionEpisodes" data-payload="${payloadAttr({ feedUrl: subscription.feedUrl })}">
            <img src="${escapeHtml(subscription.imageUrl || "./assets/podcast_mc_blue_icon.png")}" alt="${escapeHtml(subscription.title)}" />
            ${subscription.unreadCount > 0 ? `<span class="badge">${subscription.unreadCount}</span>` : ""}
          </button>
          <div>
            <p class="episode-title">${escapeHtml(subscription.title)}</p>
            <p class="episode-meta">${escapeHtml(i18n.format("subscription_episode_summary", subscription.episodeCount, synced))}</p>
          </div>
        </div>
      `;
    })
    .join("");

  return `<div class="list">${cards}</div>`;
}

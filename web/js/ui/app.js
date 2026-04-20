import { renderQueueTab } from "./queue.js";
import { renderCalendarTab } from "./calendar.js";
import { renderSubscriptionsTab } from "./subscriptions.js";
import { renderSettingsTab } from "./settings.js";
import { renderSheets } from "./sheets.js";

const TAB_ORDER = ["Queue", "Calendar", "Subscriptions", "Settings"];

function resolveLanguageTag(appLanguage) {
  switch (appLanguage) {
    case "Portuguese":
      return "pt";
    case "English":
      return "en";
    case "French":
      return "fr";
    case "Spanish":
      return "es";
    case "Italian":
      return "it";
    case "German":
      return "de";
    default:
      return navigator.language || "en";
  }
}

function resolveTheme(themeMode) {
  if (themeMode === "Light") return "light";
  if (themeMode === "Dark") return "dark";
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

function topbarForTab(state, i18n) {
  const byTab = {
    Queue: i18n.t("tab_queue"),
    Calendar: i18n.t("tab_calendar"),
    Subscriptions: i18n.t("tab_subscriptions"),
    Settings: i18n.t("tab_settings"),
  };
  let subtitle = "";
  if (state.ui.mainTab === "Queue") subtitle = i18n.plural("topbar_queue_summary", state.data.queue.length);
  if (state.ui.mainTab === "Calendar") subtitle = i18n.t("topbar_calendar_summary");
  if (state.ui.mainTab === "Subscriptions") subtitle = i18n.plural("topbar_subscriptions_summary", state.data.subscriptions.length);
  if (state.ui.mainTab === "Settings") subtitle = i18n.t("settings_subtitle");
  return { title: byTab[state.ui.mainTab] || i18n.t("app_name"), subtitle };
}

function coverUrl(url) {
  return url || "./assets/podcast_mc_blue_icon.png";
}

export function createApp({
  root,
  store,
  i18n,
  podcastRepository,
  discovery,
  settingsRepository,
  player,
  rules,
}) {
  if (!root) throw new Error("Missing #app root element.");

  async function reloadData() {
    const [queue, subscriptions, calendarEpisodes] = await Promise.all([
      podcastRepository.getQueueSnapshot(),
      podcastRepository.getSubscriptionsSnapshot(),
      podcastRepository.getCalendarEpisodesSnapshot(),
    ]);
    store.setState({
      data: { ...store.getState().data, queue, subscriptions, calendarEpisodes },
      meta: { ...store.getState().meta, loading: false },
    });
  }

  function applyThemeAndLocale(settings) {
    document.documentElement.setAttribute("data-theme", resolveTheme(settings.themeMode));
    i18n.setLocale(resolveLanguageTag(settings.appLanguage));
  }

  function updateUi(uiPatch) {
    store.setState({ ui: { ...store.getState().ui, ...uiPatch } });
  }

  async function refreshSubscriptions() {
    store.setState({ meta: { ...store.getState().meta, isRefreshing: true } });
    try {
      await podcastRepository.refreshAllFeeds();
      await reloadData();
    } catch (error) {
      store.enqueueSnackbar(error.message || i18n.t("msg_refresh_failed"));
    } finally {
      store.setState({ meta: { ...store.getState().meta, isRefreshing: false } });
    }
  }

  async function handleAction(action, payload) {
    const state = store.getState();
    switch (action) {
      case "tab":
        updateUi({ mainTab: payload.tab });
        return;
      case "refresh":
        await refreshSubscriptions();
        return;
      case "openAddSheet":
        updateUi({ sheets: { ...state.ui.sheets, addPodcast: true } });
        return;
      case "closeSheet":
        updateUi({ sheets: { ...state.ui.sheets, [payload.sheet]: false } });
        if (payload.sheet === "addPodcast") {
          store.setState({ data: { ...store.getState().data, searchResults: [] } });
        }
        return;
      case "search": {
        store.setState({ meta: { ...store.getState().meta, isSearching: true } });
        try {
          const searchResults = await discovery.search(payload.query);
          store.setState({ data: { ...store.getState().data, searchResults } });
          if (payload.query?.trim() && !searchResults.length) {
            store.enqueueSnackbar(i18n.t("msg_no_podcasts_found"));
          }
        } catch (error) {
          store.enqueueSnackbar(error.message || i18n.t("msg_search_failed"));
        } finally {
          store.setState({ meta: { ...store.getState().meta, isSearching: false } });
        }
        return;
      }
      case "subscribeFeed":
        try {
          await podcastRepository.addSubscription(payload.feedUrl);
          store.enqueueSnackbar(i18n.t("msg_podcast_added_success"));
          await reloadData();
        } catch (error) {
          store.enqueueSnackbar(error.message || i18n.t("msg_add_feed_failed"));
        }
        return;
      case "openSubscriptionEpisodes":
        updateUi({ sheets: { ...state.ui.sheets, subscriptionEpisodes: payload.feedUrl }, selectedSubscription: payload.feedUrl });
        return;
      case "openPreferences":
        updateUi({ sheets: { ...state.ui.sheets, subscriptionPreferences: payload.feedUrl }, selectedSubscription: payload.feedUrl });
        return;
      case "removeSubscription":
        await podcastRepository.removeSubscription(payload.feedUrl);
        store.enqueueSnackbar(i18n.t("msg_podcast_removed"));
        await reloadData();
        updateUi({ sheets: { ...store.getState().ui.sheets, subscriptionPreferences: null, subscriptionEpisodes: null } });
        return;
      case "updateSubscriptionSettings":
        await podcastRepository.updateSubscriptionSettings(
          payload.feedUrl,
          Boolean(payload.notifyNewEpisodes),
          Boolean(payload.includeInQueue),
          Number(payload.introSkipSeconds || 0),
          Number(payload.outroSkipSeconds || 0)
        );
        store.enqueueSnackbar(i18n.t("msg_subscription_preferences_updated"));
        await reloadData();
        return;
      case "markEpisodeRead":
        await podcastRepository.markEpisodeRead(payload.episodeId, payload.isRead);
        await reloadData();
        return;
      case "markAllReadForPodcast":
        await podcastRepository.markAllEpisodesReadForPodcast(payload.feedUrl, payload.isRead);
        await reloadData();
        return;
      case "openEpisodeDetail":
        updateUi({ sheets: { ...state.ui.sheets, episodeDetail: payload.episodeId } });
        return;
      case "openPlayer":
        updateUi({ sheets: { ...state.ui.sheets, nowPlaying: true } });
        return;
      case "playQueue":
        await player.playQueue(state.data.queue, payload.episodeId);
        return;
      case "playCalendarEpisode": {
        const episode = state.data.calendarEpisodes.find((item) => item.episodeId === payload.episodeId);
        if (episode) await player.playCalendarEpisode(episode);
        return;
      }
      case "togglePlayback":
        await player.togglePlayback();
        return;
      case "seekBack":
        player.seekBack();
        return;
      case "seekForward":
        player.seekForward();
        return;
      case "skipPrev":
        await player.skipToPrevious();
        return;
      case "skipNext":
        await player.skipToNext();
        return;
      case "seekTo":
        player.seekToPositionFromUser(Number(payload.positionMs || 0));
        return;
      case "setQueueFilters":
        updateUi({
          queueFilterRead: payload.read ?? state.ui.queueFilterRead,
          queueSort: payload.sort ?? state.ui.queueSort,
          queuePodcastFilter: payload.podcastId ?? state.ui.queuePodcastFilter,
        });
        return;
      case "setCalendarFilters":
        updateUi({
          calendarFilterRead: payload.read ?? state.ui.calendarFilterRead,
          calendarPodcastFilter: payload.podcastId ?? state.ui.calendarPodcastFilter,
        });
        return;
      case "setCalendarDate":
        updateUi({ selectedDate: payload.dateIso });
        return;
      case "setThemeMode":
        settingsRepository.setThemeMode(payload.themeMode);
        return;
      case "setAppLanguage":
        settingsRepository.setAppLanguage(payload.appLanguage);
        return;
      case "setSyncSummaryNotificationsEnabled":
        settingsRepository.setSyncSummaryNotificationsEnabled(payload.enabled);
        return;
      case "setVolumeNormalizationEnabled":
        settingsRepository.setVolumeNormalizationEnabled(payload.enabled);
        return;
      default:
        return;
    }
  }

  function bindEvents() {
    root.addEventListener("click", async (event) => {
      const target = event.target.closest("[data-action]");
      if (!target) return;
      const action = target.getAttribute("data-action");
      const payload = JSON.parse(target.getAttribute("data-payload") || "{}");
      await handleAction(action, payload);
    });

    root.addEventListener("submit", async (event) => {
      const form = event.target;
      if (!(form instanceof HTMLFormElement)) return;
      const action = form.getAttribute("data-action");
      if (!action) return;
      event.preventDefault();
      const data = Object.fromEntries(new FormData(form).entries());
      await handleAction(action, data);
    });

    root.addEventListener("change", async (event) => {
      const target = event.target;
      if (!(target instanceof HTMLElement)) return;
      const action = target.getAttribute("data-change-action");
      const key = target.getAttribute("data-change-key");
      if (!action || !key) return;
      let value = null;
      if (target instanceof HTMLInputElement && target.type === "checkbox") {
        value = target.checked;
      } else if (target instanceof HTMLInputElement) {
        value = target.value;
      } else if (target instanceof HTMLSelectElement) {
        value = target.value;
      }
      await handleAction(action, { [key]: value });
    });
  }

  function render() {
    const state = store.getState();
    const settings = state.settings || settingsRepository.getCurrentSettings();
    applyThemeAndLocale(settings);
    const top = topbarForTab(state, i18n);

    const content =
      state.ui.mainTab === "Queue"
        ? renderQueueTab({ state, i18n })
        : state.ui.mainTab === "Calendar"
          ? renderCalendarTab({ state, i18n, rules })
          : state.ui.mainTab === "Subscriptions"
            ? renderSubscriptionsTab({ state, i18n })
            : renderSettingsTab({ state, i18n });

    const playerState = state.player || {};
    const showMini = Boolean(playerState.hasMedia || playerState.currentEpisodeId || playerState.title);
    const nav = TAB_ORDER.map((tab) => {
      const key = `tab_${tab.toLowerCase()}`;
      return `<button class="${state.ui.mainTab === tab ? "active" : ""}" data-action="tab" data-payload='{"tab":"${tab}"}'>${i18n.t(key)}</button>`;
    }).join("");

    root.innerHTML = `
      <div class="app-shell">
        <header class="topbar">
          <div>
            <h1 class="topbar-title">${top.title}</h1>
            <p class="topbar-subtitle">${top.subtitle}</p>
          </div>
          <button class="icon-btn" data-action="refresh">${state.meta.isRefreshing ? "…" : "⟳"}</button>
        </header>
        <main class="content">${content}</main>
        ${
          state.ui.mainTab === "Subscriptions"
            ? `<button class="fab" data-action="openAddSheet">+</button>`
            : ""
        }
        <div class="bottom-stack">
          ${
            showMini
              ? `
                <div class="mini-player">
                  <img class="mini-player-art" src="${coverUrl(playerState.artworkUrl)}" alt="" />
                  <div data-action="openPlayer">
                    <strong>${playerState.title || i18n.t("app_name")}</strong>
                    <div class="muted">${playerState.podcastTitle || ""}</div>
                  </div>
                  <button class="icon-btn" data-action="togglePlayback">${playerState.isPlaying ? "⏸" : "▶"}</button>
                </div>
              `
              : ""
          }
          <nav class="nav">${nav}</nav>
        </div>
        ${renderSheets({ state, i18n, coverUrl, podcastRepository })}
        <div class="snackbar-stack">
          ${state.snackbars.map((snackbar) => `<div class="snackbar">${snackbar.message}</div>`).join("")}
        </div>
      </div>
    `;
  }

  async function init() {
    bindEvents();

    settingsRepository.subscribe((settings) => {
      store.setState({ settings });
    });
    podcastRepository.subscribe(() => {
      reloadData().catch(console.warn);
    });
    player.subscribe((playerState) => {
      store.setState({ player: playerState });
    });
    store.subscribe(() => render());

    await reloadData();
    render();
  }

  return { init };
}

const DEFAULT_UI = {
  mainTab: "Queue",
  queueFilterRead: "all",
  queueSort: "date_desc",
  queuePodcastFilter: "all",
  calendarFilterRead: "all",
  calendarPodcastFilter: "all",
  selectedDate: null,
  selectedMonthOffset: 0,
  sheets: {
    addPodcast: false,
    subscriptionEpisodes: null,
    subscriptionPreferences: null,
    episodeDetail: null,
    nowPlaying: false,
  },
  selectedSubscription: null,
};

const DEFAULT_DATA = {
  queue: [],
  calendarEpisodes: [],
  subscriptions: [],
  searchResults: [],
};

const DEFAULT_META = {
  isRefreshing: false,
  isSearching: false,
  loading: true,
};

export function createStore() {
  let state = {
    ui: structuredClone(DEFAULT_UI),
    data: structuredClone(DEFAULT_DATA),
    settings: null,
    player: null,
    meta: structuredClone(DEFAULT_META),
    snackbars: [],
  };
  const listeners = new Set();

  function getState() {
    return state;
  }

  function setState(patch) {
    const next = typeof patch === "function" ? patch(state) : patch;
    state = {
      ...state,
      ...next,
      ui: { ...state.ui, ...(next.ui || {}) },
      data: { ...state.data, ...(next.data || {}) },
      meta: { ...state.meta, ...(next.meta || {}) },
    };
    listeners.forEach((listener) => listener(state));
  }

  function subscribe(listener) {
    listeners.add(listener);
    listener(state);
    return () => {
      listeners.delete(listener);
    };
  }

  function enqueueSnackbar(message, timeoutMs = 3000) {
    const id = crypto.randomUUID();
    setState((current) => ({
      snackbars: [...current.snackbars, { id, message }],
    }));
    setTimeout(() => {
      setState((current) => ({
        snackbars: current.snackbars.filter((item) => item.id !== id),
      }));
    }, timeoutMs);
  }

  return {
    getState,
    setState,
    subscribe,
    enqueueSnackbar,
  };
}

const STORAGE_KEY = "mcpodcasts-web-settings";

const ThemeMode = {
  System: "System",
  Light: "Light",
  Dark: "Dark",
};

const AppLanguage = {
  System: "System",
  Portuguese: "Portuguese",
  English: "English",
  French: "French",
  Spanish: "Spanish",
  Italian: "Italian",
  German: "German",
};

const defaults = {
  themeMode: ThemeMode.System,
  appLanguage: AppLanguage.System,
  syncSummaryNotificationsEnabled: true,
  volumeNormalizationEnabled: false,
};

function readPersistedSettings() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return { ...defaults };
    }
    const parsed = JSON.parse(raw);
    return { ...defaults, ...(parsed || {}) };
  } catch {
    return { ...defaults };
  }
}

export function createSettingsRepository() {
  let state = readPersistedSettings();
  const listeners = new Set();

  function emit() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    listeners.forEach((listener) => listener({ ...state }));
  }

  function subscribe(listener) {
    listeners.add(listener);
    listener({ ...state });
    return () => listeners.delete(listener);
  }

  function getCurrentSettings() {
    return { ...state };
  }

  function setThemeMode(themeMode) {
    state = { ...state, themeMode };
    emit();
  }

  function setAppLanguage(appLanguage) {
    state = { ...state, appLanguage };
    emit();
  }

  function setSyncSummaryNotificationsEnabled(enabled) {
    state = { ...state, syncSummaryNotificationsEnabled: Boolean(enabled) };
    emit();
  }

  function setVolumeNormalizationEnabled(enabled) {
    state = { ...state, volumeNormalizationEnabled: Boolean(enabled) };
    emit();
  }

  async function getLastPlayedEpisodeId() {
    return localStorage.getItem("mcpodcasts-web-last-played-id");
  }

  async function setLastPlayedEpisodeId(episodeId) {
    if (!episodeId) {
      localStorage.removeItem("mcpodcasts-web-last-played-id");
      return;
    }
    localStorage.setItem("mcpodcasts-web-last-played-id", episodeId);
  }

  return {
    ThemeMode,
    AppLanguage,
    defaults,
    subscribe,
    getCurrentSettings,
    setThemeMode,
    setAppLanguage,
    setSyncSummaryNotificationsEnabled,
    setVolumeNormalizationEnabled,
    getLastPlayedEpisodeId,
    setLastPlayedEpisodeId,
  };
}

import { escapeHtml, payloadAttr } from "./html.js";

const THEME_MODES = ["System", "Light", "Dark"];
const LANGUAGES = ["System", "Portuguese", "English", "French", "Spanish", "Italian", "German"];

function localizedThemeLabel(mode, i18n) {
  if (mode === "Light") return i18n.t("theme_light");
  if (mode === "Dark") return i18n.t("theme_dark");
  return i18n.t("theme_system");
}

function localizedLanguageLabel(language, i18n) {
  switch (language) {
    case "Portuguese":
      return i18n.t("language_portuguese");
    case "English":
      return i18n.t("language_english");
    case "French":
      return i18n.t("language_french");
    case "Spanish":
      return i18n.t("language_spanish");
    case "Italian":
      return i18n.t("language_italian");
    case "German":
      return i18n.t("language_german");
    default:
      return i18n.t("language_system");
  }
}

function switchInput(action, enabled) {
  return `
    <label class="row-between">
      <input type="checkbox" ${enabled ? "checked" : ""} data-change-action="${action}" data-change-key="enabled" />
      <span>${enabled ? "On" : "Off"}</span>
    </label>
  `;
}

export function renderSettingsTab({ state, i18n }) {
  const settings = state.settings || {};
  const themeChips = THEME_MODES.map((mode) => {
    const selected = settings.themeMode === mode ? "selected" : "";
    return `<button class="chip ${selected}" data-action="setThemeMode" data-payload="${payloadAttr({ themeMode: mode })}">${escapeHtml(localizedThemeLabel(mode, i18n))}</button>`;
  }).join("");
  const languageChips = LANGUAGES.map((language) => {
    const selected = settings.appLanguage === language ? "selected" : "";
    return `<button class="chip ${selected}" data-action="setAppLanguage" data-payload="${payloadAttr({ appLanguage: language })}">${escapeHtml(localizedLanguageLabel(language, i18n))}</button>`;
  }).join("");

  return `
    <div class="card">
      <h2>${escapeHtml(i18n.t("settings_title"))}</h2>
      <p class="muted">${escapeHtml(i18n.t("settings_subtitle"))}</p>

      <div class="form-row">
        <strong>${escapeHtml(i18n.t("settings_theme_title"))}</strong>
        <div class="chip-row">${themeChips}</div>
      </div>

      <div class="form-row">
        <strong>${escapeHtml(i18n.t("settings_language_title"))}</strong>
        <div class="chip-row">${languageChips}</div>
      </div>

      <div class="form-row">
        <strong>${escapeHtml(i18n.t("settings_sync_notification_title"))}</strong>
        <p class="muted">${escapeHtml(i18n.t("settings_sync_notification_subtitle"))}</p>
        ${switchInput("setSyncSummaryNotificationsEnabled", settings.syncSummaryNotificationsEnabled)}
      </div>

      <div class="form-row">
        <strong>${escapeHtml(i18n.t("settings_volume_normalization_title"))}</strong>
        <p class="muted">${escapeHtml(i18n.t("settings_volume_normalization_subtitle"))}</p>
        ${switchInput("setVolumeNormalizationEnabled", settings.volumeNormalizationEnabled)}
      </div>
    </div>
  `;
}

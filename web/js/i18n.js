const SUPPORTED_LOCALES = ["en", "de", "es", "fr", "it", "pt"];
const PLURAL_KEYS = new Set([
  "topbar_queue_summary",
  "topbar_subscriptions_summary",
  "hours_label",
  "notification_title_new_episodes",
]);

function normalizeLocaleTag(languageTag) {
  const lower = String(languageTag || "").toLowerCase();
  if (!lower) {
    return "en";
  }
  const base = lower.split("-")[0];
  return SUPPORTED_LOCALES.includes(base) ? base : "en";
}

function rewritePlaceholders(input) {
  return String(input || "").replace(/%(\d+)\$s|%(\d+)\$d/g, (_, sIndex, dIndex) => {
    const index = Number(sIndex || dIndex) - 1;
    return `{${index}}`;
  });
}

function formatTemplate(template, args) {
  return template.replace(/\{(\d+)\}/g, (_, index) => {
    const value = args[Number(index)];
    return value == null ? "" : String(value);
  });
}

export async function createI18n() {
  const bundles = {};

  for (const locale of SUPPORTED_LOCALES) {
    const response = await fetch(`./locales/${locale}.json`);
    if (!response.ok) {
      throw new Error(`Unable to load locale file: ${locale}.json`);
    }
    bundles[locale] = await response.json();
  }

  let currentLocale = "en";

  function setLocale(languageTag) {
    currentLocale = normalizeLocaleTag(languageTag);
  }

  function getLocale() {
    return currentLocale;
  }

  function getValue(key) {
    const localized = bundles[currentLocale]?.[key];
    if (localized != null) {
      return localized;
    }
    return bundles.en?.[key] ?? key;
  }

  function t(key) {
    const value = getValue(key);
    if (typeof value === "string") {
      return rewritePlaceholders(value);
    }
    if (value && typeof value === "object") {
      return rewritePlaceholders(value.other || value.one || key);
    }
    return key;
  }

  function format(key, ...args) {
    return formatTemplate(t(key), args);
  }

  function plural(key, count, ...args) {
    if (!PLURAL_KEYS.has(key)) {
      return format(key, count, ...args);
    }
    const forms = getValue(key);
    if (!forms || typeof forms !== "object") {
      return format(key, count, ...args);
    }
    const pluralRules = new Intl.PluralRules(currentLocale);
    const category = pluralRules.select(Number(count));
    const picked = forms[category] || forms.other || forms.one || key;
    return formatTemplate(rewritePlaceholders(picked), [count, ...args]);
  }

  function getSupportedLocales() {
    return [...SUPPORTED_LOCALES];
  }

  return {
    setLocale,
    getLocale,
    t,
    format,
    plural,
    getSupportedLocales,
  };
}

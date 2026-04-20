function decodeDurationParts(value) {
  return String(value || "")
    .trim()
    .split(":")
    .map((part) => Number(part))
    .filter((part) => Number.isFinite(part) && part >= 0);
}

function toDurationSeconds(durationLabel) {
  const values = decodeDurationParts(durationLabel);
  if (!values.length) {
    return 0;
  }
  if (values.length === 1) {
    return values[0];
  }
  if (values.length === 2) {
    return values[0] * 60 + values[1];
  }
  const [hours, minutes, seconds] = values.slice(-3);
  return hours * 3600 + minutes * 60 + seconds;
}

function resolveDurationLabel(rawDuration, parsedDurationMs, mergedDurationMs, previousDurationLabel) {
  if (parsedDurationMs > 0) {
    return formatDurationMsForLabel(parsedDurationMs);
  }
  if (mergedDurationMs > 0) {
    return formatDurationMsForLabel(mergedDurationMs);
  }
  const raw = String(rawDuration || "").trim();
  return raw || previousDurationLabel || null;
}

async function stableIdForText(input) {
  const data = new TextEncoder().encode(input);
  const hash = await crypto.subtle.digest("SHA-256", data);
  const bytes = [...new Uint8Array(hash)];
  return bytes.map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

function formatDurationMsForLabel(durationMs) {
  if (!durationMs || durationMs <= 0) {
    return "";
  }
  const totalSeconds = Math.floor(durationMs / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
  }
  return `${minutes}:${String(seconds).padStart(2, "0")}`;
}

function buildMonthRangeForEpisodes(publishedDates, nowDate = new Date()) {
  const currentMonth = new Date(nowDate.getFullYear(), nowDate.getMonth(), 1);
  const monthValues = publishedDates
    .map((date) => new Date(date.getFullYear(), date.getMonth(), 1))
    .filter((date) => !Number.isNaN(date.getTime()));
  const minMonth =
    monthValues.length > 0
      ? new Date(Math.min(...monthValues.map((date) => date.getTime())))
      : currentMonth;
  const maxMonth = new Date(Math.max(currentMonth.getTime(), ...monthValues.map((date) => date.getTime())));
  const result = [];
  const cursor = new Date(minMonth.getTime());
  while (cursor <= maxMonth) {
    result.push(new Date(cursor.getTime()));
    cursor.setMonth(cursor.getMonth() + 1);
  }
  return result;
}

function normalizeRefreshIntervalHours(hours) {
  return Math.max(1, Number(hours) || 1);
}

export function createFeatureRules() {
  async function stableId(parsedEpisode) {
    const baseSource = String(parsedEpisode.guid || "").trim() || String(parsedEpisode.audioUrl || "");
    return stableIdForText(baseSource);
  }

  function toDurationMs(rawDuration) {
    return toDurationSeconds(rawDuration) * 1000;
  }

  async function mergeWithExistingEpisode({
    parsedEpisode,
    podcastId,
    fallbackArtworkUrl,
    existingEpisode,
  }) {
    const parsedDurationMs = toDurationMs(parsedEpisode.durationLabel);
    const mergedDurationMs = parsedDurationMs > 0 ? parsedDurationMs : existingEpisode?.durationMs || 0;
    const mergedDurationLabel = resolveDurationLabel(
      parsedEpisode.durationLabel,
      parsedDurationMs,
      mergedDurationMs,
      existingEpisode?.durationLabel || null
    );

    return {
      episodeId: await stableId(parsedEpisode),
      podcastId,
      guid: parsedEpisode.guid || null,
      title: parsedEpisode.title,
      summary: parsedEpisode.summary || null,
      audioUrl: parsedEpisode.audioUrl,
      artworkUrl: parsedEpisode.artworkUrl || fallbackArtworkUrl || null,
      episodeUrl: parsedEpisode.episodeUrl || null,
      publishedAt: Number(parsedEpisode.publishedAt || 0),
      durationLabel: mergedDurationLabel,
      durationMs: mergedDurationMs,
      playbackPositionMs: existingEpisode?.playbackPositionMs || 0,
      isRead: Boolean(existingEpisode?.isRead),
      isCompleted: Boolean(existingEpisode?.isCompleted),
    };
  }

  return {
    stableId,
    toDurationMs,
    formatDurationMsForLabel,
    mergeWithExistingEpisode,
    buildMonthRangeForEpisodes,
    normalizeRefreshIntervalHours,
  };
}

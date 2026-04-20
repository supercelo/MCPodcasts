const SEEK_BACK_MS = 10_000;
const SEEK_FORWARD_MS = 30_000;

function baseUiState() {
  return {
    connected: true,
    currentEpisodeId: null,
    title: "",
    podcastTitle: "",
    artworkUrl: null,
    publishedAtMs: null,
    summary: null,
    episodeUrl: null,
    durationMs: 0,
    positionMs: 0,
    isPlaying: false,
    isBuffering: false,
    hasMedia: false,
  };
}

function computeStartPositionWithIntro(savedMs, introSkipSeconds) {
  const saved = Math.max(0, Number(savedMs) || 0);
  const introMs = Math.max(0, Number(introSkipSeconds) || 0) * 1000;
  if (introMs <= 0) {
    return saved;
  }
  if (saved === 0) {
    return introMs;
  }
  if (saved > 0 && saved < introMs) {
    return saved;
  }
  return saved;
}

export function createPlayer({ podcastRepository, settingsRepository, store }) {
  const audio = new Audio();
  audio.preload = "metadata";
  const listeners = new Set();
  let queue = [];
  let queueIndex = -1;
  let singleEpisode = null;
  let userAllowsIntroPlayback = false;
  let userAllowsOutroPlayback = false;
  let outroSkipTriggeredForEpisodeId = null;
  let audioContext = null;
  let audioSourceNode = null;
  let compressorNode = null;

  function currentItem() {
    if (queueIndex >= 0 && queueIndex < queue.length) {
      return queue[queueIndex];
    }
    return singleEpisode;
  }

  function episodeDurationMs() {
    if (!Number.isFinite(audio.duration) || audio.duration <= 0) {
      return 0;
    }
    return Math.floor(audio.duration * 1000);
  }

  function positionMs() {
    return Math.max(0, Math.floor((audio.currentTime || 0) * 1000));
  }

  function updateStorePlayerState() {
    const item = currentItem();
    const next = {
      connected: true,
      currentEpisodeId: item?.episodeId || null,
      title: item?.title || "",
      podcastTitle: item?.podcastTitle || "",
      artworkUrl: item?.artworkUrl || null,
      publishedAtMs: item?.publishedAt ?? null,
      summary: item?.summary || null,
      episodeUrl: item?.episodeUrl || null,
      durationMs: episodeDurationMs(),
      positionMs: positionMs(),
      isPlaying: !audio.paused && !audio.ended,
      isBuffering: audio.readyState < 3 && !audio.paused,
      hasMedia: Boolean(item),
    };
    store.setState({ player: next });
    listeners.forEach((listener) => listener(next));
  }

  async function persistPlayback() {
    const item = currentItem();
    if (!item?.episodeId) {
      return;
    }
    const duration = episodeDurationMs();
    const position = positionMs();
    const outroMs = Math.max(0, Number(item.outroSkipSeconds || 0) * 1000);
    const completionThreshold =
      outroMs > 0 && duration > outroMs ? duration - outroMs : duration;
    const isEnded = Boolean(audio.ended);
    const isCompleted =
      isEnded || (completionThreshold > 0 && position >= completionThreshold - 1000);
    await settingsRepository.setLastPlayedEpisodeId(item.episodeId);
    await podcastRepository.updatePlaybackState(
      item.episodeId,
      isCompleted ? 0 : position,
      duration,
      isCompleted,
      isCompleted
    );
  }

  async function maybeAutoSkipOutro() {
    if (audio.paused || audio.ended) {
      return;
    }
    const item = currentItem();
    if (!item?.episodeId) {
      return;
    }
    const duration = episodeDurationMs();
    if (duration <= 0) {
      return;
    }
    const outroMs = Math.max(0, Number(item.outroSkipSeconds || 0) * 1000);
    if (outroMs <= 0 || outroMs >= duration || userAllowsOutroPlayback) {
      return;
    }
    const position = positionMs();
    if (position < duration - outroMs) {
      return;
    }
    if (outroSkipTriggeredForEpisodeId === item.episodeId) {
      return;
    }
    outroSkipTriggeredForEpisodeId = item.episodeId;
    await podcastRepository.updatePlaybackState(item.episodeId, 0, duration, true, true);
    if (queue.length > 0 && queueIndex < queue.length - 1) {
      await skipToNext();
      return;
    }
    audio.currentTime = duration / 1000;
    audio.pause();
    updateStorePlayerState();
  }

  async function enableNormalization(enabled) {
    if (!enabled) {
      if (compressorNode) {
        compressorNode.disconnect();
        compressorNode = null;
      }
      if (audioSourceNode && audioContext) {
        audioSourceNode.disconnect();
        audioSourceNode.connect(audioContext.destination);
      }
      return;
    }
    if (!audioContext) {
      audioContext = new AudioContext();
    }
    if (!audioSourceNode) {
      audioSourceNode = audioContext.createMediaElementSource(audio);
    }
    if (compressorNode) {
      return;
    }
    compressorNode = audioContext.createDynamicsCompressor();
    compressorNode.threshold.value = -24;
    compressorNode.knee.value = 30;
    compressorNode.ratio.value = 12;
    compressorNode.attack.value = 0.003;
    compressorNode.release.value = 0.25;
    audioSourceNode.disconnect();
    audioSourceNode.connect(compressorNode);
    compressorNode.connect(audioContext.destination);
  }

  function loadEpisode(item, startMs) {
    audio.src = item.audioUrl;
    audio.currentTime = Math.max(0, startMs) / 1000;
    updateStorePlayerState();
  }

  async function playQueue(items, selectedEpisodeId) {
    if (!Array.isArray(items) || !items.length) {
      return;
    }
    const startIndex = items.findIndex((item) => item.episodeId === selectedEpisodeId);
    if (startIndex < 0) {
      return;
    }
    queue = [...items];
    queueIndex = startIndex;
    singleEpisode = null;
    userAllowsIntroPlayback = false;
    userAllowsOutroPlayback = false;
    outroSkipTriggeredForEpisodeId = null;
    const selected = queue[queueIndex];
    const startMs = computeStartPositionWithIntro(
      selected.playbackPositionMs || 0,
      selected.introSkipSeconds || 0
    );
    loadEpisode(selected, startMs);
    await audio.play();
    updateStorePlayerState();
  }

  async function playCalendarEpisode(episode) {
    if (!episode) {
      return;
    }
    queue = [];
    queueIndex = -1;
    singleEpisode = { ...episode };
    userAllowsIntroPlayback = false;
    userAllowsOutroPlayback = false;
    outroSkipTriggeredForEpisodeId = null;
    const startMs = computeStartPositionWithIntro(
      episode.playbackPositionMs || 0,
      episode.introSkipSeconds || 0
    );
    loadEpisode(singleEpisode, startMs);
    await audio.play();
    updateStorePlayerState();
  }

  async function togglePlayback() {
    if (!currentItem()) {
      return;
    }
    if (audio.paused) {
      await audio.play();
    } else {
      audio.pause();
    }
    updateStorePlayerState();
  }

  function seekToPosition(ms) {
    audio.currentTime = Math.max(0, Number(ms) || 0) / 1000;
    updateStorePlayerState();
  }

  function seekToPositionFromUser(ms) {
    const item = currentItem();
    if (!item) {
      return;
    }
    const target = Math.max(0, Number(ms) || 0);
    const introMs = Math.max(0, Number(item.introSkipSeconds || 0) * 1000);
    const duration = episodeDurationMs();
    const outroMs = Math.max(0, Number(item.outroSkipSeconds || 0) * 1000);

    if (introMs > 0 && target < introMs) {
      userAllowsIntroPlayback = true;
    }
    if (outroMs > 0 && duration > 0) {
      userAllowsOutroPlayback = target >= duration - outroMs;
      if (target < duration - outroMs) {
        outroSkipTriggeredForEpisodeId = null;
      }
    }

    seekToPosition(target);
  }

  function seekBack() {
    seekToPosition(positionMs() - SEEK_BACK_MS);
  }

  function seekForward() {
    seekToPosition(positionMs() + SEEK_FORWARD_MS);
  }

  async function skipToNext() {
    if (!queue.length || queueIndex >= queue.length - 1) {
      return;
    }
    queueIndex += 1;
    const item = queue[queueIndex];
    userAllowsIntroPlayback = false;
    userAllowsOutroPlayback = false;
    outroSkipTriggeredForEpisodeId = null;
    const startMs = computeStartPositionWithIntro(item.playbackPositionMs || 0, item.introSkipSeconds || 0);
    loadEpisode(item, startMs);
    await audio.play();
  }

  async function skipToPrevious() {
    if (queue.length) {
      if (queueIndex <= 0) {
        seekToPosition(0);
        return;
      }
      queueIndex -= 1;
      const item = queue[queueIndex];
      userAllowsIntroPlayback = false;
      userAllowsOutroPlayback = false;
      outroSkipTriggeredForEpisodeId = null;
      const startMs = computeStartPositionWithIntro(
        item.playbackPositionMs || 0,
        item.introSkipSeconds || 0
      );
      loadEpisode(item, startMs);
      await audio.play();
      return;
    }
    seekToPosition(0);
  }

  function subscribe(listener) {
    listeners.add(listener);
    listener(baseUiState());
    return () => listeners.delete(listener);
  }

  async function restoreLastPlayed() {
    const lastPlayedId = await settingsRepository.getLastPlayedEpisodeId();
    if (!lastPlayedId) {
      updateStorePlayerState();
      return;
    }
    const queueSnapshot = await podcastRepository.getQueueSnapshot();
    const index = queueSnapshot.findIndex(
      (episode) => episode.episodeId === lastPlayedId && !episode.isCompleted
    );
    if (index < 0) {
      updateStorePlayerState();
      return;
    }
    queue = [...queueSnapshot];
    queueIndex = index;
    singleEpisode = null;
    const item = queue[queueIndex];
    const startMs = computeStartPositionWithIntro(item.playbackPositionMs || 0, item.introSkipSeconds || 0);
    loadEpisode(item, startMs);
    audio.pause();
    updateStorePlayerState();
  }

  audio.addEventListener("timeupdate", updateStorePlayerState);
  audio.addEventListener("play", updateStorePlayerState);
  audio.addEventListener("pause", updateStorePlayerState);
  audio.addEventListener("ended", async () => {
    await persistPlayback();
    if (queue.length && queueIndex < queue.length - 1) {
      await skipToNext();
      return;
    }
    updateStorePlayerState();
  });

  const persistInterval = setInterval(() => {
    persistPlayback().catch(console.warn);
  }, 2000);
  const outroInterval = setInterval(() => {
    maybeAutoSkipOutro().catch(console.warn);
  }, 350);

  settingsRepository.subscribe((settings) => {
    enableNormalization(settings.volumeNormalizationEnabled).catch(console.warn);
  });

  restoreLastPlayed().catch(console.warn);

  function destroy() {
    clearInterval(persistInterval);
    clearInterval(outroInterval);
    audio.pause();
    audio.src = "";
  }

  return {
    subscribe,
    getUiState: () => store.getState().player || baseUiState(),
    playQueue,
    playCalendarEpisode,
    togglePlayback,
    seekBack,
    seekForward,
    seekToPosition,
    seekToPositionFromUser,
    skipToPrevious,
    skipToNext,
    destroy,
  };
}

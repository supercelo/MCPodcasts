import { createStore } from "./state.js";
import { createI18n } from "./i18n.js";
import { createDatabase } from "./db.js";
import { createFeatureRules } from "./featureRules.js";
import { createRssParser } from "./rssParser.js";
import { createDiscoveryClient } from "./discovery.js";
import { createSettingsRepository } from "./settingsRepository.js";
import { createPodcastRepository } from "./podcastRepository.js";
import { createPlayer } from "./player.js";
import { createApp } from "./ui/app.js";

async function bootstrap() {
  const store = createStore();
  const i18n = await createI18n();
  const db = await createDatabase();
  const rules = createFeatureRules();
  const rssParser = createRssParser();
  const discovery = createDiscoveryClient();
  const settingsRepository = createSettingsRepository();
  const podcastRepository = createPodcastRepository({
    db,
    rules,
    rssParser,
    settingsRepository,
  });
  const player = createPlayer({
    podcastRepository,
    settingsRepository,
    store,
  });

  const app = createApp({
    root: document.getElementById("app"),
    store,
    i18n,
    podcastRepository,
    discovery,
    settingsRepository,
    player,
    rules,
  });

  await app.init();
}

bootstrap().catch((error) => {
  console.error(error);
  const root = document.getElementById("app");
  if (!root) {
    return;
  }
  root.innerHTML = `
    <div class="app-shell">
      <main class="content">
        <div class="card danger">
          <h2>Fatal error</h2>
          <pre>${String(error.message || error)}</pre>
        </div>
      </main>
    </div>
  `;
});

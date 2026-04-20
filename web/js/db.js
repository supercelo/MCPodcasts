const DB_NAME = "mcpodcasts-web";
const DB_VERSION = 1;

function openDatabase() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onerror = () => reject(request.error);
    request.onupgradeneeded = () => {
      const db = request.result;

      const podcasts = db.createObjectStore("podcasts", { keyPath: "feedUrl" });
      podcasts.createIndex("by_title", "title", { unique: false });

      const episodes = db.createObjectStore("episodes", { keyPath: "episodeId" });
      episodes.createIndex("by_podcast_id", "podcastId", { unique: false });
      episodes.createIndex("by_published_at", "publishedAt", { unique: false });
      episodes.createIndex("by_podcast_and_published", ["podcastId", "publishedAt"], { unique: false });

      db.createObjectStore("meta", { keyPath: "key" });
    };
    request.onsuccess = () => resolve(request.result);
  });
}

function transactionDone(transaction) {
  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve();
    transaction.onabort = () => reject(transaction.error);
    transaction.onerror = () => reject(transaction.error);
  });
}

function requestAsPromise(request) {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

export async function createDatabase() {
  const db = await openDatabase();

  async function getAllFrom(storeName) {
    const tx = db.transaction(storeName, "readonly");
    const store = tx.objectStore(storeName);
    const result = await requestAsPromise(store.getAll());
    await transactionDone(tx);
    return result;
  }

  async function getManyByIndex(storeName, indexName, key) {
    const tx = db.transaction(storeName, "readonly");
    const store = tx.objectStore(storeName);
    const index = store.index(indexName);
    const result = await requestAsPromise(index.getAll(key));
    await transactionDone(tx);
    return result;
  }

  async function get(storeName, key) {
    const tx = db.transaction(storeName, "readonly");
    const store = tx.objectStore(storeName);
    const result = await requestAsPromise(store.get(key));
    await transactionDone(tx);
    return result ?? null;
  }

  async function put(storeName, value) {
    const tx = db.transaction(storeName, "readwrite");
    tx.objectStore(storeName).put(value);
    await transactionDone(tx);
  }

  async function putMany(storeName, values) {
    const tx = db.transaction(storeName, "readwrite");
    const store = tx.objectStore(storeName);
    values.forEach((value) => store.put(value));
    await transactionDone(tx);
  }

  async function deleteKey(storeName, key) {
    const tx = db.transaction(storeName, "readwrite");
    tx.objectStore(storeName).delete(key);
    await transactionDone(tx);
  }

  async function deleteWhere(storeName, predicate) {
    const tx = db.transaction(storeName, "readwrite");
    const store = tx.objectStore(storeName);
    const all = await requestAsPromise(store.getAll());
    all.forEach((row) => {
      if (predicate(row)) {
        store.delete(row[store.keyPath]);
      }
    });
    await transactionDone(tx);
  }

  async function replacePodcastEpisodes(feedUrl, episodes) {
    const tx = db.transaction("episodes", "readwrite");
    const store = tx.objectStore("episodes");
    const existing = await requestAsPromise(store.index("by_podcast_id").getAll(feedUrl));
    const incomingIds = new Set(episodes.map((episode) => episode.episodeId));

    for (const item of existing) {
      if (!incomingIds.has(item.episodeId)) {
        store.delete(item.episodeId);
      }
    }
    for (const episode of episodes) {
      store.put(episode);
    }
    await transactionDone(tx);
  }

  return {
    get,
    put,
    putMany,
    deleteKey,
    getAllFrom,
    getManyByIndex,
    deleteWhere,
    replacePodcastEpisodes,
  };
}

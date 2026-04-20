const ITUNES_SEARCH_URL = "https://itunes.apple.com/search";

export function createDiscoveryClient() {
  async function search(query) {
    const normalized = String(query || "").trim();
    if (!normalized) {
      return [];
    }

    const url = new URL(ITUNES_SEARCH_URL);
    url.searchParams.set("term", normalized);
    url.searchParams.set("media", "podcast");
    url.searchParams.set("entity", "podcast");
    url.searchParams.set("limit", "25");

    const response = await fetch(url.toString(), {
      headers: { "User-Agent": "Podcast MC/1.0" },
    });
    if (!response.ok) {
      throw new Error(`Couldn't search podcasts (${response.status}).`);
    }
    const payload = await response.json();
    const items = Array.isArray(payload.results) ? payload.results : [];

    return items
      .map((item) => ({
        feedUrl: String(item.feedUrl || "").trim(),
        title: String(item.collectionName || "").trim(),
        author: String(item.artistName || "").trim(),
        imageUrl:
          String(item.artworkUrl600 || "").trim() ||
          String(item.artworkUrl100 || "").trim() ||
          null,
      }))
      .filter((item) => item.feedUrl && item.title);
  }

  return { search };
}

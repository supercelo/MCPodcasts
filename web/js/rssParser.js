function cleanText(raw) {
  const html = String(raw || "");
  const text = html.replace(/<[^>]*>/g, " ");
  return text.replace(/\s+/g, " ").trim();
}

function parsePublishedAt(raw) {
  const trimmed = String(raw || "").trim();
  if (!trimmed) {
    return 0;
  }

  const direct = Date.parse(trimmed);
  if (!Number.isNaN(direct)) {
    return direct;
  }

  const fallback = Date.parse(trimmed.replace(/GMT$/, "+0000"));
  if (!Number.isNaN(fallback)) {
    return fallback;
  }

  return 0;
}

function getFirstText(item, selectors) {
  for (const selector of selectors) {
    const node = item.querySelector(selector);
    if (node?.textContent?.trim()) {
      return node.textContent.trim();
    }
  }
  return "";
}

function getEnclosureAudioUrl(item) {
  const enclosures = [...item.getElementsByTagName("enclosure")];
  for (const enclosure of enclosures) {
    const url = enclosure.getAttribute("url");
    if (url) {
      return url.trim();
    }
  }
  return "";
}

function getItunesImageHref(node) {
  const imageNode =
    node.querySelector("itunes\\:image") ||
    [...node.getElementsByTagName("*")].find(
      (child) => child.localName === "image" && (child.prefix || "").toLowerCase() === "itunes"
    );
  const href = imageNode?.getAttribute("href");
  return href ? href.trim() : "";
}

function getChannelImage(channel) {
  const itunesImage = getItunesImageHref(channel);
  if (itunesImage) {
    return itunesImage;
  }
  const imageUrl = channel.querySelector("image > url")?.textContent?.trim();
  return imageUrl || "";
}

function parseItem(item, channelImageUrl) {
  const guid = getFirstText(item, ["guid"]);
  const title = getFirstText(item, ["title"]);
  const summary = cleanText(getFirstText(item, ["summary", "description"]));
  const episodeUrl = getFirstText(item, ["link"]);
  const publishedRaw = getFirstText(item, ["pubDate", "published", "updated"]);
  const durationLabel = getFirstText(item, ["itunes\\:duration", "duration"]);
  const audioUrl = getEnclosureAudioUrl(item);
  const episodeImage = getItunesImageHref(item);

  if (!title || !audioUrl) {
    return null;
  }

  return {
    guid: guid || null,
    title,
    summary: summary || null,
    audioUrl,
    artworkUrl: episodeImage || channelImageUrl || null,
    episodeUrl: episodeUrl || null,
    publishedAt: parsePublishedAt(publishedRaw),
    durationLabel: durationLabel || null,
  };
}

export function createRssParser() {
  function parse(xmlString) {
    const parser = new DOMParser();
    const documentXml = parser.parseFromString(xmlString, "application/xml");
    const parserError = documentXml.querySelector("parsererror");
    if (parserError) {
      throw new Error("Invalid RSS XML.");
    }

    const channel = documentXml.querySelector("channel") || documentXml.documentElement;
    const title = getFirstText(channel, ["title"]) || "Podcast";
    const author =
      getFirstText(channel, ["itunes\\:author"]) || getFirstText(channel, ["author"]) || null;
    const description = cleanText(getFirstText(channel, ["description"])) || null;
    const siteUrl = getFirstText(channel, ["link"]) || null;
    const imageUrl = getChannelImage(channel) || null;

    const items = [...documentXml.getElementsByTagName("item")];
    const episodes = items
      .map((item) => parseItem(item, imageUrl))
      .filter((episode) => Boolean(episode));

    return {
      title,
      author,
      description,
      imageUrl,
      siteUrl,
      episodes,
    };
  }

  return { parse };
}

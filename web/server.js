const http = require("http");
const fs = require("fs");
const path = require("path");

const HOST = "127.0.0.1";
const PORT = Number(process.env.PORT || 8080);
const WEB_ROOT = __dirname;

const MIME_TYPES = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".svg": "image/svg+xml",
  ".ico": "image/x-icon",
};

function setCorsHeaders(response) {
  response.setHeader("Access-Control-Allow-Origin", "*");
  response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
  response.setHeader("Access-Control-Allow-Headers", "Content-Type");
}

function sendText(response, statusCode, body, contentType = "text/plain; charset=utf-8") {
  response.writeHead(statusCode, { "Content-Type": contentType });
  response.end(body);
}

function sanitizePath(urlPathname) {
  const decoded = decodeURIComponent(urlPathname);
  const withoutQuery = decoded.split("?")[0];
  const normalized = path.posix.normalize(withoutQuery);
  if (normalized.includes("..")) {
    return null;
  }
  return normalized === "/" ? "/index.html" : normalized;
}

async function handleProxy(request, response, requestUrl) {
  setCorsHeaders(response);
  if (request.method === "OPTIONS") {
    response.writeHead(204);
    response.end();
    return;
  }

  const target = requestUrl.searchParams.get("url");
  if (!target) {
    sendText(response, 400, "Missing url query parameter.");
    return;
  }

  let targetUrl;
  try {
    targetUrl = new URL(target);
  } catch {
    sendText(response, 400, "Invalid proxy target URL.");
    return;
  }

  if (!["http:", "https:"].includes(targetUrl.protocol)) {
    sendText(response, 400, "Unsupported protocol.");
    return;
  }

  try {
    const upstream = await fetch(targetUrl.toString(), {
      headers: {
        "User-Agent": "MCPodcasts-Web/1.0",
      },
    });

    const contentType = upstream.headers.get("content-type") || "application/octet-stream";
    response.writeHead(upstream.status, {
      "Content-Type": contentType,
      "Access-Control-Allow-Origin": "*",
    });
    const arrayBuffer = await upstream.arrayBuffer();
    response.end(Buffer.from(arrayBuffer));
  } catch (error) {
    sendText(response, 502, `Proxy request failed: ${error.message}`);
  }
}

function handleStatic(request, response, requestUrl) {
  const sanitized = sanitizePath(requestUrl.pathname);
  if (!sanitized) {
    sendText(response, 400, "Invalid path.");
    return;
  }

  const absolutePath = path.join(WEB_ROOT, sanitized);
  if (!absolutePath.startsWith(WEB_ROOT)) {
    sendText(response, 403, "Forbidden.");
    return;
  }

  fs.stat(absolutePath, (statErr, stats) => {
    if (statErr || !stats.isFile()) {
      sendText(response, 404, "Not found.");
      return;
    }

    const extension = path.extname(absolutePath).toLowerCase();
    const contentType = MIME_TYPES[extension] || "application/octet-stream";
    response.writeHead(200, { "Content-Type": contentType });
    fs.createReadStream(absolutePath).pipe(response);
  });
}

const server = http.createServer(async (request, response) => {
  const requestUrl = new URL(request.url, `http://${request.headers.host}`);

  if (request.method !== "GET" && request.method !== "OPTIONS") {
    sendText(response, 405, "Method not allowed.");
    return;
  }

  if (requestUrl.pathname === "/proxy") {
    await handleProxy(request, response, requestUrl);
    return;
  }

  handleStatic(request, response, requestUrl);
});

server.listen(PORT, HOST, () => {
  console.log(`MCPodcasts web server running on http://${HOST}:${PORT}`);
});

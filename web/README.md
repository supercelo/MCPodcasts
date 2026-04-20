# MCPodcasts Web Replica

Browser-based replica of the Android `MCPodcasts` app.

## Run

From the `web` directory:

```bash
node server.js
```

Open [http://127.0.0.1:8080](http://127.0.0.1:8080).

## Notes

- The app is a static SPA (`index.html` + `styles.css` + `js/**`).
- RSS feeds are fetched through `/proxy?url=...` on the local server to bypass browser CORS limitations.
- iTunes search runs directly from the browser.

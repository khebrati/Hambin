# Hambin browser client

React/TypeScript/Vite client for the migrated WatchParty backend. Normal entry uses real Firebase anonymous identity, profiles, rooms, Socket.IO state, local video playback, explicit forward-only sync, and optional audio-only WebRTC mesh voice. No registration screen is required.

## Run locally

Start PostgreSQL, the Firebase Auth Emulator, and the Hambin Node runtime using [the local stack instructions](../backend/local/README.md). The default API is `127.0.0.1:8081`; the Auth Emulator is `127.0.0.1:9099` with project `demo-hambin`. Enable the local Coturn profile and pass the same TURN settings to Node to test voice.

```sh
cd webUI
npm ci
cp .env.example .env.local
npm run dev -- --host 127.0.0.1
```

Open <http://127.0.0.1:5173/>. Vite proxies `/v1` and WebSocket upgrades at `/socket.io` to `BACKEND_PROXY_TARGET`. Firebase browser configuration is public client configuration; never put an Admin key or TURN shared secret in a `VITE_` variable. The ignored `.env.local` is local configuration only.

To test two different people, use separate browser profiles or an incognito window. Tabs in the same profile intentionally share a Firebase identity. Each identity has one room role and one voice role, so a second tab joining the same room replaces the first tab's role. Do not run the same identity in multiple active room tabs.

The SDK persists anonymous credentials. A transient outage does not replace identity or ownership. Reloading a room tab rejoins using its session-local code; the last owned room code is retained locally for the Home return card. An ended room is not returnable. After restarting the Auth Emulator, clear site data explicitly for a fresh test identity because emulator accounts are transient. Do not clear production site data unless intentionally abandoning the saved identity.

## Player and voice

The present owner starts or ends an HTTPS direct-video stream. Play, pause, seeking, volume, subtitles, and audio-track choices affect only this browser. Sync privately advances the requester to a recent room position and never rewinds. Reloading or starting a different stream resets local player state; reconnecting within the same stream preserves it.

Progressive playback uses HTML video; HLS uses native support when available and otherwise `hls.js` for `.m3u8` URLs. Formats/codecs depend on the browser. HLS resources need appropriate cross-origin access; videos are never proxied through the backend. Live streams hide seeking controls. Local subtitles support WebVTT (`.vtt`); HLS audio/subtitle selectors appear when supported tracks are exposed by `hls.js`. Native browser embedded-track selection, ASS/SSA/SRT conversion, and Android-only media/notification APIs are outside this browser implementation. A blocked autoplay action exposes Play or Resume voice audio.

Voice is opt-in and initially muted. Join voice first, then explicitly unmute. Microphone access requires localhost or HTTPS and browser permission. Disconnect voice stops capture while keeping the room open. Stream changes leave voice intact. Leaving releases peers and capture immediately, then confirms the HTTP leave. If HTTP fails, retry Leave. Unexpected tab closure relies on server lease expiry and the reconnect grace period; browser suspension cannot promise Android foreground-service continuity.

## Hosted configuration

The shared VPS deployment is documented in [deployment operations](../backend/deploy/README.md). Use real public Firebase client settings, remove the emulator URL, and serve the build over HTTPS behind an ingress proxying `/v1` and `/socket.io` to the migrated runtime. Same-origin hosting is the supported default; the current backend does not expose browser REST CORS. `VITE_BACKEND_URL` is an explicit alternate origin and requires that deployment to provide a matching origin policy. `vite preview` serves static assets only; it does not provide the development API proxy. Production builds reject Auth Emulator configuration.

## Fixture previews

Explicit `?preview=...` links use isolated local fixtures and never initialize Auth, HTTP, or Socket.IO. Supported keys: `home`, `settings-empty`, `settings-populated`, `party-manager`, `party-manager-loading`, `party-manager-error`, `room-preview`, `room-preview-owner-absent`, `room-preview-full`, `room-preview-ended`, `owner-empty`, `player-loading`, `player-playing`, `player-paused`, `player-buffering`, `player-error`, `participant-playing`, and `participant-owner-absent`.

Use `&theme=dark`, `&largeText=1`, and `&dir=rtl` to inspect visual states. Normal entry accepts the same visual parameters and `?invite=ABCDEFGH` for a real room preview.

The directional Sync Center exploration is archived on Git branch `archive/web-directional-sync`. Its original mixed commit is `e4f7071`; rollback commit `22c34ed` changes only `webUI`. Recover just those frontend changes later with `git diff e4f7071^ e4f7071 -- webUI` and apply selectively on a new branch. Do not revert the mixed Android/backend commit wholesale.

## Validation

```sh
npm run build
npm test
npx playwright install chromium
npm run test:e2e
```

Browser integration tests need the local backend/Auth Emulator/PostgreSQL running. Mesh tests also need Coturn enabled on the backend. They use synthetic microphone devices and independent browser identities; they do not establish human-audible conversation or Android interoperability. Set `CHROME_PATH` to an installed Chrome executable if using it instead of Playwright's Chromium.

Set `ROOMIO_TEST_VIDEO` to an absolute path to a short, seekable MP4 fixture (at least five seconds) to enable the real playback/sync/reconnect test. The browser serves that file through a test-only intercepted HTTPS media request. For example, download the CC0 flower sample from `https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4` outside the repository. Set `ROOMIO_TEST_RELAY=1` to force test peer connections through TURN and assert a selected relay candidate. Synthetic microphone tests cover two, four, and eight participants and inbound audio packets on every peer connection.

Production dependency audit pins the unused Firestore gRPC transitive dependency to patched `@grpc/grpc-js` via an override; the application imports only Firebase App/Auth. Build assets and Playwright traces/results stay ignored.

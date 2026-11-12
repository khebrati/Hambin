# Room bug reports

Accepted 2026-11-11: both clients expose a bug icon beside Invite/Share in the room app bar. The dialog has one required large description field (1–8,000 characters), Cancel and Send. Sending disables editing/repeated submission; success acknowledges receipt and clears the draft. Closing or a failed send retains the draft within that room visit; errors offer retry. Browser offline state disables Send. Native offline sends fail recoverably. Optional screenshots are supported on Android and web (2026-11-12); no category, email, or report-history UI.

## HTTP contract

Authenticated `POST /v1/rooms/{roomId}/bug-reports`, JSON:

- `reportId`: client-generated UUID, retained for retries of an unchanged description and screenshots.
- `description`: trimmed nonempty string, at most 8,000 Unicode characters.
- `clientInfo`: string map, at most 32 entries, keys at most 64 characters and values at most 2,048 characters. Includes platform, app build, OS/device or browser version/user agent, locale and screen information where available. Browser hardware model is available only when the browser exposes it.
- `sessionStartedAt`: ISO UTC timestamp of application/session log initialization.
- `screenshots`: optional array (default empty), at most three objects `{contentType, data}`. `contentType` is `image/png`, `image/jpeg`, or `image/webp`; `data` is canonical padded base64 without a data-URL prefix. Maximum 5 MiB decoded bytes in total. Reject malformed base64, empty data, and mismatched image signatures with `INVALID_REPORT`; excessive count/bytes with `REPORT_TOO_LARGE`. Store with the report and deliver as numbered Sentry attachments, using generated names rather than local filenames. Support clipboard image paste on Android and web (including Ctrl/Cmd+V on web). Selection is explicit; preview/remove before Send. Retain images on failure/dismiss, freeze while reading/sending, clear after success. Changing images creates a new retry payload/ID. No automatic screenshot capture.
- `log`: complete captured application log through Send, at most 8 MiB UTF-8. No silent truncation: larger reports fail with `REPORT_TOO_LARGE` and retain the draft.

Response `201 {"reportId":"…"}` only after PostgreSQL commit. Identical report ID/identity retries return the original acknowledgement; changed content returns `409 REPORT_CONFLICT`. Active room membership is required on first insert; retries remain acknowledgeable after departure. Limits: five submissions/minute per IP plus ten new reports/day per identity. Standard auth refresh, 400 INVALID_REPORT, 403 NOT_A_MEMBER, 413 REPORT_TOO_LARGE, 429 RATE_LIMITED and recoverable service/network failures apply.

Application logs begin at web module initialization/native application graph startup and reset with page reload/process restart. Keep the full session in process memory until exit; do not collect system-wide Android logcat, cookies, credentials, microphone audio, screenshots, or local files automatically. Only explicitly selected screenshot files are uploaded. Capture Kermit messages in native, browser console/errors and HTTP outcomes plus room/voice/player state transitions. Redact URLs, authorization values, JWTs, and known credential fields before retaining log messages; structured HTTP diagnostics omit request/response bodies. Device metadata does not include serial numbers or persistent hardware identifiers.

New reports also enqueue background Sentry delivery under DD-055; descriptions, allowlisted client context and redacted session logs and explicitly selected screenshots are forwarded after durable acceptance. See [observability operations](observability.md). Historical reports are not forwarded automatically.

Store reports privately in the backend PostgreSQL `bug_reports` table with authenticated identity, room, server timestamp and diagnostic payload. No client read API. Retain for 30 days, delete expired reports at startup/hourly and before new writes. Operators inspect through privileged database access; never expose reports in public HTTP logs. This feature does not deploy itself.

## Validation (2026-11-11)

Backend typecheck and ten unit checks passed. A real disposable PostgreSQL integration check passed authentication, member restrictions, complete diagnostic persistence, duplicate retries, payload conflicts and expiry deletion. Browser production build and 79 unit tests passed. Two Playwright report workflows passed desktop light and 360px dark/large-text/RTL, including draft retention across Escape, offline disabled sending, failure/retry, identical retry payload, redacted earlier logs and cleared successful draft. Screenshot review prompted a scrollable dialog body with persistent action buttons; the updated workflows passed.

Native data JVM tests: 70 passed; shared UI JVM tests: 56 passed, one skipped. New regression checks cover log redaction and draft/error/sending/idempotent UI behavior. Android debug build passed with the Metro graph. iOS is implemented through the platform metadata seam but cannot be built or validated on this Linux host. No physical-device install, commit or deployment is part of this change.

## Client diagnostic instrumentation (2026-11-12)

Important paths record timestamped application events in release and debug builds. Browser events use `diagnosticEvent` with primitive metadata; native events use the existing Kermit writer and Metro-provided HTTP client. HTTP diagnostics use a local increasing operation number and monotonic elapsed milliseconds. These numbers correlate the local start/outcome; they are not backend trace IDs.

- Authentication: guest creation/restoration, renewal, rejection and explicit reset; no credentials or profile values.
- API operations: safe route template, method, status, timing, transport/cancellation failures and decode errors. This covers create/preview/join/leave, stream start/abort, profile changes, chat send/history and bug-report delivery. Browser auth retries share an operation number and record each response; native retries are separate HTTP exchanges alongside auth recovery events.
- Realtime: connection attempts, snapshots, reconnect delay/attempt, snapshot recovery, sync requests/results/timeouts, auth recovery and closure.
- Playback: loading/playing/paused/buffering/error transitions, browser engine recovery and autoplay denial; native Media3 error code, track-group changes, position at state changes and player lifecycle/release.
- Voice: microphone acquisition/permission failures, mute intent and acknowledged capture, connection state and peer state/retry information. Android retains existing route and periodic aggregate RTP diagnostics. No audio, SDP, ICE candidates, device labels or network addresses are added.
- Lifecycle: browser online/offline, visibility and page restore; Android player lifecycle and foreground room service command/failure/destruction/task removal. Report attempts record retry status and acceptance without description text.

New instrumentation must pass safe primitive metadata, never raw application objects, profile/chat/report text, invitation codes, filenames, request/response bodies, headers or exception messages. Native raw Ktor request logging is replaced with backend-only route templates. Unknown route details are discarded; query values are discarded. URL redaction also covers WebSocket and local `content:`, `file:` and `blob:` resources before session retention. Existing console/Kermit error capture remains available with redaction.

State logs record transitions rather than every render, playback-position tick or speaking-level sample. Keep the existing full-session retention and explicit 8 MiB upload-limit behavior; diagnostics do not silently discard older context or upload automatically. Unchanged report retries retain the original payload even when later diagnostic events have been recorded.

Validation: browser production build and 96 unit tests passed. Four Chrome checks passed: desktop/mobile bug-report attachment, redaction and identical-payload retry; local microphone activity; real integrated-player seeking during playback and pause. Native data and shared UI JVM suites passed (one pre-existing skipped shared UI test), including HTTP outcome/timing, credential/content exclusion, transport failure, cancellation and URL redaction tests. Final Android debug assembly passed. No physical Android or iOS runtime verification, deployment or commit was performed for this change.


## Screenshot and clipboard validation (2026-11-12)

Browser/backend production builds passed. Browser unit suite: 111 passed. Backend unit suite: 27 passed (12 integration-gated skips). Two actual isolated PostgreSQL checks passed report image persistence, changed-image conflicts, retention, and queued Sentry delivery with exact binary screenshot bytes. Five Chrome workflows passed, including file selection, clipboard paste events and the real browser Clipboard API, preview/removal, offline and dismissed drafts, unchanged retry payloads, changed-image IDs, invalid files/counts, and success clearing. Reviewed desktop light and compact dark/large-text/RTL screenshots. Clipboard PNGs can be re-encoded by the browser; the acceptance check compares pasted pixels.

Native data JVM suite: 105 tests, zero failures/errors, one gated skip. Shared UI JVM suite: 83 tests, zero failures/errors, one existing skip. Regression checks cover screenshot retry identity, retained images, frozen sending/reading controls, paste/remove callbacks, and English/light plus Persian/dark/large-text dialog rendering. Android debug assembly and shared app JVM compilation passed. No Android device was connected, so Android system picker/clipboard runtime verification remains outstanding; iOS/macOS was not validated. No deployment was performed. The exception-boundary audit finds no new unlogged boundaries; it still flags two pre-existing browser voice boundaries outside this change.

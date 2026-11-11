# Room bug reports

Accepted 2026-11-11: both clients expose a bug icon beside Invite/Share in the room app bar. The dialog has one required large description field (1–8,000 characters), Cancel and Send. It explains that device/browser details and the application session log are attached. Sending disables editing/repeated submission; success acknowledges receipt and clears the draft. Closing or a failed send retains the draft within that room visit; errors offer retry. Browser offline state disables Send. Native offline sends fail recoverably. No category, attachments, email, or report-history UI.

## HTTP contract

Authenticated `POST /v1/rooms/{roomId}/bug-reports`, JSON:

- `reportId`: client-generated UUID, retained for retries of an unchanged description.
- `description`: trimmed nonempty string, at most 8,000 Unicode characters.
- `clientInfo`: string map, at most 32 entries, keys at most 64 characters and values at most 2,048 characters. Includes platform, app build, OS/device or browser version/user agent, locale and screen information where available. Browser hardware model is available only when the browser exposes it.
- `sessionStartedAt`: ISO UTC timestamp of application/session log initialization.
- `log`: complete captured application log through Send, at most 8 MiB UTF-8. No silent truncation: larger reports fail with `REPORT_TOO_LARGE` and retain the draft.

Response `201 {"reportId":"…"}` only after PostgreSQL commit. Identical report ID/identity retries return the original acknowledgement; changed content returns `409 REPORT_CONFLICT`. Active room membership is required on first insert; retries remain acknowledgeable after departure. Limits: five submissions/minute per IP plus ten new reports/day per identity. Standard auth refresh, 400 INVALID_REPORT, 403 NOT_A_MEMBER, 413 REPORT_TOO_LARGE, 429 RATE_LIMITED and recoverable service/network failures apply.

Application logs begin at web module initialization/native application graph startup and reset with page reload/process restart. Keep the full session in process memory until exit; do not collect system-wide Android logcat, cookies, credentials, microphone audio, screenshots, or local files. Capture Kermit messages in native, browser console/errors and HTTP outcomes plus room/voice/player state transitions. Redact URLs, authorization values, JWTs, and known credential fields before retaining log messages; structured HTTP diagnostics omit request/response bodies. Device metadata does not include serial numbers or persistent hardware identifiers.

Store reports privately in the backend PostgreSQL `bug_reports` table with authenticated identity, room, server timestamp and diagnostic payload. No client read API. Retain for 30 days, delete expired reports at startup/hourly and before new writes. Operators inspect through privileged database access; never expose reports in public HTTP logs. This feature does not deploy itself.

## Validation (2026-11-11)

Backend typecheck and ten unit checks passed. A real disposable PostgreSQL integration check passed authentication, member restrictions, complete diagnostic persistence, duplicate retries, payload conflicts and expiry deletion. Browser production build and 79 unit tests passed. Two Playwright report workflows passed desktop light and 360px dark/large-text/RTL, including draft retention across Escape, offline disabled sending, failure/retry, identical retry payload, redacted earlier logs and cleared successful draft. Screenshot review prompted a scrollable dialog body with persistent action buttons; the updated workflows passed.

Native data JVM tests: 70 passed; shared UI JVM tests: 56 passed, one skipped. New regression checks cover log redaction and draft/error/sending/idempotent UI behavior. Android debug build passed with the Metro graph. iOS is implemented through the platform metadata seam but cannot be built or validated on this Linux host. No physical-device install, commit or deployment is part of this change.

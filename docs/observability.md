# Observability

Accepted in DD-055: PostHog EU measures usage, Sentry receives automatic errors and manual feedback, and PostgreSQL remains authoritative for submitted reports. Session replay and automatic content capture are off. Tracing, log streaming, infrastructure metrics and profiling are outside this initial integration.

## Configuration

On the configured development machine, run `python3 scripts/configure-observability.py`. It reads owner-only account setup files under `~/.config/hambin/` and merges ingestion settings into ignored files without printing their values:

- `webUI/.env.local`: `VITE_SENTRY_DSN`, `VITE_POSTHOG_TOKEN`, `VITE_POSTHOG_HOST`.
- `backend/watchparty/.env.observability`: `SENTRY_DSN`, `SENTRY_ENVIRONMENT`.
- `Roomio/observability.properties`: `sentry.dsn`, `posthog.token`, `posthog.host`, `release`.

Ingestion identifiers are embedded in clients by design. Account/OAuth/upload tokens must never enter client code, Gradle-generated source, Git, or runtime containers. The Sentry CLI authenticates separately in the local user configuration.

Browser development is disabled by default. Set `VITE_OBSERVABILITY_ENABLED=true` and `VITE_OBSERVABILITY_ENVIRONMENT=development` when explicitly verifying it. Production builds enable configured monitoring unless `VITE_OBSERVABILITY_ENABLED=false`. Native Android debug and release builds use `development` and `production`; desktop defaults to development and supports `-Dhambin.environment=production`. iOS uses the native binary's debug flag.

For a local backend, load the ignored file explicitly, for example `node --env-file=.env.observability server/server.ts`, alongside the existing database configuration. Production Compose reads `SENTRY_DSN` from `/opt/hambin/.env`, sets the environment to production, and identifies the release with `RELEASE_ID`. A missing DSN disables capture/delivery without blocking the app.

## Usage events and privacy

The browser captures `app_opened`, a sanitized `$pageview` with route `/`, `room_created`, `room_joined`, `room_left`, `playback_started`, and `bug_report_submitted`. Native captures the same app/open/create/join/playback/report events, using the existing Ktor transport behind the data layer. Product mutations emit only after successful API acceptance. Native analytics uses a bounded queue, a five-second request budget and best-effort delivery; telemetry cannot block product actions.

PostHog identifiers are random per browser/installation and separate from backend authentication. Clearing storage or using another device creates another identifier. These counts therefore measure browser/installations, not precisely deduplicated people. Filter dashboards by environment and platform.

No names, backend identities, room IDs/codes, invitation URLs, chat messages, media URLs, filenames, microphone audio or screenshots are added to analytics. The browser SDK's default properties are restricted to transport/identity metadata and explicitly allowed platform/environment/release/action/route fields. Browser Do Not Track is respected.

Automatic errors preserve stack locations while removing request payloads, user fields, extras, breadcrumbs and free-form exception text. Android/JVM sanitization uses the native SDK callback because the KMP adapter does not apply all exception-field edits. Native integrations live in `data`; domain and presentation remain independent of SDKs.

Handled exceptions are also retained in browser/native session logs and backend operator logs. Every recovery boundary records `exception.handled`, a fixed source/operation label, exception type, redacted message and stack; JavaScript causes and aggregate failures are included with cycle/depth bounds. Browser stack URLs retain source filename/line/column without the origin or query. Payload objects, request bodies and headers are never appended. Existing credential/resource-URL redaction applies before retention. These local diagnostics enter Sentry through an explicit manual report attachment; automatic Sentry event sanitization remains separate. Coroutine cancellation is logged at debug severity and keeps its existing propagation/Result semantics. Logging failures cannot interrupt application recovery or recursively report themselves.

Run `node scripts/check-exception-logging.cjs` from the repository root after installing backend dev dependencies to audit active browser, native and Node backend catch/rejection boundaries. Native `runCatching` uses a logging adapter; the pure domain coordinator receives its logging callback from the production data binding. Historical backend sources and isolated design studies are outside the runtime audit.

## Manual reports

The existing report endpoint and acknowledgement semantics remain unchanged. New report insertion also initializes delivery state within the same transaction. Earlier reports remain unqueued. Every five seconds the single-runtime worker selects up to five due reports, forwards feedback plus `session.log`, and marks delivery only after Sentry's HTTP acceptance. The report UUID, without hyphens, is the stable Sentry event ID across retries.

Delivery retries after transient failures with exponential backoff, respecting Sentry's retry headers. Expired reports are never delivered. The original 30-day retention and durable retry/conflict rules still apply. Sentry has its own attachment/feedback retention and quotas; deleting a PostgreSQL row does not delete its Sentry copy. Per stakeholder direction on 2026-11-12, English/Persian report dialogs omit the diagnostic storage and Sentry delivery notice.

Operator status query (private database access only):

```sql
SELECT report_id, created_at, sentry_attempts, sentry_next_attempt_at,
       sentry_delivered_at
FROM bug_reports
ORDER BY created_at DESC
LIMIT 50;
```

Stable event IDs limit duplicates when delivery succeeds but the database acknowledgement is interrupted. This is at-least-once delivery; it is not a cross-service transaction. A Sentry account ingestion quota or attachment quota can still prevent data retention after HTTP acceptance, so monitor those quotas in Sentry.

## Readable releases

Set `VITE_APP_BUILD` to a unique release identifier. Browser source-map generation/upload is enabled when `SENTRY_AUTH_TOKEN` is supplied to the build environment. JavaScript uses esbuild minification: a live minified-build check confirmed that the application frame resolves to its TypeScript source and context (the default Oxc output mapped this test frame incorrectly). The Vite plugin uploads to `erfan-co/watchparty-web`, and removes uploaded `.map` files from `dist`. The standard `deploy.sh build` uses existing Sentry CLI authentication when no upload token was explicitly supplied, and uses the deployment release ID in the browser build. Keep the upload token in CI secrets; it is never a `VITE_` variable.

The Android Sentry Gradle plugin creates a ProGuard mapping identifier for release builds. `SENTRY_AUTH_TOKEN` enables automatic mapping upload to `erfan-co/watchparty-native`; without it the mapping remains local. Automatic bytecode tracing, logcat capture, source uploads and native-symbol uploads are disabled. The KMP plugin owns SDK installation. Apple linkage uses the KMP SDK's SwiftPM integration on macOS and requires Xcode validation before an iOS release.

Android palette selection also changes the launcher icon. Launcher aliases route through a short-lived `LauncherActivity` into the stable `AppActivity` task, so disabling the old alias does not remove the running app's task. An operating-system task removal is different from an unhandled exception and may produce no Sentry error event. When investigating an unexplained exit, check Android task/process exit logs as well as Sentry; absence of an event alone does not establish that capture is broken.

2026-11-12 verification: the published 1.3.0/code12 APK lost its task after the initial random palette disabled its Basil launch alias on an API 36 emulator, without an unhandled exception. With the launcher bridge, opening via Cocoa and selecting Graphite preserved the same resumed `AppActivity` instance and task, with Graphite the only enabled alias. Android debug assembly and all 24 manifest alias target checks passed. The reported Poco/LineageOS device still needs confirmation with an updated APK.

## Verification and rollout

Verify errors through the running application's real initialization, not a standalone SDK call. Keep synthetic errors and analytics in the development environment. Remove temporary error triggers before preparing a release. Check source locations in a fresh minified-build error after uploading source maps/mappings.

Local evidence includes browser privacy/unit tests and production builds, backend typecheck/unit/integration tests against an isolated PostgreSQL database, native data JVM tests, application graph compilation, and Android debug assembly. Live verification confirmed Sentry backend error `160514118`, manual feedback `160514431` with its `session.log` attachment, and browser error `160514911`. PostHog EU accepted browser app-open/pageview capture. Browser direct external connections failed on this machine; the acceptance harness forwarded those requests through Node to verify ingestion without changing application behavior.

Web/backend production release `20261112-observability` was deployed to `https://watchparty.top` on 2026-11-12 after explicit approval. The production DSN is configured in the protected server environment; the running API confirmed `production` and the expected release identifier. All three report-delivery columns are present. Public readiness passed for database, dispatcher and voice. The deployed browser loaded over HTTPS without uncaught page errors, and PostHog EU accepted its usage batch (HTTP 200). This machine still required Node transport forwarding for external telemetry; the automated browser also disabled its automation indicator within the test harness to exercise normal visitor capture, since production intentionally filters bots. The release preserves the database and previously published downloads. Android signed-release publication and iOS validation remain separate work.

Production stack verification: event `c06ba180247c403a93c2d7f0c874029f` resolved the synthetic application frame to `src/observability/index.ts:61` with source context. Unmapped `UtilityScript` frames are from the Playwright evaluation harness. All temporary triggers were removed from the main source tree afterward.

Android build compatibility: the complete debug build passed with stable D8/R8 `9.5.29` from Google Maven after `9.1.29` stalled in external-library register allocation during monitoring dependency validation. The repository pins the tested version; AGP and Kotlin versions are unchanged.

Native runtime verification: the Android emulator sent handled test issue `160533671` and an actual ADB-induced uncaught crash, issue `160535155`. The crash was retained on disk and delivered after restart. The clean debug APK was rebuilt afterward; inspection confirmed the temporary native verification class is absent. Native data JVM validation completed 96 tests with zero failures/errors (one intentionally skipped). iOS remains unbuilt on this Linux host.

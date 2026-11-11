# Consolidated master release, November 12, 2026

## Source and publication

Runtime commit `da0b5fba6e84cdacc45acbc1981e71bbe2d8170c` is pushed to `origin/master`. Every fetched local and remote top-level branch tip is an ancestor of this commit. The backend remains pinned to `39d77db68ee1734fc3d5aad286b87e0d17fb9190`, equal to its published `origin/hambin-migration`; every fetched backend branch tip is included too. The deployment manifest records both SHAs and the complete branch audit.

This release includes AVPlayer default playback and browser recovery, local speaking indicators, all 24 named Material theme palettes, Shared Screen icons and theme/icon synchronization, the new Android application ID, and the browser bug-report wording change. The remaining icon concept images are committed as design artifacts. All changes were committed on feature/release branches and merged into master; no branch history was discarded or rewritten.

The payload was built from a clean checkout. The subsequent release-evidence commit changes documentation only.

## Validation

- Browser: production build and all 85 unit tests passed.
- Browser acceptance: 32 of 33 player, palette and speaking checks passed in the combined run; the playback seek check that timed out during local memory pressure passed on its targeted rerun. A missing bundled Playwright browser was resolved by selecting the installed Chrome. No source changes were needed for these reruns.
- Native data: 87 tests passed. Shared UI: 66 passed and one skipped. These include the theme/icon mappings, restored/live theme selection, dark-mode recomposition, cancellation, launcher failures and working snackbar Retry.
- Android debug and signed release assembly passed, including release lint, shrinking and optimization. App JVM graph compilation passed.
- Launcher catalog validation confirms all 24 exact seed colors, stable aliases and adaptive/legacy resources. Deployment smoke checks and shell syntax checks passed.
- Backend typecheck and 10 tests passed; seven real-service integration tests were skipped by the standard release command.
- Physical Android launcher/audio behavior and iOS compilation were not tested in this Linux session. Readiness is not a two-way voice or sustained-media acceptance result.

## Signed Android APK

- Version **1.1.3**, version code **9**, package **top.watchparty.app**.
- Public download: [roomio-1.1.3-9.apk](https://watchparty.top/downloads/roomio-1.1.3-9.apk).
- Bytes: **23,703,257**.
- SHA-256: `b51e55c24e819dfaf53626c8d560a272127cd3100b59cae2104b4e53a27c4c68`.
- Signing certificate SHA-256: `195cb06fd908490a7217bebbd7fccdf0c1abe133acb300b2768b1b625c09e227`, matching the previous signed release.
- Signature verification, non-debuggable status, 16 KiB ZIP alignment and all four CPU architectures passed: arm64-v8a, armeabi-v7a, x86 and x86_64. The minified release retains all 24 launcher aliases.
- Local APK and R8 mapping copies are in `/home/erfan/Downloads/`, named `roomio-1.1.3-9.apk` and `roomio-1.1.3-9-mapping.txt`.

The new application ID installs as a separate Android application from earlier `top.roomio.app.androidApp` APKs. Local profiles/settings do not migrate between those package IDs. The signing identity is unchanged. App Links retains the previous package association alongside the new one.

## Production activation and verification

Release **20261112-master-code9** is active at `/opt/hambin/releases/20261112-master-code9`. The server verified the transferred archive and all 99 payload-file checksums before activation. API, Caddy, TLS router and Coturn were updated; PostgreSQL and its live volume were preserved. Existing downloads and old hashed browser assets were retained. No database or file snapshots were created, following the existing deployment policy.

Public readiness reports `ready`, `database`, `dispatcher` and `voice` true. Public HTML, main JavaScript/CSS, lazy live-app code and App Links configuration match their staged SHA-256 checksums. A complete public HTTPS APK download exactly matches the local signed APK and published checksum file; the server serves Android package MIME type, the expected content length and byte ranges. The preceding 1.1.2/code 8 APK still has its original checksum `a93ea6bb9dc462339609f9051c21f150d5c00aa6911b6363733487f2910c6993`.

Fresh live Chrome acceptance verified the Home download link/version, all 25 palette choices including Roomio default, saved Sage restoration after reload, and desktop/mobile RTL large-text layouts without horizontal overflow or page errors. Theme/icon runtime behavior was verified with Compose fakes; physical OEM launcher refresh remains a device acceptance check.

Local evidence: `/tmp/hambin-20261112-activation.log`, `/tmp/hambin-code9-public-verification.json`, `/tmp/hambin-code9-remote-verification.json`, `/tmp/hambin-code9-live-browser.json`, `/tmp/hambin-20261112-android-build.log`, `/tmp/hambin-20261112-browser-tests.log`, `/tmp/hambin-20261112-browser-rerun.log`, and `.tools/deployment/20261112-master-code9/release.json`. These local files exclude signing credentials and server environment secrets.

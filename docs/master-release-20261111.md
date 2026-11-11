# Consolidated master release, November 11, 2026

## Source integration

All fetched remote and local Hambin branch tips are ancestors of runtime commit
`3c037fd2191c87690f681953460ffa0dc89a5820`, pushed to `origin/master`.
The recent feature branches were already in the active branch history. Its six
commits beyond the former master, plus pending bug reporting, Android voice
routing, invite sharing, and release changes, were fast-forwarded into master.
The separate initial `main` history was merged explicitly, preserving its MIT
license. No branches or commit histories were deleted or rewritten.

Pending application changes are commit `f1da1c6`. Backend bug-report persistence
is submodule commit `39d77db68ee1734fc3d5aad286b87e0d17fb9190`, pushed to the
backend repository's `hambin-migration` branch. All other backend branch tips
are ancestors of that commit. Hambin continues to pin this approved backend
branch rather than replacing the upstream project's default branch.

## Validation

- Browser production build and all 79 unit tests passed.
- Backend typecheck and 10 tests passed; seven real-service tests were skipped
  by the standard release command. Earlier report integration acceptance is
  recorded in the implementation work state.
- Native data: 70 tests passed. Native UI: 56 passed, one skipped on final run.
  The first UI run encountered an UnconfinedTestDispatcher exception in
  `notificationLeaveEndsVoiceAndRoom`; the complete UI suite passed on rerun
  without source changes. This intermittent test-dispatcher failure remains
  a test-infrastructure limitation.
- Android debug and signed release builds passed, including release lint.
- Deployment packaging smoke checks and shell syntax checks passed.
- iOS compilation and physical-device audio/Sharesheet acceptance were not
  performed in this Linux release session.

Local logs: `/tmp/hambin-master-deploy-build.log`,
`/tmp/hambin-master-deploy-smoke.log`, `/tmp/hambin-master-android-release.log`,
`/tmp/hambin-master-android-rerun.log`.

## Signed Android artifact

- Package: `top.roomio.app.androidApp`; version **1.1.2**, code **8**.
- File: `roomio-1.1.2-8.apk`, 23,586,196 bytes.
- SHA-256: `a93ea6bb9dc462339609f9051c21f150d5c00aa6911b6363733487f2910c6993`.
- Existing signing certificate SHA-256:
  `195cb06fd908490a7217bebbd7fccdf0c1abe133acb300b2768b1b625c09e227`.
- Signature verification and 16 KiB ZIP alignment passed.
- All four ABIs remain: arm64-v8a, armeabi-v7a, x86, x86_64.
- Release shrinking, resource shrinking, and native compression remain enabled.
- APK and R8 mapping copied into the user's local Downloads directory.
- Public URL: https://watchparty.top/downloads/roomio-1.1.2-8.apk.

The browser Home download row now selects this immutable versioned URL.
Existing versioned APKs retain their bytes and URLs.

## Deployment

Release ID: `20261111-master-code8`. The 99-file payload manifest records the
clean runtime source commit, backend commit, APK details, and SHA-256 checksums
for every staged file. Signing material and environment credentials are excluded.

Activation completed successfully at `/opt/hambin/releases/20261111-master-code8`.
The server verified the archive and every payload checksum before activation.
API, Caddy, TLS router, and Coturn started successfully; the existing PostgreSQL
container and volume were preserved. The new `bug_reports` table exists in
production. Existing APK downloads and old hashed browser assets were preserved;
the prior 1.1.1/code 7 APK still matches its original published SHA-256.

Public HTTPS readiness returned `ready`, `database`, `dispatcher`, and `voice`
all true. The public HTML exactly matches the staged build. The APK download
returns HTTP 200, Android package MIME type, and the exact expected content
length. Its published checksum file and installed server APK SHA-256 both match
the signed local artifact. This session did not download the full APK again or
perform additional phone/browser/media acceptance.

Evidence: `/tmp/hambin-master-activation.log` and
`/tmp/hambin-master-public-verification.json`.

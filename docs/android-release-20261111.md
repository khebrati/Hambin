# Android 1.1.0 release

## Build

- Package: `top.roomio.app.androidApp`.
- Version name: `1.1.0` (minor increase from `1.0.3`).
- Version code: `6` (previously `5`).
- Signed release APK: `roomio-1.1.0-6.apk`, 23,564,836 bytes.
- APK SHA-256: `651218bd03cbdbb89dcb65ce524b7c465ef28b55d2d064a323407c4d4ca82652`.
- Signing certificate SHA-256: `195cb06fd908490a7217bebbd7fccdf0c1abe133acb300b2768b1b625c09e227`, matching the existing release certificate.
- Public URL: `https://watchparty.top/downloads/roomio-1.1.0-6.apk`.
- Local copy: `/home/erfan/Downloads/roomio-1.1.0-6.apk`.

The release was built in `/tmp/hambin-android-1.1.0-6` from committed source `33ea54a32b0ccbc10bb980a96acc3f454209f79c`, plus the Android version bump, release optimization settings, and the approved browser download row. Concurrent, unrelated last-room and guest-profile edits in the shared working tree are excluded from the published artifacts. No signing key, password, or local signing configuration is distributed.

Both Android release and debug builds passed. `apksigner` verified the release signature; `aapt` verified package/version and absence of the debuggable flag; 16 KiB zip alignment passed. The shrinker is pinned to R8 9.1.29 for Kotlin 2.4 metadata compatibility. The initial unoptimized build reported metadata warnings with AGP 9.0’s older bundled compiler; the optimized build used the compatible compiler. Physical-device/background voice acceptance is separate from the emulator smoke checks.

The browser download row uses a phone icon, “Native Android app”, “Background voice party”, and a keyboard-accessible download link below Create/Join. It is visible to all visitors with explicit Android labeling. The isolated browser build and all 77 unit tests passed; desktop and dark/large-text/RTL mobile previews passed without horizontal overflow.

## Size optimization

The initial signed universal APK was 62,704,507 bytes. The optimized artifact is 23,564,836 bytes, a 62.4% reduction, while preserving `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64` as explicitly requested.

Release builds enable R8 and resource shrinking with `proguard-android-optimize.txt`. Dependency consumer rules, including WebRTC’s native entry-point protection, remain active. R8 is pinned in the version catalog and obtained from Google’s official R8 Maven archive through a repository restricted to that module. Release-only native-library compression reduces the direct-download APK further; Android extracts the device’s native libraries during installation. This trades extraction work/storage for a smaller download rather than removing supported CPUs or voice functionality.

Packaged code fell from about 18 MB to 2,621,212 bytes; native libraries fell from about 43.3 MB to 20,133,790 compressed bytes. Mapping SHA-256: `58abed7d55102b2a258b06e8bd4cf18cd6b7f145662fdca6a55f9590e99b293a`; a local mapping copy is in Downloads alongside the APK.

Sources: [Android optimization configuration](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization), [Kotlin/compiler compatibility](https://developer.android.com/build/kotlin-support), [R8 replacement instructions](https://r8.googlesource.com/r8/+/refs/heads/main/README.md), [native library compression](https://developer.android.com/guide/practices/page-sizes).

## Runtime verification

The signed, minified release installed and cold-launched on the API 36 x86_64 emulator. A real temporary production guest created a room, joined voice muted, initialized the native WebRTC worker and received its roster, played the W3C Sintel trailer through 41 seconds, and rendered a frame after seeking back to 22 seconds. The native room service remained foreground while the app was backgrounded. Leaving the room returned home and stopped the service; no app fatal exception was reported. All eight native libraries have identical decompressed bytes to the original unoptimized build across all four ABIs.

The read-only emulator needed a workaround for pre-existing Android System UI ANR dialogs: disable error dialogs and restart System UI in the temporary VM. App controls remained responsive after that workaround. The VM was closed after testing; the original AVD user data was preserved. This is an app smoke check, not physical-device or two-way/background-audio acceptance.

Evidence: `/tmp/roomio-optimized-smoke.json`, `/tmp/roomio-optimized-video-frame.png`, `/tmp/roomio-optimized-background-service.txt`, `/tmp/android-1.1.0-6-verification.json`. Both release and debug builds passed with R8 9.1.29.

## Publication

Published on 2026-11-11 to the existing static host, using release manifest `android-1.1.0-6-optimized-20261111T125030Z`. The active service release remains `/opt/hambin/releases/20261106-active-party-f04516c`; its `android-download-release.json` records APK and browser hashes. No API, database, voice, or proxy service restart occurred. The preceding browser entry and assets are backed up at `/opt/hambin/backups/before-android-1.1.0-6-optimized-20261111T125030Z/web`; previous hashed assets remain available.

All 56 staged/installed/public HTTPS browser files matched the build checksums. The real public browser download action saved `roomio-1.1.0-6.apk` with exactly 23,564,836 bytes and SHA-256 `651218bd03cbdbb89dcb65ce524b7c465ef28b55d2d064a323407c4d4ca82652`, identical to the signed local artifact. The host serves `application/vnd.android.package-archive`, supports byte ranges, and exposes the matching `.sha256` file. Public desktop and mobile dark/large-text/RTL row checks passed without horizontal overflow or page errors. Readiness remained ready/database/dispatcher/voice true.

Public verification evidence: `/tmp/android-public-download-verification.json`, `/tmp/android-public-assets-verification.json`, `/tmp/production-android-download-verified-rtl.png`. Public APKs must be preserved across future full-release deployments; see `backend/deploy/README.md`. No Git commit or push was requested or performed.


## Follow-up: Android 1.1.1 and full current deployment

The stakeholder subsequently requested that all changes be committed and deployed.
The published 1.1.0/code 6 snapshot predates the Marvel/DC guest names and persistent
last-room features, so 1.1.1/code 7 includes both without replacing the immutable
1.1.0 URL. The current home download row targets
`https://watchparty.top/downloads/roomio-1.1.1-7.apk`.

Runtime source is commit `aaaf271bf21ea66392762ca043c4d9354b309ca9` on
`feat/random-character-names`; deployment tooling is commit `334715a`. The backend
submodule is `ffe003c11e9f33462cbcc0ad165c8708149f64aa`, already published on
its `hambin-migration` branch. All source changes are pushed.

Fresh validation passed: 79 browser unit tests; seven last-room browser tests;
three active-party recovery browser tests; backend typecheck and eight unit tests
(six real-service integration cases skipped); 64 native data tests and 55 native
UI tests (one UI test skipped). Deployment packaging/routing smoke checks and
shell syntax checks passed.

The signed 1.1.1 APK is 23,573,273 bytes with SHA-256
`f95b6a5f33f30d8392cb5b32a24a3e5a50776c6cb71385b82364830b585b43d5`.
It retains all four ABIs, release shrinking/native compression, and the same
signing certificate as 1.1.0. Signature, package/version, non-debuggable status,
16 KiB ZIP alignment, and R8 9.1.29 mapping checks passed. The APK and mapping
are also saved locally in Downloads.

At the stakeholder's request, verification switched from a temporary emulator
to the connected Redmi 2207117BPG. The emulator was stopped. The signed release
installed, launched with a generated character name, and created a production
room successfully. The stakeholder then explicitly stopped further verification
and requested upload/deployment only; phone room reopening, new public download
checks, and additional browser/audio acceptance were not performed.

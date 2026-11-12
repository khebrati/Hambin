# Handled-exception diagnostics release — November 12, 2026

Handled exceptions now retain operation labels, types, redacted messages and stacks across the browser, shared native clients and active Node backend. Browser/native session diagnostics are attached to explicit manual bug reports; backend diagnostics appear in operator logs. Automatic Sentry event sanitization remains unchanged. See [observability.md](observability.md).

The release is live at [watchparty.top](https://watchparty.top), with signed Android **1.3.1 (13)**. Android also includes the previously committed launcher bridge for palette switching and the sync countdown placement fix.

## Source and deployment

- Client/logging commit: `8e795b2`; backend: `a295b8108debad50654a54f2347930741880838a`.
- Android version commit: `8a0edcd`; final browser/runtime source: `8455060e384291097271f038a5cb396f0552f97e`.
- Feature/release branches were fast-forward merged into parent `master` and backend `hambin-migration`, then pushed. This documentation commit follows the deployed runtime commits.
- Final active release: `20261112-handled-exceptions-code13-final`, under `/opt/hambin/releases/20261112-handled-exceptions-code13-final`.
- The browser/backend were deployed before Android publication. Both release packages used isolated, clean committed source; manifests record an empty source-status field. Unrelated local design/research changes were excluded and preserved.
- PostgreSQL was preserved, and all six previously published APKs retain their original checksums. Signing material and runtime credentials remain outside release payloads.

## Verification

- Coverage audit: **213** catch/rejection boundaries across active browser, native and Node sources. React recovery and socket/database error callbacks are instrumented too.
- **107 browser unit tests**, browser production build/source-map upload, **25 backend unit tests** and backend typecheck passed. Twelve service-integration tests were gated/skipped in this unit run; database integration was not rerun for this logging-only backend change.
- Native suites covered **104 data tests** and **82 shared UI tests**, with no failures and one gated skip in each suite. Android debug assembly and JVM application graph compilation passed. The coordinator diagnostics assertion passed a final targeted rerun.
- Signed Android release assembly, R8 shrinking, release lint and Sentry ProGuard mapping upload passed. Shrunk DEX files retain the handled-exception diagnostics code.
- Hosted verification against the final release injected a peer-construction `TypeError`. The report payload retained its message, operation and minified source filename/line/column; the credential and resource URL were redacted. Delivery was intercepted locally, so no synthetic manual report was added to Sentry.
- Two real browser clients created/joined a room, acquired microphones, recovered from that injected failure, established WebRTC and received audio on both clients. No uncaught page errors or Google/Firebase authentication requests occurred. The report's build field matched the final deployment, and the home page linked Android build 13.
- All **17** public browser asset hashes match the final package; source maps are excluded from the public payload. Readiness reports `ready`, `database`, `dispatcher` and `voice` true.
- An independent complete HTTPS APK download matches the signed local checksum. APK MIME type, byte length, checksum sidecar and HTTP byte-range delivery passed.

This diagnostic release does not establish the original Firefox reporter's root cause. Physical Android device confirmation and Apple compilation/device validation remain independent checks; Apple builds require macOS.

## Android download

- [Signed watch-party-1.3.1-13.apk](https://watchparty.top/downloads/watch-party-1.3.1-13.apk) — **25,904,931 bytes**; package `top.watchparty.app`; non-debuggable release.
- SHA-256: `7fc434023488490478c2a1dc2749434839aeecdf790921155c10c0f493b7c94d`. [Checksum file](https://watchparty.top/downloads/watch-party-1.3.1-13.apk.sha256).
- Signing certificate SHA-256: `195cb06fd908490a7217bebbd7fccdf0c1abe133acb300b2768b1b625c09e227`, matching the existing release identity. APK v1/v2 signature verification and 16 KiB ZIP alignment passed.
- Architectures: arm64-v8a, armeabi-v7a, x86 and x86_64.
- Uploaded ProGuard mapping UUID: `cedbbb5b-6d65-3e1c-a60c-d23ce3f25ea2`.
- Local APK, checksum and mapping: `/home/erfan/Downloads/watch-party-1.3.1-13*`.

Local evidence is retained under `.tools/deployment/20261112-handled-exceptions-code13-final/`; the clean release checkout is `/tmp/hambin-exception-release.whbk_chx/parent`.

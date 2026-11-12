# Inline sync and screenshot release — November 12, 2026

Live at [watchparty.top](https://watchparty.top), with signed Android **1.3.2 (14)**.

Accepted viewers see a small spinner and “Waiting for the owner…” / «منتظر تصمیم میزبان…» beside withdrawal in the sync dialog. The status appears during INVITING only, preserves withdrawal and disappears when the owner commits. Browser and shared native presentation use the selected Inline treatment (DD-071).

This release also publishes the committed optional screenshot reports, Firefox WebRTC-constructor recovery and Android party-header alignment changes since build 13. Existing observability remains enabled.

## Source and cutover

- Sync implementation: `af94bd6`; runtime parent: `bea6c4d3b32a388d7ec0713a18186e503438532d`; backend: `4d1b165988923be468fefb1f1302d26216b2112b`.
- Master is pushed. Clean committed source was built in an isolated checkout; unrelated local font/design changes were excluded and preserved.
- Active release: `20261112-inline-sync-code14`, `/opt/hambin/releases/20261112-inline-sync-code14`.
- Screenshot storage migration adds a non-null JSON column with an empty-array default; existing reports and clients remain compatible.
- Existing PostgreSQL container/data and all **7** previous APKs were preserved. Protected environment/signing files remain outside the payload.

## Verification

- **111 browser unit tests**, production build/source-map upload, backend typecheck and **27 backend unit tests** passed. Twelve real-service tests were gated in the unit command.
- Two real isolated PostgreSQL integrations passed: report access/persistence/idempotent retry, screenshot persistence/conflicts, retention, and asynchronous Sentry attachment delivery with retry recovery.
- Main browser component checks cover pending/accepted/owner/preparing visibility, enabled withdrawal and dialog dismissal; compact Persian large text fits without horizontal overflow. Desktop/light and compact/RTL screenshots were reviewed.
- Android debug assembly, shared JVM compilation, signed release assembly, R8 shrinking and release lint passed. Sentry ProGuard mapping `fc005b8f-9fa8-3141-bb75-208633c69b1d` uploaded successfully.
- Hosted two-client sync passed invitation, acceptance, the production inline label, withdrawal/resumed playback, reacceptance and countdown. The owner and pending viewer do not receive the waiting label. Screenshot selection and the attachment payload passed with report delivery intercepted locally; the report build matched this release.
- Hosted two-client WebRTC recovery/voice and handled-error redaction regression passed. Synthetic report delivery was intercepted locally. No uncaught page errors.
- Public readiness reports ready/database/dispatcher/voice true. All **20** browser entry/JS/CSS hashes match the staged package. Public source maps are excluded.
- Independent full HTTPS APK download, SHA-256, MIME/length, checksum sidecar and byte-range delivery passed.

Physical Android playback/audio acceptance and Apple compilation/device validation were not performed by this Linux release run.

## Android

- [Download signed Android 1.3.2 (14)](https://watchparty.top/downloads/watch-party-1.3.2-14.apk) — **26,008,494 bytes**, package `top.watchparty.app`, non-debuggable release.
- SHA-256: `4587a110e50ab4280a361d88b16af52a3a0b6f12fcaa8492f25fe542f76e6d21`. [Checksum](https://watchparty.top/downloads/watch-party-1.3.2-14.apk.sha256).
- Signing certificate SHA-256: `195cb06fd908490a7217bebbd7fccdf0c1abe133acb300b2768b1b625c09e227`, matching existing installations; v2 signature and 16 KiB ZIP alignment verified.
- Four ABIs: arm64-v8a, armeabi-v7a, x86, x86_64.
- Local APK/checksum/mapping: `/home/erfan/Downloads/watch-party-1.3.2-14*`.

Local verification artifacts are retained under `.tools/deployment/20261112-inline-sync-code14/`.

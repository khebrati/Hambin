# Owner-led sync release — November 12, 2026

Owner-led synchronization is live at [watchparty.top](https://watchparty.top) and available in signed Android **1.3.0 (12)**. Owner invitations, revocable consent, locked participants, actual buffering, a shared countdown and bounded failure recovery use the fresh-deployment contract in [synchronization-contract.md](synchronization-contract.md).

The release also includes the approved concise bilingual copy and final refinements: Persian automatic titles such as «دورهمی فرانکی», a chat icon instead of the empty-chat star, no conversation-starting helper sentence in either language, and invitation sharing only in the party top bar. Explicit custom titles are preserved. Existing deployed monitoring is retained.

## Source

- Runtime parent commit: `9b385a1de8e8388d4f0a4405e7998726b2cf992d`; backend: `d8076f9902767736ee197dac64f95e0397ae2bab`.
- Changes were committed on `feat/owner-sync-release-20261112`, fast-forward merged into parent `master` and backend `hambin-migration`, and pushed. This evidence-only commit follows the runtime commit.
- Final active release: `20261112-owner-sync-code12-final`, under `/opt/hambin/releases/20261112-owner-sync-code12-final`.
- Packaging used clean committed source. Local font studies, research, credentials and generated build output were excluded. Signing credentials and protected runtime environment remain outside the release payload.

## Verification

- Production web build and **104 browser unit tests** passed. Source maps were uploaded to Sentry and excluded from the public payload.
- **23 backend unit tests** and **13 PostgreSQL/Socket.IO integrations** passed. The Persian default-title and custom-title regression was verified after the final title change.
- Native baseline suites covered **102 data tests** and **81 shared UI tests**, with no failures and one platform-gated skip per suite. Final chat/share refinements passed **13 targeted native UI tests**, with one additional skipped case, plus Android debug assembly. Signed release assembly, shrinking and release lint passed; the ProGuard mapping upload succeeded.
- Live three-viewer sync passed in desktop/light and compact/dark/RTL/large-text layouts. A further desktop sync run passed after final activation. Checks cover consent, withdrawal, decline indicators, actual buffering, shared startup within the test's 350 ms tolerance, and private rewind to the owner.
- Live four-peer browser voice passed connectivity, received audio and mute checks.
- Final hosted English/Persian checks confirmed “Frankie's party” / «دورهمی فرانکی», the chat icon, absent helper text, exactly one top-bar invitation button with a working dialog, the new Android link/version, no horizontal overflow and no uncaught page errors. Screenshots were reviewed.
- Public readiness reports `ready`, `database`, `dispatcher` and `voice` true. Public HTML and eight critical asset checksums match the package. Existing PostgreSQL data and all five older APK downloads were preserved.

These browser tests do not establish physical Android playback/audio behavior or Apple runtime support. iOS compilation/device validation still requires macOS.

## Android download

- [Signed watch-party-1.3.0-12.apk](https://watchparty.top/downloads/watch-party-1.3.0-12.apk) — **25,903,174 bytes**.
- Package: `top.watchparty.app`; version **1.3.0**, code **12**; non-debuggable minified release.
- SHA-256: `da04c7bd44fe3dd3be63b90192d134001c425057273e1e9aee707ff41c9f96ff`. [Checksum file](https://watchparty.top/downloads/watch-party-1.3.0-12.apk.sha256).
- Signing certificate SHA-256: `195cb06fd908490a7217bebbd7fccdf0c1abe133acb300b2768b1b625c09e227`, matching the preceding release identity. APK v1/v2 signature verification passed.
- Four architectures: arm64-v8a, armeabi-v7a, x86 and x86_64. 16 KiB ZIP alignment passed.
- A complete independent HTTPS download matches the local signed APK. Package MIME type, content length, checksum sidecar and byte-range delivery passed.
- Local copies: `/home/erfan/Downloads/watch-party-1.3.0-12.apk` and `watch-party-1.3.0-12-mapping.txt`.

Local verification evidence is retained under `.tools/deployment/20261112-owner-sync-code12-final/` and `/tmp/hambin-sync-*`. No private signing material is published.

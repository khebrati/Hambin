# Persian language release, November 12, 2026

Runtime source `dc1cebc3f855bdf4f7f8c44790c07c633e869733` is pushed to `origin/master`. Backend source remains `39d77db68ee1734fc3d5aad286b87e0d17fb9190`. The release contains Persian translations, saved English/فارسی profile selection, RTL presentation, Android notifications, and the simplified web/native home screens.

## Validation

- Web production build and all 87 unit tests passed. Backend typecheck and 10 backend tests passed; seven real-service integration tests were skipped.
- Four Chrome Persian acceptance checks passed: profile Save/reload/English recovery, Back and failed Save draft behavior, compact dark/large-text layouts and expanded light layouts. Screenshots were reviewed.
- Android debug and signed release builds, release lint, and app JVM graph compilation passed. The shared Compose language/RTL switching test and all five profile-persistence tests passed. All 255 native translation keys and format placeholders match.
- The Android locale environment preserves the Activity context for fullscreen, sharing and activity results. Physical Android and iOS runtime behavior were not tested.

## Android download

Signed universal Android **1.2.0**, version code **10**, package **top.watchparty.app**.

- [Download roomio-1.2.0-10.apk](https://watchparty.top/downloads/roomio-1.2.0-10.apk)
- Bytes: **23,712,611**.
- SHA-256: `3831dd6a02e7f8341825a51708f7893d9f6bcad5c465e82016dd2a8c2a142969`.
- Signing certificate SHA-256: `195cb06fd908490a7217bebbd7fccdf0c1abe133acb300b2768b1b625c09e227`, unchanged from 1.1.3.
- Signature verification, non-debuggable status, 16 KiB ZIP alignment, and all four ABIs passed: arm64-v8a, armeabi-v7a, x86, x86_64.
- Local APK, checksum, and R8 mapping copies are in `/home/erfan/Downloads/`, named `roomio-1.2.0-10.apk`, `roomio-1.2.0-10.apk.sha256`, and `roomio-1.2.0-10-mapping.txt`.

## Deployment

Release ID: `20261112-persian-code10`. The clean-checkout deployment manifest includes 99 payload files. Existing PostgreSQL data, older downloads and hashed browser assets are preserved. No database or file snapshots are created, following the stakeholder's policy.

Activation succeeded. Public readiness reports `ready`, `database`, `dispatcher` and `voice` true. The API, Caddy, TLS router and Coturn are healthy; the existing PostgreSQL container and live volume were retained. The server verified all 99 payload files before activation; post-activation verification matched 98 unchanged payload files, excluding the Compose file intentionally adapted to use the existing production network.

Public HTTPS verification matched the staged HTML, main JavaScript/CSS, lazy LiveApp code, App Links configuration, APK checksum file and full APK download. The APK is served with Android package MIME type and the expected 23,712,611 bytes. The previous 1.1.3/code9 APK retains SHA-256 `b51e55c24e819dfaf53626c8d560a272127cd3100b59cae2104b4e53a27c4c68`.

A fresh live Chrome session saved Persian, confirmed RTL, reloaded and retained Persian, verified the new Android download link, checked 360px and 1440px layouts without horizontal overflow, and switched back to English/LTR. No page errors occurred. The live mobile screenshot was reviewed. This does not establish physical Android audio/launcher behavior or sustained media acceptance.

Local evidence: `.tools/deployment/20261112-persian-code10/release.json`, `/tmp/hambin-persian-activation.log`, `/tmp/hambin-persian-public-verification.json`, `/tmp/hambin-persian-remote-verification.json`, `/tmp/hambin-persian-live-verification.json`, `/tmp/hambin-persian-release-android.log`, and `/tmp/hambin-persian-package.log`. No signing credentials or protected server environment files are included in the release.

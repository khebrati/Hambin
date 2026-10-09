# Phase 5 evidence: mesh voice

Phase 5 replaces the Android LiveKit voice engine with microphone-only WebRTC mesh voice and adds the WatchParty-fork voice configuration/signaling backend. Room and voice roles use separate leases; signaling is room- and generation-scoped. TURN REST credentials are short-lived and supplied from private configuration. Android remains muted until the user opts in, tracks speaking indicators, caps the mesh at seven remote peers, and treats a failed peer as a recoverable room voice failure rather than claiming partial success.

## Verified locally

- WatchParty fork: `npm run typecheckServer` passed.
- WatchParty fork: `npm run test:backend` passed (5 tests; 6 real-service integration tests skipped by the normal unit command).
- WatchParty fork: with the documented local environment (`FIREBASE_PROJECT_ID=demo-hambin`, Auth Emulator at `127.0.0.1:9099`, and isolated PostgreSQL at `127.0.0.1:15432`), `npm run test:integration` passed all 7 tests. The voice test covers active-member configuration, TURN credential generation, one-use namespace tickets, independent voice-role replacement, mute roster updates, and scoped signaling.
- The local Coturn Compose profile validates with `docker compose -p hambin-watchparty-local -f backend/compose.watchparty-local.yml config -q`.
- Android: `./gradlew :data:jvmTest :sharedUI:jvmTest :androidApp:assembleDebug --offline --console=plain` passed. Tests cover voice configuration mapping, audio-energy fallback, speaker attack/release, and immediate mute/disconnect clears.
- The current debug APK installed and launched on `RoomioAVD`; the app process remained running without a fatal Android runtime exception.
- `git diff --check` passed in the parent repository and WatchParty submodule. No LiveKit references remain in Roomio source.

## Still required before claiming voice parity

This environment has only one attached Android emulator. Real peer negotiation, audible two-way media, eight-person mesh behavior, forced TURN allocation/relay, blocked-UDP fallback, device/audio-route interactions, and network handover have not been verified. The passing signaling integration test does not establish that ICE succeeds or that participants can hear one another. Keep the migration's Phase 5 real-device acceptance gate open until those tests pass.

No hosting, deployment, or push was performed. The local Auth Emulator and isolated PostgreSQL are test services only; no production Firebase Admin credential or production TURN secret was introduced.

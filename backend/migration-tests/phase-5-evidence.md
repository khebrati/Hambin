# Phase 5 evidence: mesh voice

Phase 5 replaces the Android LiveKit voice engine with microphone-only WebRTC mesh voice and adds the WatchParty-fork voice configuration/signaling backend. Room and voice roles use separate leases; signaling is room- and generation-scoped, including per-peer connection generations. TURN REST credentials are short-lived and supplied from private configuration. Android remains muted until the user opts in, tracks speaking indicators, caps the mesh at seven remote peers, preserves active media during transient signaling outages, and only reports connected when every rostered peer is connected.

## Verified locally

- WatchParty fork: `npm run typecheckServer` passed.
- WatchParty fork: `npm run test:backend` passed (5 tests; 6 real-service integration tests skipped by the normal unit command).
- WatchParty fork: with the documented local environment (`FIREBASE_PROJECT_ID=demo-hambin`, Auth Emulator at `127.0.0.1:9099`, and isolated PostgreSQL at `127.0.0.1:15432`), `npm run typecheckServer`, `npm run test:backend` (5 passed; 6 integration-only tests skipped), and full `npm run test:integration` (7/7 passed) pass. The voice integration covers active-member configuration, TURN credential generation, one-use namespace tickets, independent voice-role replacement, mute roster updates, audio-only SDP enforcement, offerer direction, peer-generation fencing, stale ICE rejection, and scoped signaling.
- Android: `./gradlew :data:jvmTest :sharedUI:jvmTest :data:compileAndroidMain :androidApp:assembleDebug --offline --console=plain` passes. The debug APK installs and launches on `RoomioAVD`; the app signs up against the Auth Emulator, bootstraps identity against the local backend, and creates a room that opens to its empty cinema. The Android preferences contain the secure ciphertext entry and no legacy session key or token fields. `.AppActivity` is foregrounded and the inspected log window contains no fatal crash. This verifies Android auth, credential persistence boundary, and room startup, not a live voice session.
- Production dependency audit: `npm audit --omit=dev --audit-level=high` exits successfully with no high/critical findings; npm reports 2 moderate transitive advisories (`uuid` through Firebase Admin's Google Cloud Storage dependency). The broader development dependency graph is not included in this production-only result.
- The local Coturn Compose profile validates with `docker compose -p hambin-watchparty-local -f backend/compose.watchparty-local.yml config -q`.
- Android: `./gradlew :data:jvmTest :sharedUI:jvmTest :androidApp:assembleDebug --offline --console=plain` passed. Tests cover voice configuration mapping, audio-energy fallback, speaker attack/release, and immediate mute/disconnect clears.
- The current debug APK installed and launched on `RoomioAVD`; the app process remained running without a fatal Android runtime exception.
- `git diff --check` passed in the parent repository and WatchParty submodule. No LiveKit references remain in Roomio source.

## Still required before claiming voice parity

Real peer negotiation, audible two-way media, eight-person mesh behavior, forced TURN allocation/relay, blocked-UDP fallback, device/audio-route interactions, and network handover have not been verified. Two-device E2E and hosted backend deployment are explicitly deferred. The passing signaling integration test and emulator launch do not establish that ICE succeeds or that participants can hear one another; retain these as later acceptance gates.

The current implementation pass is Android-focused. iOS and desktop implementation/build validation are deferred; the desktop credential adapter currently provides process-local storage only and is not suitable for durable desktop sessions.

No hosting, deployment, or push was performed. The local Auth Emulator and isolated PostgreSQL are test services only; no production Firebase Admin credential or production TURN secret was introduced.

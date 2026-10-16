# Browser to Backend Integration Evidence

Date: 2026-10-15

## Scope

The default browser entry now uses the local Firebase Auth Emulator and the migrated WatchParty HTTP/Socket.IO services. It supports saved guest profiles, room create/preview/join/leave/rejoin, owner presence, local video playback, requester-only forward sync, and opt-in muted browser mesh voice. Explicit `?preview=...` fixture routes remain offline and do not initialize auth or contact backend services.

## Local verification

The checks below ran against the local PostgreSQL, Firebase Auth Emulator, Node backend, and Coturn stack. Playwright used separate browser contexts for separate identities and a synthetic microphone device.

| Check | Result |
| --- | --- |
| Browser TypeScript production build | Passed. Vite reports the main client chunk at about 825 kB minified. |
| Browser unit and contract tests | 25 passed across 5 files. |
| Preview isolation | Passed: fixture route made no Auth, API, or Socket.IO requests and showed requester-only Sync. |
| Real browser room lifecycle | Passed: profile persistence, preview/cancel, distinct owner and guest identities, owner leave/rejoin, and ended-room handling. |
| Real local video | Passed: local controls remain independent; Sync advances without rewinding; reconnect preserves position; owner abort removes playback. |
| Browser mesh voice | Passed with four participants: peer connections established, inbound audio packets arrived, and mute disabled local audio tracks. |
| Backend type check and unit tests | Passed. |
| Backend PostgreSQL/Auth Emulator integration | 7 passed, 0 failed. Includes room/realtime, auth, role lease, and voice signaling checks. |
| Dependency audit | Passed with no reported vulnerabilities after the gRPC dependency override. |

An eight-participant browser mesh run did not complete on this host. During that stress run, Chrome renderer processes saturated the machine and clients began losing room/voice roles. The checked browser smoke test therefore validates four participants; eight-person capacity remains a separate load-test gate and is not claimed by this evidence.

## Remaining release checks

This evidence covers only local services. Hosted deployment, production Firebase credentials, a browser-to-Android voice interoperability test, and eight-person mesh capacity testing remain separate checks. Browser playback also remains subject to each browser's codec, media CORS, microphone permission, autoplay, and HTTPS rules.

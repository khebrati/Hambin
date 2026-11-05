# Quick browser check against the actual server — 2026-11-05

**Result: PASS** for a bounded two-client check against `https://watchparty.top`, deployed release `20261105-voice-v4`.

The clients ran installed Google Chrome 154.0.8037.57 on the user's local workstation, using two isolated temporary browser contexts. They used the deployed public UI and real server API/Socket.IO endpoints. Media was forced through authenticated public TURN TLS on `relay.watchparty.top:443`; certificate verification stayed enabled. No local API, PostgreSQL, relay, test-origin allowance, or substituted application modules were used.

The browser microphones were replaced with distinct synthetic tones (440 and 550 Hz). This verifies received digital audio through the server, not a physical microphone/speaker or Android test.

## Passed behavior

- Public home, automatic guest setup, owner Create party, invitation preview, and explicit guest Join party.
- Voice protocol 4 with initially disabled capture and successful acknowledged unmute.
- Both received audio directions, identifiable remote tones, positive RTP/audio energy, and actual selected relay/TLS candidates.
- UI mute disables capture; UI unmute restores it after authorization.
- Forced closure of one voice signaling transport resumes with both original peer connections retained; received RTP and energy continue increasing.

| Receiver | Remote tone | Received RTP packets | Selected path | Peer identity before/after reconnect |
| --- | --- | --- | --- | --- |
| Client 1 | 550.78 Hz | 17 → 1203 | relay / tls | 1 → 1 |
| Client 2 | 439.45 Hz | 24 → 1209 | relay / tls | 1 → 1 |

The first client's signaling socket count increased from 1 to 2 while its peer identity remained 1. The second client's peer remained 1 too. There were no uncaught page errors. The complete UI/media/cleanup run lasted 46.6 seconds, including a 15-second stable media window and five-second post-reconnect window.

## Evidence and cleanup

- UTC interval: `2026-11-05T11:37:12.904Z` to `2026-11-05T11:37:59.490Z`.
- Sanitized result: `/tmp/hambin-production-quick-evidence/result.json`.
- UI screenshots: `/tmp/hambin-production-quick-evidence/client-1.png` and `client-2.png`.
- Probe script: `/tmp/hambin-production-quick.mjs` (temporary verification only; no product source edits).

The first attempt stopped on an ambiguous Invite friends test selector; the selector was scoped and the complete run then passed. No application fix was needed. Test browser contexts were closed and the test party was left during cleanup. Personal browser profiles and existing user parties were untouched.

This quick check does not resolve the earlier eight-person local simulation failure, the two-hour soak, physical Android/browser audio, network/device recovery, or 100-room capacity gates.

## Server resource timing limit

A separate server snapshot was timestamped `11:38:17 UTC`, after the browser's logged finish at `11:37:59.490 UTC`. Clock offset was not measured. It therefore must be labeled a near/post-check snapshot, not proof of CPU consumption during active media. No further load test or measurement was run.

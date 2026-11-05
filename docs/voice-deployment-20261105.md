# Voice v4 deployment — November 5, 2026

The stakeholder explicitly requested deployment of the current candidate and a
quick local-browser check against the actual server. The eight-person local
acceptance failures remain recorded in [the release evidence](voice-release-20261105.md).
This deployment does not establish eight-person stability or capacity.

## Candidate and protected rollback

- Target: `watchparty.top` and `relay.watchparty.top`, DNS resolves to the saved
  replacement VPS `152.89.253.245`.
- Staged release: `/opt/hambin/releases/20261105-voice-v4`.
- Parent archive SHA-256:
  `d5c2de32e66b92af7903f9bbcd8b6765a657204205a6af559e6b06b378f1bec3`.
- Lean server archive SHA-256:
  `f2ff84ad992e10a093211c951ac128295cba5b8816427de1e14bb755bd8c5b0c`.
  Its 46 staged server payload files are byte-identical to the verified parent
  archive; Android and local verification fixtures are excluded from the upload.
- Protected pre-update backup:
  `/opt/hambin/backups/pre-voice-v4-20261105/`, containing the database dump,
  previous release path, API image reference, and server environment snapshot.
- Previous release: `/opt/hambin/releases/20261030-new-server`.
- PostgreSQL and Coturn image digests match the previous deployment. No database
  reset, guest-identity migration, or unrelated service change is requested.

## Result

Release `20261105-voice-v4` is active at `/opt/hambin/current`. The API runs Node
24.18.1. Public `/readyz` returned all four readiness flags true, and the public
index SHA-256 exactly matched the staged browser:
`de8dbc1cb4a37e180e1d581f5cea36846dbe0855d346056ceb14120447b4a6d9`.

The existing Docker network already had the required subnet and gateway.
Compose initially tried to replace it because its configuration hash differed,
which PostgreSQL's active endpoint prevented. The deployed Compose declaration
therefore reuses `hambin_default` as an external existing network. This
deployment-only configuration change and both file hashes are recorded in the
server `release.json`; all 46 deployed file checksums were verified afterward.
PostgreSQL was not restarted, its volume was retained, and database/TURN secrets
were preserved. The protected environment explicitly selects relay ports
49160–65535, per-credential quota 128, and global quota 16000. Those ceilings do
not establish capacity.

Caddy now publishes private TCP 8443 and public UDP 443; HAProxy owns public TCP
443 and passes website client addresses through trusted PROXY v2. Coturn listens
on TCP/UDP 3478 and TLS 5349, with its relay SNI also routed from TCP 443.
The existing INPUT policy is ACCEPT; no VPN or port-53 rules were changed.
`masterdnsvpn.service` remained active with PID 596 and zero restarts, unchanged
from the pre-deployment observation.

## Quick actual-server browser check

Two temporary installed-Chrome contexts used the deployed production UI at
`https://watchparty.top`, with normal certificate verification and the actual
server. They created/joined a test party and forced TURN TLS on port 443.
Distinct synthetic microphone tones were received in both directions, selected
ICE paths were relay/TLS, initial mute and mute/unmute passed, and signaling
resume preserved peer connections while received RTP advanced. The run passed
from 11:37:12.904 to 11:37:59.490 UTC. The test party and owned browser contexts
were cleaned up afterward.

A one-second server resource snapshot at 11:38:17 UTC was taken after the logged
browser interval, so it is **not active-call load evidence**. It recorded one
logical CPU, whole-host CPU 48.98%, 1915.8 MiB total RAM and 1315.8 MiB available.
A subsequent container interval reported API CPU 1.11%/46.5 MiB, Coturn
0.05%/6.695 MiB, and HAProxy 0.14%/12.18 MiB. These observations include unrelated
host work and cannot establish voice capacity or attribute CPU usage to voice.

The eight-person recovery failures, two-hour soak, physical Android audio, and
100-room acceptance remain unresolved/unverified. No long or load test was run
as part of this deployment.

## Rollback location

Retain `/opt/hambin/backups/pre-voice-v4-20261105` and the previous release.
To restore the previous application release, stop the new API, Caddy, Coturn,
and `tls-router` first, restore the protected environment snapshot, repoint
`current` to `/opt/hambin/releases/20261030-new-server`, then bring its API,
Caddy and Coturn back up. Stopping the new router before restoring old Caddy
avoids competing TCP-443 listeners. This voice update introduced no new schema
migration; do not reset or restore the database during an ordinary application
rollback. The cutover-time database dump is retained for recovery if needed.

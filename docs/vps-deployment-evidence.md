# First migrated VPS deployment

Date: 2026-10-15. Release: `20261015T193818Z`.

## Deployment

- Host: `46.249.98.175`, reached with the stakeholder-supplied SSH key.
- Site/API: <https://watchparty.audiosense.ir>.
- TURN: `turn.watchparty.audiosense.ir`, UDP/TCP 3478, TLS TCP 5349,
  UDP relay ports 49160–49200. Both hostnames resolve to the VPS.
- Layout: Caddy serves the production `webUI` build and proxies `/v1`,
  `/socket.io`, `/healthz`, and `/readyz` to the internal Node service.
  PostgreSQL is private. Coturn runs as the image's unprivileged nobody user.
- Parent checkpoint: `22c34ed4d52f6128a6db11c5ffa904bfdb162edf` plus the
  uncommitted browser integration working tree, explicitly deployed as authorized.
- Fork: `422fd903f6e65e13cf753482a725325677276f66`.
- Release source hashes and image digests are recorded in the VPS's `release.json`.
  Infrastructure and Node base images are pinned to their resolved SHA256 digests.
  The API image has a unique release tag. No Git commit or push was performed.
- Secrets were generated/stored outside Git and images. Firebase Admin JSON is
  mounted read-only, readable by the non-root API UID. Public Firebase browser
  settings use project `hambin-dab14`, with no Auth Emulator or alternate API origin.

## Verified

| Check | Result |
| --- | --- |
| Frontend production build | PASS under Node 24.18.1/npm 11.16.0. Existing large-chunk warning remains. |
| Frontend unit/contracts | 25/25 PASS. |
| Shell syntax, Compose config, whitespace | PASS. |
| Firebase real-project access | Admin API access PASS; Anonymous enabled; automatic anonymous cleanup disabled. |
| TLS | Let's Encrypt certificates issued for both names. HTTPS readiness succeeds through VPS/SSH proxy. TURN TLS hostname/certificate verification succeeds on VPS. |
| Process readiness | `ready`, `database`, `dispatcher`, and `voice` are true. Four services remain running; PostgreSQL/API are healthy. |
| Hosted browser live entry | PASS: real anonymous sign-in/profile restored, secure context, no failing HTTP responses in smoke run. |
| Hosted fixture isolation | PASS: preview made no Auth/API/Socket requests. |
| Hosted room lifecycle | PASS: profile persistence, preview/cancel, two identities, owner absence/rejoin, ended-room handling. |
| Hosted media | PASS: real MP4 playback, independent controls, forward-only Sync, offline/reconnect position preservation, owner abort. Test media intercepted locally; production room/realtime services were real. |
| Hosted voice | PASS with two real browser WebRTC peers forced to relay: selected local candidate type `relay` on both, inbound audio bytes on both, and microphone mute disables sender tracks. Both participants left afterward. |
| Native TURN packet probes | Authenticated client-to-client relay: UDP 20/20 and TCP 20/20 packets received; TLS 18/20 received (10% loss), all processes exit 0. These probes ran on the VPS using its public relay address. |
| UI inspection | Desktop light/live home and mobile dark/large-text/RTL fixture home visually inspected; mobile has no horizontal overflow. |
| Backup/restore | Custom-format daily local backup created and restored into a temporary database with eight public tables; scratch database removed afterward. |
| Timers | Daily local database backup and daily TURN certificate sync enabled. TURN cert sync is idempotent and restarts Coturn when bytes change. |
| Private key exclusion | No PEM private-key material in deployed frontend build. |

A Docker static-IP collision during initial Caddy start was corrected by assigning
Caddy `172.30.55.254` and trusting only that address in the API. The initial wildcard TURN relay address produced forbidden-peer channel binds. Binding both TURN listening and relay addresses explicitly to the VPS public NIC corrected it while retaining all private/loopback destination denies. The API and
PostgreSQL have no host-published ports. The VPS has 2 GiB swap for its 2 GiB RAM.
The backend production install reports two existing moderate dependency advisories;
this deployment did not change the fork dependency graph.

## Network and acceptance limits

Direct TLS connections from the development machine were reset, while the same
HTTPS URL succeeded on the VPS and through an SSH SOCKS proxy. Hosted browser
checks therefore used that proxy. One lifecycle run encountered
`ERR_NETWORK_CHANGED` on its final navigation; the isolated retry passed in full.
A separate two-peer attempt initially timed out waiting for its room connection. After correcting the TURN relay address, the final two-peer forced-relay/audio/mute check passed. The initial four-peer voice attempt reached two connected peers, then a later
participant's preview request timed out and displayed the recoverable offline
state. This is not a passing hosted four-peer result.

No human-audible conversation, physical Android/browser interoperability,
eight-peer capacity, extended load/soak, or restrictive-network fallback is claimed.
Backups are on the same VPS; an offsite destination/retention policy remains open.
There is no previous hosted Node release for an initial rollback rehearsal.

## Changing domains later

The stakeholder plans to move to `watchparty.top`; ingress/TURN domain names are
configuration, and frontend API paths are relative. Browser identity storage is
origin-scoped: existing anonymous browser identities will not automatically move.
Plan that transition before retiring the temporary hostname. See
[deployment operations](../backend/deploy/README.md).

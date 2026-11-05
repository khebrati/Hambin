# Replacement VPS deployment — October 30, 2026

The stakeholder authorized a fresh deployment to the replacement server and confirmed
that Cloudflare DNS was updated for `watchparty.top` and `relay.watchparty.top`.
The server address and SSH password are retained only in Git-ignored local files
`Roomio/.local-server/ip` and `Roomio/.local-server/password` (directory 0700,
files 0600). No credentials were committed. The previous VPS was left untouched;
no previous database or guest credentials were imported.

## Active release

- Release: `20261030-new-server`, `/opt/hambin/current`.
- Node API, PostgreSQL, Caddy, and Coturn use `backend/deploy/compose.yml`.
- Current working-tree source was deployed, including the existing uncommitted
  backend-owned guest authentication implementation. Each deployed source file
  is checksummed in the server release's `release.json`.
- The initial archive SHA256 was
  `9998ecfb77eddcb3e1cb906bb1def48fca4da8ab0ca56b8853319ae06716021f`.
  A subsequent lockfile/manifest patch fixed body-parser and qs advisories before
  final acceptance; the final manifest describes the patched source.
- Docker runtime installed on Ubuntu 22.04; services restart automatically.
- Fresh PostgreSQL and TURN secrets generated server-side in `/opt/hambin/.env`
  with mode 0600. Database and API ports are internal to Docker.
- Let's Encrypt certificates for the site and relay expire January 28, 2027.
- Daily `hambin-backup.timer` and `hambin-turn-tls.timer` enabled. A PostgreSQL
  custom-format backup was created and its table of contents read successfully.
  Backups are local to this VPS.
- Coturn's certificate directory permits traversal by its container user;
  the private key remains mode 0400 owned by that user.

## Verification

- Backend type-check and six unit tests passed after dependency updates.
  PostgreSQL integration tests were not run in this deployment session.
- Production backend dependency audit: zero reported vulnerabilities.
- Browser: 40 unit tests and production build passed.
- Direct external HTTPS readiness: database, dispatcher, and voice all ready.
- Hosted Chrome through an SSH SOCKS connection to the new server: secure host-only
  HttpOnly cookie, same guest/profile after reload, create/preview/join, realtime
  membership, and owner return all passed. No Google authentication requests;
  no access credentials in browser storage or refresh secret in browser JSON.
- Two Chrome peers using synthetic microphones and forced TURN relay candidates
  connected and received audio bytes in both directions; mute disabled tracks.
  Physical-device voice, eight-peer capacity, and sustained load were not tested.
- External UDP STUN request to port 3478 received a response; relay TLS on TCP
  5349 presented the correct certificate.
- Native client source was unchanged; no Android or iOS build was required for
  this server deployment.

## MasterDnsVPN coexistence

The existing `masterdnsvpn.service` remains active with its original PID 7500 and
zero restarts. Its configured UDP port is 53; Watchparty uses TCP 80/443, UDP 443,
TURN UDP/TCP 3478, TLS TCP 5349, and relay UDP 49160–49200. No ports overlap.
The existing INPUT accept rules for TCP/UDP 53 remain, INPUT/OUTPUT policies are
ACCEPT, and policy routing remains the standard local/main/default tables.
Docker owns its bridge/NAT rules; there is no VPN tunnel interface or conflicting
route in the inspected configuration. No VPN settings or service were changed.
An external DNS query to UDP 53 received an 83-byte response after deployment.
A complete DNS VPN client session was not available for acceptance testing.

Installer reference: [MasterDnsVPN Linux installer](https://raw.githubusercontent.com/masterking32/MasterDnsVPN/main/server_linux_install.sh).
Dependency advisory: [body-parser compressed-request DoS](https://github.com/advisories/GHSA-27r3-jxmh-p828).

# Fresh replacement VPS deployment

> Historical initial replacement deployment and authentication hotfix. DD-028 supersedes the Firebase configuration described below; see [current deployment operations](../backend/deploy/README.md) for backend-owned guest authentication.

Date: 2026-10-23. Release: `20261023-fresh`.

- Host: `45.146.241.92`; site/API: <https://watchparty.top>; TURN: `relay.watchparty.top`.
- User explicitly requested a fresh site with no data preservation. Created a new PostgreSQL volume and fresh database/TURN secrets. No old database, certificates, or server configuration were imported; the old host was left untouched.
- Retained Firebase project `hambin-dab14` and the existing Admin credential mounted outside images/Git. Firebase accounts and browser-local credentials were not deleted.
- Deployed the production browser build and pinned current WatchParty backend using the existing Compose layout at `/opt/hambin/current`. The server's `release.json` records both Git SHAs and all 68 transferred source/asset hashes; checksum verification passed. The server Caddyfile serves the primary origin, redirects `www`, and provisions the relay certificate.
- Installed Docker/Compose, 2 GiB swap, automatic service restart, daily local database backup, and daily TURN certificate synchronization. Database/API have no published host ports. Secrets have modes 0600 (configuration) and 0400/UID 1000 (Admin credential).

## Verification

- Frontend production build and all 25 unit/contract tests passed. Backend TypeScript and five unit/HTTP tests passed; six local real-service integration tests were skipped.
- All four production services running; API/PostgreSQL healthy. HTTPS `/readyz` reports database, dispatcher, and voice ready. `www` redirects to the primary site.
- Fresh browser sign-in and lobby loaded in a secure context with no HTTP errors. Mobile dark/large-text/RTL preview has no horizontal overflow.
- Two browser identities created/joined a party and connected realtime. Forced TURN relay connected on both peers, inbound audio bytes arrived on both, and muting disabled the sender tracks. Both participants left afterward.
- Let's Encrypt issued site, www, and relay certificates. TURN TLS and local backup archive checks are recorded in the deployment session.
- Browser and HTTPS checks used an SSH SOCKS proxy through the new host. A direct development-network HTTPS request failed with `SSL_ERROR_SYSCALL`; unrestricted regional access is not established by these checks.

## Remaining limits

Backups are local to this VPS. No offsite recovery, physical Android/browser audio, human-audible conversation, four/eight-peer capacity, restrictive-network fallback, or extended load/soak verification is claimed for this release. Existing two moderate production dependency advisories remain. No Git commit or push was requested or performed.

## Authentication hotfix

The frontend-only player release `20261023T081616Z-player` was accidentally
built with the local Auth Emulator environment. The production runtime rejected
that setting before making any Firebase request, but the generic error text
reported an inability to restore a session even for a fresh browser. Reproduced
with no established-session marker; API readiness remained healthy.

Replacement frontend `20261023T083000Z-auth` uses the same production Firebase
project and public client configuration as the verified fresh deployment. It
retains the player changes. Vite now rejects production builds with emulator
settings, missing Firebase settings, or a demo project/key. Runtime emulator
configuration errors also have a specific message. No browser credentials,
Firebase accounts, or database records are reset. The incorrectly configured
build is rejected and the corrected production build and 27 unit tests pass.

The correction is active on `watchparty.top`. Asset/server checksums passed;
only Caddy was recreated and `/readyz` remained healthy. Hosted Chrome verified
Firebase signup/lookup and application bootstrap HTTP 200, lobby rendering, and
session restoration after reload. System Firefox 153 on Linux independently
verified sign-in and restoration after reload. Browser checks used the SSH SOCKS
route; they do not establish access from every regional network. No site-data
clearing is required for this deployment failure.

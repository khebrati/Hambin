# Backend-owned guest authentication: deployment evidence

Date: 2026-10-23. Decision: DD-028. Production release:
`20261023T130656Z-local-auth` on `45.146.241.92` at
<https://watchparty.top>, with TURN at `relay.watchparty.top`.

## Delivered behavior

The active Node/PostgreSQL service creates automatic guest identities, issues
opaque access/refresh credentials, restores sessions, and revokes credentials.
Browser restoration uses a host-only HttpOnly/Secure/SameSite=Strict cookie;
native clients use their platform credential-storage boundary. No registration
screen or Firebase identity importer was introduced. Clients start fresh guest
identities; former room ownership is not imported. Existing product rows and
non-secret local preferences are retained.

Firebase SDKs, active authentication endpoints/configuration, emulator tooling,
and deployment credential mounts were removed. The unused stock server/UI was
retired. Historical migrations, evidence, and identifiers needed to discard old
local credentials remain historical compatibility material. The external
Firebase project was not deleted; any protected old Admin credential is retained
outside the active image only for recovery of the previous release.

## Local verification

- Backend typecheck, six unit tests, and seven real PostgreSQL/Socket.IO
  integration tests passed. Coverage includes concurrent refresh, stable identity,
  expiry/revocation, restart, cookie/Origin protections, room invariants, realtime
  reauthentication, and voice signaling regressions.
- Browser: 40 unit tests, production build, five player/fixture tests, two
  real-backend product-flow tests, and four real-backend authentication
  regressions passed. Authentication tests blocked Google/Firebase domains and
  observed zero authentication requests to them. Concurrent tabs, restoration
  outages, expired access retry, and confirmed reset were exercised.
- Native: 35 data tests and 39 presentation tests passed; one presentation test
  was skipped. JVM app graph, Android debug, and signed release builds passed.
  Android/iOS corrupt-credential recovery was corrected during independent
  review; the final APK includes the complete encrypted-envelope guard.
- Independent review exercised production cookie flags, rejected invalid/missing
  browser Origins/headers, and reviewed persistence, revocation, refresh,
  namespace identity binding, and credential retention. No deployment blocker
  remained. Active-source/dependency/configuration scans found no Firebase use.

Android release: `1.0.2`, version code `4`; v1/v2 APK signature verification
passed. SHA-256:
`41b8cbd5246587439ff79535ebf1cf2871fea221221e74ba8520cc84124e8a90`.
The stakeholder will verify the physical device; no device installation was
performed for this change. iOS compilation was unavailable on Linux. Desktop
credential storage remains process-local, as before.

## Production cutover and recovery

- Staged 44 files with exact source checksums and explicit dirty-worktree
  metadata. Archive SHA-256:
  `d53cb74c21679cf4bd06014252a21b6e5cbd00832a4848da66daa25bcfaed7d4`.
  Transfer/archive and per-file checksum checks passed. No repository metadata,
  local environment file, credential, or node_modules directory was packaged.
- Built `hambin-api:20261023T130656Z-local-auth` before stopping the old API.
  The production image install audited 119 packages with zero vulnerabilities.
- Stopped the old API, saved the matching database/environment/image checkpoint,
  and applied additive migration `0003_local_sessions.sql`; prior migration
  checksums were retained. Existing product rows were not truncated.
- Activated the new API and Caddy, verified health, then moved
  `/opt/hambin/current` to the new release. The old host was untouched. TURN
  certificate synchronization completed without an activation error.
- Recovery checkpoint:
  `/opt/hambin/backups/pre-local-auth-20261023T130656Z-local-auth.json`, with
  matching `.dump` and `.env`; all have mode `0600`. The dump was validated with
  `pg_restore --list` before activation. The previous release is
  `/opt/hambin/releases/20261023T083000Z-auth`, using image
  `hambin-api:20261023-fresh`.

Rollback requires the matching pre-cutover database/environment/image, because
migration 0003 removes the former provider UID column. Image-only rollback is
unsafe. The protected `/opt/hambin/rollback-local-auth.sh` helper restores the
checkpoint after first preserving a post-cutover safety dump. Activation included
automatic recovery for failures before successful health/current-link checks;
that path was not needed or exercised destructively against production.

## Hosted verification

- API and PostgreSQL are healthy. HTTPS `/readyz` returns
  `ready/database/dispatcher/voice: true`; startup logs report `auth: local`.
  Migration 0003 is applied and the provider UID column is absent.
- The running API has no credential mounts, Firebase environment variables, or
  Firebase dependencies.
- Firefox 153 on Linux created a fresh guest and restored the same identity on a
  second navigation. Refresh returned no refresh secret to JavaScript; the
  cookie was inaccessible to document scripts. No Google/Firebase authentication
  request was observed.
- Two Chrome peers connected with forced TURN relay, selected relay candidates
  on both sides, and received inbound audio bytes on both sides. Muting disabled
  the sender audio tracks. Both participants left afterward.

Hosted browser checks used an SSH SOCKS route through the current VPS, so they do
not establish unrestricted access from every regional network. Physical-device
audio, human-audible conversation, eight-peer capacity, restrictive-network
fallback, offsite recovery, and sustained load/soak verification are not claimed.
No Git commit or push was performed.

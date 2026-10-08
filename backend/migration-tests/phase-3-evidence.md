# Phase 3: authoritative room services and Socket.IO

Implemented on `feat/watchparty-backend-migration`, fork branch `hambin-migration`, after the Phase 2 foundation checkpoint. Local services: PostgreSQL 17 at port 15432 and Firebase Auth Emulator at 9099; Node 24.18.1/npm 11.16.0. No hosted backend, production Firebase credential, client adapter change, or voice engine is included in this phase.

The Hambin entry point now mounts the useful room REST operations and authenticated Socket.IO namespaces. Its schema separates rooms, stable memberships, streams, role leases, durable idempotency records, and sequenced outbox events. The stock WatchParty `Room`, timestamp broadcasts, global player controls, proxy, and virtual-browser routes remain outside Hambin mode. `/v1/capabilities` advertises Socket.IO and an empty voice array until Phase 5 is implemented.

## Verification

Run from the repository root with the local services running:

```sh
npm run typecheckServer --prefix backend/watchparty
npm run test:backend --prefix backend/watchparty
NODE_ENV=test DATABASE_URL=postgresql://hambin:local-development-only@127.0.0.1:15432/hambin_watchparty FIREBASE_PROJECT_ID=demo-hambin FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 npm run test:integration --prefix backend/watchparty
```

Integration fixtures are intentionally retained in the isolated, disposable migration database and Auth Emulator for inspection; every run creates fresh identities and rooms. Reset only the migration Docker volume explicitly when desired, and stop the local runtime before running fault tests. The suite temporarily renames the outbox table and terminates its own advisory-lock connection; never run it against a real Firebase project or a production database.

The integration command runs sequentially because the service is deliberately a single-node runtime. Independent `Rooms` instances/pools still exercise concurrent database transactions; a session advisory lock prevents a second full runtime from wiping live leases. Authentication uses the actual Auth Emulator/Admin SDK and transactions use actual PostgreSQL, rather than memory-store substitutes.

Evidence covered by `server/hambin/tests/rooms.integration.test.ts`:

| Gate | Observed assertion |
| --- | --- |
| Capacity and identity admission | Eight concurrent guest joins admit exactly seven; one identity racing different rooms wins one active seat; DB partial unique index protects active membership. |
| Stable ownership and return | Original owner leaves while guests remain; guest preview is full but owner preview leaves reserved admission available; owner returns with original membership UUID and role. Left historical members cannot fetch snapshots. |
| Stream authorization and serialization | Disconnected owner cannot start; concurrent starts produce one successful stream; abort retains ID/session/URL/start timestamp and increments revision; replacement gets new IDs; start/abort race revisions reflect only successful transitions. |
| Durable retries | Ten concurrent identical create keys return the same result and one room-created event; a second service instance replays the exact result; mismatched body reuse rejects; stream replay preserves telemetry; expired keys may be reused. |
| Room and voice leases | Replacement disconnect cannot remove the new role; a voice lease retains presence after room socket departure; detaching an old voice generation does not delete its replacement; an expired role is deleted and starts a new bounded grace period. The phase tests backend voice-role semantics without advertising implemented mesh voice. |
| Presence/restart/closure | Fresh membership begins disconnected; current namespace admission becomes present; recovery clears leases and gives bounded grace without changing room/stream metadata; grace expiry closes a lone room; explicit final leave closes immediately and duplicate leave is harmless; ended preview tombstones purge. |
| Snapshot/event race | Real namespace admission is paused after its consistent snapshot transaction; a newer stream mutation commits and dispatches into the registration buffer; snapshot arrives first and only higher sequences follow. Reversed event timestamps still dispatch in room sequence order. |
| Sync privacy | Fresh paused report produces requester-only advancement; an ahead requester keeps its position; current-session/membership validation and independent report cleanup preserve bounded telemetry; stock pause commands reject. |
| Authentication lifecycle | One-use ticket reuse rejects; same-UID refresh retains role; other-UID refresh rejects; provider outage maps to AUTH_UNAVAILABLE without replacing ownership; concurrent expired-verification commands share one provider check; explicit revoke drops roles/tickets; periodic revalidation rejects a disabled user. |
| Dispatcher recovery | Controlled sink and post-publish SQL-mark failures keep events pending and set readiness unavailable; an actual PostgreSQL table-unavailability interval likewise yields 503 readiness; after restoring it the same dispatcher resumes and later stream events arrive. Per-socket sequence deduplication prevents duplicate application delivery. |
| Runtime ownership loss | `pg_terminate_backend` kills the advisory-lock holder; HTTP drains, readiness returns 503, roles/tickets close, and shutdown closes all resources even when unlocking is impossible. |

The foundation integration remains part of the same suite and continues covering Firebase project/expiry/revocation checks, concurrent profile bootstrap, and failed-listen resource cleanup.

## Operational details and remaining gates

Lease renewals use one batched PostgreSQL update every ten seconds with a thirty-second lease; room grace remains thirty seconds. Room role authentication is revalidated before protected commands when its sixty-second verification window has elapsed. Healthy background checks run independently of commands; provider outages back off and block protected actions. Background checks and command checks share a global eight-call concurrency budget and per-connection single-flight. Firebase provider latency does not block the outbox dispatcher; stale verification blocks protected commands until a current check succeeds. Report retention is fifteen seconds, cleaned independently of sync requests. Initial event buffering and outgoing transport buffers are bounded; overloaded clients disconnect and recover through snapshots.

Loss of the dedicated runtime lock is fail-closed and requires restarting the local backend. Ordinary transient outbox/database failures retry with bounded backoff and automatically restore readiness. This is intentionally a single-node design; adding replicas requires shared routing, ticket/telemetry distribution and an explicit scaling design.

Remaining migration gates: native client Auth/Socket.IO adapters, actual audio/ICE/TURN and eight-device voice, Android background/media regression checks, platform builds, real Firebase-project smoke checks, representative load/soak tests and final Go/LiveKit removal. Passing these backend integration assertions is not a measured production capacity or cost claim. Existing Go/LiveKit defaults and old test database are untouched pending final cutover.

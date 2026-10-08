# Local WatchParty migration stack

This isolated stack leaves the existing Go/LiveKit default and its database untouched. No hosting account or Admin key is required for Auth Emulator tests. Use Node24.18.1/npm11.16.0.

From the repository root, install and start the isolated services:

```sh
npm ci --prefix backend/watchparty
npm ci --prefix backend/local
docker compose -p hambin-watchparty-local -f backend/compose.watchparty-local.yml up -d --wait
npm run auth --prefix backend/local
```

Wait for `All emulators ready`. In another terminal:

```sh
cd backend/watchparty
export NODE_ENV=development
export DATABASE_URL=postgresql://hambin:local-development-only@127.0.0.1:15432/hambin_watchparty
export FIREBASE_PROJECT_ID=demo-hambin
export FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099
export HOST=127.0.0.1
export PORT=8081
npm run start:hambin
```

With database/emulator running, the same configured terminal can run:

```sh
npm run typecheckServer
npm run test:backend
npm run test:integration
```

Integration tests require actual PostgreSQL/Auth Emulator; they never replace these with mocks. The normal test command skips integration unless explicitly enabled. `/healthz` asserts process liveness; `/readyz` checks DB and event-dispatcher health. Capabilities now advertise Socket.IO; voice stays unavailable until mesh is implemented. Room namespaces use one-use tickets and bootstrap with sequenced snapshots. Only one Hambin runtime may own the migration database; an advisory-lock holder loss drains it and requires a local backend restart. Transient outbox/database failures retry and recover readiness automatically. Production refuses emulator settings.

For real Firebase testing, enable Anonymous Authentication and disable automatic anonymous-account cleanup. Store the Admin service-account JSON outside the repository, set `GOOGLE_APPLICATION_CREDENTIALS` to its absolute path, use `FIREBASE_PROJECT_ID=hambin-dab14`, and unset `FIREBASE_AUTH_EMULATOR_HOST`. Android `google-services.json` is client configuration, not the Admin credential. Do not commit private keys or token dumps. Real-project smoke testing remains required before release.

Stop the isolated database with `docker compose -p hambin-watchparty-local -f backend/compose.watchparty-local.yml down`. Add `--volumes` only to deliberately reset migration test data. No old rooms/profiles are imported. Emulator users are transient; restarting it resets test authentication identities.

Phase 3 backend evidence and operational constraints are recorded in [the room-service evidence](../migration-tests/phase-3-evidence.md). Native clients still use their existing transport until Phase 4; no hosted deployment is configured.

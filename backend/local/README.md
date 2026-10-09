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

### Test the Android debug app against the local stack

Start the backend and Auth Emulator using the commands above, then boot the Android emulator and install the debug app:

```sh
adb devices -l
cd Roomio
./gradlew :androidApp:installDebug
```

Android debug builds target the host through `10.0.2.2`: Node `http://10.0.2.2:8081`, Auth Emulator `10.0.2.2:9099`, and Firebase project `demo-hambin`. The debug-only overrides are set by `AppActivity`; release builds retain the configured service URL and `hambin-dab14` Firebase project. This avoids exposing host `localhost` inside the Android virtual device. A physical device needs a reachable host LAN address and a separate local-debug config.

Integration tests require actual PostgreSQL/Auth Emulator; they never replace these with mocks. The normal test command skips integration unless explicitly enabled. `/healthz` asserts process liveness; `/readyz` checks DB and event-dispatcher health. Capabilities advertise Socket.IO and advertise mesh voice only when valid private TURN URLs and a 32-byte shared secret are configured. Room and voice namespaces use one-use tickets; room namespaces bootstrap with sequenced snapshots. Only one Hambin runtime may own the migration database; an advisory-lock holder loss drains it and requires a local backend restart. Transient outbox/database failures retry and recover readiness automatically. Production refuses emulator settings.

For local voice signaling and TURN allocation, use the private Coturn profile with fresh local-only credentials:

```sh
export TURN_SHARED_SECRET="$(openssl rand -hex 32)"
export TURN_EXTERNAL_IP=127.0.0.1
docker compose -p hambin-watchparty-local -f backend/compose.watchparty-local.yml --profile voice up -d --wait
```

Set `TURN_SHARED_SECRET` in the backend process environment and configure `TURN_URLS` to the Coturn address, for example `turn:127.0.0.1:3478?transport=udp,turn:127.0.0.1:3478?transport=tcp`. The emulator must be able to reach the advertised relay address and mapped UDP relay ports; use its host-reachable address for both `TURN_EXTERNAL_IP` and `TURN_URLS` when testing from Android. TURN credentials returned to clients expire after ten minutes. The coturn profile is optional and is not started by the default database-only Compose command.

For real Firebase testing, enable Anonymous Authentication and disable automatic anonymous-account cleanup. Store the Admin service-account JSON outside the repository, set `GOOGLE_APPLICATION_CREDENTIALS` to its absolute path, use `FIREBASE_PROJECT_ID=hambin-dab14`, and unset `FIREBASE_AUTH_EMULATOR_HOST`. Android `google-services.json` is client configuration, not the Admin credential. Do not commit private keys or token dumps. Real-project smoke testing remains required before release.

Stop the isolated database with `docker compose -p hambin-watchparty-local -f backend/compose.watchparty-local.yml down`. Add `--volumes` only to deliberately reset migration test data. No old rooms/profiles are imported. Emulator users are transient; restarting it resets test authentication identities.

The Phase 5 backend now provides mesh voice configuration, short-lived TURN REST credentials, a separately ticketed voice namespace, role leases, mute/roster updates, and scoped SDP/ICE signaling. Local verification passes `npm run typecheckServer`, `npm run test:backend`, and the full `npm run test:integration` suite (7/7 integration tests) against PostgreSQL and the Firebase Auth Emulator. These tests verify TURN credential generation and signaling; they do not prove gathered ICE candidates or real audio relay. Android WebRTC media, emulator/device relay reachability, and multi-device audio remain to be verified; see the [Phase 5 evidence](../migration-tests/phase-5-evidence.md). Room-service design and earlier operational evidence are in the [Phase 3 evidence](../migration-tests/phase-3-evidence.md). No hosted deployment is configured.

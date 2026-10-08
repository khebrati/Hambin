# Phase 0–2 evidence, 2026-10-05

Fork baseline `bf98b0a3b551cfb9d5f070c12d4fb9387e425940`, local branch `hambin-migration`; root branch `feat/watchparty-backend-migration`. No pushes. A future local implementation commit needs publication before another clone can reproduce its submodule pointer.

Discovery inspected requirements, DD-022, migration/compatibility docs, existing profile/identity code, PG persistence and REST surface, and fork bootstrap/config/room/signaling. Go baseline untouched. Profile limits use Unicode codepoints for name and UTF8 bytes for avatar/language.

Unmodified backend-source proof ran before changes: `NODE_ENV=production PORT=18080 HOST=127.0.0.1 node server/server.ts` followed by `node backend/migration-tests/stock-proof.mjs` from root. Eight actual Socket.IO clients passed ping, create, namespace/roster, name/avatar, direct URL receipt, targeted offer/answer/ICE-shaped signaling, voice-named opt-in. Demonstrated unlocked guest hosting, immediate disconnect removal, and visible timestamp maps. Stock process stopped afterwards. Dependency lock repair was in progress, without stock handler changes. No microphone, actual SDP/ICE negotiation, direct/relay audio, physical-device test, or optional stock Firebase proof was performed: these gates remain open.

Verified:

- Lock regenerated; Firebase Admin upgraded from11 to pinned14.5.0; legacy optional helper uses modular SDK imports for compatibility.
- Plain `npm ci` passed Node24.18.1/npm11.16.0 including native scripts.
- `npm run typecheckServer` passed.
- Four unit/HTTP tests passed: Unicode validation, production emulator refusal, mandatory auth, invalid/outage classification, disabled stock routes, bootstrap, actual capabilities, DB-down readiness.
- `NODE_ENV=test DATABASE_URL=postgresql://hambin:local-development-only@127.0.0.1:15432/hambin_watchparty FIREBASE_PROJECT_ID=demo-hambin FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 npm run test:integration` passed real PostgreSQL17/AuthEmulator15.32.1:15 concurrent bootstraps one UUID, wrong audience/issuer/expired/forged rejection, refresh sameUID/UUID, second pool durable mapping, disabled/revoked rejection. Initial run preceded emulator readiness and failed; rerun after readiness passed.

Open: real-project Admin key/smoke test, physical audio/TURN gates, real provider-outage injection. Phase3/5 must add membership/ticket/socket revoke hooks, session leases/reauthentication, dispatcher and realtime/voice; none are advertised yet. Identity mapping uses real PG, not memory.

Inherited audit after Admin upgrade:46 advisories all dependencies,28 production including1 critical websocket-driver. Hambin mode excludes stock proxy/VM/payments/FirebaseDatabase imports, but their dependencies remain packaged and need remediation before release. Local Firebase tooling has11 devtool advisories. No forced upgrade was used.

## Independent verification and review, 2026-10-08

An independent verifier started the isolated PostgreSQL service with `docker compose -p hambin-watchparty-local -f backend/compose.watchparty-local.yml up -d --wait` and Auth Emulator with `npm run auth --prefix backend/local`. PostgreSQL17 was healthy on port15432; Auth Emulator15.32.1 reached `All emulators ready` on port9099. Neither service uses the existing Go database volume.

From `backend/watchparty`, these commands passed:

```sh
npm run typecheckServer
npm run test:backend
NODE_ENV=development DATABASE_URL=postgresql://hambin:local-development-only@127.0.0.1:15432/hambin_watchparty FIREBASE_PROJECT_ID=demo-hambin FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 HOST=127.0.0.1 PORT=8081 npm run test:integration
```

Typecheck exited0. Backend tests reported4 passed,0 failed,1 integration skipped; the explicit integration command reported1 passed,0 failed against actual PostgreSQL/Auth Emulator. The integration includes15 concurrent bootstraps, invalid audience/issuer/expiry/malformed credentials, refresh identity stability, second-pool persistence, disabled/revoked users, health/readiness, and occupied-port startup cleanup. A premature first attempt failed with `ECONNREFUSED` before emulator readiness; the rerun after readiness passed. The emulator CLI's remote configuration fetch warning was nonfatal.

Independent code review passed after both requested fixes: unexpected application failures now return500 `INTERNAL_ERROR` rather than validation errors, and every startup failure path closes created application/auth/database resources. Unit/HTTP tests exercise the unknown-error response; the real integration forces `EADDRINUSE` and verifies no tagged database connection remains. Production emulator/project guards and mandatory revocation verification remain enforced.

This verification does not constitute real-project Firebase authentication, actual microphone capture, valid SDP/ICE negotiation, direct/relay audio, physical-device audio, or real provider-outage injection. Those release gates remain open. Only local commits are created; the fork implementation commit must be published separately before a fresh clone can fetch the parent repository's pinned submodule revision.

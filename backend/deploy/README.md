# Migrated VPS deployment

This is the active Node/Firebase/PostgreSQL/Caddy/Coturn layout. Root `deploy.sh`,
`backend/docker-compose.yml`, and `backend/deploy-aws.md` describe the previous
Go/LiveKit stack and must not be used to update this deployment.

The site is `https://watchparty.audiosense.ir`. Caddy serves the `webUI` build and
proxies `/v1/*`, `/socket.io/*`, `/healthz`, and `/readyz` to the internal API.
`turn.watchparty.audiosense.ir` serves private authenticated TURN on UDP/TCP 3478,
TLS TCP 5349, and UDP relay ports 49160–49200. Both DNS records are DNS-only and
resolve to `46.249.98.175`. Allow those ports plus TCP 80/443 and administrative
SSH at the provider firewall. PostgreSQL and the API have no public port mappings.

## Files on the VPS

- `/opt/hambin/releases/<release>/`: source subset, static web assets, Compose,
  Caddyfile, TURN launcher, and `release.json` with source checksums and Git SHAs.
- `/opt/hambin/current`: symlink to the active release.
- `/opt/hambin/.env`: server-only configuration and fresh database/TURN secrets,
  mode 0600. Each release links its `.env` here.
- `/opt/hambin/secrets/firebase-admin.json`: Admin credential, owner UID 1000 and
  mode 0400, mounted read-only into the non-root API. Never bake it into an image.
- `/opt/hambin/turn-tls/`: certificate exported from Caddy and a private key.
- `/opt/hambin/backups/`: local PostgreSQL custom-format dumps, retained until an
  explicit retention/offsite policy is chosen. These do not protect against VPS loss.

The Compose project is always `hambin`, so named volumes survive release changes.
Services restart automatically. Caddy manages publicly trusted certificates.
The `hambin-turn-tls.timer` checks certificates daily and restarts Coturn only
when certificate bytes change. `hambin-backup.timer` runs a daily local backup.

## Build and update

Build the frontend locally with Node 24.18.1/npm 11.16.0 and real public Firebase
client settings: `VITE_FIREBASE_PROJECT_ID=hambin-dab14`, the public client API key,
`VITE_FIREBASE_AUTH_DOMAIN=hambin-dab14.firebaseapp.com`, and empty
`VITE_FIREBASE_AUTH_EMULATOR_URL` / `VITE_BACKEND_URL`. Process environment values
override any local emulator configuration. Run `npm run build` and `npm test` in
`webUI`. Copy only `webUI/dist/` to the release's `web/` directory.

Stage `package.json`, `package-lock.json`, `server/`, and `sql/` from the pinned
`backend/watchparty` worktree into the release's `api/` directory, with
`Dockerfile.api` renamed to `api/Dockerfile`. Copy `compose.yml`, `Caddyfile`, and
`turn-entrypoint.sh` alongside it. Do not copy `.env`, credentials, node_modules,
or repository metadata. Record the exact source checksums, parent SHA, and fork
SHA in `release.json`; explicitly identify any uncommitted browser source.

Transfer the staged release to `/opt/hambin/releases/<release>/`, link its `.env`
to `/opt/hambin/.env`, and update `RELEASE_ID` in that protected configuration.
On the server, in the release directory:

```sh
docker compose config --quiet
docker compose build api
docker compose up -d --wait postgres api caddy
ln -sfn /opt/hambin/releases/<release> /opt/hambin/current
/opt/hambin/sync-turn-tls.sh
docker compose ps
curl --fail https://watchparty.audiosense.ir/readyz
```

Before initial TURN startup, Caddy must have issued the TURN-domain certificate.
Install the supplied certificate-sync and backup scripts under `/opt/hambin/`
with owner-only executable permissions, and enable their systemd timers.
Firebase Authentication must be initialized, Anonymous sign-in enabled, and
automatic anonymous-account cleanup disabled. Verify a real browser's sign-in,
room creation/join, Socket.IO connection, playback, and voice after deploying.

## Operations and recovery

```sh
cd /opt/hambin/current
docker compose ps
docker compose logs --tail=80 api
docker compose logs --tail=80 caddy
docker compose logs --tail=80 coturn
/opt/hambin/backup.sh
systemctl list-timers 'hambin-*'
```

To inspect a backup without changing live data:

```sh
cat /opt/hambin/backups/<backup>.dump | docker compose exec -T postgres pg_restore --list
```

For rollback, first take a backup. Set `RELEASE_ID` to the previous retained API
image tag, enter the corresponding retained release directory, and run
`docker compose up -d --wait api caddy`. Then repoint `current`. Rollback is safe
only when that Node image accepts the current migration checksums/schema; never
point migrated clients at Go/LiveKit. Do not delete named volumes or reset the
database as part of an update. First-deployment rollback has no previous release.

## Later domain change

Point `watchparty.top` and `turn.watchparty.top` at the VPS (DNS-only), update
`SITE_DOMAIN` and `TURN_DOMAIN` in `/opt/hambin/.env`, and recreate Caddy and API
with `docker compose up -d --force-recreate caddy api`. Export the new TURN
certificate with `/opt/hambin/sync-turn-tls.sh`, then recreate Coturn to apply its
new realm/configuration. The frontend uses relative API/Socket.IO URLs and needs
no backend-origin rebuild. Add the new website domain to Firebase Authentication
if its domain policy requires it. A new browser origin has separate Firebase
storage: existing anonymous credentials and ownership do not automatically move
between domains. Keep the old site available for owners or explicitly plan that
identity transition before disabling it.

## Remaining acceptance

See [the first deployment evidence](../../docs/vps-deployment-evidence.md) for checks and current limitations.

Hosted readiness is distinct from eight-peer capacity, physical Android/browser
audio interoperability, restrictive-network fallback, offsite backup/restore,
and long-running load/soak acceptance. A passing health endpoint alone does not
prove Firebase sign-in or voice relay.

#!/usr/bin/env bash
# Existing-host deployments of the Node/PostgreSQL/Caddy/Coturn stack.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_ID="${DEPLOY_RELEASE_ID:-$(date -u +%Y%m%dT%H%M%SZ)-$(git -C "$ROOT" rev-parse --short HEAD)}"
OUTPUT="${DEPLOY_OUTPUT:-$ROOT/.tools/deployment/$RELEASE_ID}"
SSH_USER="${DEPLOY_SSH_USER:-root}"
SSH_HOST="${DEPLOY_SSH_HOST:-}"
if [[ -z "$SSH_HOST" && -f "$ROOT/Roomio/.local-server/ip" ]]; then
  SSH_HOST="$(tr -d '\r\n' < "$ROOT/Roomio/.local-server/ip")"
fi
SSH_ARGS=(-o ConnectTimeout=15 -o ServerAliveInterval=15 -o StrictHostKeyChecking=yes)
if [[ -n "${DEPLOY_SSH_KEY:-}" ]]; then SSH_ARGS+=(-i "$DEPLOY_SSH_KEY" -o IdentitiesOnly=yes); fi
fail() { echo "$*" >&2; exit 1; }
[[ "$(basename "$OUTPUT")" == "$RELEASE_ID" ]] || fail 'DEPLOY_OUTPUT basename must equal DEPLOY_RELEASE_ID'
[[ "$RELEASE_ID" =~ ^[a-zA-Z0-9][a-zA-Z0-9._-]*$ ]] || fail 'Invalid DEPLOY_RELEASE_ID'
remote() {
  [[ "$SSH_HOST" =~ ^[a-zA-Z0-9._:-]+$ && "$SSH_USER" =~ ^[a-zA-Z0-9_-]+$ ]] || fail 'Set DEPLOY_SSH_HOST and DEPLOY_SSH_USER'
  ssh "${SSH_ARGS[@]}" "$SSH_USER@$SSH_HOST" "$@"
}
build() {
  [[ ! -e "$OUTPUT" ]] || fail "Output exists: $OUTPUT; choose a new DEPLOY_RELEASE_ID"
  (cd "$ROOT/backend/watchparty"; npm ci; npm run typecheckServer; npm run test:backend)
  (cd "$ROOT/webUI"; npm ci; npm test; VITE_BACKEND_URL='' npm run build)
  mkdir -p "$OUTPUT/api" "$OUTPUT/web"
  cp "$ROOT/backend/watchparty/package.json" "$ROOT/backend/watchparty/package-lock.json" "$OUTPUT/api/"
  cp -R "$ROOT/backend/watchparty/server" "$ROOT/backend/watchparty/sql" "$OUTPUT/api/"
  cp "$ROOT/backend/deploy/Dockerfile.api" "$OUTPUT/api/Dockerfile"
  cp -R "$ROOT/webUI/dist/." "$OUTPUT/web/"
  for file in compose.yml Caddyfile haproxy.cfg turn-entrypoint.sh sync-turn-tls.sh activate-release.sh remove-backups.sh; do
    cp "$ROOT/backend/deploy/$file" "$OUTPUT/"
  done
  python3 - "$ROOT" "$OUTPUT" "$RELEASE_ID" <<'PY'
import hashlib,json,pathlib,subprocess,sys
root,out,release=pathlib.Path(sys.argv[1]),pathlib.Path(sys.argv[2]),sys.argv[3]
files={str(p.relative_to(out)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(out.rglob('*')) if p.is_file()}
manifest={'releaseId':release,'commit':subprocess.check_output(['git','-C',str(root),'rev-parse','HEAD'],text=True).strip(),'sourceStatus':subprocess.check_output(['git','-C',str(root),'status','--porcelain'],text=True),'files':files}
(out/'release.json').write_text(json.dumps(manifest,indent=2)+'\n')
PY
  tar -czf "$OUTPUT.tar.gz" -C "$OUTPUT" .
  (cd "$(dirname "$OUTPUT")"; sha256sum "$(basename "$OUTPUT").tar.gz" > "$(basename "$OUTPUT").tar.gz.sha256")
  echo "Prepared $OUTPUT.tar.gz (includes current working-tree source; no automatic commit/push)"
}
deploy() {
  [[ -f "$OUTPUT.tar.gz" && -f "$OUTPUT.tar.gz.sha256" ]] || fail 'Run build with the same DEPLOY_RELEASE_ID first'
  remote "test -d /opt/hambin/current && test -f /opt/hambin/.env && test ! -e /opt/hambin/releases/$RELEASE_ID && mkdir -p /opt/hambin/incoming"
  scp "${SSH_ARGS[@]}" "$OUTPUT.tar.gz" "$OUTPUT.tar.gz.sha256" "$SSH_USER@$SSH_HOST:/opt/hambin/incoming/"
  remote "cd /opt/hambin/incoming && sha256sum -c $RELEASE_ID.tar.gz.sha256 && mkdir /opt/hambin/releases/$RELEASE_ID && tar -xzf $RELEASE_ID.tar.gz -C /opt/hambin/releases/$RELEASE_ID && bash /opt/hambin/releases/$RELEASE_ID/activate-release.sh $RELEASE_ID"
  curl --fail --silent --show-error --max-time 30 "${DEPLOY_SITE_URL:-https://watchparty.top}/readyz" | python3 -c 'import json,sys; r=json.load(sys.stdin); print(r); sys.exit(0 if all(r.get(k) is True for k in ("ready","database","dispatcher","voice")) else 1)'
}
status() {
  remote 'cd /opt/hambin/current && docker compose ps && free -h && df -h / && systemctl list-timers "hambin-*" --no-pager'
  curl --fail --silent --show-error --max-time 30 "${DEPLOY_SITE_URL:-https://watchparty.top}/readyz"
}
case "${1:---help}" in
  build) build ;;
  deploy) deploy ;;
  status) status ;;
  remove-backups) remote 'bash -s' < "$ROOT/backend/deploy/remove-backups.sh" ;;
  -h|--help) cat <<'HELP'
Hambin Node/PostgreSQL/Caddy/Coturn operations (existing /opt/hambin host).
  ./deploy.sh build            test/build and package current source locally
  ./deploy.sh deploy           transfer/verify/build API and activate prepared release
  ./deploy.sh status           show active services, resources, timers and readiness
  ./deploy.sh remove-backups   stop/delete backup automation and stored server backups

Use the SAME DEPLOY_RELEASE_ID for build and deploy, e.g.:
  export DEPLOY_RELEASE_ID=20261111-launch
  ./deploy.sh build
  ./deploy.sh deploy

DEPLOY_SSH_HOST defaults to Roomio/.local-server/ip; DEPLOY_SSH_USER defaults to root.
DEPLOY_SSH_KEY is optional (otherwise SSH agent/password); known host key required.
DEPLOY_OUTPUT optionally changes the staging directory (basename must equal release ID).
DEPLOY_SITE_URL defaults to https://watchparty.top (public verification).
The retired .tools/deploy.conf and backend/docker-compose.yml are never used.
No Git commit/push, database reset, dump, or automatic rollback is performed.
Android releases are built/published separately; existing downloads are preserved.
Existing TLS/router/network setup is required; see backend/deploy/README.md.
HELP
  ;;
  *) fail 'Unknown command; use --help' ;;
esac

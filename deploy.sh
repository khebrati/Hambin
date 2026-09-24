#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/.tools/deploy.conf"

SSH_USER="${DEPLOY_SSH_USER:-}"
SSH_HOST="${DEPLOY_SSH_HOST:-}"
SSH_KEY="${DEPLOY_SSH_KEY:-$HOME/.ssh/hambin.pem}"
SERVER_REPO="${DEPLOY_SERVER_REPO:-/home/ubuntu/Source/Hambin}"
SSH_OPTIONS=(-i "$SSH_KEY" -o ConnectTimeout=10 -o ServerAliveInterval=15 -o StrictHostKeyChecking=accept-new)

if [[ -f "$CONFIG_FILE" ]]; then
  while IFS='=' read -r key value; do
    [[ -z "$key" || "$key" == \#* ]] && continue
    case "$key" in
      SSH_USER) [[ -z "$SSH_USER" ]] && SSH_USER="$value" ;;
      SSH_HOST) [[ -z "$SSH_HOST" ]] && SSH_HOST="$value" ;;
      SSH_KEY) [[ -z "$SSH_KEY" ]] && SSH_KEY="$value" ;;
      SERVER_REPO) [[ -z "$SERVER_REPO" ]] && SERVER_REPO="$value" ;;
    esac
  done <"$CONFIG_FILE"
fi

ensure_config() {
  if [[ -z "$SSH_KEY" || ! -f "$SSH_KEY" ]]; then
    read -rp "Path to SSH private key [$HOME/.ssh/hambin.pem]: " ans
    SSH_KEY="${ans:-$HOME/.ssh/hambin.pem}"
  fi
  if [[ ! -f "$SSH_KEY" ]]; then
    echo "Private key not found: $SSH_KEY" >&2
    return 1
  fi
  if [[ -z "$SSH_USER" ]]; then
    read -rp "SSH user [ubuntu]: " ans
    SSH_USER="${ans:-ubuntu}"
  fi
  if [[ -z "$SSH_HOST" ]]; then
    read -rp "SSH host or user@host (e.g. 203.0.113.10): " ans
    if [[ "$ans" == *@* ]]; then
      SSH_HOST="${ans##*@}"
      SSH_USER="${ans%%@*}"
    else
      SSH_HOST="${ans:-}"
    fi
  fi
  if [[ -z "$SSH_HOST" ]]; then
    echo "No SSH host configured." >&2
    return 1
  fi
  mkdir -p "$SCRIPT_DIR/.tools"
  umask 177
  cat >"$CONFIG_FILE" <<EOF
SSH_USER=$SSH_USER
SSH_HOST=$SSH_HOST
SSH_KEY=$SSH_KEY
SERVER_REPO=$SERVER_REPO
EOF
  umask 022
}

ssh_run() {
  local target="$SSH_USER@$SSH_HOST"
  local cmd="$1"
  ssh -o BatchMode=yes -o IdentitiesOnly=yes "${SSH_OPTIONS[@]}" "$target" "$cmd" || {
    echo "SSH to $target failed (key: $SSH_KEY)." >&2
    return 1
  }
}

current_branch() {
  git -C "$SCRIPT_DIR" branch --show-current
}

commit_local_changes() {
  local branch msg porcelain
  branch="$(current_branch)"
  porcelain="$(git -C "$SCRIPT_DIR" status --porcelain)"
  if [[ -z "$porcelain" ]]; then
    echo "Working tree is clean; nothing to commit."
    return 0
  fi
  echo "Uncommitted changes on '$branch':"
  git -C "$SCRIPT_DIR" status --short
  read -rp "Commit message (empty to cancel commit): " msg
  if [[ -z "$msg" ]]; then
    echo "Commit cancelled."
    return 1
  fi
git -C "$SCRIPT_DIR" add -A
  git -C "$SCRIPT_DIR" commit -m "$msg"
}

push_branch() {
  local branch
  branch="$(current_branch)"
  if git -C "$SCRIPT_DIR" rev-parse --abbrev-ref --symbolic-full-name @{u} >/dev/null 2>&1; then
    git -C "$SCRIPT_DIR" push
  else
    git -C "$SCRIPT_DIR" push -u origin "$branch"
  fi
}

option_build() {
  echo "== Building backend API (docker) =="
  if docker compose -f "$SCRIPT_DIR/backend/docker-compose.yml" build api; then
    echo "Build OK."
    return 0
  fi
  echo "docker compose build failed; trying plain docker build..."
  if docker build -t hambin-api "$SCRIPT_DIR/backend"; then
    echo "Build OK (docker build)."
    return 0
  fi
  echo "Build FAILED." >&2
  return 1
}

option_deploy() {
  ensure_config || return 1
  local branch
  branch="$(current_branch)"
  echo "Branch: $branch"
  commit_local_changes || return 1
  echo "== Pushing to origin =="
  push_branch
  echo "== Deploying on $SSH_USER@$SSH_HOST =="
  ssh_run "
    set -e
    if [ ! -d '$SERVER_REPO' ]; then
      echo 'Server repo not found at $SERVER_REPO' >&2
      exit 1
    fi
    cd '$SERVER_REPO'
    git fetch origin
    git checkout -B '$branch' origin/'$branch'
    cd backend
    echo '== Building API on server =='
    docker compose build api
    echo '== Starting stack =='
    docker compose up -d
    docker compose ps
    DOMAIN=\$(grep -E '^HAM_DOMAIN=' .env | head -1 | cut -d= -f2-)
    if [ -n \"\$DOMAIN\" ]; then
      echo \"== Health: https://\$DOMAIN/healthz ==\"
      curl -s -m 10 \"https://\$DOMAIN/healthz\" || echo 'healthz unreachable'
    fi
  " || return 1
}

option_status() {
  ensure_config || return 1
  echo "== Server diagnostics: $SSH_USER@$SSH_HOST =="
  ssh_run "
    echo '===== host ====='
    hostname
    uname -a
    echo
    echo '===== uptime ====='
    uptime
    echo
    echo '===== memory ====='
    free -h
    echo
    echo '===== disk (/) ====='
    df -h / | sort -rk5
    echo
    echo '===== kernel OOM / panics ====='
    dmesg -T 2>/dev/null | grep -iE 'oom|killed process|panic' | tail -20 || echo '(none found)'
    echo
    echo '===== docker disk usage ====='
    docker system df || true
    echo
    if [ -d '$SERVER_REPO' ]; then
      cd '$SERVER_REPO'/backend
      echo '===== compose ps ====='
      docker compose ps
      echo
      echo '===== api logs (last 40) ====='
      docker compose logs --tail=40 api || true
      echo
      echo '===== caddy logs (last 20) ====='
      docker compose logs --tail=20 caddy || true
      echo
      echo '===== livekit logs (last 20) ====='
      docker compose logs --tail=20 livekit || true
      echo
      DOMAIN=\$(grep -E '^HAM_DOMAIN=' .env | head -1 | cut -d= -f2-)
      if [ -n \"\$DOMAIN\" ]; then
        echo \"===== health: https://\$DOMAIN/healthz =====\"
        curl -s -m 10 \"https://\$DOMAIN/healthz\" || echo 'healthz unreachable'
      fi
    else
      echo 'Server repo not found at $SERVER_REPO'
    fi
  " || return 1
}

main_menu() {
  while true; do
    clear
    cat <<'EOF'

   ██╗  ██╗ █████╗ ███╗   ███╗██████╗ ██╗███╗   ██╗
   ██║  ██║██╔══██╗████╗ ████║██╔══██╗██║████╗  ██║
   ███████║███████║██╔████╔██║██████╔╝██║██╔██╗ ██║
   ██╔══██║██╔══██║██║╚██╔╝██║██╔══██╗██║██║╚██╗██║
   ██║  ██║██║  ██║██║ ╚═╝ ██║██████╔╝██║██║ ╚████║
   ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝     ╚═╝╚═════╝ ╚═╝╚═╝  ╚═══╝
           backend operations
EOF
    echo
    echo "  Target: ${SSH_USER:-<user>}@${SSH_HOST:-<host>}  |  Key: ${SSH_KEY:-<none>}"
    echo "  Server repo: $SERVER_REPO"
    echo
    echo "  1) Build backend project"
    echo "  2) Deploy to server (ssh)"
    echo "  3) Server status + diagnostics"
    echo "  q) Quit"
    echo
    read -rp "Select: " choice
    case "$choice" in
      1) option_build || true ;;
      2) option_deploy || true ;;
      3) option_status || true ;;
      q|Q|exit) echo "Bye."; exit 0 ;;
      *) echo "Invalid option." ;;
    esac
    echo
    read -rp "Press Enter to continue..."
  done
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<HELP
Hambin backend operations TUI.

Env overrides (or .tools/deploy.conf):
  DEPLOY_SSH_USER      SSH user for the server (default: ubuntu)
  DEPLOY_SSH_HOST      Server host or IP
  DEPLOY_SSH_KEY       SSH private key (default: ~/.ssh/hambin.pem)
  DEPLOY_SERVER_REPO   Git repo path on the server (default: /home/ubuntu/Source/Hambin)

Run non-interactively:
  $0 build      build the backend API image
  $0 deploy     commit dirty work, push, pull + rebuild on the server
  $0 status     show server status and diagnostics
HELP
  exit 0
fi

case "${1:-}" in
  build) option_build ;;
  deploy) option_deploy ;;
  status) option_status ;;
  "") main_menu ;;
  *) echo "Unknown command: $1 (see --help)" >&2; exit 1 ;;
esac
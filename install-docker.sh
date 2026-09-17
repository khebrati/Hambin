#!/usr/bin/env bash
# Install Docker Engine + Compose plugin on Linux Mint 22 (Ubuntu 24.04 "noble").
#
# Run from your own terminal because sudo will prompt for your password:
#   bash /home/erfan/Documents/Source/Hambin/install-docker.sh
#
# Note: this machine has ~3.4 GB free on /. Docker images (Go builder,
# Postgres, Caddy, LiveKit) may exceed that. Free space first if the build
# fails with "no space left on device".

set -euo pipefail

echo "==> Removing conflicting distro packages (if any)"
sudo apt-get update
sudo apt-get remove -y docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc || true

echo "==> Adding Docker's official GPG key and repository"
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu noble stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

echo "==> Installing Docker Engine and Compose plugin"
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

echo "==> Enabling the Docker daemon"
sudo systemctl enable --now docker

echo "==> Allowing your user to run docker without sudo"
sudo usermod -aG docker "$USER"

echo "==> Verifying"
sudo docker run --rm hello-world
sudo docker compose version
sudo docker info

cat <<'EOF'

Docker is installed.

Next: close and reopen your terminal (or run `newgrp docker`) so the docker
group membership applies, then confirm:

    docker info

After that, tell me it is ready and I will verify the backend:
    docker compose -f backend/docker-compose.yml config
    docker build backend/
    start Postgres + API and re-run the end-to-end smoke test
EOF

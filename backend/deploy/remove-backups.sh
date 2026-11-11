#!/usr/bin/env bash
# Explicitly authorized removal of Hambin's backup automation and saved snapshots.
set -euo pipefail
[[ "$(id -u)" == 0 ]] || { echo 'Run as root on the deployment host' >&2; exit 1; }
if systemctl cat hambin-backup.timer >/dev/null 2>&1; then systemctl disable --now hambin-backup.timer; fi
if systemctl cat hambin-backup.service >/dev/null 2>&1; then systemctl stop hambin-backup.service; fi
rm -f /etc/systemd/system/hambin-backup.timer /etc/systemd/system/hambin-backup.service /opt/hambin/backup.sh
systemctl daemon-reload
# Restrict deletion to the fixed application backup path; never follow a symlink.
if [[ -L /opt/hambin/backups ]]; then
  unlink /opt/hambin/backups
elif [[ -d /opt/hambin/backups ]]; then
  rm -rf -- /opt/hambin/backups
fi
systemctl reset-failed hambin-backup.service hambin-backup.timer 2>/dev/null || true
echo 'Hambin backup automation and /opt/hambin/backups removed.'

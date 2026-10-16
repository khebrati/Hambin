#!/bin/sh
set -eu
umask 077
cd /opt/hambin/current
backup=/opt/hambin/backups/hambin-$(date -u +%Y%m%dT%H%M%SZ).dump
docker compose exec -T postgres pg_dump -U hambin -d hambin -Fc > "$backup.tmp"
mv "$backup.tmp" "$backup"
# Retain snapshots until an explicit retention policy and offsite destination are selected.

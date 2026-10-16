#!/bin/sh
set -eu
test -s /tls/fullchain.pem
test -s /tls/privkey.pem
exec turnserver --no-cli --fingerprint --use-auth-secret \
  --static-auth-secret="$TURN_SHARED_SECRET" --realm="$TURN_DOMAIN" \
  --external-ip="$PUBLIC_IP" --relay-ip="$PUBLIC_IP" --listening-ip="$PUBLIC_IP" --listening-port=3478 \
  --tls-listening-port=5349 --cert=/tls/fullchain.pem --pkey=/tls/privkey.pem \
  --min-port=49160 --max-port=49200 --no-multicast-peers --no-tlsv1 --no-tlsv1_1 \
  --user-quota=24 --total-quota=400 --max-bps=128000 --bps-capacity=51200000 \
  --denied-peer-ip=0.0.0.0-0.255.255.255 \
  --denied-peer-ip=10.0.0.0-10.255.255.255 \
  --denied-peer-ip=100.64.0.0-100.127.255.255 \
  --denied-peer-ip=127.0.0.0-127.255.255.255 \
  --denied-peer-ip=169.254.0.0-169.254.255.255 \
  --denied-peer-ip=172.16.0.0-172.31.255.255 \
  --denied-peer-ip=192.168.0.0-192.168.255.255 \
  --denied-peer-ip=224.0.0.0-255.255.255.255 \
  --denied-peer-ip=::1 --denied-peer-ip=fc00::-fdff:ffff:ffff:ffff:ffff:ffff:ffff:ffff \
  --denied-peer-ip=fe80::-febf:ffff:ffff:ffff:ffff:ffff:ffff:ffff \
  --log-file=stdout --simple-log

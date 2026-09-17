# Deploying Hambin on a single AWS VM

This guide takes you from an empty AWS account to a running, HTTPS-secured
Hambin stack on one Linux VM:

```
                 Internet
                    │
          ┌─────────┴──────────┐
          │  Elastic IP (static)│
          └─────────┬──────────┘
                    │  :80/:443            :3478/udp+tcp, :50000-50100/udp
              ┌─────▼─────┐                 ┌─────▼─────┐
              │   Caddy   │                 │  LiveKit  │
              │ (TLS/443) │                 │ + TURN    │
              └──┬─────┬──┘                 └─────┬─────┘
        wss://api  │     │ wss://livekit          │ (internal)
             ┌─────▼──┐  └──────────────────►┌────▼─────┐
             │  API   │◄──────────────────── │ webhook  │
             └───┬────┘                      └──────────┘
                 │ internal only
            ┌────▼────┐
            │ Postgres│
            └─────────┘
```

Only Caddy (80/443) and the LiveKit TURN endpoint (3478 + relay range) are
exposed publicly. The API and Postgres are reachable only inside the VM.

You will need:

- An AWS account.
- A domain you can create DNS records for (example: `example.com`).
- A terminal with `ssh`.
- About 20 minutes.

The running cost is roughly a `t3.small` (~$15/mo) + a small EBS volume + a
static pack of TURN relay bandwidth. Voice relay is the only significant
variable cost.

---

## Part 1 — Launch the EC2 instance

1. Sign in to the AWS Console and pick a region close to your users
   (for example `eu-central-1` or `us-east-1`). Keep this region for every
   step below.
2. Open **EC2 → Instances → Launch instance**.
3. **Name:** `hambin`.
4. **Application and OS Image (AMI):** `Ubuntu` → **Ubuntu Server 24.04 LTS
   (HVM), SSD Volume Type**, architecture `64-bit (x86)`.
5. **Instance type:** `t3.small` (2 vCPU, 2 GiB). Use `t3.medium` if you
   expect heavier voice relay.
6. **Key pair:** click **Create new key pair**, name it `hambin`, type `RSA`,
   format `.pem`, and download it. You will need this to SSH in.
7. **Network settings → Edit:**
   - **Auto-assign public IP:** `Enable`.
   - **Firewall (security group):** `Create security group`, name `hambin-sg`.
     Add the inbound rules from Part 3 now (you can also add them later).
8. **Configure storage:** `30 GiB`, type `gp3`.
9. Click **Launch instance**.

Wait until the instance state is **Running**.

---

## Part 2 — Allocate a static IP (Elastic IP)

A normal public IP changes if you stop/start the instance, which would break
DNS and TLS. An Elastic IP stays fixed.

1. Open **EC2 → Elastic IPs → Allocate Elastic IP address**.
2. **Network border group:** leave the default. Click **Allocate**.
3. Select the new address → **Actions → Associate Elastic IP address**.
4. **Resource type:** `Instance`. **Instance:** select `hambin`.
   **Private IP:** select the instance's IP. Click **Associate**.
5. Copy the **Elastic IP address** (for example `203.0.113.10`). This is your
   server's permanent address. Everywhere below, replace `<EIP>` with it.

---

## Part 3 — Open the firewall (Security Group)

Open **EC2 → Security Groups → `hambin-sg` → Edit inbound rules** and add:

| Type | Protocol | Port range | Source | Why |
|------|----------|------------|--------|-----|
| SSH | TCP | 22 | **My IP** | Admin access only |
| HTTP | TCP | 80 | `0.0.0.0/0`, `::/0` | Caddy ACME + redirect to HTTPS |
| HTTPS | TCP | 443 | `0.0.0.0/0`, `::/0` | API + LiveKit signaling |
| Custom UDP | UDP | 3478 | `0.0.0.0/0` | TURN |
| Custom TCP | TCP | 3478 | `0.0.0.0/0` | TURN fallback |
| Custom UDP | UDP | 50000-50100 | `0.0.0.0/0` | TURN relay allocations |

Leave outbound rules as the default (allow all).

> Do **not** open 5432 (Postgres) or 8080/7880/7881/7882. Those are internal.
> Restricting SSH (22) to **My IP** is important.

---

## Part 4 — Point your domains at the VM

You need three DNS names. The examples below assume `example.com`; substitute
your own domain.

- `api.example.com`
- `livekit.example.com`
- `turn.example.com`

### If your domain is hosted in Amazon Route 53

1. Open **Route 53 → Hosted zones**. If your domain already has a zone, open it.
   If not, click **Create hosted zone**, enter `example.com`, type `Public`, and
   create it.
2. Inside the zone, click **Create record** and add three `A` records, each
   with **Value = `<EIP>`**, **TTL = 300**:

   | Record name | Type | Value |
   |-------------|------|-------|
   | `api` | A | `<EIP>` |
   | `livekit` | A | `<EIP>` |
   | `turn` | A | `<EIP>` |

3. If you created a new hosted zone, copy its four **NS** records into your
   domain registrar so the internet can resolve it.

### If your domain is hosted elsewhere (Namecheap, Cloudflare, GoDaddy, …)

Add the same three `A` records (`api`, `livekit`, `turn`) pointing to `<EIP>`
in that provider's DNS panel. **Turn off Cloudflare's orange-cloud proxy** for
these records (the proxy does not pass WebRTC/TURN traffic).

Check propagation from your laptop:

```bash
dig +short api.example.com livekit.example.com turn.example.com
# each should print <EIP>
```

---

## Part 5 — Connect and install Docker

From your laptop (replace the key path and `<EIP>`):

```bash
chmod 400 ~/Downloads/hambin.pem
ssh -i ~/Downloads/hambin.pem ubuntu@<EIP>
```

On the server, install Docker. If you have this repository on the machine, run
its helper; otherwise use the official one-liner:

```bash
# Option A: the script in this repository
bash /path/to/Hambin/install-docker.sh

# Option B: Docker's official convenience script
curl -fsSL https://get.docker.com | sudo sh
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
```

Log out and back in so the `docker` group applies:

```bash
exit
ssh -i ~/Downloads/hambin.pem ubuntu@<EIP>
docker info
```

---

## Part 6 — Get the code and configure secrets

```bash
git clone https://github.com/khebrati/Hambin.git
cd Hambin/backend
cp .env.example .env
chmod 600 .env
```

Generate three strong secrets and keep them handy:

```bash
openssl rand -hex 32   # -> POSTGRES_PASSWORD
openssl rand -hex 32   # -> HAM_ACCESS_TOKEN_SECRET
openssl rand -hex 16   # -> LIVEKIT_API_KEY
openssl rand -hex 32   # -> LIVEKIT_API_SECRET
```

Edit `.env`:

```bash
nano .env
```

Set at least these values (leave everything else at its default):

```dotenv
HAM_APP_ENV=production

POSTGRES_PASSWORD=<the 32-byte hex from above>
HAM_DATABASE_URL=postgres://hambin:<same password>@postgres:5432/hambin?sslmode=disable

HAM_ACCESS_TOKEN_SECRET=<the 32-byte hex from above>

HAM_LIVEKIT_URL=wss://livekit.example.com
LIVEKIT_API_KEY=<the 16-byte hex from above>
LIVEKIT_API_SECRET=<the 32-byte hex from above>

HAM_DOMAIN=api.example.com
HAM_LIVEKIT_DOMAIN=livekit.example.com
HAM_TURN_DOMAIN=turn.example.com
```

Save (`Ctrl+O`, Enter) and exit (`Ctrl+X`).

> The API and the LiveKit server read the same `LIVEKIT_API_KEY` /
> `LIVEKIT_API_SECRET`, so tokens and webhooks always match. Leave both blank
> if you want to run without voice.

---

## Part 7 — Start the stack

```bash
docker compose up -d --build
```

Watch it come up:

```bash
docker compose ps
docker compose logs -f caddy
```

Caddy will request Let's Encrypt certificates for `api.example.com` and
`livekit.example.com` on first start. This needs ports 80/443 open and DNS
already pointing at `<EIP>`. Look for `certificate obtained successfully` in
the logs, then `Ctrl+C` to stop following.

---

## Part 8 — Verify

Health check over HTTPS:

```bash
curl https://api.example.com/healthz
# {"status":"ok"}
```

Create a guest, then a room (copy the `accessToken` from the first response):

```bash
TOKEN=$(curl -s https://api.example.com/v1/guest-sessions \
  -H 'Content-Type: application/json' \
  -d '{"name":"Mira","avatar":"COMET","language":"en"}' | \
  python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')

curl -s https://api.example.com/v1/rooms \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Friday night"}'
```

The second response contains a `room.code` (eight characters). Preview it:

```bash
curl -s https://api.example.com/v1/rooms/<CODE>/preview \
  -H "Authorization: Bearer $TOKEN"
```

Voice: the app calls `POST /v1/rooms/{roomId}/voice/session-token` and, if voice
is configured, receives a LiveKit `token` and `url`. If you get
`503 VOICE_DISABLED`, the LiveKit variables in `.env` are blank or the API was
not recreated after you edited them.

Confirm LiveKit sees a public IP:

```bash
docker compose logs livekit | grep -i "external\|node"
```

---

## Updating the deployment

```bash
cd ~/Hambin
git pull
cd backend
docker compose up -d --build
```

The API runs database migrations automatically at startup, so no manual
migration step is needed.

---

## Useful commands

```bash
docker compose ps                 # status
docker compose logs -f api        # API logs
docker compose logs -f livekit    # LiveKit logs
docker compose restart api        # restart one service
docker compose down               # stop (keeps data volume)
docker compose down -v            # stop and DELETE Postgres data
```

Postgres data lives in the `hambin_postgres_data` Docker volume. It survives
`down` and VM reboots, but not `down -v`.

---

## Troubleshooting

- **Caddy cannot get a certificate.** DNS has not propagated, or 80/443 are
  closed. Verify `dig +short api.example.com` returns `<EIP>`, then
  `docker compose restart caddy`.
- **Voice connects but you hear nothing.** TURN ports are likely blocked.
  Confirm 3478/udp, 3478/tcp, and 50000-50100/udp are open in the security
  group, and that `turn.example.com` resolves to `<EIP>`.
- **`permission denied` running docker.** Re-login after `usermod -aG docker`,
  or prefix commands with `sudo`.
- **API restarts in a loop.** Check `docker compose logs api`; the usual cause
  is a missing `HAM_ACCESS_TOKEN_SECRET` or a bad `HAM_DATABASE_URL`.

---

## Deferred hardening (do these later)

This pass keeps the stack minimal. Recommended next steps, in order:

1. **Backups.** A nightly `pg_dump` copied off the VM (for example to S3).
2. **TURN over TLS (5349).** Add a certificate to LiveKit's `turn` block if you
   expect clients on networks that block UDP.
3. **Monitoring.** LiveKit exposes Prometheus metrics; add the optional
   Prometheus/Grafana services behind a Compose profile.
4. **Separate media VM.** Once you approach ~100 concurrent rooms, move
   LiveKit/TURN to a dedicated VM (see the scaling stages in
   `backend-architecture.md`).

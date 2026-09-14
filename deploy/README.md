# Handball Support — Cloudflare Tunnel Deployment

No inbound router port forwarding or DynDNS required. Traffic flows through a
Cloudflare Tunnel (cloudflared container) to the internal Docker network.

## Architecture

```
Internet → Cloudflare Edge → Tunnel → cloudflared container
  → Caddy (internal reverse proxy, no published ports)
    → /api/*  → handball-backend:8090
    → /*      → coaching-web:8080
```

## Prerequisites

1. Domain managed by Cloudflare (nameservers set to Cloudflare).
2. `cloudflared` installed on the host for initial tunnel setup.
3. Docker images built (see below).

## First-time setup

### 1. Authenticate cloudflared

```bash
cloudflared tunnel login
```

Browser opens → select your zone → authorize. Creates `~/.cloudflared/cert.pem`.

### 2. Create the tunnel

```bash
cloudflared tunnel create handball
```

Output includes the tunnel UUID and a credentials file path. The tunnel token
is shown by:

```bash
cloudflared tunnel token handball
```

### 3. Configure DNS

Create a CNAME for the hostname pointing to the tunnel:

```bash
cloudflared tunnel route dns handball referee-coaching.schaltstelle.org
```

This replaces any existing DNS record for that hostname. Other records
(root A, MX, SPF, DKIM, DMARC) are untouched.

### 4. Set the tunnel token

```bash
cd deploy
cp .env.example .env
# Edit .env and set CLOUDFLARED_TUNNEL_TOKEN to the token from step 2
```

### 5. Build Docker images

```bash
# Build the coaching web image
cp dist/web/index.html composeApp/docker/
cp -r dist/web/* composeApp/docker/  # wasm + JS assets
docker build -f composeApp/docker/Dockerfile.coaching -t handball-coaching-web:local composeApp/docker/

# Build the backend image
cd server
./gradlew :server:buildFatJar
cp build/libs/server-all.jar ../server-all.jar
docker build -t handball-backend:local .
cd ..
```

### 6. Prepare data files

```bash
mkdir -p deploy/data
cp server/data/auth-users.json deploy/data/
cp server/data/tournaments.json deploy/data/
cp server/data/performance-evaluations.json deploy/data/
cp server/data/coaching-evaluations.sqlite deploy/data/
```

### 7. Start the stack

```bash
cd deploy
docker compose up -d
```

### 8. Verify

```bash
curl -s https://referee-coaching.schaltstelle.org/health
# Should return: {"status":"UP"}
```

## Adding another subdomain

To route a new Docker service through the same tunnel:

1. Add a Caddy block in `Caddyfile`:
   ```
   new-service.schaltstelle.org {
       reverse_proxy new-service:PORT
   }
   ```

2. Add the service to `docker-compose.yml` on the `handball-net` network.

3. Create the DNS route:
   ```bash
   cloudflared tunnel route dns handball new-service.schaltstelle.org
   ```

4. Restart Caddy:
   ```bash
   docker compose restart caddy
   ```

No changes to the cloudflared container — it routes all hostnames configured
in the Cloudflare dashboard for the same tunnel.

## Notes

- The tunnel token in `.env` must never be committed. `.gitignore` already
  excludes `.env`.
- Application authentication (JWT) is separate from the tunnel. The tunnel
  provides transport only.
- Caddy runs without published ports. Only cloudflared makes outbound
  connections to Cloudflare's edge.
- FritzBox port forwarding for TCP 80/443 is no longer needed and can be
  removed if no other service uses it.
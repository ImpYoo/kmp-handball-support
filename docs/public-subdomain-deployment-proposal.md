# Proposal: Public subdomains for KMP Handball Support behind FritzBox DynDNS

## Goal

Use your own TLD (e.g. `mydomain.tld`) to access the KMP Handball Support app
as two isolated public services:

| Subdomain | Variant | What's inside |
|---|---|---|
| `referee-coaching.mydomain.tld` | coaching | Schiedsrichter-Coaching, Notizblock, Taktiktafel (no backend needed) |
| `referee-voting.mydomain.tld` | rating + scoreboard | Spielbewertung plus time/scorekeeper modules (needs Ktor backend) |

The FritzBox only exposes a single dynamic public IP via DynDNS
(`myhost.dyndns.tld`). Your TLD hoster controls the real subdomains and points
them to that IP. A reverse proxy on the home server routes by `Host` header to
the right Docker container.

## 1. DNS at your TLD hoster

Your DynDNS provider cannot create subdomains, so your own TLD hoster does the
job. Create two records:

```
referee-coaching.mydomain.tld  A  <public IP of FritzBox>
referee-voting.mydomain.tld    A  <public IP of FritzBox>
```

Or use CNAMEs pointing at the DynDNS host:

```
referee-coaching.mydomain.tld  CNAME  myhost.dyndns.tld
referee-voting.mydomain.tld    CNAME  myhost.dyndns.tld
```

When the FritzBox IP changes, `myhost.dyndns.tld` updates automatically. If you
use CNAMEs, the subdomains follow that change. If you use A records, you must
update them manually or via an API when the IP changes.

## 2. FritzBox port forwarding

Forward TCP 80 and 443 from the FritzBox public IP to the internal server that
runs Docker. A dedicated machine is recommended, not your daily desktop.

```
FritzBox public IP :80  →  192.168.178.143:80
FritzBox public IP :443 →  192.168.178.143:443
```

## 3. Docker Compose layout

```text
┌─────────────────────────────────────────────────────────────────┐
│                         Docker host                             │
│  ┌──────────────┐                                               │
│  │    Caddy     │  listens on 80/443, routes by Host header      │
│  └──────┬───────┘                                               │
│         │                                                       │
│    ┌────┴────┬────────────┐                                     │
│    ▼         ▼            ▼                                     │
│ ┌────────┐ ┌──────────┐ ┌──────────────┐                      │
│ │coaching│ │  rating  │ │  handball    │                      │
│ │ -web   │ │   -web   │ │   -backend   │                      │
│ │ :8082  │ │  :8083   │ │    :8090     │                      │
│ └────────┘ └──────────┘ └──────────────┘                      │
│                                                   ┌──────────┐  │
│                                                   │  SQLite  │  │
│                                                   │ /data/db │  │
│                                                   └──────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## 4. File layout on the server

```
~/handball-deploy/
├── docker-compose.yml
├── Caddyfile
├── .env
└── data/
    ├── db/                 # SQLite or Postgres files
    ├── tournaments.json    # tournament/season IDs
    └── config/
```

## 5. docker-compose.yml

```yaml
version: "3.8"

services:
  caddy:
    image: caddy:2-alpine
    container_name: handball-caddy
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy_data:/data
      - caddy_config:/config
    networks:
      - handball-net

  coaching-web:
    image: ghcr.io/impyoo/handball-coaching-web:latest
    container_name: handball-coaching-web
    restart: unless-stopped
    expose:
      - "8080"
    networks:
      - handball-net

  rating-web:
    image: ghcr.io/impyoo/handball-rating-web:latest
    container_name: handball-rating-web
    restart: unless-stopped
    expose:
      - "8080"
    environment:
      # The production web build needs to know where the backend lives.
      # This is injected into index.html at container startup.
      - API_BASE_URL=https://referee-voting.mydomain.tld/api
    networks:
      - handball-net

  handball-backend:
    image: ghcr.io/impyoo/handball-backend:latest
    container_name: handball-backend
    restart: unless-stopped
    expose:
      - "8090"
    env_file:
      - .env
    volumes:
      - ./data:/app/data
    networks:
      - handball-net

networks:
  handball-net:
    driver: bridge

volumes:
  caddy_data:
  caddy_config:
```

## 6. Caddyfile

```caddy
{
    auto_https off
}

referee-coaching.mydomain.tld {
    reverse_proxy coaching-web:8080
    encode gzip
    header {
        X-Frame-Options DENY
        X-Content-Type-Options nosniff
        Referrer-Policy strict-origin-when-cross-origin
    }
}

referee-voting.mydomain.tld {
    # Static SPA
    reverse_proxy / rating-web:8080

    # API + health go to the Ktor backend
    reverse_proxy /api/* handball-backend:8090
    reverse_proxy /health handball-backend:8090

    encode gzip
    header {
        X-Frame-Options DENY
        X-Content-Type-Options nosniff
        Referrer-Policy strict-origin-when-cross-origin
    }
}
```

Caddy requests Let's Encrypt certificates automatically. The domains must be
publicly reachable on port 80/443.

## 7. Container images for the KMP frontends

Each frontend variant is a static web build served by a tiny HTTP server. The
builds are produced locally and then baked into images.

### Coaching image

```dockerfile
# composeApp/docker/Dockerfile.coaching
FROM oven/bun:1-alpine AS runner
WORKDIR /app
COPY build/coaching/ .
EXPOSE 8080
CMD ["bun", "run", "serve.js"]
```

`serve.js` is a minimal SPA server that falls back to `index.html`:

```javascript
import { serve } from "bun";

serve({
  port: 8080,
  fetch(req) {
    const url = new URL(req.url);
    let path = `./${url.pathname}`;
    const file = Bun.file(path);
    if (await file.exists()) return new Response(file);
    return new Response(Bun.file("./index.html"));
  },
});
```

Build script:

```shell
# Build coaching variant
./gradlew :composeApp:wasmJsBrowserProductionWebpack -PappVariant=coaching

# Collect static files
mkdir -p composeApp/docker/build/coaching
cp composeApp/build/kotlin-webpack/wasmJs/productionExecutable/* composeApp/docker/build/coaching/
cp -r composeApp/build/generated/compose/resourceGenerator/assembledResources/wasmJsMain/composeResources composeApp/docker/build/coaching/
cp composeApp/docker/index-coaching.html composeApp/docker/build/coaching/index.html
cp composeApp/docker/serve.js composeApp/docker/build/coaching/

# Build and push image
docker build -t ghcr.io/impyoo/handball-coaching-web:latest -f composeApp/docker/Dockerfile.coaching composeApp/docker/build/coaching
docker push ghcr.io/impyoo/handball-coaching-web:latest
```

The coaching `index.html` needs no API base URL:

```html
<!DOCTYPE html>
<html lang="de">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="api-base-url" content="">
  <title>Handball Schiedsrichter-Coaching</title>
</head>
<body>
  <div id="root"></div>
  <script src="composeApp.js"></script>
</body>
</html>
```

### Rating image

```dockerfile
# composeApp/docker/Dockerfile.rating
FROM oven/bun:1-alpine AS runner
WORKDIR /app
COPY build/rating/ .
EXPOSE 8080
CMD ["bun", "run", "serve.js"]
```

Build script:

```shell
./gradlew :composeApp:wasmJsBrowserProductionWebpack -PappVariant=rating

mkdir -p composeApp/docker/build/rating
cp composeApp/build/kotlin-webpack/wasmJs/productionExecutable/* composeApp/docker/build/rating/
cp -r composeApp/build/generated/compose/resourceGenerator/assembledResources/wasmJsMain/composeResources composeApp/docker/build/rating/
cp composeApp/docker/index-rating.html composeApp/docker/build/rating/index.html
cp composeApp/docker/serve.js composeApp/docker/build/rating/

docker build -t ghcr.io/impyoo/handball-rating-web:latest -f composeApp/docker/Dockerfile.rating composeApp/docker/build/rating
docker push ghcr.io/impyoo/handball-rating-web:latest
```

The rating `index.html` points at the public backend subdomain:

```html
<meta name="api-base-url" content="https://referee-voting.mydomain.tld">
```

### Backend image

```dockerfile
# server/Dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/server-all.jar app.jar
COPY data/ /app/data/
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build script:

```shell
./gradlew :server:buildFatJar
cp server/build/libs/server-all.jar server/build/docker/
cp -r server/data server/build/docker/

docker build -t ghcr.io/impyoo/handball-backend:latest -f server/Dockerfile server/build/docker
docker push ghcr.io/impyoo/handball-backend:latest
```

## 8. Backend environment (.env)

```env
SERVER_PORT=8090
APP_DEVELOPMENT=false
JWT_SECRET=[REDACTED]
JWT_ISSUER=[REDACTED]
JWT_AUDIENCE=[REDACTED]
CORS_ALLOWED_ORIGINS=https://referee-voting.mydomain.tld,https://referee-coaching.mydomain.tld
EXTERNAL_API_ENABLED=true
EXTERNAL_API_KEY=[REDACTED]
EXTERNAL_API_BASE_URL=https://api.sportradar.com/
EXTERNAL_API_TOURNAMENTS_FILE=/app/data/tournaments.json
AUTH_USERS_FILE=/app/data/auth-users.json
PERFORMANCE_EVALUATIONS_FILE=/app/data/evaluations.json
```

Note: `CORS_ALLOWED_ORIGINS` must list `https://referee-voting.mydomain.tld` so
the browser allows the rating frontend to call the backend.

## 9. Security hardening

Public exposure of a home server is not zero-risk. Do at least this:

1. Run the Docker host as a dedicated machine/VM, not your main desktop.
2. Keep only ports 80/443 forwarded on the FritzBox. Close everything else.
3. Add fail2ban or CrowdSec on the Docker host.
4. Keep the OS and Docker images patched.
5. Pin image digests in `docker-compose.yml` to avoid surprise updates.
6. Use strong JWT secrets and rotate them periodically.
7. Enable server-side rate limiting on the Ktor backend for login/vote endpoints.
8. Add HTTP Basic Auth or Cloudflare Access in front of admin routes if any.
9. Back up `./data` nightly (sqlite/json files).
10. Monitor logs with `docker logs` or a simple forwarder.

If you only want trusted users (referees, coaches) to access the services, the
Tailscale subdomain approach is much safer than public DynDNS. See
`docs/public-deployment-checklist.md` for a comparison.

## 10. Alternative: Cloudflare Tunnel on a VPS

If exposing your home IP makes you uncomfortable, host Caddy + containers on a
small VPS and create two Cloudflare Tunnels from the VPS to your home server.
This hides the home IP and gives you Cloudflare DDoS protection + access rules.
But it costs a VPS and adds tunnel complexity.

## 11. Deployment steps (summary)

1. Create DNS records at your TLD hoster.
2. Forward ports 80/443 on the FritzBox to the Docker host.
3. Copy `docker-compose.yml`, `Caddyfile`, `.env`, and `data/` to the host.
4. Build and push the three Docker images.
5. Pull images on the host: `docker compose pull`.
6. Start: `docker compose up -d`.
7. Verify Let's Encrypt: `docker logs handball-caddy`.
8. Open `https://referee-coaching.mydomain.tld` and
   `https://referee-voting.mydomain.tld`.

## Open questions

- Which container registry will you use? (GitHub Container Registry, Docker Hub,
  self-hosted?)
- Do you want to build images locally on the homeserver or build in CI and pull
  them?
- Is the homeserver the Apple Silicon Mac, or do you have a separate Linux host?
  ARM images need ARM runners or cross-build; x86 VPS needs x86 images.
- Do you want to keep `referee-voting` as only Spielbewertung, or include
  scorekeeper/time modules too? That changes which `AppVariant` to build.

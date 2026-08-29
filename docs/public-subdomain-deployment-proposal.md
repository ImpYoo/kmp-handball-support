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
│  │    Caddy     │  listens on 80/443, routes by Host header    │
│  └──────┬───────┘                                               │
│         │                                                       │
│    ┌────┴────┬────────────┐                                     │
│    ▼         ▼            ▼                                     │
│ ┌────────┐ ┌──────────┐ ┌──────────────┐                        │
│ │coaching│ │  rating  │ │  handball    │                        │
│ │ -web   │ │   -web   │ │   -backend   │                        │
│ │ :8080  │ │  :8080   │ │    :8090     │                        │
│ └────────┘ └──────────┘ └──────────────┘                        │
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
    ├── db/                 # SQLite files if used
    ├── tournaments.json    # tournament/season IDs
    ├── auth-users.json     # login users (backend)
    └── evaluations.json    # persisted evaluations (backend)
```

The `deploy/` folder in this repo already contains the compose file and
Caddyfile. Copy them to the Docker host, adjust domain names and secrets, then
run.

## 5. Local-only image build (no registry)

Build the images directly on the Docker host. No container registry, no push,
no pull.

Run from the repo root on the Docker host:

```shell
./scripts/build-images-local.sh
```

This builds three local images:

- `handball-coaching-web:local`
- `handball-rating-web:local`
- `handball-backend:local`

Then start the stack:

```shell
cd deploy
docker compose up -d
```

If you later want CI or a separate build machine, use
`scripts/build-and-push-images.sh` with a container registry instead.

## 6. Caddyfile

`deploy/Caddyfile` (replace `mydomain.tld` with your real domain):

```caddy
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
    # API + health go to the Ktor backend first.
    reverse_proxy /api/* handball-backend:8090
    reverse_proxy /health handball-backend:8090

    # Everything else is the rating SPA.
    reverse_proxy /* rating-web:8080

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
COPY . .
EXPOSE 8080
CMD ["bun", "run", "serve.js"]
```

`serve.js` is a minimal SPA server that falls back to `index.html`:

```javascript
import { serve } from "bun";

serve({
  port: 8080,
  async fetch(req) {
    const url = new URL(req.url);
    const path = url.pathname === "/" ? "./index.html" : `.${url.pathname}`;
    const file = Bun.file(path);
    if (await file.exists()) return new Response(file);
    return new Response(Bun.file("./index.html"));
  },
});
```

The coaching `index.html` needs no API base URL:

```html
<meta name="api-base-url" content="">
```

### Rating image

```dockerfile
# composeApp/docker/Dockerfile.rating
FROM oven/bun:1-alpine AS runner
WORKDIR /app
COPY . .
EXPOSE 8080
CMD ["bun", "run", "serve.js"]
```

The rating `index.html` points at the public backend subdomain:

```html
<meta name="api-base-url" content="https://referee-voting.mydomain.tld">
```

Currently this URL is a placeholder in the template
(`composeApp/docker/index-rating.html`) and must be replaced with your real
domain before building. The simplest way is to modify the template or generate
`index.html` in `build-images-local.sh`.

### Backend image

```dockerfile
# server/Dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY server-all.jar app.jar
COPY data/ /app/data/
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 8. Backend environment (.env)

`deploy/.env.example`:

```env
SERVER_PORT=8090
APP_DEVELOPMENT=false
JWT_SECRET=change-me
JWT_ISSUER=handball-support
JWT_AUDIENCE=handball-users
CORS_ALLOWED_ORIGINS=https://referee-voting.mydomain.tld
EXTERNAL_API_ENABLED=true
EXTERNAL_API_KEY=change-me
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
5. Pin base image digests in Dockerfiles to avoid surprise updates.
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

1. Create DNS records at your TLD hoster (A or CNAME to FritzBox IP/DynDNS).
2. Forward ports 80/443 on the FritzBox to the Docker host.
3. On the Docker host:
   - Clone the repo.
   - Copy `deploy/` contents to a runtime directory.
   - Fill `.env` and put data files into `./data/`.
   - Run `./scripts/build-images-local.sh`.
   - Run `cd deploy && docker compose up -d`.
4. Verify Let's Encrypt: `docker logs handball-caddy`.
5. Open `https://referee-coaching.mydomain.tld` and
   `https://referee-voting.mydomain.tld`.

## Open questions

- Is the Docker host the Apple Silicon Mac, or a separate Linux machine?
  ARM images need ARM runners; x86 host needs x86 builds.
- Do you want to keep `referee-voting` as only Spielbewertung, or include
  scorekeeper/time modules too? That changes which `AppVariant` to build.
- Where will `tournaments.json`, `auth-users.json`, and evaluations live? The
  backend currently uses JSON files inside `./data/`.

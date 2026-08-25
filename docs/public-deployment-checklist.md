# Public Deployment Checklist

This service is close to public exposure readiness for a single-node deployment, but the deployment still has to enforce a few non-code controls.

## Required before public exposure

- Terminate TLS at a reverse proxy or ingress. Do not expose plain HTTP publicly.
- Set a strong `JWT_SECRET` through real secret management, not through committed files.
- Set `CORS_ALLOWED_ORIGINS` explicitly to the browser origins that may call the API.
- Keep `APP_DEVELOPMENT=false` in public environments.
- Provide a bootstrap admin password only for the first secure startup, then rotate or remove it.
- Persist `server/data/auth-users.json` and `server/data/performance-evaluations.json` on durable volumes if JSON persistence is still used.
- Back up both JSON files and test restore procedures.

## Client API base URL injection

The Compose Multiplatform client reads its API base URL per target:

- **Web**: `<meta name="api-base-url" content="https://api.example.com">` in `index.html` (set at deploy time). Falls back to the page origin when empty.
- **Android**: Gradle property `apiBaseUrl` or env `API_BASE_URL` baked into `BuildConfig.API_BASE_URL`. Defaults to `http://10.0.2.2:8090` for the emulator (current local setup uses port 8090; port 8080 is occupied by the unrelated `skyjo-backend`).
- **iOS**: `Info.plist` key `ApiBaseUrl`, populated from `Configuration/Config.xcconfig`.
- **Desktop JVM**: env `API_BASE_URL` or system property `api.base.url`. Default fallback is `http://localhost:8090` for this project.

Verify in release builds that `localhost` is **not** the value being served.

## Client-side authentication

- Tokens are persisted to browser `localStorage` on web. Android/iOS/JVM currently use an in-memory placeholder; replace `TokenStorage.<target>.kt` with Keystore / Keychain / OS-keyring backed implementations before shipping those builds.
- The client clears its persisted token and routes the user back to login on any HTTP 401 response.

## Current architecture caveats

- Login throttling is in-memory, so it only protects a single node.
- JWT revocation is enforced through user-level token cutoffs stored in the auth JSON file.
- Logging is local application logging, not a centralized audit pipeline.
- JSON persistence is acceptable for initial deployments but should be replaced by a database in the planned Docker Compose evolution.

## Recommended reverse-proxy responsibilities

- TLS certificates and HTTPS redirects
- request size limits
- request timeout enforcement
- access logging
- IP-based rate limiting in front of the app

## Recommended next infrastructure steps

1. Move auth users and evaluations to a database inside Docker Compose (SQLite is a pragmatic first step; PostgreSQL for multi-node).
2. Put the app behind Traefik, Nginx, or another reverse proxy with TLS.
3. Store `JWT_SECRET` in Docker secrets or another secret manager.
4. If only the "Spielbewertung" module is needed on the VPS, build a stripped Compose app with only the voting routes and hide/disable the coaching modules.
5. Add automated backups for SQLite/JSON persistence and test restore procedures.
6. Ship audit logs to a central sink.
7. Add monitoring and alerting for failed logins, 5xx errors, and startup failures.

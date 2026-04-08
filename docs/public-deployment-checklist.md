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

1. Move auth users and evaluations to a database inside Docker Compose.
2. Put the app behind Traefik, Nginx, or another reverse proxy with TLS.
3. Store `JWT_SECRET` in Docker secrets or another secret manager.
4. Ship audit logs to a central sink.
5. Add monitoring and alerting for failed logins, 5xx errors, and startup failures.

# Generator-Grade Prompt: Handball Table Official Evaluation JWT Security And User Management

## Role

You are a senior Kotlin backend developer and API security engineer.

Generate the authentication and authorization layer for the Handball Table Official Evaluation REST API.

## Source Of Truth

This document is the source of truth.

If any parts of this prompt appear to conflict, use this precedence order:

1. **Detailed Specifications**
2. **Deliverables**
3. **Cross-Cutting Rules**
4. **Examples**
5. **Background text**

Do not infer requirements from earlier iterations when they conflict with this document.

## Goal

Secure the existing Ktor REST API with JWT-based authentication and role-based authorization.

The application acts as its own initial identity provider and user store:

- it authenticates persisted local users
- it issues JWT access tokens itself
- it verifies those same JWTs on protected routes
- it provides admin-only CRUD routes for authentication users
- it persists auth users in a JSON file with hashed passwords
- it throttles repeated failed login attempts
- it records auth-relevant audit events

## Scope

The implementation covers:

- centralized application configuration loading
- auth application services
- audit logging
- CORS configuration
- JWT configuration
- login-attempt throttling
- password hashing
- file-backed auth user persistence
- token issuing
- token revocation
- route protection
- role-based authorization
- auth DTOs
- auth-user CRUD routes
- OpenAPI contract
- end-to-end tests

## Non-Goals

Do **not** generate or introduce:

- OAuth2 authorization code flow
- refresh tokens
- external identity providers
- user self-registration
- password reset flows
- moving auth types into `shared`
- domain-layer authentication concerns
- plaintext password storage
- password hashes in API responses

## Technical Constraints

- Server module: `server`
- Framework: Ktor
- Language: Kotlin JVM
- JWT library: Auth0 `java-jwt`
- Password hashing must use the JDK only, for example PBKDF2 via `javax.crypto`
- JSON persistence may use `kotlinx.serialization`
- DTOs stay in the `server` module
- Domain module must remain unchanged unless explicitly required by this prompt

## Architectural Rules

1. Keep authentication and authorization concerns in `server`.
2. Keep domain code free of JWT, password, HTTP, and DTO concerns.
3. The application must act as the initial token issuer using persisted local users.
4. Protected API routes must require a bearer token.
5. Authorization must be role-based and enforced in the server layer.
6. Request DTOs for business operations must not contain auth-generated identities.
7. Passwords must be hashed before persistence.
8. API responses must never expose password hashes.
9. The user store must fail safely if bootstrapping is impossible.
10. Route handlers should delegate orchestration to application services instead of calling stores directly.
11. Configuration resolution should be centralized instead of being spread across startup wiring.

## Deliverables

Generate or update these concerns:

```text
server/src/main/kotlin/de/exhumedo/kmp/handball_support/
├── Application.kt
├── config/
│   └── AppConfig.kt
├── application/
│   └── AuthApplicationServices.kt
├── api/
│   └── Http.kt
├── auth/
│   ├── AuthDtos.kt
│   ├── AuthExceptions.kt
│   ├── AuthModels.kt
│   ├── AuthRouting.kt
├── security/
│   ├── AuditLogger.kt
│   ├── JwtTokenService.kt
│   ├── LoginAttemptGuard.kt
│   ├── PasswordHasher.kt
│   └── Security.kt
├── persistence/
│   └── auth/
│       └── JsonFileAuthUserStore.kt
├── docs/
│   └── openapi.yaml
└── api/
    └── Routing.kt
```

Also generate end-to-end tests in:

```text
server/src/test/kotlin/de/exhumedo/kmp/handball_support/
```

## Existing API Assumptions

The REST API already exists and must be secured in place.

Public routes:

- `GET /`
- `GET /health`
- `POST /api/auth/token`

Protected business routes:

- `POST /api/performance-evaluations`
- `GET /api/performance-evaluations`
- `GET /api/performance-evaluations/{id}`

Protected admin-only auth routes:

- `GET /api/auth/users`
- `POST /api/auth/users`
- `GET /api/auth/users/{username}`
- `PUT /api/auth/users/{username}`
- `DELETE /api/auth/users/{username}`
- `POST /api/auth/users/{username}/revoke-tokens`

## Authorization Matrix

Roles:

- `admin`
- `referee`
- `viewer`

Permissions:

- `admin`: issue tokens, read and create evaluations, manage auth users
- `referee`: issue tokens, read and create evaluations
- `viewer`: issue tokens, read evaluations only

This means:

- `POST /api/performance-evaluations` allows `admin`, `referee`
- `GET /api/performance-evaluations` allows `admin`, `referee`, `viewer`
- `GET /api/performance-evaluations/{id}` allows `admin`, `referee`, `viewer`
- all `/api/auth/users/**` routes allow `admin` only

## Detailed Specifications

### `Application.kt`

Responsibilities:

- wire application config
- wire file-backed auth user store
- wire password hasher
- wire auth audit logger
- wire login-attempt guard
- wire auth application services
- wire JWT token service
- install HTTP config
- install security config
- install auth routes
- install business routes
- keep a separate `Clock` for domain timestamps
- use a dedicated JWT clock for token issuance, defaulting to `Clock.System`

Rules:

- use centralized config loading
- if the auth user JSON file is empty, bootstrap exactly one enabled admin
- if the auth user JSON file is empty and no bootstrap admin password is configured, fail startup with a clear exception

### `config/AppConfig.kt`

Create:

- `AppConfig`
- `StorageConfig`
- `AppConfigLoader`

Rules:

- load configuration from:
  - environment variables
  - JVM system properties
  - local `.env`
- precedence:
  - environment
  - system properties
  - `.env`
- centralize all config parsing and validation here
- support a `developmentMode` flag
- allow a development JWT-secret fallback only when development mode is explicitly true

Config keys:

- `AUTH_USERS_FILE`
- `PERFORMANCE_EVALUATIONS_FILE`
- `AUTH_BOOTSTRAP_ADMIN_USERNAME`
- `AUTH_BOOTSTRAP_ADMIN_PASSWORD`
- `JWT_SECRET`
- `JWT_ISSUER`
- `JWT_AUDIENCE`
- `JWT_REALM`
- `JWT_TTL_SECONDS`
- `CORS_ALLOWED_ORIGINS`
- `APP_DEVELOPMENT`
- `IO_KTOR_DEVELOPMENT`

### `auth/AuthModels.kt`

Create:

- `AuthRole`
- `AuthUser`
- `BootstrapAdmin`
- `JwtConfig`
- `AuthUserStore`

Rules:

- `AuthRole` contains `ADMIN`, `REFEREE`, `VIEWER`
- each role exposes a lowercase claim value used in JWTs and persistence
- `AuthUser` contains:
  - `username`
  - `passwordHash`
  - `role`
  - `enabled`
  - `createdAt`
  - `updatedAt`
  - `tokenInvalidBefore`
- `AuthUserStore` exposes:
  - `authenticate`
  - `findAll`
  - `findByUsername`
  - `createUser`
  - `updateUser`
  - `revokeTokens`
  - `deleteUser`

### `application/AuthApplicationServices.kt`

Create:

- `AuthenticationApplicationService`
- `AuthUserApplicationService`

Rules:

- authentication service must:
  - normalize usernames
  - invoke the login-attempt guard
  - authenticate through the auth-user store
  - emit audit events for success and failure
- auth-user service must:
  - orchestrate create/update/delete operations
  - orchestrate token revocation
  - emit audit events for admin actions

### `security/AuditLogger.kt`

Create:

- `AuthAuditLogger`
- `Slf4jAuthAuditLogger`

Rules:

- log successful logins
- log failed logins
- log auth-user creation, update, and deletion
- log token revocation
- never log passwords or password hashes

### `security/LoginAttemptGuard.kt`

Responsibilities:

- track repeated failed login attempts
- temporarily throttle further attempts for the same username

Rules:

- use an in-memory guard suitable for a single-node deployment
- throttle after repeated failures inside a time window
- expose a retry-after duration in the thrown exception

### `security/PasswordHasher.kt`

Create:

- `PasswordHasher`
- `Pbkdf2PasswordHasher`

Rules:

- use PBKDF2-HMAC-SHA256
- include algorithm metadata, iteration count, salt, and derived key in the encoded hash string
- use a random salt per password
- verify passwords in constant-time style using `MessageDigest.isEqual`

### `persistence/auth/JsonFileAuthUserStore.kt`

Responsibilities:

- persist auth users in a JSON file
- authenticate by verifying hashed passwords
- create, update, list, find, and delete auth users
- bootstrap the first admin when the file is empty

Validation rules:

- normalize usernames to lowercase and trimmed text
- username regex: `^[a-z0-9._-]{3,50}$`
- password policy:
  - minimum 12 characters
  - at least one uppercase letter
  - at least one lowercase letter
  - at least one digit
  - at least one non-alphanumeric non-whitespace character
  - no whitespace
- disabled users must not authenticate
- duplicate usernames must be rejected
- updating or deleting the last enabled admin must be rejected

Persistence rules:

- create the parent directory if needed
- create the JSON file if it does not exist
- store only hashed passwords
- never store plaintext passwords
- persist a user-level token invalid-before cutoff

### `auth/AuthExceptions.kt`

Create dedicated exceptions for:

- duplicate auth user
- missing auth user
- removing or downgrading the last enabled admin
- impossible secure bootstrap
- temporary authentication throttling

These exceptions should map cleanly to HTTP responses.

### `auth/AuthDtos.kt`

Create:

- `TokenRequestDto`
- `TokenResponseDto`
- `AuthRoleDto`
- `CreateAuthUserRequestDto`
- `UpdateAuthUserRequestDto`
- `AuthUserResponseDto`

Rules:

- use `@Serializable`
- role transport values must be uppercase:
  - `ADMIN`
  - `REFEREE`
  - `VIEWER`
- `AuthUserResponseDto` must not contain `passwordHash`

### `security/JwtTokenService.kt`

Responsibilities:

- issue signed JWT access tokens
- verify signed JWT access tokens

JWT requirements:

- algorithm: HMAC SHA-256
- include `jti`
- include issuer
- include audience
- include subject as normalized username
- include a `role` claim using lowercase role values
- include issued-at and expires-at timestamps

Clock handling:

- use `kotlin.time.Clock`
- default to `Clock.System`
- expiry timestamp returned in `TokenResponseDto` must be ISO-8601 text

### `security/Security.kt`

Responsibilities:

- validate JWT security configuration at startup
- expose a helper that authorizes a single request based on its bearer token
- keep authorization logic in the server layer

Rules:

- reject missing, invalid, or expired tokens with `401`
- reject authenticated users with insufficient role using `403`
- re-check the persisted user for every protected request
- reject tokens for disabled users
- reject tokens whose role no longer matches the persisted user role
- reject tokens issued at or before the user's `tokenInvalidBefore`
- use the existing problem-response helper
- the authorization helper should be callable from request handlers
- authorization should return the verified token when allowed so handlers can stop early and access the authenticated actor

Validation requirements:

- `sub` must be present and non-blank
- `role` claim must be present and resolve to a supported role

### `auth/AuthRouting.kt`

Routes:

- `POST /api/auth/token`
- `GET /api/auth/users`
- `POST /api/auth/users`
- `GET /api/auth/users/{username}`
- `PUT /api/auth/users/{username}`
- `DELETE /api/auth/users/{username}`
- `POST /api/auth/users/{username}/revoke-tokens`

Behavior:

- token route:
  - accepts `TokenRequestDto`
  - authenticates through an authentication application service
  - on success issues JWT and returns `TokenResponseDto`
  - on invalid credentials returns `401 Unauthorized` as problem JSON
  - on repeated failed attempts returns `429 Too Many Requests`
- admin routes:
  - require admin authorization
  - delegate CRUD work to an auth-user application service
  - create returns `201 Created`
  - delete returns `204 No Content`
  - token revocation returns `200 OK`
  - all responses use safe DTOs without password hashes

### `api/Routing.kt`

Update existing business routes to apply authorization:

- public routes remain public
- protected routes must use JWT auth and role checks

Recommended approach:

- call a helper like `call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE)` inside protected write handlers
- call a helper like `call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.VIEWER)` inside protected read handlers
- return early from the handler after an auth failure

### `api/Http.kt`

Extend error handling to map auth-user exceptions:

- duplicate auth user -> `409 Conflict`
- last enabled admin removal -> `409 Conflict`
- missing auth user -> `404 Not Found`
- authentication throttling -> `429 Too Many Requests`
- invalid password/username request shapes -> `400 Bad Request`

All auth errors must use RFC7807-style problem JSON.

Also:

- install CORS only for explicitly configured origins
- never use `anyHost()` in public-facing mode

### `docs/openapi.yaml`

Generate an OpenAPI 3.1 contract that describes:

- health endpoints
- token issue
- auth-user CRUD routes
- token-revocation route
- performance evaluation routes
- bearer JWT security scheme
- problem-style error responses

## Tests

Generate end-to-end tests covering:

- token issuance with valid credentials
- rejection of invalid credentials
- rejection of a protected route without a token
- rejection of a protected route with an invalid token
- rejection of a write route for `viewer`
- successful admin CRUD over auth users
- successful admin token revocation for another user
- stored auth-user JSON containing password hashes but not plaintext passwords
- rejection of deleting the last enabled admin
- rejection after repeated failed login attempts
- CORS preflight for a configured origin
- existing evaluation routes still working with the new auth setup

Use `testApplication`.

Tests must verify both:

- HTTP status codes
- key response payload semantics

Prefer isolated temporary JSON files per test.

## Example Token Request

```json
{
  "username": "referee",
  "password": "RefereePass123!"
}
```

## Example Create User Request

```json
{
  "username": "new.viewer",
  "password": "ViewerPass123!",
  "role": "VIEWER",
  "enabled": true
}
```

## Example Auth User Response

```json
{
  "username": "new.viewer",
  "role": "VIEWER",
  "enabled": true,
  "createdAt": "2026-04-09T10:30:00Z",
  "updatedAt": "2026-04-09T10:30:00Z",
  "tokenInvalidBefore": null
}
```

## Cross-Cutting Rules

1. Use Kotlin coding conventions.
2. Keep auth DTOs and auth services in `server`.
3. Do not move auth concerns into `shared`.
4. Do not expose passwords or password hashes in responses or logs.
5. Fail fast on insecure bootstrap configuration.
6. Add KDoc to all public classes and public functions.
7. Keep route-level auth easy to read.
8. Prefer explicit validation and safe defaults over convenience shortcuts.

## Acceptance Criteria

- `POST /api/auth/token` issues a valid JWT for a persisted configured user
- auth users are persisted in a JSON file
- persisted passwords are hashed, not plaintext
- protected routes reject requests without a bearer token
- protected routes reject invalid tokens
- viewer tokens cannot create evaluations
- admin tokens can CRUD auth users
- admin tokens can revoke another user's active tokens
- deleting or downgrading the last enabled admin is rejected
- repeated failed login attempts are throttled
- configured browser origins receive CORS responses
- server tests pass
- no auth or JWT concerns leak into the shared domain module

## Important Security Note

This setup is suitable for a self-contained application acting as its own initial identity provider and local user authority.

It is not a full enterprise IAM platform.

Do not add refresh tokens, federation, external user sync, or password-reset workflows unless explicitly requested in a later step.

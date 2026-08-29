# Implementation Plan: Referee Coaching REST API + Result Presentation

## Overview

Add a Ktor REST API that stores referee-coaching evaluations, computes their
scores with the existing domain rules, and exposes result presentations.

## Architecture Decisions

See `docs/decisions/ADR-001-referee-coaching-rest-api.md` for the full ADR.
Key points:

- Reuse `shared/referee_coaching/domain` scoring logic.
- Add `RefereeCoachingEvaluation` aggregate root in `shared`.
- Persist in SQLite (user preference); repository in `server`.
- DTOs live in `server/api/dto/coaching/`.
- Reuse existing JWT auth; add `COACH` role.
- Static coaching SPA remains static unless user explicitly saves an evaluation.

## Task List

### Phase 1: Domain foundation
- [ ] Task 1: Add `RefereeCoachingEvaluation` aggregate and commands in `shared`
  - Files: `shared/.../referee_coaching/domain/model/RefereeCoachingEvaluation.kt`
  - Acceptance: builds, existing 72 shared tests still pass.
- [ ] Task 2: Add `RefereeCoachingReport` value object for computed results
  - Files: `shared/.../referee_coaching/domain/model/RefereeCoachingReport.kt`
  - Acceptance: report can be derived from an evaluation using existing scoring rules.

### Checkpoint 1
- [ ] `./gradlew :shared:jvmTest` passes.

### Phase 2: Server persistence
- [ ] Task 3: Define `CoachingEvaluationRepository` interface and SQLite schema
  - Files: `server/.../persistence/coaching/CoachingEvaluationRepository.kt`, SQL schema.
  - Acceptance: interface exposes save/find/list; schema created on first start.
- [ ] Task 4: Implement `SqliteCoachingEvaluationRepository`
  - Files: `server/.../persistence/coaching/SqliteCoachingEvaluationRepository.kt`
  - Acceptance: round-trip tests for save + find + list pass.

### Checkpoint 2
- [ ] `./gradlew :server:test --tests "*CoachingEvaluationRepository*"` passes.

### Phase 3: API layer
- [ ] Task 5: Add coaching request/response DTOs and mappers
  - Files: `server/.../api/dto/coaching/*Dtos.kt`
  - Acceptance: DTO → command → domain → response DTO round-trip in tests.
- [ ] Task 6: Add `CoachingApplicationService` (orchestrate save + report generation)
  - Files: `server/.../application/CoachingApplicationService.kt`
  - Acceptance: service creates evaluation, computes report, persists.
- [ ] Task 7: Add `CoachingRouting` with `/api/coaching/evaluations`, `/api/coaching/evaluations/:id/report`, `/api/coaching/catalog`
  - Files: `server/.../api/CoachingRouting.kt`
  - Acceptance: routing wired in `Application.kt`.

### Checkpoint 3
- [ ] `./gradlew :server:test` passes.

### Phase 4: Auth + deployment
- [ ] Task 8: Add `COACH` role and update auth bootstrap / authorization matrix
  - Files: `server/.../auth/AuthRole.kt`, `server/.../persistence/auth/JsonFileAuthUserStore.kt`, `server/.../config/AppConfig.kt` defaults.
  - Acceptance: `COACH` role recognized, can write evaluations, cannot manage users.
- [ ] Task 9: Update `.env.example` and deployment docs for SQLite path
  - Files: `.env.example`, `docs/public-subdomain-deployment-proposal.md`, `deploy/docker-compose.yml` volume mount.

### Checkpoint 4
- [ ] `./gradlew :server:test` passes.

### Phase 5: Frontend integration
- [ ] Task 10: Add a minimal coaching API client in `composeApp` (multiplatform expect/actual)
  - Files: `composeApp/.../network/CoachingApiClient.kt` + JVM/Web actuals.
  - Acceptance: web actual can POST/GET JSON from `api-base-url`.
- [ ] Task 11: Add "Bogen speichern" button in `CoachingSessionScreen` and report preview
  - Files: `composeApp/.../ui/screens/CoachingSessionScreen.kt`, new `CoachingReportScreen.kt`.
  - Acceptance: saves evaluation, displays total score + per-criterion breakdown.

### Checkpoint 5
- [ ] `./gradlew :composeApp:jvmTest :composeApp:wasmJsBrowserProductionWebpack -PappVariant=full` passes.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Domain model in `shared` needs breaking changes | Medium | Keep new aggregate additive; do not modify `Criterion` scoring contract. |
| SQLite schema migration later | Medium | Add `schema_version` table from the start. |
| Frontend SPA ↔ backend CORS issues | Low | Backend already whitelists LAN origin; add production origins in `.env`. |
| Auth role change breaks existing tests | Low | Update tests to include `COACH`; no role removal. |

## Open Questions

1. Single-match reports first, or aggregated multi-match reports immediately?
2. PDF export for v1, or web UI report only?
3. Auto-save criterion changes to backend, or explicit "Bogen speichern" button?

## Verification

Before implementation starts, confirm:

- [ ] ADR reviewed and accepted.
- [ ] Open questions answered by the user.
- [ ] Every task has acceptance criteria and verification step.
- [ ] Task dependencies are ordered correctly.
- [ ] No task touches more than ~5 files.

## See Also

- `docs/decisions/ADR-001-referee-coaching-rest-api.md`
- `docs/rest-api-prompt.md` (existing table-official API prompt, useful pattern reference)

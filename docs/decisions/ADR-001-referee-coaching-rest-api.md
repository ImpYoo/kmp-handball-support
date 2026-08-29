# ADR-001: REST API for Referee Coaching Evaluation and Result Presentation

## Status
Proposed

## Date
2026-08-29

## Context

The KMP Handball Support coaching-only build currently runs as a static web app (Schiedsrichter-Coaching, Notizblock, Taktiktafel). State is local (browser localStorage / file / in-memory). The user wants to:

1. Collect referee-coaching evaluations through a REST API.
2. Evaluate / compute results from those evaluations.
3. Present the results.

This requires a backend component for the coaching module, not only a static SPA. We need to decide:

- What is the domain model and API contract?
- How is it persisted?
- How are write/read operations authorized?
- How does evaluation flow into result presentation?
- What changes for deployment and frontend?

### Existing assets we can reuse

- `shared/src/commonMain/kotlin/de/exhumedo/kmp/handball_support/referee_coaching/domain/model/Criterion.kt` and `ScoringConfig` define the observation sheet.
- `AdjustCriterionScoreUseCase` and `CriterionScoringService` already implement the incremental point-deduction algorithm.
- `DefaultCriterionCatalogRepository` / `LoadCriteriaUseCase` provide the standard HVNB "Beobachterbogen" criteria catalog.
- The server already has JWT auth (`security/JwtTokenService`, `auth/AuthRole`, `auth/AuthRouting`) and file-backed repositories.
- `PerformanceEvaluationApiTest` and `AuthApiTest` provide the testing pattern for Ktor server tests.

### Existing problem: performance-evaluation JSON storage

The `JsonFilePerformanceEvaluationRepository` uses a single JSON file. It works for the table-official voting use case, but it already centralizes all evaluations in one mutable file and requires in-process locking. For coaching evaluations, which are written more frequently and queried by match/referee/date, a structured store is the safer default.

## Decision

Add a dedicated backend for referee-coaching evaluations with the following shape:

### 1. Domain reuse

Keep the existing `shared/referee_coaching/domain` types (`Criterion`, `DefectGroup`, `RootCause`, `ScoringConfig`) as the source of truth for scoring. Add a new aggregate root `RefereeCoachingEvaluation` in `shared` that represents one completed observation sheet for a specific match, evaluator, and referee pair.

### 2. REST resource

Resource: `/api/coaching/evaluations`

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | `/api/coaching/evaluations` | admin / referee / coach | Submit a completed coaching evaluation |
| GET | `/api/coaching/evaluations` | admin / referee / coach / viewer | List evaluations; filter by `gameId`, `refereePersonId`, `evaluatorUsername`, `from`, `to` |
| GET | `/api/coaching/evaluations/:id` | admin / referee / coach / viewer | Fetch one evaluation |
| GET | `/api/coaching/evaluations/:id/report` | admin / referee / coach / viewer | Fetch computed result presentation for one evaluation |
| GET | `/api/coaching/reports/referees/:personId` | admin / referee / coach / viewer | Aggregated report across evaluations for one referee |
| GET | `/api/coaching/catalog` | public | The read-only HVNB criteria catalog (rules/personal impression) |

### 3. DTOs

DTOs live in `server/src/main/kotlin/.../api/dto/coaching/`. Domain objects stay in `shared`. Mapping is one-way (DTO → command; domain → response DTO).

Request DTO for creation:

```json
{
  "gameId": "sportradar:12345",
  "matchDate": "2026-09-15",
  "homeTeam": "THW Kiel",
  "awayTeam": "SC Magdeburg",
  "evaluatorUsername": "observer.otto",
  "firstReferee": { "personId": "p1", "firstName": "Max", "lastName": "Mustermann" },
  "secondReferee": { "personId": "p2", "firstName": "Anna", "lastName": "Schmidt" },
  "criterionScores": [
    {
      "criterionId": "rule_1",
      "rootCauseCounts": {
        "group_1": { "rc_1": 2, "rc_2": 1 }
      }
    }
  ],
  "comment": "Strong positioning, weak timeout handling"
}
```

Response DTO adds:
- server-generated `id`
- `createdAt`
- `updatedAt`
- per-criterion `score`, `maxScore`, `deductionPoints`
- `totalScore`, `maxTotalScore`, `percentage`

### 4. Persistence

Use **SQLite** in `server/data/coaching-evaluations.sqlite`. This matches the user's stated preference ("Prefers SQLite") and avoids the single-file JSON locking issues of the existing performance-evaluation store.

Access layer:
- `server/src/main/kotlin/.../persistence/coaching/CoachingEvaluationRepository` interface.
- `SqliteCoachingEvaluationRepository` using JDBC/SQLite.
- Schema: two tables:
  - `coaching_evaluations` — metadata + summary score.
  - `coaching_criterion_counts` — one row per (evaluationId, criterionId, groupId, rootCauseId, count).

### 5. Result presentation

The report endpoints read stored counts and run the same domain scoring rules
for **one stored evaluation** to produce:

- Per-criterion score, deductions, and selected root-cause names.
- Total score and percentage vs. max.
- A short textual summary (e.g., "A · Spielregeln: 4/9 Punkte").

Aggregated reports across multiple matches are explicitly out of scope for the
first deliverable.

Report output is rendered in the web UI for v1. PDF export is a future
enhancement.

### 6. Authorization

Reuse existing JWT infrastructure. Add a new role `COACH` to `AuthRole`. Authorization matrix:

| Endpoint | Allowed roles |
|----------|---------------|
| POST /api/coaching/evaluations | ADMIN, REFEREE, COACH |
| GET /api/coaching/evaluations | ADMIN, REFEREE, COACH, VIEWER |
| GET /api/coaching/evaluations/:id | ADMIN, REFEREE, COACH, VIEWER |
| GET /api/coaching/evaluations/:id/report | ADMIN, REFEREE, COACH, VIEWER |
| GET /api/coaching/reports/referees/:personId | ADMIN, REFEREE, COACH, VIEWER |
| GET /api/coaching/catalog | public |

A user can only create evaluations under their own `evaluatorUsername` unless they are ADMIN.

### 7. Frontend changes

- Add an optional API client path for the coaching module.
- For the first publishable step the static-only build remains untouched; the API is consumed by an extended build or a later "full" variant.
- Persist every criterion change to the backend automatically **when online**.
- Keep a local fallback copy (browser localStorage / file / in-memory) so the observer can continue working offline; sync/merge when connectivity returns.

### 8. Deployment changes

The coaching-only static SPA currently needs no backend. After this change, the coaching backend becomes required for storing and presenting evaluations. The existing `deploy/docker-compose.yml` already includes a `backend` container; the coaching SPA will be configured (via `api-base-url` / meta tag) to call it. No new container is needed.

## Alternatives Considered

### A. Extend the existing performance-evaluation JSON file
- Rejected: The existing JSON file mixes two different domains (table-official ratings vs. referee coaching). It also uses in-process locking and does not scale with frequent writes or queries by referee/date.

### B. Use SQLDelight instead of raw JDBC/SQLite
- Rejected for first iteration: SQLDelight adds source-set complexity to a Kotlin JVM server that is already simple. We can migrate to SQLDelight later if the schema stabilizes. Raw JDBC with a small repository keeps the first deliverable small and testable.

### C. Store coaching evaluations directly from the browser without a backend
- Rejected: The user explicitly asked for evaluation and result presentation, which implies shared/persistent data that a browser-only localStorage solution cannot provide.

### D. A separate microservice for coaching
- Rejected: The project already has a Ktor server. Adding a second service multiplies deployment complexity for no benefit at this scale.

## Consequences

- Backend becomes required for the coaching evaluation workflow.
- SQLite schema needs migration/versioning from the start.
- Existing auth bootstrap must create a user with COACH role in addition to ADMIN/REFEREE/VIEWER.
- Frontend coaching module needs a network client for at least the save/catalog/report endpoints.
- The static-only "coaching" variant can keep localStorage for drawings/tactics; evaluation persistence moves to the API only when the user explicitly saves.
- Result presentation can be rendered as a server-produced HTML/PDF later or as a Compose screen consuming the report DTO.

## Open Questions

All answered.

1. Reports are **single-evaluation only** for v1; multi-match aggregation is out of scope.
2. Reports are rendered **in the web UI** for v1; PDF export is future.
3. The frontend **auto-saves** each criterion change to the backend while online, and keeps a local copy as offline backup.

# Generator-Grade Prompt: Handball Table Official Evaluation Domain

## Role

You are a senior Kotlin developer and Domain-Driven Design expert.

Generate the complete production-ready domain layer for a Handball Table Official Evaluation application built with Kotlin Multiplatform.

## Source Of Truth

This document is the source of truth.

If any parts of the prompt appear to conflict, use this precedence order:

1. **Detailed Specifications**
2. **Deliverables**
3. **Cross-Cutting Rules**
4. **Examples**
5. **Background text**

Do not infer requirements from earlier iterations, repository history, or generic DDD preferences when they conflict with this document.

## Goal

Generate a pure Kotlin domain model for evaluating a table official team in a handball game.

The generated code must:

- compile in `shared/src/commonMain`
- be framework-free
- enforce the domain invariants stated below
- match the package structure exactly
- be accompanied by focused common tests for the domain invariants

## Scope

The domain covers:

- people participating in a game context
- contextual official roles
- referee pair assignment
- table official team assignment
- evaluation criteria and scoring
- one completed performance evaluation
- repository port definitions

## Non-Goals

Do **not** generate or introduce:

- repository implementations
- database models
- DTOs or API contracts
- UI models
- serialization annotations
- framework-specific code
- networking code
- dependency injection setup
- draft/submitted workflow
- secretary role
- license number handling
- dynamic or database-driven evaluation criteria

## Technical Constraints

- Language: Kotlin Multiplatform
- Source set: `shared/src/commonMain`
- Test source set: `shared/src/commonTest`
- Base package: `de.exhumedo.kmp.handball_support.domain.rating`
- Allowed imports:
  - Kotlin standard library
  - sibling imports under `de.exhumedo.kmp.handball_support.domain.rating.*`
- No imports from:
  - Android SDK
  - Spring
  - Ktor
  - `kotlinx.coroutines`
  - serialization libraries
  - any third-party framework

Notes:

- `kotlin.time.Clock` is allowed
- Kotlin stdlib UUID support is allowed
- `suspend` is allowed because it is a Kotlin language feature

## Deliverables

Generate exactly these production files:

```text
shared/src/commonMain/kotlin/de/exhumedo/kmp/handball_support/domain/rating/
├── exception/
│   └── DomainException.kt
├── model/
│   ├── Person.kt
│   ├── OfficialRole.kt
│   ├── RoleAssignment.kt
│   ├── RefereePair.kt
│   ├── TableOfficialTeam.kt
│   ├── Game.kt
│   ├── PerformanceEvaluation.kt
│   ├── EvaluationScore.kt
│   └── Score.kt
└── repository/
    └── PerformanceEvaluationRepository.kt
```

Also generate focused tests in:

```text
shared/src/commonTest/kotlin/de/exhumedo/kmp/handball_support/domain/rating/model/
```

The test suite should validate the important domain invariants rather than just constructor happy paths.

## Cross-Cutting Rules

1. Every property must be immutable: use `val` only.
2. Every stated invariant must be enforced at construction time or inside the specified factory method.
3. Illegal states should be unrepresentable wherever practical.
4. Use KDoc on every public class, public property, and public function.
5. Use Kotlin idioms and 4-space indentation.
6. Use `data class` only for value objects and simple structural objects.
7. Use regular classes for identity-based entities and aggregate roots.
8. When a domain-specific exception is specified, throw that exception.
9. `IllegalArgumentException` is acceptable for generic argument validation when explicitly specified or when using `require(...)`.
10. Do not rename packages, files, or domain concepts defined in this prompt.

## Domain Overview

- A `Game` is an immutable external reference.
- A `Person` is an identity-based domain entity.
- Roles are contextual and represented through `RoleAssignment`.
- A `RefereePair` is the collective evaluator.
- A `TableOfficialTeam` is the evaluated subject.
- `EvaluationScore` stores raw criterion values.
- Weighted totals are derived through `toScore(...)` because weighting rules may evolve later.
- `PerformanceEvaluation` is the aggregate root for one completed evaluation.

## Detailed Specifications

### `exception/DomainException.kt`

Define:

```kotlin
sealed class DomainException(message: String) : Exception(message)
```

Add exactly these subclasses:

1. `InvalidScoreRange(val value: Int, val min: Int, val max: Int)`
   Message:
   `"Score value $value is out of valid range [$min, $max]"`

2. `DuplicatePersonInTeam(val personId: String)`
   Message:
   `"Person with id '$personId' is assigned to multiple roles in the same context"`

3. `InvalidRoleForPosition(val expectedRole: String, val actualRole: String)`
   Message:
   `"Expected role $expectedRole but got $actualRole"`

Do not add any obsolete exception types.

### `model/Score.kt`

Type:

- `@JvmInline value class Score(val value: Int)`

Rules:

- valid range is `1..10`
- values outside that range must throw `DomainException.InvalidScoreRange`
- override `toString()` to return the raw numeric value as text

### `model/EvaluationScore.kt`

Type:

- `data class`

Fields:

- `appearance: Score`
- `influence: Score`
- `teamwork: Score`

Function:

```kotlin
fun toScore(
    appearanceWeight: Int = 1,
    influenceWeight: Int = 1,
    teamworkWeight: Int = 1,
): Int
```

Rules:

- the three raw ratings are the persisted source values
- `toScore(...)` derives a weighted total from those stored values
- negative weights are invalid and must throw `IllegalArgumentException`
- criteria are fixed at compile time and are not configurable

### `model/OfficialRole.kt`

Type:

- sealed class

Nested subtypes:

- `FirstReferee`
- `SecondReferee`
- `TimeKeeper`
- `ScoreKeeper`
- `Delegate`

Also provide:

- `abstract val displayName: String`
- `companion object`
- `val refereeRoles: Set<OfficialRole> = setOf(FirstReferee, SecondReferee)`
- `val tableRoles: Set<OfficialRole> = setOf(TimeKeeper, ScoreKeeper, Delegate)`

### `model/Person.kt`

Type:

- regular `class`, not `data class`

Fields:

- `id: String`
- `firstName: String`
- `lastName: String`

Rules:

- all fields must be non-blank
- expose `fullName`
- equality and `hashCode()` must be based on `id` only
- provide a useful `toString()`
- do not include license number support

### `model/RoleAssignment.kt`

Type:

- `data class`

Fields:

- `person: Person`
- `role: OfficialRole`

### `model/RefereePair.kt`

Type:

- `data class`

Fields:

- `firstReferee: RoleAssignment`
- `secondReferee: RoleAssignment`

Invariants:

- `firstReferee.role` must be `OfficialRole.FirstReferee`
- `secondReferee.role` must be `OfficialRole.SecondReferee`
- both assigned persons must be distinct

Expose:

- `val persons: Set<Person>`

Throw:

- `DomainException.InvalidRoleForPosition`
- `DomainException.DuplicatePersonInTeam`

### `model/TableOfficialTeam.kt`

Type:

- `data class`

Fields:

- `timeKeeper: RoleAssignment`
- `scoreKeeper: RoleAssignment`
- `delegate: RoleAssignment? = null`

Invariants:

- `timeKeeper.role` must be `OfficialRole.TimeKeeper`
- `scoreKeeper.role` must be `OfficialRole.ScoreKeeper`
- if present, `delegate.role` must be `OfficialRole.Delegate`
- all assigned persons must be distinct

Expose:

- `val members: Set<Person>`

### `model/Game.kt`

Type:

- `data class`

Fields:

- `gameId: String`
- `date: String`
- `homeTeam: String`
- `awayTeam: String`
- `venue: String`

Rules:

- all fields must be non-blank
- `Game` is an immutable external reference, not a domain-owned aggregate
- use strings intentionally for date values

### `model/PerformanceEvaluation.kt`

Type:

- regular `class`, not `data class`

Fields:

- `id: String`
- `game: Game`
- `refereePair: RefereePair`
- `tableOfficialTeam: TableOfficialTeam`
- `score: EvaluationScore`
- `comment: String?`
- `createdAt: String`

Invariants:

- `id` must be non-blank
- `createdAt` must be non-blank
- no person may appear both in the referee pair and in the table official team

Rules:

- implement `equals()` and `hashCode()` by `id` only
- provide a useful `toString()`
- there is no draft or submitted lifecycle

Factory:

Provide a companion factory with this signature:

```kotlin
fun create(
    id: String = Uuid.random().toString(),
    game: Game,
    refereePair: RefereePair,
    tableOfficialTeam: TableOfficialTeam,
    score: EvaluationScore,
    comment: String?,
    clock: Clock = Clock.System,
): PerformanceEvaluation
```

Factory rules:

- `id` defaults to a generated Kotlin stdlib UUID
- `createdAt` must be derived inside the factory with `clock.now().toString()`
- keep the public constructor explicit with `createdAt: String`

### `repository/PerformanceEvaluationRepository.kt`

Type:

- interface

Methods:

- `suspend fun save(evaluation: PerformanceEvaluation): PerformanceEvaluation`
- `suspend fun findById(id: String): PerformanceEvaluation?`
- `suspend fun findByGameId(gameId: String): PerformanceEvaluation?`
- `suspend fun findByRefereePairPersonIds(firstRefereeId: String, secondRefereeId: String): List<PerformanceEvaluation>`
- `suspend fun existsByGameId(gameId: String): Boolean`
- `suspend fun findAll(): List<PerformanceEvaluation>`

Do not add implementation details.

## Test Expectations

Generate focused common tests that cover at least:

- invalid `Score` range
- invalid negative weights in `EvaluationScore.toScore(...)`
- `Person` blank field validation
- `Person` identity-based equality
- invalid roles in `RefereePair`
- duplicate people in `TableOfficialTeam`
- optional delegate membership behavior
- overlap rejection between referee pair and table team
- identity-based equality in `PerformanceEvaluation`
- deterministic `createdAt` generation via injected `Clock`

## Example

This example is illustrative only. If it conflicts with the formal specification above, the formal specification wins.

```kotlin
val evaluation = PerformanceEvaluation.create(
    game = Game("G-001", "2026-04-06", "THW Kiel", "SG Flensburg", "Sparkassen-Arena"),
    refereePair = RefereePair(
        RoleAssignment(Person("R1", "Max", "Mueller"), OfficialRole.FirstReferee),
        RoleAssignment(Person("R2", "Anna", "Schmidt"), OfficialRole.SecondReferee),
    ),
    tableOfficialTeam = TableOfficialTeam(
        RoleAssignment(Person("T1", "Jan", "Weber"), OfficialRole.TimeKeeper),
        RoleAssignment(Person("T2", "Lisa", "Koch"), OfficialRole.ScoreKeeper),
    ),
    score = EvaluationScore(
        appearance = Score(8),
        influence = Score(7),
        teamwork = Score(9),
    ),
    comment = "Solid performance",
)
```

## Acceptance Criteria

The result is acceptable only if all of the following are true:

- code is generated in the exact package and file structure specified above
- no forbidden dependencies are imported
- all required invariants are enforced
- obsolete concepts are not reintroduced
- the shared module compiles
- the shared domain te sts pass

## Verification

The generated code should satisfy:

```bash
./gradlew :shared:jvmTest
```

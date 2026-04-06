# Handball Table Official Evaluation Domain Prompt

## Objective

Generate the complete production-ready domain layer for a Handball Table Official Evaluation application in Kotlin Multiplatform.

The generated code must match the target model described in this document exactly.

## Priority Order

If any parts of this prompt appear to conflict, use this order of precedence:

1. File-by-file specifications in **Detailed Specifications**
2. **Cross-Cutting Rules**
3. **Package Structure**
4. **Expected Usage**
5. Introductory/background text

Do not infer requirements from older code or earlier iterations. This document is the source of truth.

## Technical Constraints

- Code lives in `shared/src/commonMain`
- Base package is `de.exhumedo.kmp.handball_support.domain.rating`
- Pure Kotlin only
- No framework dependencies
- Allowed imports:
  - Kotlin standard library
  - sibling imports inside `de.exhumedo.kmp.handball_support.domain.rating.*`
- No Android APIs
- No Spring, Ktor, or serialization libraries
- No `kotlinx.coroutines` imports
- Immutable properties only: `val`

## What To Generate

Generate complete, compilable Kotlin source files for:

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

Each file must include:

- correct `package` declaration
- all necessary imports
- KDoc on every public class, public property, and public function
- constructor or `init` validation for all stated invariants

## Domain Model

- A `Game` is an external reference, not a domain-owned aggregate.
- A `Person` has identity, but no inherent role.
- Roles are contextual to a game and are represented through `RoleAssignment`.
- A `RefereePair` is the collective voter.
- A `TableOfficialTeam` is the evaluated subject.
- A `PerformanceEvaluation` is the aggregate root.
- The evaluation is lifecycle-free: no draft state, no submitted state.
- Raw criterion values are stored directly.
- Weighted totals are derived later through `EvaluationScore.toScore(...)` because the weighting rules may change over time.

## Detailed Specifications

### `exception/DomainException.kt`

Define:

```kotlin
sealed class DomainException(message: String) : Exception(message)
```

Subclasses:

1. `InvalidScoreRange(val value: Int, val min: Int, val max: Int)`
   Message: `"Score value $value is out of valid range [$min, $max]"`
2. `DuplicatePersonInTeam(val personId: String)`
   Message: `"Person with id '$personId' is assigned to multiple roles in the same context"`
3. `InvalidRoleForPosition(val expectedRole: String, val actualRole: String)`
   Message: `"Expected role $expectedRole but got $actualRole"`

Do not add obsolete exceptions for draft/submission workflow or license validation.

### `model/Score.kt`

Requirements:

- `@JvmInline value class Score(val value: Int)`
- valid range is `1..10`
- throw `DomainException.InvalidScoreRange` if out of range
- override `toString()` to return the numeric value as text

### `model/EvaluationScore.kt`

This is a value object with exactly these fixed criteria:

- `appearance: Score`
- `influence: Score`
- `teamwork: Score`

Provide:

```kotlin
fun toScore(
    appearanceWeight: Int = 1,
    influenceWeight: Int = 1,
    teamworkWeight: Int = 1,
): Int
```

Rules:

- `toScore(...)` returns the weighted total
- raw ratings are stored in the three properties
- weights may change later, so totals must be derived, not stored
- negative weights are invalid and must throw `IllegalArgumentException`

### `model/OfficialRole.kt`

Use a sealed class with nested `data object` subtypes:

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

Requirements:

- regular `class`, not `data class`
- properties:
  - `id: String`
  - `firstName: String`
  - `lastName: String`
- validate all three as non-blank
- provide `fullName`
- implement `equals()` and `hashCode()` by `id` only
- provide a useful `toString()`
- do not include license number handling

### `model/RoleAssignment.kt`

Requirements:

- `data class RoleAssignment(val person: Person, val role: OfficialRole)`

### `model/RefereePair.kt`

Requirements:

- `data class RefereePair(val firstReferee: RoleAssignment, val secondReferee: RoleAssignment)`

Invariants:

- `firstReferee.role` must be `OfficialRole.FirstReferee`
- `secondReferee.role` must be `OfficialRole.SecondReferee`
- both persons must be distinct

Expose:

- `val persons: Set<Person>`

Throw:

- `DomainException.InvalidRoleForPosition`
- `DomainException.DuplicatePersonInTeam`

### `model/TableOfficialTeam.kt`

Requirements:

- `data class TableOfficialTeam(val timeKeeper: RoleAssignment, val scoreKeeper: RoleAssignment, val delegate: RoleAssignment? = null)`

Invariants:

- `timeKeeper.role` must be `OfficialRole.TimeKeeper`
- `scoreKeeper.role` must be `OfficialRole.ScoreKeeper`
- if present, `delegate.role` must be `OfficialRole.Delegate`
- all assigned persons must be distinct

Expose:

- `val members: Set<Person>`

The team consists of timekeeper, scorekeeper, and optional delegate only.

### `model/Game.kt`

Requirements:

- `data class Game(val gameId: String, val date: String, val homeTeam: String, val awayTeam: String, val venue: String)`
- validate all properties as non-blank
- treat `Game` as an immutable external reference

Use string dates intentionally. Do not introduce a date library type here.

### `model/PerformanceEvaluation.kt`

Requirements:

- regular `class`, not `data class`
- properties:
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
- no person may appear both in the referee pair and the table official team

Equality:

- implement `equals()` and `hashCode()` by `id` only
- provide a useful `toString()`

Factory:

Provide a companion factory:

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

- default `id` should be generated with Kotlin stdlib UUID support
- `createdAt` must be derived internally with `clock.now().toString()`
- keep the public constructor explicit with `createdAt: String`

### `repository/PerformanceEvaluationRepository.kt`

Define a domain port interface with these `suspend` functions:

- `save(evaluation: PerformanceEvaluation): PerformanceEvaluation`
- `findById(id: String): PerformanceEvaluation?`
- `findByGameId(gameId: String): PerformanceEvaluation?`
- `findByRefereePairPersonIds(firstRefereeId: String, secondRefereeId: String): List<PerformanceEvaluation>`
- `existsByGameId(gameId: String): Boolean`
- `findAll(): List<PerformanceEvaluation>`

Notes:

- do not import `kotlinx.coroutines`
- `suspend` is allowed because it is a Kotlin language feature

## Cross-Cutting Rules

1. Use only Kotlin stdlib and sibling domain imports.
2. Every property must be immutable.
3. Validate all stated invariants at construction time.
4. Illegal states must be unrepresentable wherever practical.
5. Do not introduce obsolete concepts:
   - secretary
   - draft/submitted lifecycle
   - five fixed evaluation criteria
   - license number handling
6. Match the package names and source paths exactly.
7. Prefer domain exceptions for domain invariant failures where specified.

## Expected Usage

This example is illustrative. If it ever conflicts with the file-by-file specification, the specification wins.

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

## Verification Expectations

After generation, the shared module should compile and the shared domain tests should pass:

```bash
./gradlew :shared:jvmTest
```

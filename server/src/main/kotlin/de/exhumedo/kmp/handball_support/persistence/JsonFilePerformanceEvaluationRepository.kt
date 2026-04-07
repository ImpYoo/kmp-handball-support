package de.exhumedo.kmp.handball_support.persistence

import de.exhumedo.kmp.handball_support.domain.rating.model.EvaluationScore
import de.exhumedo.kmp.handball_support.domain.rating.model.Game
import de.exhumedo.kmp.handball_support.domain.rating.model.OfficialRole
import de.exhumedo.kmp.handball_support.domain.rating.model.PerformanceEvaluation
import de.exhumedo.kmp.handball_support.domain.rating.model.Person
import de.exhumedo.kmp.handball_support.domain.rating.model.RefereePair
import de.exhumedo.kmp.handball_support.domain.rating.model.RoleAssignment
import de.exhumedo.kmp.handball_support.domain.rating.model.Score
import de.exhumedo.kmp.handball_support.domain.rating.model.TableOfficialTeam
import de.exhumedo.kmp.handball_support.domain.rating.repository.PerformanceEvaluationRepository
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * File-backed repository storing evaluations as JSON on the local filesystem.
 *
 * @property storagePath Path to the JSON storage file.
 */
class JsonFilePerformanceEvaluationRepository(
    private val storagePath: Path,
) : PerformanceEvaluationRepository {
    private val lock = Any()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        ensureStorageExists()
    }

    override suspend fun save(evaluation: PerformanceEvaluation): PerformanceEvaluation = synchronized(lock) {
        val stored = loadAllMutable()
        val sameGameDifferentId = stored.any {
            it.game.gameId == evaluation.game.gameId && it.id != evaluation.id
        }

        if (sameGameDifferentId) {
            throw DuplicateGameEvaluationException(evaluation.game.gameId)
        }

        val record = evaluation.toStored()
        val existingIndex = stored.indexOfFirst { it.id == evaluation.id }
        if (existingIndex >= 0) {
            stored[existingIndex] = record
        } else {
            stored.add(record)
        }

        persistAll(stored)
        evaluation
    }

    override suspend fun findById(id: String): PerformanceEvaluation? = synchronized(lock) {
        loadAll().firstOrNull { it.id == id }?.toDomain()
    }

    override suspend fun findByGameId(gameId: String): PerformanceEvaluation? = synchronized(lock) {
        loadAll().firstOrNull { it.game.gameId == gameId }?.toDomain()
    }

    override suspend fun findByRefereePairPersonIds(
        firstRefereeId: String,
        secondRefereeId: String,
    ): List<PerformanceEvaluation> = synchronized(lock) {
        loadAll()
            .filter {
                it.refereePair.firstReferee.person.id == firstRefereeId &&
                    it.refereePair.secondReferee.person.id == secondRefereeId
            }
            .map { it.toDomain() }
    }

    override suspend fun existsByGameId(gameId: String): Boolean = synchronized(lock) {
        loadAll().any { it.game.gameId == gameId }
    }

    override suspend fun findAll(): List<PerformanceEvaluation> = synchronized(lock) {
        loadAll().map { it.toDomain() }
    }

    private fun ensureStorageExists() {
        storagePath.parent?.let { Files.createDirectories(it) }
        if (!Files.exists(storagePath)) {
            Files.writeString(storagePath, "[]")
        }
    }

    private fun loadAllMutable(): MutableList<StoredPerformanceEvaluation> = loadAll().toMutableList()

    private fun loadAll(): List<StoredPerformanceEvaluation> {
        val content = Files.readString(storagePath)
        if (content.isBlank()) {
            return emptyList()
        }

        return json.decodeFromString<List<StoredPerformanceEvaluation>>(content)
    }

    private fun persistAll(evaluations: List<StoredPerformanceEvaluation>) {
        Files.writeString(storagePath, json.encodeToString(evaluations))
    }
}

/**
 * Indicates a repository-level uniqueness conflict for a game evaluation.
 *
 * @property gameId Game identifier for which a duplicate evaluation was attempted.
 */
class DuplicateGameEvaluationException(
    val gameId: String,
) : IllegalStateException("An evaluation for game '$gameId' already exists")

@Serializable
private data class StoredPerformanceEvaluation(
    val id: String,
    val game: StoredGame,
    val refereePair: StoredRefereePair,
    val tableOfficialTeam: StoredTableOfficialTeam,
    val score: StoredEvaluationScore,
    val comment: String? = null,
    val createdAt: String,
)

@Serializable
private data class StoredGame(
    val gameId: String,
    val date: String,
    val homeTeam: String,
    val awayTeam: String,
    val venue: String,
)

@Serializable
private data class StoredPerson(
    val id: String,
    val firstName: String,
    val lastName: String,
)

@Serializable
private enum class StoredOfficialRole {
    FIRST_REFEREE,
    SECOND_REFEREE,
    TIME_KEEPER,
    SCORE_KEEPER,
    DELEGATE,
}

@Serializable
private data class StoredRoleAssignment(
    val person: StoredPerson,
    val role: StoredOfficialRole,
)

@Serializable
private data class StoredRefereePair(
    val firstReferee: StoredRoleAssignment,
    val secondReferee: StoredRoleAssignment,
)

@Serializable
private data class StoredTableOfficialTeam(
    val timeKeeper: StoredRoleAssignment,
    val scoreKeeper: StoredRoleAssignment,
    val delegate: StoredRoleAssignment? = null,
)

@Serializable
private data class StoredEvaluationScore(
    val appearance: Int,
    val influence: Int,
    val teamwork: Int,
)

private fun PerformanceEvaluation.toStored(): StoredPerformanceEvaluation = StoredPerformanceEvaluation(
    id = id,
    game = game.toStored(),
    refereePair = refereePair.toStored(),
    tableOfficialTeam = tableOfficialTeam.toStored(),
    score = score.toStored(),
    comment = comment,
    createdAt = createdAt,
)

private fun StoredPerformanceEvaluation.toDomain(): PerformanceEvaluation = PerformanceEvaluation(
    id = id,
    game = game.toDomain(),
    refereePair = refereePair.toDomain(),
    tableOfficialTeam = tableOfficialTeam.toDomain(),
    score = score.toDomain(),
    comment = comment,
    createdAt = createdAt,
)

private fun Game.toStored(): StoredGame = StoredGame(
    gameId = gameId,
    date = date,
    homeTeam = homeTeam,
    awayTeam = awayTeam,
    venue = venue,
)

private fun StoredGame.toDomain(): Game = Game(
    gameId = gameId,
    date = date,
    homeTeam = homeTeam,
    awayTeam = awayTeam,
    venue = venue,
)

private fun Person.toStored(): StoredPerson = StoredPerson(
    id = id,
    firstName = firstName,
    lastName = lastName,
)

private fun StoredPerson.toDomain(): Person = Person(
    id = id,
    firstName = firstName,
    lastName = lastName,
)

private fun RoleAssignment.toStored(): StoredRoleAssignment = StoredRoleAssignment(
    person = person.toStored(),
    role = role.toStored(),
)

private fun StoredRoleAssignment.toDomain(): RoleAssignment = RoleAssignment(
    person = person.toDomain(),
    role = role.toDomain(),
)

private fun RefereePair.toStored(): StoredRefereePair = StoredRefereePair(
    firstReferee = firstReferee.toStored(),
    secondReferee = secondReferee.toStored(),
)

private fun StoredRefereePair.toDomain(): RefereePair = RefereePair(
    firstReferee = firstReferee.toDomain(),
    secondReferee = secondReferee.toDomain(),
)

private fun TableOfficialTeam.toStored(): StoredTableOfficialTeam = StoredTableOfficialTeam(
    timeKeeper = timeKeeper.toStored(),
    scoreKeeper = scoreKeeper.toStored(),
    delegate = delegate?.toStored(),
)

private fun StoredTableOfficialTeam.toDomain(): TableOfficialTeam = TableOfficialTeam(
    timeKeeper = timeKeeper.toDomain(),
    scoreKeeper = scoreKeeper.toDomain(),
    delegate = delegate?.toDomain(),
)

private fun EvaluationScore.toStored(): StoredEvaluationScore = StoredEvaluationScore(
    appearance = appearance.value,
    influence = influence.value,
    teamwork = teamwork.value,
)

private fun StoredEvaluationScore.toDomain(): EvaluationScore = EvaluationScore(
    appearance = Score(appearance),
    influence = Score(influence),
    teamwork = Score(teamwork),
)

private fun OfficialRole.toStored(): StoredOfficialRole = when (this) {
    OfficialRole.FirstReferee -> StoredOfficialRole.FIRST_REFEREE
    OfficialRole.SecondReferee -> StoredOfficialRole.SECOND_REFEREE
    OfficialRole.TimeKeeper -> StoredOfficialRole.TIME_KEEPER
    OfficialRole.ScoreKeeper -> StoredOfficialRole.SCORE_KEEPER
    OfficialRole.Delegate -> StoredOfficialRole.DELEGATE
}

private fun StoredOfficialRole.toDomain(): OfficialRole = when (this) {
    StoredOfficialRole.FIRST_REFEREE -> OfficialRole.FirstReferee
    StoredOfficialRole.SECOND_REFEREE -> OfficialRole.SecondReferee
    StoredOfficialRole.TIME_KEEPER -> OfficialRole.TimeKeeper
    StoredOfficialRole.SCORE_KEEPER -> OfficialRole.ScoreKeeper
    StoredOfficialRole.DELEGATE -> OfficialRole.Delegate
}

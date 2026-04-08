package de.exhumedo.kmp.handball_support.persistence

import de.exhumedo.kmp.handball_support.domain.rating.exception.DomainException
import de.exhumedo.kmp.handball_support.domain.rating.model.EvaluationScore
import de.exhumedo.kmp.handball_support.domain.rating.model.Evaluator
import de.exhumedo.kmp.handball_support.domain.rating.model.EvaluatorReference
import de.exhumedo.kmp.handball_support.domain.rating.model.EvaluatorType
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON-file-backed repository adapter for performance evaluations.
 *
 * The adapter persists dedicated storage records instead of serializing domain
 * objects directly, and enforces uniqueness per game and evaluator type.
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
        val sameGameSameEvaluatorTypeDifferentId = stored.any {
            it.game.gameId == evaluation.game.gameId &&
                it.evaluator.type == evaluation.evaluator.type.toStored() &&
                it.id != evaluation.id
        }

        if (sameGameSameEvaluatorTypeDifferentId) {
            throw DomainException.DuplicateGameEvaluation(
                gameId = evaluation.game.gameId,
                evaluatorType = evaluation.evaluator.type.name,
            )
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

    override suspend fun findByGameId(gameId: String): List<PerformanceEvaluation> = synchronized(lock) {
        loadAll().filter { it.game.gameId == gameId }.map { it.toDomain() }
    }

    override suspend fun findByEvaluatorReference(
        evaluatorReference: EvaluatorReference,
    ): List<PerformanceEvaluation> = synchronized(lock) {
        loadAll()
            .filter { stored ->
                when (evaluatorReference) {
                    is EvaluatorReference.RefereeTeam -> {
                        stored.evaluator.type == StoredEvaluatorType.REFEREE_TEAM &&
                            stored.evaluator.refereePair?.firstReferee?.person?.id == evaluatorReference.firstRefereeId &&
                            stored.evaluator.refereePair.secondReferee.person.id == evaluatorReference.secondRefereeId
                    }

                    is EvaluatorReference.Delegate -> {
                        stored.evaluator.type == StoredEvaluatorType.DELEGATE &&
                            stored.evaluator.delegate?.person?.id == evaluatorReference.delegateId
                    }
                }
            }
            .map { it.toDomain() }
    }

    override suspend fun existsByGameIdAndEvaluatorType(
        gameId: String,
        evaluatorType: EvaluatorType,
    ): Boolean = synchronized(lock) {
        loadAll().any {
            it.game.gameId == gameId && it.evaluator.type == evaluatorType.toStored()
        }
    }

    override suspend fun findAll(): List<PerformanceEvaluation> = synchronized(lock) {
        loadAll().map { it.toDomain() }
    }

    /**
     * Creates parent directories and an empty JSON file when storage is missing.
     */
    private fun ensureStorageExists() {
        storagePath.parent?.let { Files.createDirectories(it) }
        if (!Files.exists(storagePath)) {
            Files.writeString(storagePath, "[]")
        }
    }

    /**
     * Loads all stored evaluations into a mutable list for in-place updates.
     */
    private fun loadAllMutable(): MutableList<StoredPerformanceEvaluation> = loadAll().toMutableList()

    /**
     * Reads and deserializes all stored evaluations from disk.
     *
     * Blank files are treated as empty storage.
     */
    private fun loadAll(): List<StoredPerformanceEvaluation> {
        val content = Files.readString(storagePath)
        if (content.isBlank()) {
            return emptyList()
        }

        return json.decodeFromString<List<StoredPerformanceEvaluation>>(content)
    }

    /**
     * Serializes and writes all stored evaluations to disk.
     */
    private fun persistAll(evaluations: List<StoredPerformanceEvaluation>) {
        Files.writeString(storagePath, json.encodeToString(evaluations))
    }
}

/**
 * Persistence record for one stored performance evaluation.
 */
@Serializable
private data class StoredPerformanceEvaluation(
    val id: String,
    val game: StoredGame,
    val evaluator: StoredEvaluator,
    val tableOfficialTeam: StoredTableOfficialTeam,
    val score: StoredEvaluationScore,
    val comment: String? = null,
    val createdAt: String,
)

/**
 * Persistence record for one stored game reference.
 */
@Serializable
private data class StoredGame(
    val gameId: String,
    val date: String,
    val homeTeam: String,
    val awayTeam: String,
    val venue: String,
)

/**
 * Persistence record for one stored person.
 */
@Serializable
private data class StoredPerson(
    val id: String,
    val firstName: String,
    val lastName: String,
)

/**
 * Persistence enum for official roles.
 */
@Serializable
private enum class StoredOfficialRole {
    FIRST_REFEREE,
    SECOND_REFEREE,
    TIME_KEEPER,
    SCORE_KEEPER,
    DELEGATE,
}

/**
 * Persistence enum for evaluator kinds.
 */
@Serializable
private enum class StoredEvaluatorType {
    @SerialName("REFEREE_TEAM")
    REFEREE_TEAM,

    @SerialName("DELEGATE")
    DELEGATE,
}

/**
 * Persistence record for one stored role assignment.
 */
@Serializable
private data class StoredRoleAssignment(
    val person: StoredPerson,
    val role: StoredOfficialRole,
)

/**
 * Persistence record for one stored referee pair.
 */
@Serializable
private data class StoredRefereePair(
    val firstReferee: StoredRoleAssignment,
    val secondReferee: StoredRoleAssignment,
)

/**
 * Persistence record for one stored evaluator.
 */
@Serializable
private data class StoredEvaluator(
    val type: StoredEvaluatorType,
    val refereePair: StoredRefereePair? = null,
    val delegate: StoredRoleAssignment? = null,
)

/**
 * Persistence record for one stored table official team.
 */
@Serializable
private data class StoredTableOfficialTeam(
    val timeKeeper: StoredRoleAssignment,
    val scoreKeeper: StoredRoleAssignment,
    val delegate: StoredRoleAssignment? = null,
)

/**
 * Persistence record for one stored evaluation score.
 */
@Serializable
private data class StoredEvaluationScore(
    val appearance: Int,
    val influence: Int,
    val teamwork: Int,
)

/**
 * Maps a domain aggregate to its stored persistence record.
 */
private fun PerformanceEvaluation.toStored(): StoredPerformanceEvaluation = StoredPerformanceEvaluation(
    id = id,
    game = game.toStored(),
    evaluator = evaluator.toStored(),
    tableOfficialTeam = tableOfficialTeam.toStored(),
    score = score.toStored(),
    comment = comment,
    createdAt = createdAt,
)

/**
 * Maps a stored persistence record to the domain aggregate.
 */
private fun StoredPerformanceEvaluation.toDomain(): PerformanceEvaluation = PerformanceEvaluation(
    id = id,
    game = game.toDomain(),
    evaluator = evaluator.toDomain(),
    tableOfficialTeam = tableOfficialTeam.toDomain(),
    score = score.toDomain(),
    comment = comment,
    createdAt = createdAt,
)

/**
 * Maps a domain game to its stored representation.
 */
private fun Game.toStored(): StoredGame = StoredGame(
    gameId = gameId,
    date = date,
    homeTeam = homeTeam,
    awayTeam = awayTeam,
    venue = venue,
)

/**
 * Maps a stored game to the domain model.
 */
private fun StoredGame.toDomain(): Game = Game(
    gameId = gameId,
    date = date,
    homeTeam = homeTeam,
    awayTeam = awayTeam,
    venue = venue,
)

/**
 * Maps a domain person to its stored representation.
 */
private fun Person.toStored(): StoredPerson = StoredPerson(
    id = id,
    firstName = firstName,
    lastName = lastName,
)

/**
 * Maps a stored person to the domain model.
 */
private fun StoredPerson.toDomain(): Person = Person(
    id = id,
    firstName = firstName,
    lastName = lastName,
)

/**
 * Maps a domain role assignment to its stored representation.
 */
private fun RoleAssignment.toStored(): StoredRoleAssignment = StoredRoleAssignment(
    person = person.toStored(),
    role = role.toStored(),
)

/**
 * Maps a stored role assignment to the domain model.
 */
private fun StoredRoleAssignment.toDomain(): RoleAssignment = RoleAssignment(
    person = person.toDomain(),
    role = role.toDomain(),
)

/**
 * Maps a domain referee pair to its stored representation.
 */
private fun RefereePair.toStored(): StoredRefereePair = StoredRefereePair(
    firstReferee = firstReferee.toStored(),
    secondReferee = secondReferee.toStored(),
)

/**
 * Maps a stored referee pair to the domain model.
 */
private fun StoredRefereePair.toDomain(): RefereePair = RefereePair(
    firstReferee = firstReferee.toDomain(),
    secondReferee = secondReferee.toDomain(),
)

/**
 * Maps a domain evaluator to its stored representation.
 */
private fun Evaluator.toStored(): StoredEvaluator = when (this) {
    is Evaluator.RefereeTeam -> StoredEvaluator(
        type = StoredEvaluatorType.REFEREE_TEAM,
        refereePair = refereePair.toStored(),
        delegate = null,
    )

    is Evaluator.Delegate -> StoredEvaluator(
        type = StoredEvaluatorType.DELEGATE,
        refereePair = null,
        delegate = assignment.toStored(),
    )
}

/**
 * Maps a stored evaluator to the domain model.
 */
private fun StoredEvaluator.toDomain(): Evaluator = when (type) {
    StoredEvaluatorType.REFEREE_TEAM -> Evaluator.RefereeTeam(
        refereePair = requireNotNull(refereePair) { "Stored evaluator refereePair is required" }.toDomain(),
    )

    StoredEvaluatorType.DELEGATE -> Evaluator.Delegate(
        assignment = requireNotNull(delegate) { "Stored evaluator delegate is required" }.toDomain(),
    )
}

/**
 * Maps a domain table official team to its stored representation.
 */
private fun TableOfficialTeam.toStored(): StoredTableOfficialTeam = StoredTableOfficialTeam(
    timeKeeper = timeKeeper.toStored(),
    scoreKeeper = scoreKeeper.toStored(),
    delegate = delegate?.toStored(),
)

/**
 * Maps a stored table official team to the domain model.
 */
private fun StoredTableOfficialTeam.toDomain(): TableOfficialTeam = TableOfficialTeam(
    timeKeeper = timeKeeper.toDomain(),
    scoreKeeper = scoreKeeper.toDomain(),
    delegate = delegate?.toDomain(),
)

/**
 * Maps a domain evaluation score to its stored representation.
 */
private fun EvaluationScore.toStored(): StoredEvaluationScore = StoredEvaluationScore(
    appearance = appearance.value,
    influence = influence.value,
    teamwork = teamwork.value,
)

/**
 * Maps a stored evaluation score to the domain model.
 */
private fun StoredEvaluationScore.toDomain(): EvaluationScore = EvaluationScore(
    appearance = Score(appearance),
    influence = Score(influence),
    teamwork = Score(teamwork),
)

/**
 * Maps a domain official role to its stored enum value.
 */
private fun OfficialRole.toStored(): StoredOfficialRole = when (this) {
    OfficialRole.FirstReferee -> StoredOfficialRole.FIRST_REFEREE
    OfficialRole.SecondReferee -> StoredOfficialRole.SECOND_REFEREE
    OfficialRole.TimeKeeper -> StoredOfficialRole.TIME_KEEPER
    OfficialRole.ScoreKeeper -> StoredOfficialRole.SCORE_KEEPER
    OfficialRole.Delegate -> StoredOfficialRole.DELEGATE
}

/**
 * Maps a stored official role to the domain model.
 */
private fun StoredOfficialRole.toDomain(): OfficialRole = when (this) {
    StoredOfficialRole.FIRST_REFEREE -> OfficialRole.FirstReferee
    StoredOfficialRole.SECOND_REFEREE -> OfficialRole.SecondReferee
    StoredOfficialRole.TIME_KEEPER -> OfficialRole.TimeKeeper
    StoredOfficialRole.SCORE_KEEPER -> OfficialRole.ScoreKeeper
    StoredOfficialRole.DELEGATE -> OfficialRole.Delegate
}

/**
 * Maps a domain evaluator type to its stored enum value.
 */
private fun EvaluatorType.toStored(): StoredEvaluatorType = when (this) {
    EvaluatorType.REFEREE_TEAM -> StoredEvaluatorType.REFEREE_TEAM
    EvaluatorType.DELEGATE -> StoredEvaluatorType.DELEGATE
}

package de.exhumedo.kmp.handball_support.persistence

import de.exhumedo.kmp.handball_support.domain.rating.model.Evaluator
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

class JsonFilePerformanceEvaluationRepository(private val storagePath: Path) : PerformanceEvaluationRepository {

    private val lock = Any()
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    private data class StoredPerformanceEvaluation(
        val id: String,
        val gameId: String,
        val gameDate: String,
        val homeTeam: String,
        val awayTeam: String,
        val venue: String,
        val evaluatorType: String,
        val r1Id: String = "",
        val r1FirstName: String = "",
        val r1LastName: String = "",
        val r2Id: String = "",
        val r2FirstName: String = "",
        val r2LastName: String = "",
        val delegateId: String = "",
        val delegateFirstName: String = "",
        val delegateLastName: String = "",
        val tkId: String,
        val tkFirstName: String,
        val tkLastName: String,
        val skId: String,
        val skFirstName: String,
        val skLastName: String,
        val tdId: String = "",
        val tdFirstName: String = "",
        val tdLastName: String = "",
        val appearance: Int,
        val influence: Int,
        val teamwork: Int,
        val comment: String,
        val createdAt: String,
    )

    override fun save(evaluation: PerformanceEvaluation): PerformanceEvaluation = synchronized(lock) {
        ensureStorageExists()
        val all = loadAll().toMutableList()
        all.removeIf { it.id == evaluation.id }
        all.add(toStored(evaluation))
        persist(all)
        evaluation
    }

    override fun findById(id: String): PerformanceEvaluation? = synchronized(lock) {
        ensureStorageExists()
        loadAll().find { it.id == id }?.let { toDomain(it) }
    }

    override fun findAll(): List<PerformanceEvaluation> = synchronized(lock) {
        ensureStorageExists()
        loadAll().map { toDomain(it) }
    }

    override fun findByGameId(gameId: String): List<PerformanceEvaluation> = synchronized(lock) {
        ensureStorageExists()
        loadAll().filter { it.gameId == gameId }.map { toDomain(it) }
    }

    override fun findByRefereeTeam(firstRefereeId: String, secondRefereeId: String): List<PerformanceEvaluation> = synchronized(lock) {
        ensureStorageExists()
        loadAll().filter { it.r1Id == firstRefereeId && it.r2Id == secondRefereeId }.map { toDomain(it) }
    }

    override fun findByDelegate(delegateId: String): List<PerformanceEvaluation> = synchronized(lock) {
        ensureStorageExists()
        loadAll().filter { it.delegateId == delegateId }.map { toDomain(it) }
    }

    private fun ensureStorageExists() {
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath.parent)
            Files.writeString(storagePath, "[]")
        }
    }

    private fun loadAll(): List<StoredPerformanceEvaluation> {
        val content = Files.readString(storagePath).trim()
        if (content.isEmpty() || content == "null") return emptyList()
        return json.decodeFromString(content)
    }

    private fun persist(list: List<StoredPerformanceEvaluation>) {
        Files.writeString(storagePath, json.encodeToString(list))
    }

    private fun toStored(ev: PerformanceEvaluation): StoredPerformanceEvaluation {
        val r1 = (ev.evaluator as? Evaluator.RefereeTeam)?.refereePair?.firstReferee?.person
        val r2 = (ev.evaluator as? Evaluator.RefereeTeam)?.refereePair?.secondReferee?.person
        val delegate = (ev.evaluator as? Evaluator.Delegate)?.assignment?.person
        return StoredPerformanceEvaluation(
            id = ev.id,
            gameId = ev.game.gameId,
            gameDate = ev.game.date,
            homeTeam = ev.game.homeTeam,
            awayTeam = ev.game.awayTeam,
            venue = ev.game.venue,
            evaluatorType = ev.evaluator.type.name,
            r1Id = r1?.id ?: "",
            r1FirstName = r1?.firstName ?: "",
            r1LastName = r1?.lastName ?: "",
            r2Id = r2?.id ?: "",
            r2FirstName = r2?.firstName ?: "",
            r2LastName = r2?.lastName ?: "",
            delegateId = delegate?.id ?: "",
            delegateFirstName = delegate?.firstName ?: "",
            delegateLastName = delegate?.lastName ?: "",
            tkId = ev.tableOfficialTeam.timeKeeper.person.id,
            tkFirstName = ev.tableOfficialTeam.timeKeeper.person.firstName,
            tkLastName = ev.tableOfficialTeam.timeKeeper.person.lastName,
            skId = ev.tableOfficialTeam.scoreKeeper.person.id,
            skFirstName = ev.tableOfficialTeam.scoreKeeper.person.firstName,
            skLastName = ev.tableOfficialTeam.scoreKeeper.person.lastName,
            tdId = ev.tableOfficialTeam.delegate?.person?.id ?: "",
            tdFirstName = ev.tableOfficialTeam.delegate?.person?.firstName ?: "",
            tdLastName = ev.tableOfficialTeam.delegate?.person?.lastName ?: "",
            appearance = ev.score.appearance.value,
            influence = ev.score.influence.value,
            teamwork = ev.score.teamwork.value,
            comment = ev.comment,
            createdAt = ev.createdAt,
        )
    }

    private fun toDomain(stored: StoredPerformanceEvaluation): PerformanceEvaluation {
        val game = Game(stored.gameId, stored.gameDate, stored.homeTeam, stored.awayTeam, stored.venue)
        val evaluator: Evaluator = when (stored.evaluatorType) {
            "REFEREE_TEAM" -> Evaluator.RefereeTeam(
                RefereePair(
                    firstReferee = RoleAssignment(Person(stored.r1Id, stored.r1FirstName, stored.r1LastName), OfficialRole.FirstReferee),
                    secondReferee = RoleAssignment(Person(stored.r2Id, stored.r2FirstName, stored.r2LastName), OfficialRole.SecondReferee),
                )
            )
            "DELEGATE" -> Evaluator.Delegate(
                assignment = RoleAssignment(Person(stored.delegateId, stored.delegateFirstName, stored.delegateLastName), OfficialRole.Delegate)
            )
            else -> throw IllegalStateException("Unknown evaluator type: ${stored.evaluatorType}")
        }
        val tableOfficialTeam = TableOfficialTeam(
            timeKeeper = RoleAssignment(Person(stored.tkId, stored.tkFirstName, stored.tkLastName), OfficialRole.TimeKeeper),
            scoreKeeper = RoleAssignment(Person(stored.skId, stored.skFirstName, stored.skLastName), OfficialRole.ScoreKeeper),
            delegate = if (stored.tdId.isNotBlank()) RoleAssignment(Person(stored.tdId, stored.tdFirstName, stored.tdLastName), OfficialRole.Delegate) else null,
        )
        val score = EvaluationScore(Score(stored.appearance), Score(stored.influence), Score(stored.teamwork))
        return PerformanceEvaluation(stored.id, game, evaluator, tableOfficialTeam, score, stored.comment, stored.createdAt)
    }
}

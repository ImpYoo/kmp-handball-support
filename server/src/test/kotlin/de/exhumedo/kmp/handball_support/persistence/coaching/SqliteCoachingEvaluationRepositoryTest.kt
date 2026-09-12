package de.exhumedo.kmp.handball_support.persistence.coaching

import de.exhumedo.kmp.handball_support.referee_coaching.data.DefaultCriterionCatalog
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CoachingGame
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CoachingPerson
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.RefereeCoachingEvaluation
import de.exhumedo.kmp.handball_support.referee_coaching.domain.scoring.CriterionScoringService
import java.sql.DriverManager
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqliteCoachingEvaluationRepositoryTest {

    private val tempDir = createTempDirectory("coaching-test-")
    private val dbPath = tempDir.resolve("coaching.db")
    private val repository = SqliteCoachingEvaluationRepository(dbPath)
    private val catalog = DefaultCriterionCatalog().loadCriteria()
    private val scoring = CriterionScoringService()

    @AfterTest
    fun cleanup() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun saveAndFindById() {
        val evaluation = sampleEvaluation(catalog)
        val saved = repository.save(evaluation)
        val loaded = repository.findById(saved.id)

        assertNotNull(loaded)
        assertEquals(saved.id, loaded.id)
        assertEquals(saved.totalScore, loaded.totalScore)
        assertEquals(saved.maxTotalScore, loaded.maxTotalScore)
    }

    @Test
    fun findByIdReturnsNullForMissing() {
        assertNull(repository.findById("does-not-exist"))
    }

    @Test
    fun updateKeepsLatestCounts() {
        val v1 = sampleEvaluation(catalog)
        repository.save(v1)

        val updatedCriteria = catalog.map { criterion ->
            if (criterion.id == "a1-spielgedanke-vorteil") {
                (1..3).fold(criterion) { acc, _ ->
                    scoring.incrementRootCause(acc, "a1-spielverstaendnis", "a1-schneller-anwurf")
                }
            } else criterion
        }
        val v2 = v1.copy(criteria = updatedCriteria, updatedAt = "2026-09-15T11:00:00Z")
        repository.save(v2)

        val loaded = repository.findById(v1.id)!!
        assertEquals(v2.totalScore, loaded.totalScore)
        assertEquals("2026-09-15T11:00:00Z", loaded.updatedAt)
    }

    @Test
    fun findAllFiltersByGameIdAndEvaluator() {
        repository.save(sampleEvaluation(catalog, gameId = "g-1", evaluator = "otto"))
        repository.save(sampleEvaluation(catalog, gameId = "g-2", evaluator = "anna"))

        assertEquals(1, repository.findAll(CoachingEvaluationFilter(gameId = "g-1")).size)
        assertEquals(1, repository.findAll(CoachingEvaluationFilter(evaluatorUsername = "anna")).size)
        assertEquals(2, repository.findAll().size)
    }

    @Test
    fun deleteByIdRemovesEvaluation() {
        val evaluation = sampleEvaluation(catalog)
        repository.save(evaluation)
        assertTrue(repository.deleteById(evaluation.id))
        assertNull(repository.findById(evaluation.id))
        assertEquals(false, repository.deleteById(evaluation.id))
    }

    @Test
    fun deleteByIdCascadesToChildTables() {
        // Save an evaluation with root-cause counts and history entries,
        // then verify that deleting it removes the child rows too.
        // Without PRAGMA foreign_keys = ON the cascade silently does nothing.
        val criteria = catalog.map { criterion ->
            if (criterion.id == "a1-spielgedanke-vorteil") {
                scoring.incrementRootCause(criterion, "a1-spielverstaendnis", "a1-schneller-anwurf")
            } else criterion
        }
        val evaluation = sampleEvaluation(criteria)
        repository.save(evaluation)

        assertTrue(repository.deleteById(evaluation.id))

        // Directly query the child tables to confirm cascaded deletion.
        val dbUrl = "jdbc:sqlite:$dbPath"
        DriverManager.getConnection(dbUrl).use { conn ->
            conn.createStatement().use { stmt ->
                val counts = stmt.executeQuery(
                    "SELECT COUNT(*) FROM coaching_criterion_counts WHERE evaluation_id = '${evaluation.id}'"
                ).use { rs -> rs.getInt(1) }
                assertEquals(0, counts, "coaching_criterion_counts rows should be cascade-deleted")

                val history = stmt.executeQuery(
                    "SELECT COUNT(*) FROM coaching_history_entries WHERE evaluation_id = '${evaluation.id}'"
                ).use { rs -> rs.getInt(1) }
                assertEquals(0, history, "coaching_history_entries rows should be cascade-deleted")
            }
        }
    }

    private fun sampleEvaluation(
        criteria: List<de.exhumedo.kmp.handball_support.referee_coaching.domain.model.Criterion>,
        gameId: String = "g-1",
        evaluator: String = "observer.otto",
    ) = RefereeCoachingEvaluation(
        id = "ev-$gameId",
        game = CoachingGame(gameId, "2026-09-15", "THW Kiel", "SC Magdeburg"),
        evaluatorUsername = evaluator,
        firstReferee = CoachingPerson("p1", "Max", "Mustermann"),
        secondReferee = CoachingPerson("p2", "Anna", "Schmidt"),
        criteria = criteria,
        comment = "",
        createdAt = "2026-09-15T10:00:00Z",
        updatedAt = "2026-09-15T10:00:00Z",
    )
}

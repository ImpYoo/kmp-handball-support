package de.exhumedo.kmp.handball_support.referee_coaching.domain

import de.exhumedo.kmp.handball_support.referee_coaching.data.DefaultCriterionCatalog
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CoachingGame
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.CoachingPerson
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.RefereeCoachingEvaluation
import de.exhumedo.kmp.handball_support.referee_coaching.domain.scoring.CriterionScoringService
import de.exhumedo.kmp.handball_support.referee_coaching.domain.service.RefereeCoachingReportBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RefereeCoachingEvaluationTest {

    private val criteria = DefaultCriterionCatalog().loadCriteria()

    @Test
    fun freshEvaluationHasDefaultScore() {
        val evaluation = sampleEvaluation(criteria)
        assertEquals(criteria.size * 6, evaluation.maxTotalScore)
        assertEquals(criteria.size * 6, evaluation.totalScore)
        assertEquals(100, evaluation.percentage)
        assertFalse(evaluation.isStarted)
    }

    @Test
    fun selectingRootCauseLowersScoreAndMarksStarted() {
        val scoring = CriterionScoringService()
        val updated = criteria.map { criterion ->
            if (criterion.id == "a1-spielgedanke-vorteil") {
                (1..3).fold(criterion) { acc, _ ->
                    scoring.incrementRootCause(acc, "a1-spielverstaendnis", "a1-schneller-anwurf")
                }
            } else criterion
        }
        val evaluation = sampleEvaluation(updated)
        assertTrue(evaluation.isStarted)
        assertEquals((criteria.size * 6) - 1, evaluation.totalScore)
        assertTrue(evaluation.percentage < 100)
    }

    @Test
    fun reportBuildsReadableRows() {
        val scoring = CriterionScoringService()
        val updated = criteria.map { criterion ->
            when (criterion.id) {
                "a1-spielgedanke-vorteil" -> scoring.incrementRootCause(
                    scoring.incrementRootCause(criterion, "a1-spielverstaendnis", "a1-schneller-anwurf"),
                    "a1-spielverstaendnis", "a1-schneller-anwurf",
                )
                "b4-spielleitung-insgesamt" -> scoring.incrementRootCause(
                    scoring.incrementRootCause(
                        scoring.incrementRootCause(criterion, "b4-gesamtlinie-der-sr", "b4-zu-grosszuegig"),
                        "b4-gesamtlinie-der-sr", "b4-zu-grosszuegig",
                    ),
                    "b4-gesamtlinie-der-sr", "b4-zu-grosszuegig",
                )
                else -> criterion
            }
        }
        val evaluation = sampleEvaluation(updated, comment = "Testkommentar")
        val report = RefereeCoachingReportBuilder().build(evaluation)

        assertEquals(evaluation.id, report.evaluationId)
        assertEquals("Testkommentar", report.comment)
        assertEquals(evaluation.totalScore, report.totalScore)
        assertEquals(evaluation.maxTotalScore, report.maxTotalScore)
        assertEquals(evaluation.percentage, report.percentage)

        val a1Row = report.rows.first { it.criterionId == "a1-spielgedanke-vorteil" }
        // 2 increments of the same root cause -> threshold is 3, so penalty stays 0
        assertEquals(6, a1Row.score)
        assertEquals(0, a1Row.deductionPoints)
        assertEquals(2, a1Row.defectGroups.first().selectedRootCauses.first().count)

        val b4Row = report.rows.first { it.criterionId == "b4-spielleitung-insgesamt" }
        // 3 increments -> first threshold reached, penalty 1, score 5
        assertEquals(5, b4Row.score)
        assertEquals(1, b4Row.deductionPoints)
        assertEquals(3, b4Row.defectGroups.first().selectedRootCauses.first().count)
    }

    private fun sampleEvaluation(criteria: List<de.exhumedo.kmp.handball_support.referee_coaching.domain.model.Criterion>, comment: String = "") =
        RefereeCoachingEvaluation(
            id = "ev-1",
            game = CoachingGame("g-1", "2026-09-15", "THW Kiel", "SC Magdeburg"),
            evaluatorUsername = "observer.otto",
            firstReferee = CoachingPerson("p1", "Max", "Mustermann"),
            secondReferee = CoachingPerson("p2", "Anna", "Schmidt"),
            criteria = criteria,
            comment = comment,
            createdAt = "2026-09-15T10:00:00Z",
            updatedAt = "2026-09-15T10:00:00Z",
        )
}

package de.exhumedo.kmp.handball_support.domain.rating.model

import de.exhumedo.kmp.handball_support.domain.rating.exception.DomainException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Instant

class PerformanceEvaluationDomainRulesTest {

    @Test
    fun rejectsOverlapBetweenDelegateEvaluatorAndTableTeam() {
        val overlappingPerson = person("d1")
        val evaluator = Evaluator.Delegate(
            RoleAssignment(overlappingPerson, OfficialRole.Delegate),
        )
        val tableTeam = TableOfficialTeam(
            timeKeeper = RoleAssignment(person("t1"), OfficialRole.TimeKeeper),
            scoreKeeper = RoleAssignment(person("t2"), OfficialRole.ScoreKeeper),
            delegate = RoleAssignment(overlappingPerson, OfficialRole.Delegate),
        )

        val exception = assertFailsWith<DomainException.DuplicatePersonInTeam> {
            PerformanceEvaluation.create(
                id = "evaluation-1",
                game = game(),
                evaluator = evaluator,
                tableOfficialTeam = tableTeam,
                score = evaluationScore(),
                comment = null,
                clock = fixedClock("2026-04-06T12:00:00Z"),
            )
        }

        assertEquals(overlappingPerson.id, exception.personId)
    }

    private fun fixedClock(isoInstant: String): Clock = object : Clock {
        override fun now(): Instant = Instant.parse(isoInstant)
    }
}

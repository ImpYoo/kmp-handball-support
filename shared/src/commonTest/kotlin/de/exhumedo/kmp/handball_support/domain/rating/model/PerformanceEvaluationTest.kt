package de.exhumedo.kmp.handball_support.domain.rating.model

import de.exhumedo.kmp.handball_support.domain.rating.exception.DomainException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock
import kotlin.time.Instant

class PerformanceEvaluationTest {

    @Test
    fun rejectsOverlapBetweenRefereesAndTableTeam() {
        val overlappingPerson = person("p1")

        val refereePair = RefereePair(
            firstReferee = RoleAssignment(overlappingPerson, OfficialRole.FirstReferee),
            secondReferee = RoleAssignment(person("r2"), OfficialRole.SecondReferee),
        )

        val tableTeam = TableOfficialTeam(
            timeKeeper = RoleAssignment(overlappingPerson, OfficialRole.TimeKeeper),
            scoreKeeper = RoleAssignment(person("t2"), OfficialRole.ScoreKeeper),
            delegate = null,
        )

        val exception = assertFailsWith<DomainException.DuplicatePersonInTeam> {
            PerformanceEvaluation.create(
                id = "evaluation-1",
                game = game(),
                refereePair = refereePair,
                tableOfficialTeam = tableTeam,
                score = evaluationScore(),
                comment = "Overlap should fail",
                clock = fixedClock("2026-04-06T12:00:00Z"),
            )
        }

        assertEquals(overlappingPerson.id, exception.personId)
    }

    @Test
    fun usesIdentityEquality() {
        val first = PerformanceEvaluation.create(
            id = "evaluation-1",
            game = game(),
            refereePair = refereePair(),
            tableOfficialTeam = tableOfficialTeam(),
            score = evaluationScore(),
            comment = "First",
            clock = fixedClock("2026-04-06T12:00:00Z"),
        )

        val second = PerformanceEvaluation.create(
            id = "evaluation-1",
            game = Game(
                gameId = "game-2",
                date = "2026-04-07",
                homeTeam = "Away",
                awayTeam = "Home",
                venue = "Other Hall",
            ),
            refereePair = RefereePair(
                firstReferee = RoleAssignment(person("x1"), OfficialRole.FirstReferee),
                secondReferee = RoleAssignment(person("x2"), OfficialRole.SecondReferee),
            ),
            tableOfficialTeam = TableOfficialTeam(
                timeKeeper = RoleAssignment(person("x3"), OfficialRole.TimeKeeper),
                scoreKeeper = RoleAssignment(person("x4"), OfficialRole.ScoreKeeper),
                delegate = null,
            ),
            score = EvaluationScore(
                appearance = Score(10),
                influence = Score(10),
                teamwork = Score(10),
            ),
            comment = "Second",
            clock = fixedClock("2026-04-07T12:00:00Z"),
        )

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun derivesCreatedAtFromProvidedClock() {
        val evaluation = PerformanceEvaluation.create(
            id = "evaluation-1",
            game = game(),
            refereePair = refereePair(),
            tableOfficialTeam = tableOfficialTeam(),
            score = evaluationScore(),
            comment = null,
            clock = fixedClock("2026-04-08T09:15:00Z"),
        )

        assertEquals("2026-04-08T09:15:00Z", evaluation.createdAt)
    }

    private fun fixedClock(isoInstant: String): Clock = object : Clock {
        override fun now(): Instant = Instant.parse(isoInstant)
    }
}

package de.exhumedo.kmp.handball_support.persistence

import de.exhumedo.kmp.handball_support.coaching.CoachingHistoryEntry
import de.exhumedo.kmp.handball_support.coaching.CoachingHistoryPresenter
import de.exhumedo.kmp.handball_support.matchconsole.MatchSetupPresenter
import de.exhumedo.kmp.handball_support.matchconsole.RosterPresenter
import de.exhumedo.kmp.handball_support.matchconsole.ScoreboardPresenter
import de.exhumedo.kmp.handball_support.matchconsole.StopwatchPresenter
import de.exhumedo.kmp.handball_support.coaching.RefereeCoachingPresenter
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionManagerTest {

    private fun newManagers() = Triple(SessionManager(InMemorySessionStorage()), RefereeCoachingPresenter(), CoachingHistoryPresenter())

    @Test
    fun restoreIntoFreshPresentersRoundTripsAllState() {
        val (manager, coaching, history) = newManagers()
        val stopwatch = StopwatchPresenter()
        val scoreboard = ScoreboardPresenter()
        val roster = RosterPresenter()
        val matchSetup = MatchSetupPresenter()

        // Fill some state.
        matchSetup.homeTeamName = "THW Kiel"
        matchSetup.guestTeamName = "SC Magdeburg"
        scoreboard.incrementHome()
        scoreboard.incrementGuest()
        history.record(
            gameTimeMillis = 1234L,
            homeScore = scoreboard.homeScore,
            guestScore = scoreboard.guestScore,
            criterionId = "c1",
            defectGroupId = "g1",
            rootCauseId = "r1",
        )
        val criterion = coaching.criteria.first()
        val group = criterion.defectGroups.first()
        val cause = group.rootCauses.first()
        repeat(3) { coaching.select(criterion.id, group.id, cause.id) }

        manager.save(coaching, history, stopwatch, scoreboard, roster, matchSetup)

        // Fresh presenters, as after an app restart.
        val coaching2 = RefereeCoachingPresenter()
        val history2 = CoachingHistoryPresenter()
        val stopwatch2 = StopwatchPresenter()
        val scoreboard2 = ScoreboardPresenter()
        val roster2 = RosterPresenter()
        val matchSetup2 = MatchSetupPresenter()

        val restored = manager.restoreInto(coaching2, history2, stopwatch2, scoreboard2, roster2, matchSetup2)
        assertEquals(true, restored)
        assertEquals("THW Kiel", matchSetup2.homeTeamName)
        assertEquals("SC Magdeburg", matchSetup2.guestTeamName)
        assertEquals(1, scoreboard2.homeScore)
        assertEquals(1, scoreboard2.guestScore)
        assertEquals(1, history2.entries.size)
        assertEquals(coaching.totalScore, coaching2.totalScore)
    }

    @Test
    fun clearWipesPersistedSession() {
        val (manager, coaching, history) = newManagers()
        val matchSetup = MatchSetupPresenter()
        matchSetup.homeTeamName = "A"
        manager.save(coaching, history, StopwatchPresenter(), ScoreboardPresenter(), RosterPresenter(), matchSetup)

        manager.clear()

        assertEquals(false, manager.restoreInto(coaching, history, StopwatchPresenter(), ScoreboardPresenter(), RosterPresenter(), matchSetup))
    }
}
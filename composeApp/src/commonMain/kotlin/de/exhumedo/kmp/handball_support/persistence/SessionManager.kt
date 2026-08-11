package de.exhumedo.kmp.handball_support.persistence

import de.exhumedo.kmp.handball_support.coaching.CoachingHistoryPresenter
import de.exhumedo.kmp.handball_support.coaching.RefereeCoachingPresenter
import de.exhumedo.kmp.handball_support.matchconsole.MatchSetupPresenter
import de.exhumedo.kmp.handball_support.matchconsole.RosterPresenter
import de.exhumedo.kmp.handball_support.matchconsole.ScoreboardPresenter
import de.exhumedo.kmp.handball_support.matchconsole.StopwatchPresenter

/**
 * Coordinates persistence of a coaching session: captures the presenters into a
 * [PersistedSession] for saving and restores a stored snapshot back into them.
 *
 * Backed by the platform-specific [SessionStorage].
 */
class SessionManager(
    private val storage: SessionStorage = sessionStorage(),
) {
    /** Captures the current presenter state and persists it. */
    fun save(
        coaching: RefereeCoachingPresenter,
        history: CoachingHistoryPresenter,
        stopwatch: StopwatchPresenter,
        scoreboard: ScoreboardPresenter,
        roster: RosterPresenter,
        matchSetup: MatchSetupPresenter,
    ) {
        storage.save(
            SessionMapper.capture(
                coaching = coaching,
                history = history,
                stopwatch = stopwatch,
                scoreboard = scoreboard,
                roster = roster,
                matchSetup = matchSetup,
            ),
        )
    }

    /**
     * Restores a previously stored session into the presenters.
     * Returns true if a session was found and applied.
     */
    fun restoreInto(
        coaching: RefereeCoachingPresenter,
        history: CoachingHistoryPresenter,
        stopwatch: StopwatchPresenter,
        scoreboard: ScoreboardPresenter,
        roster: RosterPresenter,
        matchSetup: MatchSetupPresenter,
    ): Boolean {
        val session = storage.read() ?: return false
        SessionMapper.restore(
            session = session,
            coaching = coaching,
            history = history,
            stopwatch = stopwatch,
            scoreboard = scoreboard,
            roster = roster,
            matchSetup = matchSetup,
        )
        return true
    }

    /** Persists an already-captured snapshot (used by the auto-save flow). */
    fun save(session: PersistedSession) = storage.save(session)

    fun clear() = storage.clear()
}



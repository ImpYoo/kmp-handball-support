package de.exhumedo.kmp.handball_support.coaching

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.exhumedo.kmp.handball_support.matchconsole.RosterTeam

/**
 * Optional context attached to a history entry, connecting the event to a team,
 * a specific player and/or a referee. Display labels are denormalized so the
 * history panel can render without extra lookups.
 */
data class CoachingHistoryAttachment(
    val team: RosterTeam? = null,
    val teamLabel: String? = null,
    val playerId: String? = null,
    val playerLabel: String? = null,
    val refereeName: String? = null,
) {
    val isEmpty: Boolean
        get() = team == null && playerId == null && refereeName == null
}

/** Kind of event recorded in the coaching history. */
enum class HistoryEventType { ROOT_CAUSE, GOAL }

/**
 * A single recorded change during a coaching session, captured with the game
 * time and score at that moment.
 *
 * For [HistoryEventType.ROOT_CAUSE] the criterion/group/rootCause ids are set and
 * [selected] indicates select (+) vs deselect (−). For [HistoryEventType.GOAL]
 * [goalTeam] is set and [selected] indicates a goal (+) vs a correction (−).
 *
 * @param attachment optional link to a team/player/referee.
 * @param note free-text note for the event.
 */
data class CoachingHistoryEntry(
    val id: String,
    val gameTimeMillis: Long,
    val homeScore: Int,
    val guestScore: Int,
    val type: HistoryEventType = HistoryEventType.ROOT_CAUSE,
    val criterionId: String? = null,
    val defectGroupId: String? = null,
    val rootCauseId: String? = null,
    val goalTeam: RosterTeam? = null,
    val selected: Boolean = true,
    val attachment: CoachingHistoryAttachment? = null,
    val note: String = "",
)

/**
 * Append-only log of selected root causes during a coaching session. Entries can
 * later be annotated with a team/player/referee via [attach].
 */
class CoachingHistoryPresenter {
    var entries by mutableStateOf<List<CoachingHistoryEntry>>(emptyList())
        private set

    private var nextId = 0

    /** Entries newest-first, for descending display. */
    val entriesDescending: List<CoachingHistoryEntry>
        get() = entries.asReversed()

    /** Records a root-cause select/deselect event and returns its generated id. */
    fun record(
        gameTimeMillis: Long,
        homeScore: Int,
        guestScore: Int,
        criterionId: String,
        defectGroupId: String,
        rootCauseId: String,
        selected: Boolean = true,
    ): String {
        val id = "h${nextId++}"
        entries = entries + CoachingHistoryEntry(
            id = id,
            gameTimeMillis = gameTimeMillis,
            homeScore = homeScore,
            guestScore = guestScore,
            type = HistoryEventType.ROOT_CAUSE,
            criterionId = criterionId,
            defectGroupId = defectGroupId,
            rootCauseId = rootCauseId,
            selected = selected,
        )
        return id
    }

    /** Records a goal (or goal correction) event and returns its generated id. */
    fun recordGoal(
        gameTimeMillis: Long,
        homeScore: Int,
        guestScore: Int,
        team: RosterTeam,
        scored: Boolean = true,
    ): String {
        val id = "h${nextId++}"
        entries = entries + CoachingHistoryEntry(
            id = id,
            gameTimeMillis = gameTimeMillis,
            homeScore = homeScore,
            guestScore = guestScore,
            type = HistoryEventType.GOAL,
            goalTeam = team,
            selected = scored,
        )
        return id
    }

    /** Sets (or clears) the attachment for the entry with [entryId]. */
    fun attach(entryId: String, attachment: CoachingHistoryAttachment?) {
        entries = entries.map { entry ->
            if (entry.id == entryId) entry.copy(attachment = attachment?.takeUnless { it.isEmpty }) else entry
        }
    }

    /** Updates the free-text note of the entry with [entryId]. */
    fun setNote(entryId: String, note: String) {
        entries = entries.map { entry ->
            if (entry.id == entryId) entry.copy(note = note) else entry
        }
    }

    fun find(entryId: String): CoachingHistoryEntry? = entries.firstOrNull { it.id == entryId }

    /** Restores the history from persisted state, preserving entry ids. */
    fun restore(restored: List<CoachingHistoryEntry>) {
        entries = restored
        nextId = (restored.mapNotNull { it.id.removePrefix("h").toIntOrNull() }.maxOrNull() ?: -1) + 1
    }

    fun clear() {
        entries = emptyList()
    }
}


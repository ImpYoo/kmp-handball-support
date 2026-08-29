package de.exhumedo.kmp.handball_support.referee_coaching.domain.model

/**
 * One chronological entry in the referee coaching observation history.
 *
 * Kept intentionally flat so it can be serialized and stored without coupling
 * to the Compose/UI presenter.
 */
data class CoachingHistoryEntry(
    val id: String,
    val gameTimeMillis: Long,
    val homeScore: Int,
    val guestScore: Int,
    val type: CoachingHistoryEventType,
    val criterionId: String? = null,
    val defectGroupId: String? = null,
    val rootCauseId: String? = null,
    val goalTeam: String? = null,
    val selected: Boolean = true,
    val team: String? = null,
    val teamLabel: String? = null,
    val playerId: String? = null,
    val playerLabel: String? = null,
    val refereeName: String? = null,
    val note: String = "",
)

enum class CoachingHistoryEventType { ROOT_CAUSE, GOAL }

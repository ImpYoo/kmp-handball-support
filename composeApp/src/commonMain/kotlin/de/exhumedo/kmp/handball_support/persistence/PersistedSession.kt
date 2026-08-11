package de.exhumedo.kmp.handball_support.persistence

import kotlinx.serialization.Serializable

/**
 * Serializable snapshot of a coaching session, persisted across app restarts /
 * browser reloads. Domain/presenter types are mapped to these flat DTOs so the
 * persisted format stays stable and decoupled.
 */
@Serializable
data class PersistedSession(
    val matchSetup: PersistedMatchSetup = PersistedMatchSetup(),
    val homePlayers: List<PersistedPlayer> = emptyList(),
    val guestPlayers: List<PersistedPlayer> = emptyList(),
    val homeScore: Int = 0,
    val guestScore: Int = 0,
    val stopwatchElapsedMillis: Long = 0L,
    val history: List<PersistedHistoryEntry> = emptyList(),
    val criterionCounts: List<PersistedRootCauseCount> = emptyList(),
)

@Serializable
data class PersistedMatchSetup(
    val homeTeamName: String = "",
    val homeTeamAbbreviation: String = "",
    val guestTeamName: String = "",
    val guestTeamAbbreviation: String = "",
    val firstRefereeName: String = "",
    val secondRefereeName: String = "",
)

@Serializable
data class PersistedPlayer(
    val id: String,
    val number: String,
    val name: String,
)

@Serializable
data class PersistedRootCauseCount(
    val criterionId: String,
    val groupId: String,
    val rootCauseId: String,
    val count: Int,
)

@Serializable
data class PersistedAttachment(
    val team: String? = null,
    val teamLabel: String? = null,
    val playerId: String? = null,
    val playerLabel: String? = null,
    val refereeName: String? = null,
)

@Serializable
data class PersistedHistoryEntry(
    val id: String,
    val gameTimeMillis: Long,
    val homeScore: Int,
    val guestScore: Int,
    val type: String = "ROOT_CAUSE",
    val criterionId: String? = null,
    val defectGroupId: String? = null,
    val rootCauseId: String? = null,
    val goalTeam: String? = null,
    val selected: Boolean = true,
    val attachment: PersistedAttachment? = null,
    val note: String = "",
)




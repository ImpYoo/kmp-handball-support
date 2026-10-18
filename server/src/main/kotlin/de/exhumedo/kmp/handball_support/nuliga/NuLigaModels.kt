package de.exhumedo.kmp.handball_support.nuliga

import java.time.Instant

/**
 * The resolved match, mapped from nuLiga's payloads into the server's own shape. The API
 * layer turns this into its response DTOs; nothing here knows about HTTP.
 *
 * Ported from the predecessor's ResolvedMatch (org.schaltwerk.handball), which was
 * reverse-engineered from the nuScoreLive REST backend — the semantics documented there
 * (personType roles, the ±offset timestamp quirk, IHF letters in `nr`) are preserved.
 */
data class NuLigaResolvedMatch(
    /** Canonical meeting id at nuLiga, e.g. `8278820`. */
    val sourceMatchId: String,
    val scheduledAt: Instant?,
    val isLive: Boolean,
    val periods: Int,
    val minutesPerPeriod: Int,
    val competitionName: String?,
    val venueName: String?,
    val home: NuLigaResolvedTeam,
    val away: NuLigaResolvedTeam,
    val referees: List<NuLigaMatchPerson>,
    val tableOfficials: List<NuLigaMatchPerson>,
    val supervisors: List<NuLigaMatchPerson>,
)

data class NuLigaResolvedTeam(
    val name: String,
    val logoUrl: String?,
    val players: List<NuLigaPlayer>,
    val officials: List<NuLigaOfficial>,
)

data class NuLigaPlayer(
    val id: String,
    val firstName: String,
    val lastName: String,
    val number: Int?,
)

data class NuLigaOfficial(
    val id: String,
    val firstName: String,
    val lastName: String,
    /** IHF designation A–E, which nuLiga publishes in the `nr` field for officials. */
    val badge: String?,
)

data class NuLigaMatchPerson(
    val id: String,
    val firstName: String,
    val lastName: String,
    val roleCode: String,
    val position: Int,
)
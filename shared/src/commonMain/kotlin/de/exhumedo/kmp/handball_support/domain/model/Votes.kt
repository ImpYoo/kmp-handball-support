package de.exhumedo.kmp.handball_support.domain.model

data class VoteByReferees(
    val id: String,
    val matchId: Int,
    val phaseId: Int,
    val phaseName: PhaseName,
    val timestamp: Long,
    val homeTeam: Team,
    val awayTeam: Team,
    val refereeA: Person,
    val refereeB: Person,
    val delegate: Person?,
    val timekeeper: Person,
    val scorekeeper: Person,
    val isVoteByDelegate: Boolean,
    val appearanceRating: Int,
    val influenceRating: Int,
    val teamworkRating: Int,
    val comment: String = "",
) {
    companion object {
        fun refereeVoteId(matchId: Int, refereeAId: Int, refereeBId: Int): String =
            "$matchId-$refereeAId-$refereeBId"

        fun delegateVoteId(matchId: Int, delegateId: Int): String =
            "$matchId-$delegateId"
    }
}


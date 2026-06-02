package de.exhumedo.kmp.handball_support.ui

import de.exhumedo.kmp.handball_support.client.HasVoteByDto
import de.exhumedo.kmp.handball_support.client.MatchDayResponseDto
import de.exhumedo.kmp.handball_support.client.MatchPersonDto
import de.exhumedo.kmp.handball_support.client.MatchResponseDto
import de.exhumedo.kmp.handball_support.client.PhaseResponseDto
import de.exhumedo.kmp.handball_support.client.TeamDto
import de.exhumedo.kmp.handball_support.vote.VoteAppPresenter
import de.exhumedo.kmp.handball_support.vote.VoteEvaluatorType

internal val previewMatches: List<MatchResponseDto> = listOf(
    MatchResponseDto(
        id = 100001,
        phaseId = 76123,
        timestamp = 1_750_000_000_000L,
        homeTeam = TeamDto(id = 1, name = "THW Kiel"),
        awayTeam = TeamDto(id = 2, name = "Fuchse Berlin"),
        refereeA = MatchPersonDto(id = 101, name = "Alex Ref A"),
        refereeB = MatchPersonDto(id = 102, name = "Alex Ref B"),
        timekeeper = MatchPersonDto(id = 201, name = "Taylor Time"),
        scorekeeper = MatchPersonDto(id = 202, name = "Sam Score"),
        delegate = MatchPersonDto(id = 301, name = "Dana Delegate"),
        result = "32:28",
        halftimeResult = "15:14",
        hasVoteBy = HasVoteByDto(refereeTeam = false, delegate = false),
    ),
    MatchResponseDto(
        id = 100002,
        phaseId = 76123,
        timestamp = 1_750_086_400_000L,
        homeTeam = TeamDto(id = 3, name = "Rhein-Neckar Lowen"),
        awayTeam = TeamDto(id = 4, name = "SC Magdeburg"),
        refereeA = MatchPersonDto(id = 103, name = "Robin Ref A"),
        refereeB = MatchPersonDto(id = 104, name = "Robin Ref B"),
        timekeeper = MatchPersonDto(id = 203, name = "Casey Time"),
        scorekeeper = MatchPersonDto(id = 204, name = "Jordan Score"),
        delegate = null,
        result = "26:30",
        halftimeResult = "13:14",
        hasVoteBy = HasVoteByDto(refereeTeam = true, delegate = false),
    ),
)

internal val previewPhases: List<PhaseResponseDto> = listOf(
    PhaseResponseDto(
        phaseId = 76123,
        tournamentId = 921,
        seasonId = 33765,
        name = "DAIKIN HBL 2025/26 - Matchday 1",
        shortName = "MD 1",
        matchDays = listOf(MatchDayResponseDto(id = 1, matches = previewMatches)),
    ),
    PhaseResponseDto(
        phaseId = 76124,
        tournamentId = 921,
        seasonId = 33765,
        name = "DAIKIN HBL 2025/26 - Matchday 2",
        shortName = "MD 2",
    ),
)

internal fun previewPresenter(configure: VoteAppPresenter.() -> Unit = {}): VoteAppPresenter {
    val presenter = VoteAppPresenter()
    presenter.phases = previewPhases
    presenter.configure()
    return presenter
}

internal fun VoteAppPresenter.withLoadedData(selectedMatchIndex: Int = 0): VoteAppPresenter {
    token = "preview-token"
    role = "ADMIN"
    phases = previewPhases
    selectedPhaseId = previewPhases.first().phaseId
    matches = previewMatches
    selectedMatch = previewMatches.getOrNull(selectedMatchIndex)
    evaluatorType = VoteEvaluatorType.REFEREE_TEAM
    appearance = "4"
    influence = "5"
    teamwork = "3"
    comment = "Good communication and positioning"
    statusMessage = "Preview state"
    return this
}


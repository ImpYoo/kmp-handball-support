package de.exhumedo.kmp.handball_support.domain.rating.model

internal fun person(id: String): Person = Person(
    id = id,
    firstName = "First$id",
    lastName = "Last$id",
)

internal fun game(): Game = Game(
    gameId = "game-1",
    date = "2026-04-06",
    homeTeam = "Home Team",
    awayTeam = "Away Team",
    venue = "Main Hall",
)

internal fun evaluationScore(): EvaluationScore = EvaluationScore(
    appearance = Score(7),
    influence = Score(8),
    teamwork = Score(7),
)

internal fun refereePair(): RefereePair = RefereePair(
    firstReferee = RoleAssignment(person("r1"), OfficialRole.FirstReferee),
    secondReferee = RoleAssignment(person("r2"), OfficialRole.SecondReferee),
)

internal fun refereeTeamEvaluator(): Evaluator = Evaluator.RefereeTeam(refereePair())

internal fun delegateEvaluator(): Evaluator = Evaluator.Delegate(
    RoleAssignment(person("d1"), OfficialRole.Delegate),
)

internal fun tableOfficialTeam(): TableOfficialTeam = TableOfficialTeam(
    timeKeeper = RoleAssignment(person("t1"), OfficialRole.TimeKeeper),
    scoreKeeper = RoleAssignment(person("t2"), OfficialRole.ScoreKeeper),
    delegate = RoleAssignment(person("t3"), OfficialRole.Delegate),
)

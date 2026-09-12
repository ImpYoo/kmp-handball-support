package de.exhumedo.kmp.handball_support.navigation

/** Human-readable URL path helpers for [AppRoute]. */
fun phasesPath(day: Int?, month: Int?, year: Int?, showFilter: Boolean = false): String =
    "phases" + phasesQuery(day, month, year, showFilter)

private fun phasesQuery(day: Int?, month: Int?, year: Int?, showFilter: Boolean = false): String =
    queryString("day" to day, "month" to month, "year" to year, "showFilter" to showFilter.takeIf { it })

private fun filterQuery(day: Int?, month: Int?, year: Int?): String =
    queryString("day" to day, "month" to month, "year" to year)

private fun queryString(vararg params: Pair<String, Any?>): String {
    val parts = params.mapNotNull { (k, v) -> v?.let { "$k=$it" } }
    return if (parts.isEmpty()) "" else "?" + parts.joinToString("&")
}

/** Converts a route to the shared browser-path representation. */
fun AppRoute.toPath(): String = when (this) {
    is AppRoute.Home -> ""
    is AppRoute.Phases -> phasesPath(day, month, year, showFilter)
    is AppRoute.Login -> "login" + queryString(
        "phaseId" to phaseId,
        "matchId" to matchId,
        "day" to day,
        "month" to month,
        "year" to year,
    )
    is AppRoute.PhaseDetail -> "phases/$phaseId" + filterQuery(day, month, year)
    is AppRoute.Vote -> "phases/$phaseId/matches/$matchId" + filterQuery(day, month, year)
    is AppRoute.CoachingList -> "coaching-list" + queryString("tab" to tab)
    AppRoute.Admin -> "admin"
    AppRoute.Settings -> "settings"
    AppRoute.ChangePassword -> "change-password"
    AppRoute.RefereeCoaching -> "coaching"
    is AppRoute.CoachingSession -> "session" + queryString("evaluationId" to evaluationId)
    is AppRoute.CoachingReport -> "coaching-report/$evaluationId"
    AppRoute.CoachingSheet -> "sheet"
    AppRoute.MatchConsole -> "clock"
    AppRoute.Roster -> "roster"
    AppRoute.MatchSetup -> "setup"
    AppRoute.DrawingPad -> "drawing"
    is AppRoute.TacticBoard -> "tactic" + queryString("debug" to debug.takeIf { it })
}

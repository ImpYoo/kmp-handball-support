package de.exhumedo.kmp.handball_support.navigation

/** Parsed URL query parameters plus the [AppRoute] they map to. */
data class ParsedDeepLink(
    val route: AppRoute,
    val day: Int?,
    val month: Int?,
    val year: Int?,
    val showFilter: Boolean,
)

/** Parses a deep-link string (no leading slash, may contain a query) into a [ParsedDeepLink]. */
fun parseDeepLink(raw: String): ParsedDeepLink {
    val (path, query) = raw.splitOnce('?')
    val params = parseQuery(query)
    val day = params["day"]?.toIntOrNull()
    val month = params["month"]?.toIntOrNull()
    val year = params["year"]?.toIntOrNull()
    val showFilter = params["showFilter"]?.equals("true", ignoreCase = true) ?: false

    val normalized = path.trim('/').ifEmpty { "home" }
    val phaseAndMatch = "^phases/(\\d+)/matches/(\\d+)$".toRegex().matchEntire(normalized)
    val phaseOnly = "^phases/(\\d+)$".toRegex().matchEntire(normalized)

    val route: AppRoute = when {
        phaseAndMatch != null -> AppRoute.Vote(
            phaseId = phaseAndMatch.groupValues[1].toInt(),
            matchId = phaseAndMatch.groupValues[2].toInt(),
            day = day,
            month = month,
            year = year,
        )
        phaseOnly != null -> AppRoute.PhaseDetail(
            phaseId = phaseOnly.groupValues[1].toInt(),
            day = day,
            month = month,
            year = year,
        )
        normalized == "login" -> AppRoute.Login(
            phaseId = params["phaseId"]?.toIntOrNull(),
            matchId = params["matchId"]?.toIntOrNull(),
            day = day,
            month = month,
            year = year,
        )
        normalized == "phases" -> AppRoute.Phases(day = day, month = month, year = year, showFilter = showFilter)
        normalized == "coaching" -> AppRoute.RefereeCoaching
        normalized == "session" -> AppRoute.CoachingSession
        normalized == "sheet" -> AppRoute.CoachingSheet
        normalized == "clock" -> AppRoute.MatchConsole
        normalized == "roster" -> AppRoute.Roster
        normalized == "setup" -> AppRoute.MatchSetup
        normalized == "drawing" -> AppRoute.DrawingPad
        normalized == "tactic" -> AppRoute.TacticBoard
        else -> AppRoute.Home
    }

    return ParsedDeepLink(route = route, day = day, month = month, year = year, showFilter = showFilter)
}

fun String.splitOnce(separator: Char): Pair<String, String> {
    val idx = indexOf(separator)
    return if (idx < 0) this to "" else substring(0, idx) to substring(idx + 1)
}

fun parseQuery(query: String): Map<String, String> {
    if (query.isBlank()) return emptyMap()
    return query.split('&')
        .mapNotNull { part ->
            if (part.isBlank()) return@mapNotNull null
            val idx = part.indexOf('=')
            if (idx < 0) part to "" else part.substring(0, idx) to part.substring(idx + 1)
        }
        .toMap()
}

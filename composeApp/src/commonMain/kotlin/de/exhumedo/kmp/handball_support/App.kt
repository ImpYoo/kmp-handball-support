package de.exhumedo.kmp.handball_support

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import de.exhumedo.kmp.handball_support.navigation.bindBrowserNavigation
import de.exhumedo.kmp.handball_support.navigation.initialDeepLink
import de.exhumedo.kmp.handball_support.ui.LoadingOverlay
import de.exhumedo.kmp.handball_support.ui.LoginScreen
import de.exhumedo.kmp.handball_support.ui.PhaseDetailScreen
import de.exhumedo.kmp.handball_support.ui.PhasesScreen
import de.exhumedo.kmp.handball_support.ui.VoteFormScreen
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.vote.UiEvent
import de.exhumedo.kmp.handball_support.vote.VoteAppPresenter
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Composable
@Preview
fun App() {
    val presenter = remember { VoteAppPresenter() }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show the loading overlay immediately on first composition so the very first
    // thing users see is the three-dot loader (not a blank page). The overlay's
    // `minDisplayDuration` keeps it on screen for the configured time even after
    // we flip this flag back to false.
    var initialLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        initialLoading = false
    }

    LaunchedEffect(navController) {
        bindBrowserNavigation(navController) { entry ->
            when (val route = entry.destination.route) {
                null -> ""
                else -> when {
                    route.startsWith("phases?") -> {
                        val phases = entry.toRoute<AppRoute.Phases>()
                        phasesPath(phases.day, phases.month, phases.year)
                    }
                    route.startsWith("login") -> {
                        val login = entry.toRoute<AppRoute.Login>()
                        "login" + queryString(
                            "phaseId" to login.phaseId,
                            "matchId" to login.matchId,
                            "day" to login.day,
                            "month" to login.month,
                            "year" to login.year,
                        )
                    }
                    route.startsWith("phase/") -> {
                        val detail = entry.toRoute<AppRoute.PhaseDetail>()
                        "phases/${detail.phaseId}" + filterQuery(detail.day, detail.month, detail.year)
                    }
                    route.startsWith("vote/") -> {
                        val vote = entry.toRoute<AppRoute.Vote>()
                        "phases/${vote.phaseId}/matches/${vote.matchId}" +
                            filterQuery(vote.day, vote.month, vote.year)
                    }
                    else -> ""
                }
            }
        }
    }

    LaunchedEffect(navController) {
        withFrameNanos { }
        applyInitialDeepLink(presenter, navController)
    }

    LaunchedEffect(presenter, navController) {
        presenter.events.collect { event ->
            when (event) {
                is UiEvent.Error -> snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Long,
                )
                is UiEvent.Info -> snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short,
                )
                UiEvent.SessionExpired -> {
                    navController.navigateSingleTop(AppRoute.Login())
                    snackbarHostState.showSnackbar(
                        message = "Session expired. Please sign in again.",
                        duration = SnackbarDuration.Long,
                    )
                }
            }
        }
    }

    AppTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                NavHost(
                    navController = navController,
                    startDestination = "phases",
                ) {
                        composable<AppRoute.Phases> { entry ->
                            val route = entry.toRoute<AppRoute.Phases>()
                            LaunchedEffect(route.day, route.month, route.year) {
                                presenter.setDateFilter(day = route.day, month = route.month, year = route.year)
                            }

                            // Reflect filter changes made from the UI in the URL.
                            LaunchedEffect(presenter.filterDay, presenter.filterMonth, presenter.filterYear) {
                                val day = presenter.filterDay
                                val month = presenter.filterMonth
                                val year = presenter.filterYear
                                if (day != route.day || month != route.month || year != route.year) {
                                    navController.navigateSingleTop(AppRoute.Phases(day, month, year))
                                }
                            }

                            PhasesScreen(
                                presenter = presenter,
                                onAction = { action ->
                                    scope.launch {
                                        action()
                                        presenter.selectedPhaseId?.let { phaseId ->
                                            navController.navigateSingleTop(
                                                AppRoute.PhaseDetail(
                                                    phaseId = phaseId,
                                                    day = presenter.filterDay,
                                                    month = presenter.filterMonth,
                                                    year = presenter.filterYear,
                                                ),
                                            )
                                        }
                                    }
                                },
                            )
                        }

                        composable<AppRoute.Login> { entry ->
                            val login = entry.toRoute<AppRoute.Login>()

                            LaunchedEffect(login.day, login.month, login.year) {
                                presenter.setDateFilter(day = login.day, month = login.month, year = login.year)
                            }

                            // Restore pending-vote target from the URL so a refresh on /login keeps
                            // the user heading to the right Vote screen after sign-in.
                            LaunchedEffect(login.phaseId, login.matchId) {
                                if (login.phaseId != null && login.matchId != null) {
                                    presenter.markVotePending(login.phaseId, login.matchId)
                                }
                            }

                            LoginScreen(
                                presenter = presenter,
                                onAction = { action ->
                                    scope.launch {
                                        action()
                                        if (presenter.token == null) return@launch
                                        val phaseId = presenter.selectedPhaseId ?: login.phaseId
                                        val matchId = presenter.selectedMatch?.id ?: login.matchId
                                        when {
                                            phaseId != null && matchId != null ->
                                                navController.navigateSingleTop(
                                                    AppRoute.Vote(
                                                        phaseId = phaseId,
                                                        matchId = matchId,
                                                        day = presenter.filterDay,
                                                        month = presenter.filterMonth,
                                                        year = presenter.filterYear,
                                                    ),
                                                )
                                            phaseId != null ->
                                                navController.navigateSingleTop(
                                                    AppRoute.PhaseDetail(
                                                        phaseId = phaseId,
                                                        day = presenter.filterDay,
                                                        month = presenter.filterMonth,
                                                        year = presenter.filterYear,
                                                    ),
                                                )
                                            else ->
                                                navController.navigateSingleTop(
                                                    phasesPath(presenter.filterDay, presenter.filterMonth, presenter.filterYear),
                                                )
                                        }
                                    }
                                },
                                onBackToPhases = {
                                    presenter.cancelPendingVote()
                                    navController.navigateSingleTop(
                                        phasesPath(presenter.filterDay, presenter.filterMonth, presenter.filterYear),
                                    )
                                },
                            )
                        }

                        composable<AppRoute.PhaseDetail> { entry ->
                            val detail = entry.toRoute<AppRoute.PhaseDetail>()
                            val phaseId = detail.phaseId

                            LaunchedEffect(detail.day, detail.month, detail.year) {
                                presenter.setDateFilter(day = detail.day, month = detail.month, year = detail.year)
                            }

                            LaunchedEffect(phaseId) {
                                if (presenter.selectedPhaseId != phaseId || presenter.matches.isEmpty()) {
                                    presenter.openPhase(phaseId)
                                }
                            }

                            LaunchedEffect(presenter.pendingMatchId, presenter.token) {
                                if (presenter.token == null && presenter.pendingMatchId != null) {
                                    navController.navigateSingleTop(
                                        AppRoute.Login(
                                            phaseId = presenter.pendingPhaseId,
                                            matchId = presenter.pendingMatchId,
                                            day = presenter.filterDay,
                                            month = presenter.filterMonth,
                                            year = presenter.filterYear,
                                        ),
                                    )
                                }
                            }

                            LaunchedEffect(presenter.selectedMatch?.id, presenter.token, phaseId) {
                                val matchId = presenter.selectedMatch?.id ?: return@LaunchedEffect
                                if (presenter.token != null && presenter.selectedPhaseId == phaseId) {
                                    navController.navigateSingleTop(
                                        AppRoute.Vote(
                                            phaseId = phaseId,
                                            matchId = matchId,
                                            day = presenter.filterDay,
                                            month = presenter.filterMonth,
                                            year = presenter.filterYear,
                                        ),
                                    )
                                }
                            }

                            PhaseDetailScreen(
                                presenter = presenter,
                                onBack = {
                                    presenter.selectedMatch = null
                                    navController.navigateSingleTop(
                                        phasesPath(presenter.filterDay, presenter.filterMonth, presenter.filterYear),
                                    )
                                },
                            )
                        }

                        composable<AppRoute.Vote> { entry ->
                            val route = entry.toRoute<AppRoute.Vote>()
                            val phaseId = route.phaseId
                            val matchId = route.matchId

                            LaunchedEffect(route.day, route.month, route.year) {
                                presenter.setDateFilter(day = route.day, month = route.month, year = route.year)
                            }

                            LaunchedEffect(phaseId, matchId, presenter.token) {
                                if (presenter.selectedPhaseId != phaseId || presenter.matches.none { it.id == matchId }) {
                                    presenter.openPhase(phaseId)
                                }
                                val match = presenter.matches.firstOrNull { it.id == matchId } ?: return@LaunchedEffect
                                if (presenter.token == null) {
                                    presenter.markVotePending(phaseId, matchId)
                                    navController.navigateSingleTop(
                                        AppRoute.Login(
                                            phaseId = phaseId,
                                            matchId = matchId,
                                            day = presenter.filterDay,
                                            month = presenter.filterMonth,
                                            year = presenter.filterYear,
                                        ),
                                    )
                                    return@LaunchedEffect
                                }
                                if (presenter.selectedMatch?.id != matchId) {
                                    presenter.chooseMatch(match)
                                }
                            }

                            VoteFormScreen(
                                presenter = presenter,
                                onAction = { action -> scope.launch { action() } },
                                onBack = {
                                    val phaseRoute = presenter.selectedPhaseId
                                    presenter.selectedMatch = null
                                    if (phaseRoute != null) {
                                        navController.navigateSingleTop(
                                            AppRoute.PhaseDetail(
                                                phaseId = phaseRoute,
                                                day = presenter.filterDay,
                                                month = presenter.filterMonth,
                                                year = presenter.filterYear,
                                            ),
                                        )
                                    } else {
                                        navController.navigateSingleTop(
                                            phasesPath(presenter.filterDay, presenter.filterMonth, presenter.filterYear),
                                        )
                                    }
                                },
                            )
                        }
                }
                // Loading overlay that appears on top of all content
                LoadingOverlay(
                    isVisible = initialLoading || presenter.isBusy,
                    statusMessage = if (initialLoading) "Loading..." else presenter.statusMessage,
                )
            }
        }
    }
}

@Serializable
private sealed interface AppRoute {
    @Serializable
    @SerialName("phases")
    data class Phases(
        val day: Int? = null,
        val month: Int? = null,
        val year: Int? = null,
    ) : AppRoute

    @Serializable
    @SerialName("login")
    data class Login(
        val phaseId: Int? = null,
        val matchId: Int? = null,
        val day: Int? = null,
        val month: Int? = null,
        val year: Int? = null,
    ) : AppRoute

    @Serializable
    @SerialName("phase")
    data class PhaseDetail(
        val phaseId: Int,
        val day: Int? = null,
        val month: Int? = null,
        val year: Int? = null,
    ) : AppRoute

    @Serializable
    @SerialName("vote")
    data class Vote(
        val phaseId: Int,
        val matchId: Int,
        val day: Int? = null,
        val month: Int? = null,
        val year: Int? = null,
    ) : AppRoute
}

private fun phasesPath(day: Int?, month: Int?, year: Int?): String =
    "phases" + filterQuery(day, month, year)

private fun filterQuery(day: Int?, month: Int?, year: Int?): String =
    queryString("day" to day, "month" to month, "year" to year)

private fun queryString(vararg params: Pair<String, Any?>): String {
    val parts = params.mapNotNull { (k, v) -> v?.let { "$k=$it" } }
    return if (parts.isEmpty()) "" else "?" + parts.joinToString("&")
}

private fun NavHostController.navigateSingleTop(route: Any) {
    navigate(route = route) { launchSingleTop = true }
}

private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route = route) { launchSingleTop = true }
}

/**
 * Reads the platform deep link and navigates to the matching route. Supports hash-style
 * (`#phases/...`) and legacy path-style (`/phases/...`) URLs.
 */
private suspend fun applyInitialDeepLink(
    presenter: VoteAppPresenter,
    navController: NavController,
) {
    val raw = initialDeepLink().ifBlank { return }
    val (path, query) = raw.splitOnce('?')
    val params = parseQuery(query)
    val day = params["day"]?.toIntOrNull()
    val month = params["month"]?.toIntOrNull()
    val year = params["year"]?.toIntOrNull()

    presenter.setDateFilter(day = day, month = month, year = year)

    val normalized = path.trim('/').ifEmpty { "phases" }
    val phaseAndMatch = "^phases/(\\d+)/matches/(\\d+)$".toRegex().matchEntire(normalized)
    val phaseOnly = "^phases/(\\d+)$".toRegex().matchEntire(normalized)

    when {
        phaseAndMatch != null -> {
            val phaseId = phaseAndMatch.groupValues[1].toInt()
            val matchId = phaseAndMatch.groupValues[2].toInt()
            navController.navigate(
                AppRoute.Vote(phaseId = phaseId, matchId = matchId, day = day, month = month, year = year),
            ) { launchSingleTop = true }
        }
        phaseOnly != null -> {
            val phaseId = phaseOnly.groupValues[1].toInt()
            navController.navigate(
                AppRoute.PhaseDetail(phaseId = phaseId, day = day, month = month, year = year),
            ) { launchSingleTop = true }
        }
        normalized == "login" -> {
            navController.navigate(
                AppRoute.Login(
                    phaseId = params["phaseId"]?.toIntOrNull(),
                    matchId = params["matchId"]?.toIntOrNull(),
                    day = day,
                    month = month,
                    year = year,
                ),
            ) { launchSingleTop = true }
        }
        normalized == "phases" -> {
            navController.navigate(
                phasesPath(presenter.filterDay, presenter.filterMonth, presenter.filterYear),
            ) { launchSingleTop = true }
        }
    }
}

private fun String.splitOnce(separator: Char): Pair<String, String> {
    val idx = indexOf(separator)
    return if (idx < 0) this to "" else substring(0, idx) to substring(idx + 1)
}

private fun parseQuery(query: String): Map<String, String> {
    if (query.isBlank()) return emptyMap()
    return query.split('&')
        .mapNotNull { part ->
            if (part.isBlank()) return@mapNotNull null
            val idx = part.indexOf('=')
            if (idx < 0) part to "" else part.substring(0, idx) to part.substring(idx + 1)
        }
        .toMap()
}


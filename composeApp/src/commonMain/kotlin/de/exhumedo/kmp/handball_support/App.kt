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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import de.exhumedo.kmp.handball_support.client.CoachingApiClient
import de.exhumedo.kmp.handball_support.coaching.CoachingHistoryPresenter
import de.exhumedo.kmp.handball_support.coaching.CoachingSessionSync
import de.exhumedo.kmp.handball_support.coaching.RefereeCoachingPresenter
import de.exhumedo.kmp.handball_support.config.AppConfig
import de.exhumedo.kmp.handball_support.config.AppVariant
import de.exhumedo.kmp.handball_support.matchconsole.MatchSetupPresenter
import de.exhumedo.kmp.handball_support.matchconsole.RosterPresenter
import de.exhumedo.kmp.handball_support.matchconsole.ScoreboardPresenter
import de.exhumedo.kmp.handball_support.matchconsole.StopwatchPresenter
import de.exhumedo.kmp.handball_support.matchconsole.TacticBoardPresenter
import de.exhumedo.kmp.handball_support.navigation.AppRoute
import de.exhumedo.kmp.handball_support.navigation.phasesPath
import de.exhumedo.kmp.handball_support.navigation.rememberAppNavigator
import de.exhumedo.kmp.handball_support.persistence.DrawingPadPresenter
import de.exhumedo.kmp.handball_support.persistence.SessionManager
import de.exhumedo.kmp.handball_support.persistence.SessionMapper
import de.exhumedo.kmp.handball_support.persistence.drawingStorage
import de.exhumedo.kmp.handball_support.persistence.tacticStorage
import de.exhumedo.kmp.handball_support.ui.AdminScreen
import de.exhumedo.kmp.handball_support.ui.ApplicationSelectionScreen
import de.exhumedo.kmp.handball_support.ui.ChangePasswordScreen
import de.exhumedo.kmp.handball_support.ui.CoachingListScreen
import de.exhumedo.kmp.handball_support.ui.CoachingSessionScreen
import de.exhumedo.kmp.handball_support.ui.CoachingSetupScreen
import de.exhumedo.kmp.handball_support.ui.CoachingSheetScreen
import de.exhumedo.kmp.handball_support.ui.DrawingPadScreen
import de.exhumedo.kmp.handball_support.ui.LoginScreen
import de.exhumedo.kmp.handball_support.ui.MatchConsoleScreen
import de.exhumedo.kmp.handball_support.ui.MatchSetupScreen
import de.exhumedo.kmp.handball_support.ui.PhaseDetailScreen
import de.exhumedo.kmp.handball_support.ui.PhasesScreen
import de.exhumedo.kmp.handball_support.ui.RosterScreen
import de.exhumedo.kmp.handball_support.ui.SettingsScreen
import de.exhumedo.kmp.handball_support.ui.TacticBoardScreen
import de.exhumedo.kmp.handball_support.ui.UserAdminScreen
import de.exhumedo.kmp.handball_support.ui.VoteFormScreen
import de.exhumedo.kmp.handball_support.ui.screens.CoachingEvaluationReadOnlyScreen
import de.exhumedo.kmp.handball_support.ui.screens.LoadingOverlay
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.coaching.restoreCoachingSessionFromReport
import de.exhumedo.kmp.handball_support.vote.UiEvent
import de.exhumedo.kmp.handball_support.vote.VoteAppPresenter
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@Composable
@Preview
fun App() {
    val presenter = remember { VoteAppPresenter() }
    val coachingPresenter = remember { RefereeCoachingPresenter() }
    val coachingHistoryPresenter = remember { CoachingHistoryPresenter() }
    val coachingSync = remember { CoachingSessionSync() }
    val stopwatchPresenter = remember { StopwatchPresenter() }
    val scoreboardPresenter = remember { ScoreboardPresenter() }
    val rosterPresenter = remember { RosterPresenter() }
    val matchSetupPresenter = remember { MatchSetupPresenter() }
    val tacticBoardPresenter = remember { TacticBoardPresenter(tacticStorage()) }
    val drawingPadPresenter = remember { DrawingPadPresenter(drawingStorage()) }
    val sessionManager = remember { SessionManager() }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val navigator = rememberAppNavigator(navController, presenter)

    // Restore a persisted session on first composition, then auto-save (debounced)
    // whenever any session state changes so coaching survives restarts / reloads.
    LaunchedEffect(Unit) {
        sessionManager.restoreInto(
            coaching = coachingPresenter,
            history = coachingHistoryPresenter,
            stopwatch = stopwatchPresenter,
            scoreboard = scoreboardPresenter,
            roster = rosterPresenter,
            matchSetup = matchSetupPresenter,
        )
        snapshotFlow {
            SessionMapper.capture(
                coaching = coachingPresenter,
                history = coachingHistoryPresenter,
                stopwatch = stopwatchPresenter,
                scoreboard = scoreboardPresenter,
                roster = rosterPresenter,
                matchSetup = matchSetupPresenter,
            )
        }
            .distinctUntilChanged()
            .debounce(600)
            .collect { session -> sessionManager.save(session) }
    }

    // Online auto-save to the coaching REST API whenever the session has an identity.
    // Stable LaunchedEffect keys: only restart when token/baseUrl presence toggles.
    val baseUrl = AppConfig.baseApiUrl
    val token = presenter.token
    LaunchedEffect(token?.isNotBlank(), baseUrl.isNotBlank()) {
        if (token.isNullOrBlank() || baseUrl.isBlank()) {
            coachingSync.stopAutoSave()
            return@LaunchedEffect
        }
        // Drain any offline-queued evaluations before starting live auto-save.
        if (coachingSync.hasPending()) {
            coachingSync.drainQueue(baseUrl = baseUrl, token = token)
        }
        coachingSync.startAutoSave(
            scope = this,
            baseUrl = baseUrl,
            token = token,
            snapshotFlow = snapshotFlow {
                coachingSync.buildRequest(
                    gameId = matchSetupPresenter.gameId.ifBlank {
                        "${matchSetupPresenter.homeTeamName}-${matchSetupPresenter.guestTeamName}-${matchSetupPresenter.matchDate}"
                    },
                    matchDate = matchSetupPresenter.matchDate,
                    homeTeam = matchSetupPresenter.homeTeamName,
                    awayTeam = matchSetupPresenter.guestTeamName,
                    evaluatorUsername = presenter.username,
                    firstRefereeName = matchSetupPresenter.firstRefereeName,
                    secondRefereeName = matchSetupPresenter.secondRefereeName,
                    criteria = coachingPresenter.criteria,
                    comment = "",
                    history = coachingHistoryPresenter.entries,
                )
            },
        )
    }

    // Show the loading overlay immediately on first composition so the very first
    // thing users see is the three-dot loader (not a blank page). The overlay's
    // `minDisplayDuration` keeps it on screen for the configured time even after
    // we flip this flag back to false.
    var initialLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        initialLoading = false
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
                    if (AppVariant.showRating) {
                        navController.navigate(AppRoute.Login()) { launchSingleTop = true }
                    }
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
                    startDestination = AppRoute.Home,
                ) {
                    composable<AppRoute.Home> {
                        ApplicationSelectionScreen(
                            username = presenter.username,
                            isLoggedIn = !presenter.token.isNullOrBlank(),
                            onOpenSettings = { navigator.navigate(AppRoute.Settings) },
                            onLogout = { presenter.logout() },
                            onLogin = { navigator.navigate(AppRoute.RefereeCoaching) },
                            onOpenPhases = {
                                navigator.navigate(
                                    AppRoute.Phases(
                                        day = presenter.filterDay,
                                        month = presenter.filterMonth,
                                        year = presenter.filterYear,
                                    ),
                                )
                            },
                            onOpenCoaching = { navigator.navigate(AppRoute.RefereeCoaching) },
                            onOpenCoachingSheet = { navigator.navigate(AppRoute.CoachingSheet) },
                            onOpenMatchConsole = { navigator.navigate(AppRoute.MatchConsole) },
                            onOpenRoster = { navigator.navigate(AppRoute.Roster) },
                            onOpenMatchSetup = { navigator.navigate(AppRoute.MatchSetup) },
                            onOpenDrawingPad = { navigator.navigate(AppRoute.DrawingPad) },
                            onOpenTacticBoard = { navigator.navigate(AppRoute.TacticBoard()) },
                        )
                    }

                    if (AppVariant.showRating) {
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
                                    navigator.navigate(AppRoute.Phases(day, month, year, route.showFilter))
                                }
                            }

                            PhasesScreen(
                                showFilter = route.showFilter,
                                presenter = presenter,
                                onAction = { action ->
                                    scope.launch {
                                        action()
                                        presenter.selectedPhaseId?.let { phaseId ->
                                            navigator.navigate(
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
                                onNavigateHome = {
                                    navigator.navigate(AppRoute.Home)
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
                                                navigator.navigate(
                                                    AppRoute.Vote(
                                                        phaseId = phaseId,
                                                        matchId = matchId,
                                                        day = presenter.filterDay,
                                                        month = presenter.filterMonth,
                                                        year = presenter.filterYear,
                                                    ),
                                                )
                                            phaseId != null ->
                                                navigator.navigate(
                                                    AppRoute.PhaseDetail(
                                                        phaseId = phaseId,
                                                        day = presenter.filterDay,
                                                        month = presenter.filterMonth,
                                                        year = presenter.filterYear,
                                                    ),
                                                )
                                            else ->
                                                navigator.navigate(
                                                    AppRoute.Phases(
                                                        day = presenter.filterDay,
                                                        month = presenter.filterMonth,
                                                        year = presenter.filterYear,
                                                    ),
                                                )
                                        }
                                    }
                                },
                                onBackToPhases = {
                                    presenter.cancelPendingVote()
                                    navigator.navigate(
                                        AppRoute.Phases(
                                            day = presenter.filterDay,
                                            month = presenter.filterMonth,
                                            year = presenter.filterYear,
                                        ),
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
                                    navigator.navigate(
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

                            PhaseDetailScreen(
                                presenter = presenter,
                                onBack = {
                                    presenter.selectedMatch = null
                                    navigator.navigate(
                                        AppRoute.Phases(
                                            day = presenter.filterDay,
                                            month = presenter.filterMonth,
                                            year = presenter.filterYear,
                                        ),
                                    )
                                },
                                onMatchSelected = { match ->
                                    if (presenter.token != null) {
                                        navigator.navigate(
                                            AppRoute.Vote(
                                                phaseId = phaseId,
                                                matchId = match.id,
                                                day = presenter.filterDay,
                                                month = presenter.filterMonth,
                                                year = presenter.filterYear,
                                            ),
                                        )
                                    }
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
                                    navigator.navigate(
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
                                        navigator.navigate(
                                            AppRoute.PhaseDetail(
                                                phaseId = phaseRoute,
                                                day = presenter.filterDay,
                                                month = presenter.filterMonth,
                                                year = presenter.filterYear,
                                            ),
                                        )
                                    } else {
                                        navigator.navigate(
                                            AppRoute.Phases(
                                                day = presenter.filterDay,
                                                month = presenter.filterMonth,
                                                year = presenter.filterYear,
                                            ),
                                        )
                                    }
                                },
                            )
                        }
                    }

                    composable<AppRoute.RefereeCoaching> {
                        val sessionActive = stopwatchPresenter.isRunning ||
                            stopwatchPresenter.elapsedMillis > 0L ||
                            scoreboardPresenter.homeScore > 0 ||
                            scoreboardPresenter.guestScore > 0 ||
                            coachingHistoryPresenter.entries.isNotEmpty() ||
                            coachingPresenter.adjustedCriteriaCount > 0 ||
                            rosterPresenter.homePlayers.isNotEmpty() ||
                            rosterPresenter.guestPlayers.isNotEmpty()
                        CoachingSetupScreen(
                            matchSetup = matchSetupPresenter,
                            roster = rosterPresenter,
                            isSessionActive = sessionActive,
                            username = presenter.username,
                            password = presenter.password,
                            token = presenter.token,
                            role = presenter.role,
                            onUsernameChange = { presenter.username = it },
                            onPasswordChange = { presenter.password = it },
                            onLogin = {
                                scope.launch {
                                    presenter.login()
                                }
                            },
                            onLogout = { presenter.logout() },
                            onResetAll = {
                                coachingPresenter.reset()
                                coachingHistoryPresenter.clear()
                                stopwatchPresenter.reset()
                                scoreboardPresenter.reset()
                                rosterPresenter.reset()
                                matchSetupPresenter.reset()
                                sessionManager.clear()
                            },
                            onContinue = {
                                val id = coachingSync.evaluationId
                                navigator.navigate(AppRoute.CoachingSession(evaluationId = id))
                            },
                            onOpenList = {
                                navigator.navigate(AppRoute.CoachingList())
                            },
                            onOpenSettings = {
                                navigator.navigate(AppRoute.Settings)
                            },
                            onNavigateHome = {
                                navigator.navigate(AppRoute.Home)
                            },
                        )
                    }

                    composable<AppRoute.CoachingSession> { entry ->
                        val route = entry.toRoute<AppRoute.CoachingSession>()
                        // Seed the sync with the evaluation id from the deep link, if any.
                        LaunchedEffect(route.evaluationId) {
                            route.evaluationId?.let { coachingSync.evaluationId = it }
                        }
                        CoachingSessionScreen(
                            coaching = coachingPresenter,
                            sync = coachingSync,
                            stopwatch = stopwatchPresenter,
                            scoreboard = scoreboardPresenter,
                            history = coachingHistoryPresenter,
                            roster = rosterPresenter,
                            matchSetup = matchSetupPresenter,
                            username = presenter.username,
                            isLoggedIn = !presenter.token.isNullOrBlank(),
                            onOpenSettings = { navigator.navigate(AppRoute.Settings) },
                            onLogout = { presenter.logout() },
                            onNavigateHome = {
                                navigator.navigate(AppRoute.Home)
                            },
                        )
                    }

                    composable<AppRoute.CoachingList> { entry ->
                        val route = entry.toRoute<AppRoute.CoachingList>()
                        val token = presenter.token
                        if (token.isNullOrBlank()) {
                            // Not signed in: bounce to coaching setup.
                            LaunchedEffect(Unit) {
                                navigator.navigate(AppRoute.RefereeCoaching)
                            }
                        } else {
                            CoachingListScreen(
                                token = token,
                                username = presenter.username,
                                role = presenter.role,
                                initialTab = route.tab,
                                onOpenEvaluation = { evaluationId ->
                                    navigator.navigate(AppRoute.CoachingReport(evaluationId = evaluationId))
                                },
                                onContinueEvaluation = { evaluationId ->
                                    scope.launch {
                                        try {
                                            val report = CoachingApiClient().getReport(
                                                AppConfig.baseApiUrl,
                                                presenter.token!!,
                                                evaluationId,
                                            )
                                            sessionManager.clear()
                                            restoreCoachingSessionFromReport(
                                                report = report,
                                                coaching = coachingPresenter,
                                                matchSetup = matchSetupPresenter,
                                                history = coachingHistoryPresenter,
                                                scoreboard = scoreboardPresenter,
                                                stopwatch = stopwatchPresenter,
                                                roster = rosterPresenter,
                                            )
                                            coachingSync.evaluationId = evaluationId
                                            navigator.navigate(AppRoute.CoachingSession(evaluationId = evaluationId))
                                        } catch (e: Throwable) {
                                            // Fail open: navigate to live session anyway.
                                            coachingSync.evaluationId = evaluationId
                                            navigator.navigate(AppRoute.CoachingSession(evaluationId = evaluationId))
                                        }
                                    }
                                },
                                onOpenSettings = { navigator.navigate(AppRoute.Settings) },
                                onLogout = { presenter.logout() },
                                onNavigateHome = { navigator.navigate(AppRoute.Home) },
                            )
                        }
                    }

                    composable<AppRoute.CoachingReport> { entry ->
                        val route = entry.toRoute<AppRoute.CoachingReport>()
                        val token = presenter.token
                        if (token.isNullOrBlank()) {
                            LaunchedEffect(Unit) {
                                navigator.navigate(AppRoute.RefereeCoaching)
                            }
                        } else {
                            CoachingEvaluationReadOnlyScreen(
                                token = token,
                                username = presenter.username,
                                evaluationId = route.evaluationId,
                                onOpenSettings = { navigator.navigate(AppRoute.Settings) },
                                onLogout = { presenter.logout() },
                                onNavigateHome = { navigator.navigate(AppRoute.Home) },
                            )
                        }
                    }

                    composable<AppRoute.Admin> {
                        val token = presenter.token
                        if (token.isNullOrBlank()) {
                            LaunchedEffect(Unit) {
                                navigator.navigate(AppRoute.RefereeCoaching)
                            }
                        } else {
                            AdminScreen(
                                token = token,
                                username = presenter.username,
                                role = presenter.role,
                                onOpenSettings = { navigator.navigate(AppRoute.Settings) },
                                onLogout = { presenter.logout() },
                                onLogin = { navigator.navigate(AppRoute.RefereeCoaching) },
                                onNavigateHome = { navigator.navigate(AppRoute.Home) },
                            )
                        }
                    }

                    composable<AppRoute.Settings> {
                        SettingsScreen(
                            username = presenter.username,
                            role = presenter.role,
                            isLoggedIn = !presenter.token.isNullOrBlank(),
                            onOpenChangePassword = {
                                navigator.navigate(AppRoute.ChangePassword)
                            },
                            onOpenAdmin = {
                                navigator.navigate(AppRoute.Admin)
                            },
                            onLogout = { presenter.logout() },
                            onLogin = { navigator.navigate(AppRoute.RefereeCoaching) },
                            onNavigateHome = { navigator.navigate(AppRoute.Home) },
                        )
                    }

                    composable<AppRoute.ChangePassword> {
                        val token = presenter.token
                        if (token.isNullOrBlank()) {
                            LaunchedEffect(Unit) {
                                navigator.navigate(AppRoute.RefereeCoaching)
                            }
                        } else {
                            ChangePasswordScreen(
                                token = token,
                                username = presenter.username,
                                onPasswordChanged = {
                                    navigator.navigate(AppRoute.Settings)
                                },
                                onOpenSettings = { navigator.navigate(AppRoute.Settings) },
                                onLogout = { presenter.logout() },
                                onNavigateHome = { navigator.navigate(AppRoute.Home) },
                            )
                        }
                    }

                    if (AppVariant.showMatchConsole) {
                        composable<AppRoute.MatchConsole> {
                            MatchConsoleScreen(
                                stopwatch = stopwatchPresenter,
                                scoreboard = scoreboardPresenter,
                                onNavigateHome = {
                                    navigator.navigate(AppRoute.Home)
                                },
                            )
                        }
                    }

                    if (AppVariant.showCoachingSheet) {
                        composable<AppRoute.CoachingSheet> {
                            CoachingSheetScreen(
                                coaching = coachingPresenter,
                                onNavigateHome = {
                                    navigator.navigate(AppRoute.Home)
                                },
                            )
                        }
                    }

                    if (AppVariant.showRoster) {
                        composable<AppRoute.Roster> {
                            RosterScreen(
                                roster = rosterPresenter,
                                onNavigateHome = {
                                    navigator.navigate(AppRoute.Home)
                                },
                            )
                        }
                    }

                    if (AppVariant.showMatchSetup) {
                        composable<AppRoute.MatchSetup> {
                            MatchSetupScreen(
                                setup = matchSetupPresenter,
                                onNavigateHome = {
                                    navigator.navigate(AppRoute.Home)
                                },
                            )
                        }
                    }

                    if (AppVariant.showDrawingPad) {
                        composable<AppRoute.DrawingPad> {
                            DrawingPadScreen(
                                presenter = drawingPadPresenter,
                                onNavigateHome = { navigator.navigate(AppRoute.Home) },
                            )
                        }
                    }

                    if (AppVariant.showTacticBoard) {
                        composable<AppRoute.TacticBoard> { entry ->
                            val route = entry.toRoute<AppRoute.TacticBoard>()
                            TacticBoardScreen(
                                presenter = tacticBoardPresenter,
                                debug = route.debug,
                                onNavigateHome = { navigator.navigate(AppRoute.Home) },
                            )
                        }
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

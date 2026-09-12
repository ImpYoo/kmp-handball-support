package de.exhumedo.kmp.handball_support.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import de.exhumedo.kmp.handball_support.vote.VoteAppPresenter

/**
 * Thin abstraction over [NavHostController] for this app. Centralizes
 * browser-history binding, deep-link application, and single-top navigation so
 * screens don't wire URL plumbing themselves.
 */
class AppNavigator(
    private val navController: NavHostController,
    private val presenter: VoteAppPresenter,
) {
    /** Navigate to [route], replacing the top entry. */
    fun navigate(route: AppRoute) {
        navController.navigate(route = route) { launchSingleTop = true }
    }

    /** Navigate to a path-based route (e.g. `phases?day=…`). */
    fun navigate(path: String) {
        navController.navigate(route = path) { launchSingleTop = true }
    }

    /** Pop to [route]'s entry if it exists; returns false otherwise. */
    fun popTo(route: AppRoute): Boolean =
        navController.popBackStack(route = route, inclusive = false)

    /** Apply the launch URL deep link once. */
    suspend fun applyInitialDeepLink() {
        val raw = initialDeepLink().ifBlank { return }
        val parsed = parseDeepLink(raw)
        presenter.setDateFilter(day = parsed.day, month = parsed.month, year = parsed.year)
        parsed.route.let { navigate(it) }
    }

    /** Drive the NavController from a browser back/forward event. */
    fun handleBrowserBack(deepLink: String) {
        val parsed = parseDeepLink(deepLink)
        presenter.setDateFilter(day = parsed.day, month = parsed.month, year = parsed.year)
        val target = parsed.route as Any
        if (!navController.popBackStack(route = target, inclusive = false)) {
            navController.navigate(route = target) { launchSingleTop = true }
        }
    }
}

/** Wire the navigator into the Compose frame: browser history + back/forward. */
@Composable
fun rememberAppNavigator(
    navController: NavHostController,
    presenter: VoteAppPresenter,
): AppNavigator {
    val navigator = androidx.compose.runtime.remember(navController) {
        AppNavigator(navController, presenter)
    }

    LaunchedEffect(navController) {
        withFrameNanos { }
        navigator.applyInitialDeepLink()
    }

    LaunchedEffect(navController) {
        bindBrowserNavigation(navController) { entry ->
            when (val route = entry.destination.route) {
                null -> ""
                else -> when {
                    route.startsWith("home") -> ""
                    route.startsWith("phases?") -> {
                        val phases = entry.toRoute<AppRoute.Phases>()
                        phasesPath(phases.day, phases.month, phases.year, phases.showFilter)
                    }
                    route.startsWith("login") -> {
                        entry.toRoute<AppRoute.Login>().toPath()
                    }
                    route.startsWith("phase/") -> {
                        entry.toRoute<AppRoute.PhaseDetail>().toPath()
                    }
                    route.startsWith("vote/") -> {
                        entry.toRoute<AppRoute.Vote>().toPath()
                    }
                    route.startsWith("coaching-list") -> entry.toRoute<AppRoute.CoachingList>().toPath()
                    route.startsWith("admin") -> "admin"
                    route.startsWith("settings") -> "settings"
                    route.startsWith("change-password") -> "change-password"
                    route.startsWith("coaching") -> "coaching"
                    route.startsWith("session") -> {
                        val session = entry.toRoute<AppRoute.CoachingSession>()
                        session.toPath()
                    }
                    route.startsWith("coaching-report") -> {
                        entry.toRoute<AppRoute.CoachingReport>().toPath()
                    }
                    route.startsWith("sheet") -> "sheet"
                    route.startsWith("clock") -> "clock"
                    route.startsWith("roster") -> "roster"
                    route.startsWith("setup") -> "setup"
                    route.startsWith("drawing") -> "drawing"
                    route.startsWith("tactic") -> "tactic"
                    else -> ""
                }
            }
        }
    }

    LaunchedEffect(navController) {
        bindBrowserBackHandler { deepLink ->
            navigator.handleBrowserBack(deepLink)
        }
    }

    return navigator
}

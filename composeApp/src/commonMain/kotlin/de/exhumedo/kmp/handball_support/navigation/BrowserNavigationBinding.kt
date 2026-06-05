package de.exhumedo.kmp.handball_support.navigation

import androidx.navigation.NavController
import androidx.navigation.NavBackStackEntry

/**
 * Binds a [NavController] to browser history on web targets. No-op elsewhere.
 */
expect suspend fun bindBrowserNavigation(
	navController: NavController,
	routeForBackStackEntry: ((NavBackStackEntry) -> String)? = null,
)

/**
 * Registers a handler invoked on browser back/forward (popstate) with the
 * current location as a deep-link string (e.g. "phases/7045?day=1"). Lets the
 * app drive the [NavController] to match the URL when the platform binding's
 * own reverse sync does not. No-op on non-web targets.
 */
expect fun bindBrowserBackHandler(onBrowserBack: (String) -> Unit)


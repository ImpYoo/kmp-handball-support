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


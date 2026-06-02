package de.exhumedo.kmp.handball_support.navigation

import androidx.navigation.NavController
import androidx.navigation.NavBackStackEntry

actual suspend fun bindBrowserNavigation(
    navController: NavController,
    routeForBackStackEntry: ((NavBackStackEntry) -> String)?,
) {
    // No browser history on Android.
}


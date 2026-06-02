package de.exhumedo.kmp.handball_support.navigation

import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.bindToBrowserNavigation

@OptIn(ExperimentalBrowserHistoryApi::class)
actual suspend fun bindBrowserNavigation(
    navController: NavController,
    routeForBackStackEntry: ((NavBackStackEntry) -> String)?,
) {
    navController.bindToBrowserNavigation(getBackStackEntryRoute = routeForBackStackEntry)
}


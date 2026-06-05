@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.exhumedo.kmp.handball_support.navigation

import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.bindToBrowserNavigation
import kotlinx.browser.window

@OptIn(ExperimentalBrowserHistoryApi::class)
actual suspend fun bindBrowserNavigation(
    navController: NavController,
    routeForBackStackEntry: ((NavBackStackEntry) -> String)?,
) {
    navController.bindToBrowserNavigation(getBackStackEntryRoute = routeForBackStackEntry)
}

actual fun bindBrowserBackHandler(onBrowserBack: (String) -> Unit) {
    window.addEventListener("popstate") { _ ->
        onBrowserBack(currentLocationDeepLink())
    }
}

/** Reads the current browser location as a deep-link string (no leading slash). */
private fun currentLocationDeepLink(): String {
    val path = window.location.pathname.trim('/')
    val search = window.location.search.removePrefix("?")
    return when {
        path.isEmpty() && search.isEmpty() -> ""
        search.isEmpty() -> path
        path.isEmpty() -> "?$search"
        else -> "$path?$search"
    }
}


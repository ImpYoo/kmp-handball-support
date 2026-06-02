@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.exhumedo.kmp.handball_support.navigation

import kotlinx.browser.window

private var capturedDeepLink: String? = null

/**
 * Captures the launch URL and rewrites history to `/`. Must run before
 * `bindToBrowserNavigation`, which snapshots the current pathname as its base path; otherwise
 * a legacy deep-link such as `/phases/19696` is prepended to every pushed route.
 */
fun captureLaunchDeepLink() {
    if (capturedDeepLink != null) return

    val hash = window.location.hash.trimStart('#').trim()
    val path = window.location.pathname.trim('/')
    val search = window.location.search.removePrefix("?")

    capturedDeepLink = when {
        hash.isNotEmpty() -> hash
        path.isEmpty() && search.isEmpty() -> ""
        search.isEmpty() -> path
        path.isEmpty() -> "?$search"
        else -> "$path?$search"
    }

    if (path.isNotEmpty() || search.isNotEmpty() || hash.isNotEmpty()) {
        window.history.replaceState(null, "", "/")
    }
}

actual fun initialDeepLink(): String {
    capturedDeepLink?.let { return it }

    val hash = window.location.hash.trimStart('#').trim()
    if (hash.isNotEmpty()) return hash

    val path = window.location.pathname.trim('/')
    val search = window.location.search.removePrefix("?")
    return when {
        path.isEmpty() && search.isEmpty() -> ""
        search.isEmpty() -> path
        path.isEmpty() -> "?$search"
        else -> "$path?$search"
    }
}

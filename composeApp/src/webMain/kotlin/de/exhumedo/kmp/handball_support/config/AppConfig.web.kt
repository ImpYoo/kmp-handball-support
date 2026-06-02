package de.exhumedo.kmp.handball_support.config

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLMetaElement

internal actual fun defaultBaseApiUrl(): String {
    metaTagApiBaseUrl()?.let { return it }

    // On localhost the dev web server (e.g. :8081) and the API (:8080) live on different ports,
    // so falling through to the common `http://localhost:8080` default is correct. In production
    // (and any other host) prefer the page origin.
    val hostname = runCatching { window.location.hostname }.getOrDefault("")
    if (hostname == "localhost" || hostname == "127.0.0.1") return ""

    return runCatching { window.location.origin }.getOrDefault("")
}

private fun metaTagApiBaseUrl(): String? {
    val element = document.querySelector("meta[name='api-base-url']") as? HTMLMetaElement
    return element?.content?.takeIf { it.isNotBlank() }
}


